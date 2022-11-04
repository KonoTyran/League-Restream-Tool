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
        applySettings();
        this.setVisible(false);
    }

    private void applySettings() {
        settings.setObsServer(tfServer.getText());
        settings.setObsPort(Integer.parseInt(tfPort.getText()));
        settings.setObsPassword(String.valueOf(pfPassword.getPassword()));
    }

    private void cancel(ActionEvent e) {
        this.setVisible(false);
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
        okButton = new JButton();
        cancelButton = new JButton();
        button1 = new JButton();

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

                //---- okButton ----
                okButton.setText("OK");
                okButton.addActionListener(e -> ok(e));
                buttonBar.add(okButton, "cell 0 0");

                //---- cancelButton ----
                cancelButton.setText("Cancel");
                cancelButton.addActionListener(e -> cancel(e));
                buttonBar.add(cancelButton, "cell 1 0");

                //---- button1 ----
                button1.setText("Apply");
                buttonBar.add(button1, "cell 2 0");
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
    private JButton okButton;
    private JButton cancelButton;
    private JButton button1;
    // JFormDesigner - End of variables declaration  //GEN-END:variables  @formatter:on
}
