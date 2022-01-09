package org.t3.g11.proj2.peer.ui.swing.menu;

import org.t3.g11.proj2.peer.ui.swing.SwingInterface;
import org.t3.g11.proj2.peer.ui.swing.menu.popups.FollowActionListener;
import org.t3.g11.proj2.peer.ui.swing.menu.popups.NewPostActionListener;
import org.t3.g11.proj2.peer.ui.swing.menu.popups.UnfollowActionListener;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class MenuBar extends JMenuBar {

    public MenuBar(SwingInterface swi) {
        // add 2 items to menu 2
        JMenu m1 = new JMenu("Posts");
        m1.setMnemonic(KeyEvent.VK_P);
        m1.getAccessibleContext().setAccessibleDescription("Manage your posts");
        JMenuItem mi1_1 = new JMenuItem("New", KeyEvent.VK_N);
        mi1_1.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
        mi1_1.getAccessibleContext().setAccessibleDescription("Add a new post to the network");
        mi1_1.addActionListener(new NewPostActionListener(swi));
        m1.add(mi1_1);

        // add 4 items to menu 2
        JMenu m2 = new JMenu("Social");
        m2.setMnemonic(KeyEvent.VK_S);
        m2.getAccessibleContext().setAccessibleDescription("Manage followed users");

        JMenuItem mi2_1 = new JMenuItem("Follow", KeyEvent.VK_F);
        mi2_1.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, ActionEvent.CTRL_MASK));
        mi2_1.getAccessibleContext().setAccessibleDescription("Follow another user");
        mi2_1.addActionListener(new FollowActionListener(swi));
        m2.add(mi2_1);

        JMenuItem mi2_2 = new JMenuItem("Unfollow", KeyEvent.VK_U);
        mi2_2.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, ActionEvent.CTRL_MASK));
        mi2_2.getAccessibleContext().setAccessibleDescription("Unfollow a user");
        mi2_2.addActionListener(new UnfollowActionListener(swi));
        m2.add(mi2_2);


        // add menus to bar
        add(m1);
        add(m2);
    }

}
