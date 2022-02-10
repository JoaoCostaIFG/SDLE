package org.t3.g11.proj2.peer.ui.swing.menu.popups;

import org.t3.g11.proj2.peer.ui.swing.SwingInterface;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class FollowActionListener implements ActionListener {

    private final SwingInterface swi;

    public FollowActionListener(SwingInterface swi) {
        this.swi = swi;
    }

    public void actionPerformed(ActionEvent e) {

        String username = JOptionPane.showInputDialog(null, "Username", "Follow user", JOptionPane.PLAIN_MESSAGE);
        username = username.trim();

        if (username.isBlank()) {
            JOptionPane.showMessageDialog(null, "Username must not be empty", "Alert", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            swi.peer.subscribe(username);
        } catch (Exception exception) {
            String message = "Failed to follow '" + username + "'\n" + exception.getMessage();
            JOptionPane.showMessageDialog(null, message, "Alert", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(null, "You are now following '" + username + "'!", "Alert", JOptionPane.INFORMATION_MESSAGE);
    }
}
