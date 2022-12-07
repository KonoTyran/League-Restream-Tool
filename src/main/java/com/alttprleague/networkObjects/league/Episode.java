package com.alttprleague.networkObjects.league;

public class Episode {
    public String background = "";
    public String restreamer = "";
    public String[] trackers;
    public String[] comms;
    public Player[] players;
    public String racetime_room = "";
    public String stage = "";
    public String mode = "";
    public Season season = new Season();
    public String[] playlist = new String[]{};
    public boolean is_playoff = false;
}
