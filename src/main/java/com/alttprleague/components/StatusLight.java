package com.alttprleague.components;

import javax.swing.*;
import java.awt.*;
import java.awt.geom.Ellipse2D;
import java.util.ArrayList;
import java.util.function.Consumer;

public class StatusLight extends JLabel {

    private final ArrayList<Consumer<Status>> statusChange = new ArrayList<>();
    private Status status;

    public StatusLight() {
        setStatus(Status.Disconnected);
    }

    public void onStatusChange(Consumer<Status> callback) {
        statusChange.add(callback);
    }

    public void setStatus(Status status) {
        this.status = status;
        statusChange.forEach(statusConsumer -> statusConsumer.accept(status));
        Color statusColor = switch (status) {
            case Pending -> Color.YELLOW;
            case Connected -> Color.GREEN;
            case Disconnected -> Color.RED;
        };
        int size = 16;
        this.setIcon(new Icon() {
            @Override
            public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g;
                Ellipse2D.Double circle = new Ellipse2D.Double(0, 0, size, size);
                g2.setColor(statusColor);
                g2.fill(circle);
            }

            @Override
            public int getIconWidth() {
                return size;
            }

            @Override
            public int getIconHeight() {
                return size;
            }
        });
    }

    public Status getStatus() {
        return status;
    }

    public enum Status {
        Connected, Pending, Disconnected
    }
}
