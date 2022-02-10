package org.t3.g11.proj2.peer.ui.swing.menu.popups;

import org.t3.g11.proj2.peer.ui.swing.SwingInterface;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.Set;

public class UnfollowActionListener implements ActionListener {

    private final SwingInterface swi;

    public UnfollowActionListener(SwingInterface swi) {
        this.swi = swi;
    }

    public void actionPerformed(ActionEvent e) {

        String[] followedUsers = new String[]{};
        try {
            Set<String> followedSet = swi.peer.getPeerData().getSubs();
            if (followedSet.isEmpty())
                return;

            followedUsers = followedSet.toArray(followedUsers);
        } catch (SQLException exception) {
            followedUsers = null;
        }

        Object selectedObj = JOptionPane.showInputDialog(null, "Username", "Unfollow", JOptionPane.PLAIN_MESSAGE, null, followedUsers, null);

        if (selectedObj == null)
            return;

        String username = selectedObj.toString().trim();
        if (username.isBlank()) {
            JOptionPane.showMessageDialog(null, "Username must not be empty", "Alert", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            swi.peer.unsubscribe(username);
        } catch (Exception exception) {
            String message = "Failed to unfollow '" + username + "'\n" + exception.getMessage();
            JOptionPane.showMessageDialog(null, message, "Alert", JOptionPane.ERROR_MESSAGE);
            return;
        }

        JOptionPane.showMessageDialog(null, "You have unfollowed '" + username + "'!", "Alert", JOptionPane.INFORMATION_MESSAGE);
    }
}
