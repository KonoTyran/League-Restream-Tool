package com.alttprleague;

import com.alttprleague.components.Console;
import com.alttprleague.components.StatusLight;
import com.alttprleague.networkObjects.league.*;
import com.alttprleague.networkObjects.racetime.Room;
import com.formdev.flatlaf.FlatDarkLaf;
import com.google.gson.Gson;
import net.miginfocom.swing.MigLayout;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.slf4j.Logger;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.ZonedDateTime;
import java.util.Timer;
import java.util.*;
import java.util.stream.Collectors;

import static org.slf4j.LoggerFactory.getLogger;

public class LeagueRestreamTool {

    private static final Logger log = getLogger(LeagueRestreamTool.class.getName());
    private Process TLProcess;
    private Process TRProcess;
    private Process BLProcess;
    private Process BRProcess;

    private final int TOP_LEFT_PORT = 27770;
    private final int TOP_RIGHT_PORT = 27771;
    private final int BOTTOM_LEFT_PORT = 27772;
    private final int BOTTOM_RIGHT_PORT = 27773;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    public final Settings settings = new Settings();

    private final String baseDir = "output\\";
    private Timer raceTimeCheckTimer = new Timer();

    private ZonedDateTime matchStart;
    private Channel channel;
    private final HashMap<Integer, Crop> newCrops = new HashMap<>();

    static private String twoPlayerHTML;
    static private String fourPlayerHTML;
    static private String twoPreRaceHTML;
    static private String fourPreRaceHTML;
    static private String postRaceHTML;

    static private final String scheduleHTML = "<div class=\"card\"> <table class=\"info\"> <thead> <tr> <th>Channel</th> <th class=\"expand\">Teams</th> <th>Time (Eastern)</th> <th>Crew</th> </tr></thead> <tbody id=\"schedule\"></tbody> </table> </div>";
    static private final String standingsHTML = "<div class=\"card\"> <table class=\"info\"> <thead> <tr> <th class=\"expand\" colspan=\"3\" id=\"division_name\">Division</th> </tr></thead> <tbody id=\"standings\"> </tbody> </table> </div>";
    private final OBSClient obsRelay;

    public static JFrame window;

    public static void main(String[] args) throws IOException {
        var restreamTool = new LeagueRestreamTool();
        window = new JFrame("League Restream Tool V3.1");
        URL iconURL = LeagueRestreamTool.class.getResource("/LeagueLogo.png");
        window.setIconImage(ImageIO.read(iconURL));
        window.setContentPane(restreamTool.pMain);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setMinimumSize(new Dimension(900,400));
        window.pack();
        window.setVisible(true);

        try(var in = LeagueRestreamTool.class.getResourceAsStream("/html/TwoPlayer.html")) {
            twoPlayerHTML = new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining("\n"));
        }
        try(var in = LeagueRestreamTool.class.getResourceAsStream("/html/FourPlayer.html")) {
            fourPlayerHTML = new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining("\n"));
        }
        try(var in = LeagueRestreamTool.class.getResourceAsStream("/html/TwoPreRace.html")) {
            twoPreRaceHTML = new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining("\n"));
        }
        try(var in = LeagueRestreamTool.class.getResourceAsStream("/html/FourPreRace.html")) {
            fourPreRaceHTML = new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining("\n"));
        }
        try(var in = LeagueRestreamTool.class.getResourceAsStream("/html/PostRace.html")) {
            postRaceHTML = new BufferedReader(new InputStreamReader(in)).lines().collect(Collectors.joining("\n"));
        }



        window.addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                restreamTool.closeAllStreams();
            }
        });

        Runtime.getRuntime().addShutdownHook(new Thread(restreamTool::closeAllStreams));
    }

    public LeagueRestreamTool() {
        FlatDarkLaf.setup();
        initComponents();
        obsRelay = new OBSClient(obsStatus, this);
        new File(baseDir).mkdirs();
        setAuthKey(settings.getAuthKey());

        obsRelay.toggleConnect();
        obsStatus.onStatusChange(this::obsStatusChange);
    }

    private void obsStatusChange(StatusLight.Status status) {
        switch (status) {
            case Pending -> {
                btnOBSConnect.setEnabled(false);
                btnOBSConnect.setText("Connecting");
            }
            case Connected -> {
                btnOBSConnect.setText("Disconnect");
                btnOBSConnect.setEnabled(true);
                btnStreamKey.setText("Send Key to OBS");
            }

            case Disconnected -> {
                btnOBSConnect.setText("Connect");
                btnOBSConnect.setEnabled(true);
                btnStreamKey.setText("Copy Key to Clipboard");
            }
        }
    }

    public void setAuthKey(String restreamerCode) {
        if (restreamerCode == null) return;
        if (restreamerCode.isBlank()) {
            lStatus.setText("No restreamer code entered.");
            btnLeague1.setEnabled(false);
            btnLeague2.setEnabled(false);
            btnLeague3.setEnabled(false);
            btnLeague4.setEnabled(false);
        } else {
            lStatus.setText("");
            if(obsStatus.getStatus() == StatusLight.Status.Connected)
                btnOBSSaveCrop.setEnabled(true);
            btnLeague1.setEnabled(true);
            btnLeague2.setEnabled(true);
            btnLeague3.setEnabled(true);
            btnLeague4.setEnabled(true);
            settings.setAuthKey(restreamerCode);
        }
    }

    public void closeAllStreams() {
        if(TLProcess != null)
            TLProcess.destroyForcibly();

        if(TRProcess != null)
            TRProcess.destroyForcibly();

        if(BLProcess != null)
            BLProcess.destroyForcibly();
        if(BRProcess != null)
            BRProcess.destroyForcibly();
        consoleBottomLeft.clear(true);
        consoleBottomRight.clear(true);
        consoleTopLeft.clear(true);
        consoleTopRight.clear(true);
    }
    public void startStreamLink(Screen screen, String streamFrom) {
        switch (screen) {
            case TOP_LEFT -> {
                if(TLProcess != null)
                    TLProcess.destroy();
                consoleTopLeft.clear();
                TLProcess = startLink(streamFrom,TOP_LEFT_PORT, consoleTopLeft);
            }
            case TOP_RIGHT -> {
                if(TRProcess != null)
                    TRProcess.destroy();
                consoleTopRight.clear();
                TRProcess = startLink(streamFrom,TOP_RIGHT_PORT, consoleTopRight);
            }
            case BOTTOM_LEFT -> {
                if(BLProcess != null)
                    BLProcess.destroy();
                consoleBottomLeft.clear();
                BLProcess = startLink(streamFrom,BOTTOM_LEFT_PORT, consoleBottomLeft);
            }
            case BOTTOM_RIGHT -> {
                if(BRProcess != null)
                    BRProcess.destroy();
                consoleBottomRight.clear();
                BRProcess = startLink(streamFrom,BOTTOM_RIGHT_PORT, consoleBottomRight);
            }
        }
    }

    private Process startLink(String streamFrom, int port, Console console) {
        return startLink(streamFrom,port, console, false);
    }

    private Process startLink(String streamFrom, int port, Console console, boolean external) {
        var args = new ArrayList<>(Arrays.asList("streamlink.exe",
                streamFrom,
                "720p,480p,best",
                "--ringbuffer-size", "32M",
                "--player-external-http",
                "--player-external-http-port", Integer.toString(port),
                "--twitch-disable-ads",
                "--twitch-disable-reruns",
                "--retry-streams", "5"));
        var extArgs = Arrays.asList("cmd.exe", "/c", "start");
        if (external) {
            args.addAll(0, extArgs);
        }
        try {
            ProcessBuilder builder = new ProcessBuilder();
            builder.command(args);
            builder.redirectErrorStream(true);
            //builder.inheritIO();
            Process newProcess = builder.start();
            console.read(newProcess.getInputStream());
            return newProcess;
        } catch (IOException e) {
            if (!external) {
                console.appendError("Error Starting streamlink for " + streamFrom +", starting streamlink externally.");
                console.appendError("reason: " + e.getLocalizedMessage());
                startLink(streamFrom,port,console,true);
            } else {
                console.appendError("Error Starting streamlink for " + streamFrom +" externally.");
                console.appendError("reason: " + e.getLocalizedMessage());
                console.appendError("args: " + String.join(", ", args));
                console.appendError("StackTrace: ");
                for (StackTraceElement stackTraceElement : e.getStackTrace()) {
                    console.appendError(stackTraceElement.toString());
                }
            }
        }
        return null;
    }

    private void fetchChannel(String channelName) {
        try {
            var request = HttpRequest.newBuilder()
                    .uri(URI.create("https://alttprleague.com/api/restream/?channel="+ channelName))
                    .header("Authorization", "Bearer " + settings.getAuthKey())
                    .build();

            httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenAccept(this::ProcessWebsiteResponse);
        }
        catch (Exception e) {
            consoleTopLeft.appendError("Error while fetching restream data from League website.");
            consoleTopLeft.appendError("Reason: " + e.getLocalizedMessage());
        }
    }

    private void ProcessWebsiteResponse(String json) {
        try {
            var gson = new Gson();
            var channel = gson.fromJson(json, Channel.class);
            if(!channel.error.isBlank()) {
                consoleTopLeft.appendError(channel.error);
                return;
            }
            if(channel.episode == null) {
                consoleTopLeft.appendError("No episode loaded on Restreamer Dashboard for "+channel.twitch_name+".");
                return;
            }

            this.channel = channel;
            btnStreamKey.setEnabled(true);
            obsRelay.setStreamKey(channel.stream_key);

            Document raceTemplate;
            Document preRaceTemplate;
            Document postRaceTemplate = Jsoup.parse(postRaceHTML);
            if(channel.episode.players.length == 4) {
                raceTemplate = Jsoup.parse(fourPlayerHTML);
                preRaceTemplate = Jsoup.parse(fourPreRaceHTML);
            } else {
                raceTemplate = Jsoup.parse(twoPlayerHTML);
                preRaceTemplate = Jsoup.parse(twoPreRaceHTML);
            }

            // LEFT TEAM
            raceTemplate.getElementById("t1_logo").attr("src",channel.episode.players[0].team.logo_url);
            raceTemplate.getElementById("t1_name").text(channel.episode.players[0].team.name + " ("+ channel.episode.players[0].team.points +")");
            preRaceTemplate.getElementById("t1_logo").attr("src",channel.episode.players[0].team.logo_url);
            preRaceTemplate.getElementById("t1_name").text(channel.episode.players[0].team.name + " ("+ channel.episode.players[0].team.points +")");
            if(channel.episode.is_playoff) {
                raceTemplate.getElementById("t1_wins").removeClass("hidden");
                raceTemplate.getElementById("t1_wins").attr("data-wins", String.valueOf(channel.episode.players[0].team.stage_wins));
                raceTemplate.getElementById("t1_name").text(channel.episode.players[0].team.name);
                preRaceTemplate.getElementById("t1_name").text(channel.episode.players[0].team.name + " ("+ channel.episode.players[0].team.stage_wins +")");
            }

            // LEFT TEAM PLAYER 1
            startStreamLink(Screen.TOP_LEFT,channel.episode.players[0].streaming_from);
            preRaceTemplate.getElementById("p1_logo").attr("src",channel.episode.players[0].logo_url);
            //preRaceTemplate.getElementById("p1_sprite").attr("src",channel.episode.players[0].sprite_url);
            preRaceTemplate.getElementById("p1_name").text(channel.episode.players[0].name);
            raceTemplate.getElementById("p1_name").text(channel.episode.players[0].name);
            raceTemplate.getElementById("p1_tracker").attr("src", channel.episode.players[0].tracker);
            obsRelay.setCrop(1,channel.episode.players[0].crop != null ? channel.episode.players[0].crop : new Crop());


            // LEFT TEAM PLAYER 2
            if(channel.episode.players.length >= 3) {
                startStreamLink(Screen.BOTTOM_LEFT,channel.episode.players[2].streaming_from);
                preRaceTemplate.getElementById("p3_logo").attr("src",channel.episode.players[2].logo_url);
                //preRaceTemplate.getElementById("p3_sprite").attr("src",channel.episode.players[2].sprite_url);
                preRaceTemplate.getElementById("p3_name").text(channel.episode.players[2].name);
                raceTemplate.getElementById("p3_name").text(channel.episode.players[2].name);
                raceTemplate.getElementById("p3_tracker").attr("src", channel.episode.players[2].tracker);
                obsRelay.setCrop(3,channel.episode.players[2].crop != null ? channel.episode.players[2].crop : new Crop());
            }

            // RIGHT TEAM
            raceTemplate.getElementById("t2_logo").attr("src",channel.episode.players[1].team.logo_url);
            raceTemplate.getElementById("t2_name").text(channel.episode.players[1].team.name + " ("+ channel.episode.players[1].team.points +")");
            preRaceTemplate.getElementById("t2_logo").attr("src",channel.episode.players[1].team.logo_url);
            preRaceTemplate.getElementById("t2_name").text(channel.episode.players[1].team.name + " ("+ channel.episode.players[1].team.points +")");
            if(channel.episode.is_playoff) {
                raceTemplate.getElementById("t2_wins").removeClass("hidden");
                raceTemplate.getElementById("t2_wins").attr("data-wins", String.valueOf(channel.episode.players[1].team.stage_wins));
                raceTemplate.getElementById("t2_name").text(channel.episode.players[1].team.name);
                preRaceTemplate.getElementById("t2_name").text(channel.episode.players[1].team.name + " ("+ channel.episode.players[1].team.stage_wins +")");
            }


            // RIGHT TEAM PLAYER 1
            startStreamLink(Screen.TOP_RIGHT,channel.episode.players[1].streaming_from);
            preRaceTemplate.getElementById("p2_logo").attr("src",channel.episode.players[1].logo_url);
            //preRaceTemplate.getElementById("p2_sprite").attr("src",channel.episode.players[1].sprite_url);
            preRaceTemplate.getElementById("p2_name").text(channel.episode.players[1].name);
            raceTemplate.getElementById("p2_name").text(channel.episode.players[1].name);
            raceTemplate.getElementById("p2_tracker").attr("src", channel.episode.players[1].tracker);
            obsRelay.setCrop(2,channel.episode.players[1].crop != null ? channel.episode.players[1].crop : new Crop());

            // RIGHT TEAM PLAYER 2
            if(channel.episode.players.length >= 4) {
                startStreamLink(Screen.BOTTOM_RIGHT,channel.episode.players[3].streaming_from);
                preRaceTemplate.getElementById("p4_logo").attr("src",channel.episode.players[3].logo_url);
                //preRaceTemplate.getElementById("p4_sprite").attr("src",channel.episode.players[3].sprite_url);
                preRaceTemplate.getElementById("p4_name").text(channel.episode.players[3].name);
                raceTemplate.getElementById("p4_name").text(channel.episode.players[3].name);
                raceTemplate.getElementById("p4_tracker").attr("src", channel.episode.players[3].tracker);
                obsRelay.setCrop(4,channel.episode.players[3].crop != null ? channel.episode.players[3].crop : new Crop());
            }

            // Stage & MODE
            preRaceTemplate.getElementById("stage").text(channel.episode.stage);
            preRaceTemplate.getElementById("mode").text(channel.episode.mode);
            preRaceTemplate.getElementById("open").text(channel.episode.season.open ? "Open":"Invitational");
            if(channel.episode.is_playoff && (channel.episode.background.toLowerCase().contains("power") || channel.episode.background.toLowerCase().contains("game4"))) {
                raceTemplate.getElementById("mode").removeClass("hidden");
                raceTemplate.getElementById("mode").text(channel.episode.mode);
            }


            // BACKGROUND
            raceTemplate.getElementById("layout").attr("src", channel.episode.background);

            // COMMENTATORS
            preRaceTemplate.getElementById("commentators").text(String.join(", ", channel.episode.comms));
            raceTemplate.getElementById("commentators").text(String.join(", ", channel.episode.comms));

            // TRACKERS
            preRaceTemplate.getElementById("trackers").text(String.join(", ", channel.episode.trackers));
            raceTemplate.getElementById("trackers").text(String.join(", ", channel.episode.trackers));

            // RESTREAMER
            preRaceTemplate.getElementById("restreamer").text(String.join(", ", channel.episode.restreamer));


            // POST RACE
            postRaceTemplate.getElementById("channel").text(channel.twitch_name);

            var cardContainer = postRaceTemplate.getElementById("card_container");
            cardContainer.children().remove();

            Element scheduleCard = Jsoup.parseBodyFragment(scheduleHTML).body().child(0);
            var schedule = scheduleCard.getElementById("schedule");
            schedule.children().remove();

            for (Match match : channel.schedule) {
                var row = new Element("tr");
                var channels = new Element("td").text(String.join("<br \\>", match.channels));
                var matchUp = new Element("td").text(match.matchup).addClass("wrap");
                var when = new Element("td").text(match.when);
                var crew = new Element("td");
                crew.appendChild(new Element("span").text(Integer.toString(match.comms.length)).addClass("comm"));
                crew.appendChild(new TextNode(" "));
                crew.appendChild(new Element("span").text(Integer.toString(match.trackers.length)).addClass("tracker"));
                row.appendChild(channels).appendChild(matchUp).appendChild(when).appendChild(crew);
                schedule.appendChild(row);
            }
            cardContainer.appendChild(scheduleCard);

            for (Division division : channel.divisions) {
                ArrayList<Team> teams = new ArrayList<>(Arrays.asList(division.teams));
                teams.sort(Comparator.comparingInt(team -> team.points));
                Collections.reverse(teams);

                Element standingsCard = Jsoup.parseBodyFragment(standingsHTML).body().child(0);
                var standings = standingsCard.getElementById("standings");
                standings.children().remove();

                standingsCard.getElementById("division_name").text(division.name);

                for (Team team : teams) {
                    var row = new Element("tr");
                    var teamName = new Element("td").text(team.name).addClass("wrap");
                    var teamPoints = new Element("td").text(Integer.toString(team.points));
                    var teamStanding = new Element("td").text(team.standings);
                    row.appendChild(teamName).appendChild(teamPoints).appendChild(teamStanding);
                    standings.appendChild(row);
                }
                cardContainer.appendChild(standingsCard);
            }

            cardContainer.child(0).addClass("active");

            FileWriter raceOut = new FileWriter(new File(baseDir,"RaceLayout.html"));
            raceOut.write(raceTemplate.outerHtml());
            raceOut.close();

            FileWriter preRaceOut = new FileWriter(new File(baseDir,"PreRaceLayout.html"));
            preRaceOut.write(preRaceTemplate.outerHtml());
            preRaceOut.close();

            FileWriter postRaceOut = new FileWriter(new File(baseDir,"PostRaceLayout.html"));
            postRaceOut.write(postRaceTemplate.outerHtml());
            postRaceOut.close();

            matchStart = null;

            if(!channel.episode.racetime_room.isBlank())
                fetchRaceTimeData(channel.episode.racetime_room + "/data");

            FileWriter playlist = new FileWriter(new File(baseDir, "playlist.m3u"));
            playlist.write(String.join("\n", channel.episode.playlist));
            playlist.close();


        } catch (IOException e) {
            e.printStackTrace();
            consoleTopLeft.appendError("Error Writing Files");
        } catch (Exception e) {
            e.printStackTrace();
            consoleTopLeft.appendError("Error Creating Files");
        }

    }

    private void fetchRaceTimeData(String roomURL) {
        raceTimeCheckTimer.cancel();
        raceTimeCheckTimer = new Timer();
        try {
            var raceTimeRoom = HttpRequest.newBuilder(new URI(roomURL)).build();

            httpClient.sendAsync(raceTimeRoom, HttpResponse.BodyHandlers.ofString())
                    .thenApply(HttpResponse::body)
                    .thenAccept(json -> {
                        var room = new Gson().fromJson(json, Room.class);
                        if (room.started_at != null) {
                            matchStart = ZonedDateTime.parse(room.started_at).plusMinutes(10);
                            try {
                                var doc = Jsoup.parse(Files.readString(new File(baseDir, "PreRaceLayout.html").toPath()));
                                var script = doc.getElementById("timer_script");
                                script.text("let countDownDate = new Date("+( matchStart.toEpochSecond() * 1000)+")");
                                FileWriter timeOut = new FileWriter(new File(baseDir, "PreRaceLayout.html"));
                                timeOut.write(doc.outerHtml());
                                timeOut.close();
                            } catch (IOException e) {
                                consoleTopLeft.appendError("Error updating layout with race start time.");
                            }
                        } else {
                            raceTimeCheckTimer.schedule(new TimerTask() {
                                @Override
                                public void run() {
                                    fetchRaceTimeData(roomURL);
                                }
                            },5000);
                        }
                    });
        } catch (URISyntaxException e) {
            consoleTopLeft.appendError("invalid RaceTime url");
        }
    }

    private void miExit(ActionEvent e) {
        System.exit(0);
    }

    private void miAuthKey(ActionEvent e) {
        setAuthKey(JOptionPane.showInputDialog(pMain, "Enter your restreamer code from the League restreamer dashboard.","Restreamer Code",JOptionPane.PLAIN_MESSAGE));
    }

    private void btnLeague1(ActionEvent e) {
        fetchChannel("TheALTTPRLeague");
    }

    private void btnLeague2(ActionEvent e) {
        fetchChannel("TheALTTPRLeague2");
    }

    private void btnLeague3(ActionEvent e) {
        fetchChannel("TheALTTPRLeague3");
    }

    private void btnLeague4(ActionEvent e) {
        fetchChannel("TheALTTPRLeague4");
    }

    private void btnCopy(ActionEvent e) {
        if(obsRelay.isConnected()) {
            obsRelay.setStreamKey(channel.stream_key);
        } else {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(channel.stream_key), null);
        }
    }

    private void lObsStatusMouseClicked(MouseEvent e) {
        obsRelay.toggleConnect();
    }

    private void btnOBSConnect(ActionEvent e) {
        obsRelay.toggleConnect();
    }

    private void btnOBSSaveCrop(ActionEvent event) {

        newCrops.forEach((playerId, crop) -> {
            try {
                String json = new Gson().toJson(crop);
                var request = HttpRequest.newBuilder()
                        .uri(URI.create("https://alttprleague.com/api/crop/?id="+playerId))
                        .header("Content-Type","application/json")
                        .header("Authorization", "Bearer " + settings.getAuthKey())
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build();
                httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenAccept((response) -> {
                    if (response.statusCode() != 200) {
                        log.error("error sending crop info for {}\nReason: ({}) {}",playerId,response.statusCode(),response.body());
                    };
                });
            }
            catch (Exception e) {
                consoleTopLeft.appendError("Error sending crop info to leauge website.");
                consoleTopLeft.appendError("Reason: " + e.getLocalizedMessage());
            }
        });

        btnOBSSaveCrop.setEnabled(false);
        btnOBSSaveCrop.setText("Crops Sent");
        newCrops.clear();
    }

    public void setCrop(boolean timer, Integer playerID, Integer cropTop, Integer cropLeft, Integer cropRight, Integer cropBottom) {
        if(channel == null) return;
        if(cropTop + cropLeft + cropRight + cropBottom == 0) return;
        btnOBSSaveCrop.setEnabled(true);
        btnOBSSaveCrop.setText("Save Crops");
        int leaugeID = channel.episode.players[playerID-1].id;
        var crop = newCrops.getOrDefault(leaugeID, new Crop());
        if(!newCrops.containsKey(leaugeID)) newCrops.put(leaugeID, crop);

        if(timer) {
            crop.timer_top = cropTop;
            crop.timer_left = cropLeft;
            crop.timer_right = cropRight;
            crop.timer_bottom = cropBottom;
        } else {
            crop.game_top = cropTop;
            crop.game_left = cropLeft;
            crop.game_right = cropRight;
            crop.game_bottom = cropBottom;
        }
    }

    public void logError(String error) {
        log.error(error);
        consoleTopLeft.appendError(error);
    }

    private void miOBSSettings(ActionEvent e) {
        var panel = new obsConnectPanel(window, settings);
        panel.setVisible(true);
    }

    private enum Screen {
        TOP_LEFT,TOP_RIGHT,BOTTOM_LEFT,BOTTOM_RIGHT
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        pMain = new JPanel();
        mbMain = new JMenuBar();
        mFile = new JMenu();
        miExit = new JMenuItem();
        mEdit = new JMenu();
        miAuthKey = new JMenuItem();
        miOBSSettings = new JMenuItem();
        pBody = new JPanel();
        btnLeague1 = new JButton();
        btnLeague2 = new JButton();
        btnLeague3 = new JButton();
        btnLeague4 = new JButton();
        lStatus = new JLabel();
        btnStreamKey = new JButton();
        pConsoleContainer = new JPanel();
        spTopLeft = new JScrollPane();
        consoleTopLeft = new Console();
        spTopRight = new JScrollPane();
        consoleTopRight = new Console();
        spBottomLeft = new JScrollPane();
        consoleBottomLeft = new Console();
        spBottomRight = new JScrollPane();
        consoleBottomRight = new Console();
        pUtil = new JPanel();
        obsStatus = new StatusLight();
        btnOBSConnect = new JButton();
        btnOBSSaveCrop = new JButton();

        //======== pMain ========
        {
            pMain.setMinimumSize(null);
            pMain.setPreferredSize(new Dimension(900, 400));
            pMain.setLayout(new MigLayout(
                "insets 0,novisualpadding,hidemode 3,gap 5 5",
                // columns
                "[876,grow,fill]",
                // rows
                "[fill]" +
                "[415,grow,fill]"));

            //======== mbMain ========
            {

                //======== mFile ========
                {
                    mFile.setText("File");
                    mFile.setDisplayedMnemonicIndex(0);

                    //---- miExit ----
                    miExit.setText("Exit");
                    miExit.setDisplayedMnemonicIndex(1);
                    miExit.addActionListener(e -> miExit(e));
                    mFile.add(miExit);
                }
                mbMain.add(mFile);

                //======== mEdit ========
                {
                    mEdit.setText("Settings");
                    mEdit.setDisplayedMnemonicIndex(0);

                    //---- miAuthKey ----
                    miAuthKey.setText("Restreamer Auth Key");
                    miAuthKey.addActionListener(e -> miAuthKey(e));
                    mEdit.add(miAuthKey);

                    //---- miOBSSettings ----
                    miOBSSettings.setText("OBS Conneciton Settings");
                    miOBSSettings.addActionListener(e -> miOBSSettings(e));
                    mEdit.add(miOBSSettings);
                }
                mbMain.add(mEdit);
            }
            pMain.add(mbMain, "cell 0 0");

            //======== pBody ========
            {
                pBody.setLayout(new MigLayout(
                    "insets panel,novisualpadding,hidemode 3",
                    // columns
                    "[fill]" +
                    "[fill]" +
                    "[fill]" +
                    "[fill]" +
                    "[grow,fill]" +
                    "[fill]",
                    // rows
                    "[]" +
                    "[grow,fill]" +
                    "[]"));

                //---- btnLeague1 ----
                btnLeague1.setText("theALTTPRLeague");
                btnLeague1.setEnabled(false);
                btnLeague1.addActionListener(e -> btnLeague1(e));
                pBody.add(btnLeague1, "cell 0 0");

                //---- btnLeague2 ----
                btnLeague2.setText("theALTTPRLeague2");
                btnLeague2.setEnabled(false);
                btnLeague2.addActionListener(e -> btnLeague2(e));
                pBody.add(btnLeague2, "cell 1 0");

                //---- btnLeague3 ----
                btnLeague3.setText("theALTTPRLeague3");
                btnLeague3.setEnabled(false);
                btnLeague3.addActionListener(e -> btnLeague3(e));
                pBody.add(btnLeague3, "cell 2 0");

                //---- btnLeague4 ----
                btnLeague4.setText("theALTTPRLeague4");
                btnLeague4.setEnabled(false);
                btnLeague4.addActionListener(e -> btnLeague4(e));
                pBody.add(btnLeague4, "cell 3 0");

                //---- lStatus ----
                lStatus.setHorizontalAlignment(SwingConstants.CENTER);
                pBody.add(lStatus, "cell 4 0");

                //---- btnStreamKey ----
                btnStreamKey.setText("Copy Stream Key");
                btnStreamKey.setEnabled(false);
                btnStreamKey.addActionListener(e -> btnCopy(e));
                pBody.add(btnStreamKey, "cell 5 0");

                //======== pConsoleContainer ========
                {
                    pConsoleContainer.setLayout(new MigLayout(
                        "insets 0,hidemode 3,gap 03 3",
                        // columns
                        "[grow,sizegroup 1,fill]" +
                        "[grow,sizegroup 1,fill]",
                        // rows
                        "[grow,sizegroup 1,fill]" +
                        "[grow,sizegroup 1,fill]"));

                    //======== spTopLeft ========
                    {
                        spTopLeft.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
                        spTopLeft.setViewportView(consoleTopLeft);
                    }
                    pConsoleContainer.add(spTopLeft, "cell 0 0");

                    //======== spTopRight ========
                    {
                        spTopRight.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
                        spTopRight.setViewportView(consoleTopRight);
                    }
                    pConsoleContainer.add(spTopRight, "cell 1 0");

                    //======== spBottomLeft ========
                    {
                        spBottomLeft.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
                        spBottomLeft.setViewportView(consoleBottomLeft);
                    }
                    pConsoleContainer.add(spBottomLeft, "cell 0 1");

                    //======== spBottomRight ========
                    {
                        spBottomRight.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
                        spBottomRight.setViewportView(consoleBottomRight);
                    }
                    pConsoleContainer.add(spBottomRight, "cell 1 1");
                }
                pBody.add(pConsoleContainer, "cell 0 1 6 1");

                //======== pUtil ========
                {
                    pUtil.setLayout(new MigLayout(
                        "insets 0,hidemode 3",
                        // columns
                        "[fill]" +
                        "[grow,fill]",
                        // rows
                        "[]"));

                    //---- obsStatus ----
                    obsStatus.setToolTipText("OBS Connection");
                    obsStatus.addMouseListener(new MouseAdapter() {
                        @Override
                        public void mouseClicked(MouseEvent e) {
                            lObsStatusMouseClicked(e);
                        }
                    });
                    pUtil.add(obsStatus, "cell 0 0");

                    //---- btnOBSConnect ----
                    btnOBSConnect.setText("Connect to OBS");
                    btnOBSConnect.addActionListener(e -> btnOBSConnect(e));
                    pUtil.add(btnOBSConnect, "cell 0 0");

                    //---- btnOBSSaveCrop ----
                    btnOBSSaveCrop.setText("Save Crops");
                    btnOBSSaveCrop.setEnabled(false);
                    btnOBSSaveCrop.addActionListener(e -> btnOBSSaveCrop(e));
                    pUtil.add(btnOBSSaveCrop, "cell 0 0");
                }
                pBody.add(pUtil, "cell 0 2 6 1");
            }
            pMain.add(pBody, "cell 0 1");
        }
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JPanel pMain;
    private JMenuBar mbMain;
    private JMenu mFile;
    private JMenuItem miExit;
    private JMenu mEdit;
    private JMenuItem miAuthKey;
    private JMenuItem miOBSSettings;
    private JPanel pBody;
    private JButton btnLeague1;
    private JButton btnLeague2;
    private JButton btnLeague3;
    private JButton btnLeague4;
    private JLabel lStatus;
    private JButton btnStreamKey;
    private JPanel pConsoleContainer;
    private JScrollPane spTopLeft;
    private Console consoleTopLeft;
    private JScrollPane spTopRight;
    private Console consoleTopRight;
    private JScrollPane spBottomLeft;
    private Console consoleBottomLeft;
    private JScrollPane spBottomRight;
    private Console consoleBottomRight;
    private JPanel pUtil;
    private StatusLight obsStatus;
    private JButton btnOBSConnect;
    private JButton btnOBSSaveCrop;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
