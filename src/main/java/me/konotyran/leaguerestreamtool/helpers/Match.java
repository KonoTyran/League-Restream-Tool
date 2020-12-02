/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package me.konotyran.leaguerestreamtool.helpers;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 *
 * @author HAklo
 */
@JsonIgnoreProperties(ignoreUnknown=true)
public class Match {
    
    public int id;
    public String note;
    public String title;
    public Player[] players;
    
}
