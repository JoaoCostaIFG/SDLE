package org.t3.g11.proj2.peer.ui.swing.menu;

import com.formdev.flatlaf.FlatDarkLaf;
import org.t3.g11.proj2.peer.ui.swing.SwingInterface;
import org.t3.g11.proj2.peer.ui.swing.menu.popups.FollowActionListener;
import org.t3.g11.proj2.peer.ui.swing.menu.popups.NewPostActionListener;
import org.t3.g11.proj2.peer.ui.swing.menu.popups.UnfollowActionListener;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

public class MenuBar extends JMenuBar {

    public MenuBar(SwingInterface swi) {
        // POSTS MENU
        JMenu postsMenu = new JMenu("Posts");
        postsMenu.setMnemonic(KeyEvent.VK_P);
        postsMenu.getAccessibleContext().setAccessibleDescription("Manage your posts");

        JMenuItem newPost = new JMenuItem("New", KeyEvent.VK_N);
        newPost.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_N, ActionEvent.CTRL_MASK));
        newPost.getAccessibleContext().setAccessibleDescription("Add a new post to the network");
        newPost.addActionListener(new NewPostActionListener(swi));
        postsMenu.add(newPost);

        // SOCIAL MENU
        JMenu socialMenu = new JMenu("Social");
        socialMenu.setMnemonic(KeyEvent.VK_S);
        socialMenu.getAccessibleContext().setAccessibleDescription("Manage followed users");

        JMenuItem follow = new JMenuItem("Follow", KeyEvent.VK_F);
        follow.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F, ActionEvent.CTRL_MASK));
        follow.getAccessibleContext().setAccessibleDescription("Follow another user");
        follow.addActionListener(new FollowActionListener(swi));
        socialMenu.add(follow);

        JMenuItem unfollow = new JMenuItem("Unfollow", KeyEvent.VK_U);
        unfollow.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_U, ActionEvent.CTRL_MASK));
        unfollow.getAccessibleContext().setAccessibleDescription("Unfollow a user");
        unfollow.addActionListener(new UnfollowActionListener(swi));
        socialMenu.add(unfollow);

        JMenu configMenu = new JMenu("Config");
        configMenu.setMnemonic(KeyEvent.VK_C);
        configMenu.getAccessibleContext().setAccessibleDescription("App config");

        ButtonGroup group = new ButtonGroup();
        JRadioButtonMenuItem rbMenuItem = new JRadioButtonMenuItem("Dark theme");
        rbMenuItem.setSelected(true);
        group.add(rbMenuItem);
        configMenu.add(rbMenuItem);

        rbMenuItem = new JRadioButtonMenuItem("Light theme");
        group.add(rbMenuItem);
        configMenu.add(rbMenuItem);

        // add menus to bar
        add(postsMenu);
        add(socialMenu);
        add(configMenu);
    }

}
