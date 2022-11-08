/*
 * Created by JFormDesigner on Thu Nov 03 13:50:48 PDT 2022
 */

package com.alttprleague;

import net.miginfocom.swing.MigLayout;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

/**
 * @author unknown
 */
public class obsConnectPanel extends JDialog {
    private final Settings settings;
    public obsConnectPanel(Window owner, Settings settings) {
        super(owner);
        initComponents();
        this.settings = settings;
        tfServer.setText(settings.getObsServer());
        tfPort.setText(String.valueOf(settings.getObsPort()));
        pfPassword.setText(settings.getObsPassword());
    }

    private void ok(ActionEvent e) {
        this.setVisible(false);
        applySettings();
    }

    private void applySettings() {
        settings.setObsServer(tfServer.getText());
        try {
            settings.setObsPort(Integer.parseInt(tfPort.getText()));
        } catch (NumberFormatException ignored) {
            settings.setObsPort(0);
        }
        settings.setObsPassword(String.valueOf(pfPassword.getPassword()));
    }

    private void cancel(ActionEvent e) {
        this.setVisible(false);
        tfServer.setText(settings.getObsServer());
        tfPort.setText(String.valueOf(settings.getObsPort()));
        pfPassword.setText(settings.getObsPassword());
    }

    private void btnApply(ActionEvent e) {
        applySettings();
    }

    private void initComponents() {
        // JFormDesigner - Component initialization - DO NOT MODIFY  //GEN-BEGIN:initComponents  @formatter:off
        dialogPane = new JPanel();
        contentPanel = new JPanel();
        label1 = new JLabel();
        tfServer = new JTextField();
        label2 = new JLabel();
        tfPort = new JTextField();
        label3 = new JLabel();
        pfPassword = new JPasswordField();
        buttonBar = new JPanel();
        btnOk = new JButton();
        btnCancel = new JButton();
        btnApply = new JButton();

        //======== this ========
        setTitle("OBS Server Information");
        var contentPane = getContentPane();
        contentPane.setLayout(new BorderLayout());

        //======== dialogPane ========
        {
            dialogPane.setLayout(new BorderLayout());

            //======== contentPanel ========
            {
                contentPanel.setLayout(new MigLayout(
                    "insets dialog,hidemode 3",
                    // columns
                    "[67,fill]" +
                    "[251,grow,fill]",
                    // rows
                    "[]" +
                    "[]" +
                    "[]"));

                //---- label1 ----
                label1.setText("Server");
                contentPanel.add(label1, "cell 0 0");
                contentPanel.add(tfServer, "cell 1 0");

                //---- label2 ----
                label2.setText("Port");
                contentPanel.add(label2, "cell 0 1");
                contentPanel.add(tfPort, "cell 1 1");

                //---- label3 ----
                label3.setText("Password");
                contentPanel.add(label3, "cell 0 2");
                contentPanel.add(pfPassword, "cell 1 2");
            }
            dialogPane.add(contentPanel, BorderLayout.CENTER);

            //======== buttonBar ========
            {
                buttonBar.setLayout(new MigLayout(
                    "insets dialog,alignx right",
                    // columns
                    "[button,fill]" +
                    "[button,fill]" +
                    "[fill]",
                    // rows
                    null));

                //---- btnOk ----
                btnOk.setText("OK");
                btnOk.addActionListener(e -> ok(e));
                buttonBar.add(btnOk, "cell 0 0");

                //---- btnCancel ----
                btnCancel.setText("Cancel");
                btnCancel.addActionListener(e -> cancel(e));
                buttonBar.add(btnCancel, "cell 1 0");

                //---- btnApply ----
                btnApply.setText("Apply");
                btnApply.addActionListener(e -> btnApply(e));
                buttonBar.add(btnApply, "cell 2 0");
            }
            dialogPane.add(buttonBar, BorderLayout.SOUTH);
        }
        contentPane.add(dialogPane, BorderLayout.CENTER);
        pack();
        setLocationRelativeTo(getOwner());
        // JFormDesigner - End of component initialization  //GEN-END:initComponents  @formatter:on
    }

    // JFormDesigner - Variables declaration - DO NOT MODIFY  //GEN-BEGIN:variables  @formatter:off
    private JPanel dialogPane;
    private JPanel contentPanel;
    private JLabel label1;
    private JTextField tfServer;
    private JLabel label2;
    private JTextField tfPort;
    private JLabel label3;
    private JPasswordField pfPassword;
    private JPanel buttonBar;
    private JButton btnOk;
    private JButton btnCancel;
    private JButton btnApply;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
