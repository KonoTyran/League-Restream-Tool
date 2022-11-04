package com.alttprleague;

import com.alttprleague.components.StatusLight;
import com.alttprleague.networkObjects.league.Crop;
import com.google.gson.JsonObject;
import io.obswebsocket.community.client.OBSRemoteController;
import io.obswebsocket.community.client.WebSocketCloseCode;
import io.obswebsocket.community.client.listener.lifecycle.ReasonThrowable;
import io.obswebsocket.community.client.message.event.sceneitems.SceneItemTransformChangedEvent;
import io.obswebsocket.community.client.model.Scene;
import io.obswebsocket.community.client.model.SceneItem;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Objects;

import static org.slf4j.LoggerFactory.getLogger;


public class OBSClient {

    private static final Logger log = getLogger(OBSClient.class.getName());
    private OBSRemoteController obsRemoteController;
    private final Settings settings;
    private final StatusLight obsStatus;
    private boolean connected = false;
    private final LeagueRestreamTool leagueRestreamTool;
    private final HashMap<Integer, HashMap<String, HashMap<String, Integer>>> cropIDs = new HashMap<>();

    public OBSClient(StatusLight statusLabel, LeagueRestreamTool leagueRestreamTool, Settings settings) {
        this.settings = settings;
        this.obsStatus = statusLabel;
        this.leagueRestreamTool = leagueRestreamTool;
        this.obsRemoteController = buildOBSConnection();

    }

    private OBSRemoteController buildOBSConnection() {
        return OBSRemoteController.builder()
                .host(settings.getObsServer())
                .port(settings.getObsPort())
                .password(settings.getObsPassword())
                .connectionTimeout(3)
                .lifecycle()
                .onReady(this::onReady)
                .onCommunicatorError(this::onError)
                .onDisconnect(this::onDisconnect)
                .onClose(this::onClose)
                .withControllerDefaultLogging(false)
                .and()
                .registerEventListener(SceneItemTransformChangedEvent.class, this::TransformChanged)
                .build();
    }

    private void onClose(WebSocketCloseCode webSocketCloseCode) {
        if(webSocketCloseCode.getCode() == -1) return;
        leagueRestreamTool.logError("connection to OBS closed, reason: ("+webSocketCloseCode.getCode()+") " + webSocketCloseCode.name());
    }

    private void TransformChanged(SceneItemTransformChangedEvent event) {
        cropIDs.forEach((playerID,sources) -> sources.forEach((source, scenes) -> scenes.forEach((scene, sourceID) -> {
            if(sourceID == event.getSceneItemId().intValue() && Objects.equals(scene, event.getSceneName())) {
                log.info("playerID {}, Source {}, sourceID {}",playerID,source,sourceID);
                var transform = event.getSceneItemTransform();
                if(source.toLowerCase().contains("alt")) return; //dont save alt crops.
                if(source.toLowerCase().contains("game"))
                   leagueRestreamTool.setCrop(false,playerID,transform.getCropTop(),transform.getCropLeft(),transform.getCropRight(),transform.getCropBottom());
                if(source.toLowerCase().contains("timer"))
                   leagueRestreamTool.setCrop(true,playerID,transform.getCropTop(),transform.getCropLeft(),transform.getCropRight(),transform.getCropBottom());
            }
        })));

    }

    private void onDisconnect() {
        connected = false;
        obsStatus.setStatus(StatusLight.Status.Disconnected);
    }

    public void toggleConnect() {
        obsStatus.setStatus(StatusLight.Status.Pending);
        if(isConnected()) {
            obsRemoteController.disconnect();
            return;
        }
        obsRemoteController = buildOBSConnection();
        obsRemoteController.connect();
    }

    private void onError(ReasonThrowable reasonThrowable) {
        connected = false;
        obsStatus.setStatus(StatusLight.Status.Disconnected);
        leagueRestreamTool.logError("Failed to connect to OBS, reason: "+reasonThrowable.getReason());
    }

    private void onReady() {
        connected = true;
        obsStatus.setStatus(StatusLight.Status.Connected);
        fetchStreams();
    }

    private void fetchStreams() {
        if (!isConnected()) return;
        cropIDs.put(1,new HashMap<>() {{
            put("03 LeftGame", new HashMap<>());
            put("LeftTimer", new HashMap<>());
        }});
        cropIDs.put(2,new HashMap<>() {{
            put("04 RightGame", new HashMap<>());
            put("RightTimer", new HashMap<>());
        }});
        cropIDs.put(3,new HashMap<>() {{
            put("05 BLGame", new HashMap<>());
            put("BLTimer", new HashMap<>());
        }});
        cropIDs.put(4,new HashMap<>() {{
            put("06 BRGame", new HashMap<>());
            put("BRTimer", new HashMap<>());
        }});




        // Get Scene List
        obsRemoteController.getSceneList(sceneListResponse -> {
            if (!sceneListResponse.isSuccessful()) {
                //consoleTopLeft.appendError(sceneListResponse.getMessageData().getRequestStatus().getComment());
                return;
            }

            for (Scene scene : sceneListResponse.getScenes()) {
                obsRemoteController.getSceneItemList(scene.getSceneName(),itemList -> {
                    if(!itemList.isSuccessful())
                        return;

                    for (SceneItem sceneItem : itemList.getSceneItems()) {
                        cropIDs.forEach((key,value) -> {
                            if(value.containsKey(sceneItem.getSourceName())) {
                                value.get(sceneItem.getSourceName()).put(scene.getSceneName() ,sceneItem.getSceneItemId());
                                log.info("Found {} ({}) in scene {}",sceneItem.getSourceName(),sceneItem.getSceneItemId(),scene.getSceneName());
                            }
                        });
                    }
                });
            }
        });

        obsRemoteController.getGroupList(groupList -> {
            if(!groupList.isSuccessful()) return;

            for (var group : groupList.getGroups()) {
                obsRemoteController.getGroupSceneItemList(group,itemList -> {
                    if(!itemList.isSuccessful())
                        return;

                    for (SceneItem sceneItem : itemList.getSceneItems()) {
                        cropIDs.forEach((key,value) -> {
                            if (value.containsKey(sceneItem.getSourceName())) {
                                value.get(sceneItem.getSourceName()).put(group, sceneItem.getSceneItemId());
                                log.info("Found {} ({}) in group {}", sceneItem.getSourceName(), sceneItem.getSceneItemId(), group);
                            }
                        });
                    }
                });
            }
        });
    }

    public void setCrop(int id, Crop crop) {
        if(!isConnected()) return;
        var sources = cropIDs.get(id);

        sources.forEach((source, containers) -> {
            if(source.toLowerCase().contains("game"))
                pushCrop(containers,crop.game_top,crop.game_left,crop.game_right,crop.game_bottom);
            else
                pushCrop(containers,crop.timer_top,crop.timer_left,crop.timer_right,crop.timer_bottom);
        });
    }

    private void pushCrop(HashMap<String, Integer> containers, int top, int left, int right, int bottom) {
        for (String container : containers.keySet()) {
            int sourceID = containers.get(container);
            obsRemoteController.getSceneItemTransform(container, sourceID, tResponse-> {
                if(!tResponse.isSuccessful()) {
                    log.warn("error fetching transform for supposedly known good Container {} / sourceID {}",container,sourceID);
                    return;
                }
                var transform = tResponse.getSceneItemTransform();
                transform.setCropBottom(bottom);
                transform.setCropTop(top);
                transform.setCropLeft(left);
                transform.setCropRight(right);
                transform.setScaleX(0F);
                transform.setScaleY(0F);
                obsRemoteController.setSceneItemTransform(container, sourceID, transform, setTransform -> {
                    if(!setTransform.isSuccessful()) {
                        log.warn("error setting transform in {}. Reason: {}", container,setTransform.getMessageData().getRequestStatus().getComment());
                    }
                });
            });
        }
    }

    public void setStreamKey(String key) {
        if (!isConnected()) return;

        var settings = new JsonObject();
        settings.addProperty("bwtest", false);
        settings.addProperty("key", key);
        settings.addProperty("server", "auto");
        settings.addProperty("service", "Twitch");
        obsRemoteController.setStreamServiceSettings("rtmp_common", settings, response -> {
            if(!response.isSuccessful()) {
                log.error("error setting stream key.");
            }
        });
    }

    public boolean isConnected() {
        return connected;
    }
}
