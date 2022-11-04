package com.alttprleague.components;

import javax.swing.*;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Scanner;

public class Console extends JTextPane {

    ArrayList<String> messages = new ArrayList<>();

    private Thread updateThread;
    public Console() {
        this.setEditable(false);
        this.setContentType("text/html");
    }


    public void append(String text) {
        messages.add(text);
        while (messages.size() > 50) {
            messages.remove(0);
        }
        writeText();
    }
    public void appendError(String text) {
        append("<font color='#ff0033'>"+text+"</font>");
        writeText();
    }

    public void clear() {
        messages.clear();
        writeText();
    }

    public void clear(boolean stopUpdates) {
        if (updateThread != null && stopUpdates )
            updateThread.interrupt();
        clear();

    }

    private void writeText() {
        StringBuilder msg = new StringBuilder();
        boolean first = true;
        for (String message : messages) {
            if(first)
                first = false;
            else
                msg.append("<br>");

            msg.append(message);
        }
        this.setText(msg.toString());
    }

    public void read(InputStream inputStream) {
        var scanner = new Scanner(inputStream);
        if(updateThread != null)
            updateThread.interrupt();
        updateThread = new Thread(() -> {
            while(!Thread.interrupted()) {
                if(scanner.hasNext()) {
                    append(scanner.nextLine());
                }
            }
        });
        updateThread.start();
    }

}
