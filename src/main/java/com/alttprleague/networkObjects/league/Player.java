package com.alttprleague.networkObjects.league;

public class Player {
    public int id;
    public String logo_url = "";
    public String name = "";
    public String sprite_url = "";
    public String streaming_from = "";
    public Team team =  new Team();
    public String tracker = "";
    public String twitch_name = "";
    public Crop crop = new Crop();
}
