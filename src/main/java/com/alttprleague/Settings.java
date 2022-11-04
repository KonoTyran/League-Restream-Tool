package com.alttprleague;

import java.util.prefs.Preferences;

public class Settings {
    private String authKey;
    private String obsServer;
    private String obsPassword;
    private int obsPort;
    private final Preferences root;

    private final String OBS_PASSWORD = "obs password";
    private final String OBS_PORT = "obs port";
    private final String OBS_SERVER = "obs server";
    private final String RESTREAMER_CODE = "restreamer code";

    public Settings(Preferences root) {
        this.root = root;
        obsPassword = root.get(OBS_PASSWORD, "");
        obsServer = root.get(OBS_SERVER, "localhost");
        obsPort = root.getInt(OBS_PORT, 8456);
        authKey = root.get(RESTREAMER_CODE, "");
    }

    public String getAuthKey() {
        return authKey;
    }

    public void setAuthKey(String authKey) {
        this.authKey = authKey;
        root.put(RESTREAMER_CODE, authKey);
    }

    public String getObsServer() {
        return obsServer;
    }

    public void setObsServer(String obsServer) {
        this.obsServer = obsServer;
        root.put(OBS_SERVER, obsServer);
    }

    public String getObsPassword() {
        return obsPassword;
    }

    public void setObsPassword(String obsPassword) {
        this.obsPassword = obsPassword;
        root.put(OBS_PASSWORD, obsPassword);
    }

    public int getObsPort() {
        return obsPort;
    }

    public void setObsPort(int obsPort) {
        this.obsPort = obsPort;
        root.putInt(OBS_PORT, obsPort);
    }
}
