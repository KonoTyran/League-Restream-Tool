package com.alttprleague.networkObjects.league;

public class Crop {
    public int game_top;
    public int game_left;
    public int game_right;
    public int game_bottom;
    public int timer_top;
    public int timer_left;
    public int timer_right;
    public int timer_bottom;

    public Crop() {
        this.game_top = 0;
        this.game_left = 0;
        this.game_right = 0;
        this.game_bottom = 0;
        this.timer_top = 0;
        this.timer_left = 0;
        this.timer_right = 0;
        this.timer_bottom = 0;
    }

    public Crop(int game_top, int game_left, int game_right, int game_bottom, int timer_top, int timer_left,int timer_right,int timer_bottom){
        this.game_top = game_top;
        this.game_left = game_left;
        this.game_right = game_right;
        this.game_bottom = game_bottom;
        this.timer_top = timer_top;
        this.timer_left = timer_left;
        this.timer_right = timer_right;
        this.timer_bottom = timer_bottom;
    }

}
