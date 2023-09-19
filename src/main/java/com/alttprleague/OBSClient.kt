package com.alttprleague

import com.alttprleague.components.StatusLight
import com.alttprleague.networkObjects.league.Crop
import com.google.gson.JsonObject
import io.obswebsocket.community.client.OBSRemoteController
import io.obswebsocket.community.client.WebSocketCloseCode
import io.obswebsocket.community.client.listener.lifecycle.ReasonThrowable
import io.obswebsocket.community.client.message.event.sceneitems.SceneItemTransformChangedEvent
import io.obswebsocket.community.client.message.request.inputs.PressInputPropertiesButtonRequest
import io.obswebsocket.community.client.message.request.ui.GetStudioModeEnabledRequest
import io.obswebsocket.community.client.message.response.RequestResponse
import io.obswebsocket.community.client.message.response.config.SetStreamServiceSettingsResponse
import io.obswebsocket.community.client.message.response.inputs.PressInputPropertiesButtonResponse
import io.obswebsocket.community.client.message.response.sceneitems.GetGroupSceneItemListResponse
import io.obswebsocket.community.client.message.response.sceneitems.GetSceneItemListResponse
import io.obswebsocket.community.client.message.response.sceneitems.GetSceneItemTransformResponse
import io.obswebsocket.community.client.message.response.sceneitems.SetSceneItemTransformResponse
import io.obswebsocket.community.client.message.response.scenes.GetGroupListResponse
import io.obswebsocket.community.client.message.response.scenes.GetSceneListResponse
import io.obswebsocket.community.client.message.response.ui.GetStudioModeEnabledResponse
import org.slf4j.LoggerFactory
import java.util.*


class OBSClient(
    private val obsStatus: StatusLight,
    private val leagueRestreamTool: LeagueRestreamTool,
) {
    private var obsRemoteController: OBSRemoteController
    var isConnected = false
        private set
    private val cropIDs = HashMap<Int, HashMap<String, HashMap<String, Int>>>()

    init {
        obsRemoteController = buildOBSConnection()
    }

    private fun buildOBSConnection(): OBSRemoteController {
        return OBSRemoteController.builder()
            .host(leagueRestreamTool.settings.obsServer)
            .port(leagueRestreamTool.settings.obsPort)
            .password(leagueRestreamTool.settings.obsPassword)
            .connectionTimeout(3)
            .lifecycle()
            .onReady { onReady() }
            .onCommunicatorError { reasonThrowable: ReasonThrowable -> onError(reasonThrowable) }
            .onDisconnect { onDisconnect() }
            .onClose { webSocketCloseCode: WebSocketCloseCode -> onClose(webSocketCloseCode) }
            .withControllerDefaultLogging(false)
            .and()
            .registerEventListener(SceneItemTransformChangedEvent::class.java) { event: SceneItemTransformChangedEvent ->
                transformChanged(
                    event
                )
            }
            .build()
    }

    private fun onClose(webSocketCloseCode: WebSocketCloseCode) {
        if (webSocketCloseCode.code == -1) return
        leagueRestreamTool.logError("connection to OBS closed, reason: (${webSocketCloseCode.code}) ${webSocketCloseCode.name}")
    }

    private fun transformChanged(event: SceneItemTransformChangedEvent) {
        for ((playerID,sources) in cropIDs) {
            for ((source,scenes) in sources) {
                for ((scene,sourceID) in scenes) {
                    if (sourceID == event.sceneItemId.toInt() && scene == event.sceneName) {
                        log.info("playerID $playerID, Source $source, sourceID $sourceID")
                        val transform = event.sceneItemTransform
                        if (source.lowercase(Locale.getDefault())
                                .contains("alt")
                        ) continue
                        if (source.lowercase(Locale.getDefault()).contains("game")) leagueRestreamTool.setCrop(
                            false,
                            playerID,
                            transform.cropTop,
                            transform.cropLeft,
                            transform.cropRight,
                            transform.cropBottom
                        )
                        if (source.lowercase(Locale.getDefault()).contains("timer")) leagueRestreamTool.setCrop(
                            true,
                            playerID,
                            transform.cropTop,
                            transform.cropLeft,
                            transform.cropRight,
                            transform.cropBottom
                        )
                    }
                }
            }
        }
    }

    private fun onDisconnect() {
        isConnected = false
        obsStatus.status = StatusLight.Status.Disconnected
    }

    fun toggleConnect() {
        obsStatus.status = StatusLight.Status.Pending
        if (isConnected) {
            obsRemoteController.disconnect()
            return
        }
        obsRemoteController = buildOBSConnection()
        obsRemoteController.connect()
    }

    private fun onError(reasonThrowable: ReasonThrowable) {
        isConnected = false
        obsStatus.status = StatusLight.Status.Disconnected
        leagueRestreamTool.logError("Failed to connect to OBS, reason: ${reasonThrowable.reason}")
    }

    private fun onReady() {
        isConnected = true
        obsStatus.status = StatusLight.Status.Connected
        fetchStreams()
    }

    private fun fetchStreams() {
        if (!isConnected) return
        cropIDs[1] = object : HashMap<String, HashMap<String, Int>>() {
            init {
                put("03 LeftGame", HashMap())
                put("LeftTimer", HashMap())
            }
        }
        cropIDs[2] = object : HashMap<String, HashMap<String, Int>>() {
            init {
                put("04 RightGame", HashMap())
                put("RightTimer", HashMap())
            }
        }
        cropIDs[3] = object : HashMap<String, HashMap<String, Int>>() {
            init {
                put("05 BLGame", HashMap())
                put("BLTimer", HashMap())
            }
        }
        cropIDs[4] = object : HashMap<String, HashMap<String, Int>>() {
            init {
                put("06 BRGame", HashMap())
                put("BRTimer", HashMap())
            }
        }


        // Get Scene List
        obsRemoteController.getSceneList { sceneListResponse: GetSceneListResponse ->
            if (!sceneListResponse.isSuccessful) {
                //consoleTopLeft.appendError(sceneListResponse.getMessageData().getRequestStatus().getComment());
                return@getSceneList
            }
            for (scene in sceneListResponse.scenes) {
                obsRemoteController.getSceneItemList(scene.sceneName) { itemList: GetSceneItemListResponse ->
                    if (!itemList.isSuccessful) return@getSceneItemList
                    for (sceneItem in itemList.sceneItems) {
                        cropIDs.forEach { (key: Int, value: HashMap<String, HashMap<String, Int>>) ->
                            if (value.containsKey(sceneItem.sourceName)) {
                                value[sceneItem.sourceName]!![scene.sceneName] = sceneItem.sceneItemId
                                log.info("Found ${sceneItem.sourceName} (${sceneItem.sceneItemId}) in scene ${scene.sceneName}")
                            }
                        }
                    }
                }
            }
        }
        obsRemoteController.getGroupList { groupList: GetGroupListResponse ->
            if (!groupList.isSuccessful) return@getGroupList
            for (group in groupList.groups) {
                obsRemoteController.getGroupSceneItemList(group) { itemList: GetGroupSceneItemListResponse ->
                    if (!itemList.isSuccessful) return@getGroupSceneItemList
                    for (sceneItem in itemList.sceneItems) {
                        cropIDs.forEach { (key: Int?, value: HashMap<String, HashMap<String, Int>>) ->
                            if (value.containsKey(sceneItem.sourceName)) {
                                value[sceneItem.sourceName]!![group] = sceneItem.sceneItemId
                                log.info("Found ${sceneItem.sourceName} (${sceneItem.sceneItemId}) in group $group")
                            }
                        }
                    }
                }
            }
        }
    }

    fun setCrop(id: Int, crop: Crop) {
        if (!isConnected) return
        val sources = cropIDs[id]!!
        sources.forEach { (source: String, containers: HashMap<String, Int>) ->
            if (source.lowercase(Locale.getDefault()).contains("game")) pushCrop(
                containers,
                crop.game_top,
                crop.game_left,
                crop.game_right,
                crop.game_bottom
            ) else pushCrop(
                containers,
                crop.timer_top,
                crop.timer_left,
                crop.timer_right,
                crop.timer_bottom
            )
        }
    }

    private fun pushCrop(containers: HashMap<String, Int>, top: Int, left: Int, right: Int, bottom: Int) {
        if (!isConnected) return
        for (container in containers.keys) {
            val sourceID = containers[container]!!
            obsRemoteController.getSceneItemTransform(container, sourceID) { tResponse: GetSceneItemTransformResponse ->
                if (!tResponse.isSuccessful) {
                    log.warn("error fetching transform for supposedly known good Container $container / sourceID $sourceID")
                    return@getSceneItemTransform
                }
                val transform = tResponse.sceneItemTransform
                transform.cropBottom = bottom
                transform.cropTop = top
                transform.cropLeft = left
                transform.cropRight = right
                transform.scaleX = 0f
                transform.scaleY = 0f
                obsRemoteController.setSceneItemTransform(
                    container,
                    sourceID,
                    transform
                ) { setTransform: SetSceneItemTransformResponse ->
                    if (!setTransform.isSuccessful) {
                        log.warn("error setting transform in $container. Reason: ${setTransform.messageData.requestStatus.comment}")
                    }
                }
            }
        }
    }

    fun setStreamKey(key: String?) {
        if (!isConnected) return
        val settings: JsonObject = JsonObject()
        settings.addProperty("bwtest", false)
        settings.addProperty("key", key)
        settings.addProperty("server", "auto")
        settings.addProperty("service", "Twitch")
        obsRemoteController.setStreamServiceSettings(
            "rtmp_common",
            settings
        ) { response: SetStreamServiceSettingsResponse ->
            if (!response.isSuccessful) {
                log.error("error setting stream key.")
            }
        }
    }

    fun reloadLayout() {
        if (!isConnected) return
        obsRemoteController.sendRequest(PressInputPropertiesButtonRequest.builder().inputName("PreRaceLayout").propertyName("refreshnocache").build()) { _: PressInputPropertiesButtonResponse -> return@sendRequest }
        obsRemoteController.sendRequest(PressInputPropertiesButtonRequest.builder().inputName("WebLayout").propertyName("refreshnocache").build()) { _: PressInputPropertiesButtonResponse -> return@sendRequest }
        obsRemoteController.sendRequest(PressInputPropertiesButtonRequest.builder().inputName("PostRaceLayout").propertyName("refreshnocache").build()) { _: PressInputPropertiesButtonResponse -> return@sendRequest }
    }

    companion object {
        private val log = LoggerFactory.getLogger(OBSClient::class.java.name)
    }
}