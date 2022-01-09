package org.t3.g11.proj2.peer.ui.swing.menu;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class MenuBar extends JMenuBar {

    public MenuBar(JFrame frame) {
        // add 2 items to menu 2
        JMenu m1 = new JMenu("Posts");
        m1.setMnemonic(KeyEvent.VK_P);
        m1.getAccessibleContext().setAccessibleDescription("Manage your posts");
        JMenuItem mi1_1 = new JMenuItem("New", KeyEvent.VK_P);
        mi1_1.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_P, ActionEvent.ALT_MASK));
        mi1_1.getAccessibleContext().setAccessibleDescription("Add a new post to the network");
        mi1_1.addActionListener(new MenuItemListener(frame));
        m1.add(mi1_1);

        // add 4 items to menu 2
        JMenu m2 = new JMenu("Social");
        m2.setMnemonic(KeyEvent.VK_S);
        m2.getAccessibleContext().setAccessibleDescription("Manage followed users");

        JMenuItem mi2_1 = new JMenuItem("Follow", KeyEvent.VK_F);
        mi2_1.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, ActionEvent.ALT_MASK));
        mi2_1.getAccessibleContext().setAccessibleDescription("Follow another user");
        m2.add(mi2_1);

        JMenuItem mi2_2 = new JMenuItem("Unfollow", KeyEvent.VK_U);
        mi2_2.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, ActionEvent.ALT_MASK));
        mi2_2.getAccessibleContext().setAccessibleDescription("Unfollow a user");
        m2.add(mi2_2);


        // add menus to bar
        add(m1);
        add(m2);
    }

}
