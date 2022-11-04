package com.alttprleague.networkObjects.racetime;

public class Status {

    public Value value;
    public String verbose_value;
    public String help_text;

    public enum Value {
        open,invitational,pending,in_progress,finished,cancelled
    }
}
