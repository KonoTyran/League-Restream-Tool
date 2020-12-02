/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.konotyran.leaguerestreamtool.helpers;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 *
 * @author HAklo
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class Event {
       
    @JsonProperty("match1")
    public Match match;
    
    public Commentator[] commentators;
    
    @JsonProperty("when")
    public String time;
    
    public Channel[] channels;
    
    public Tracker[] trackers;
    
    public String getTitle() {
        return match.title;
    }
    
}
