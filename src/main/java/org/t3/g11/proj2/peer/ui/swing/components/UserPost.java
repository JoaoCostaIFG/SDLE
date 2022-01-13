package org.t3.g11.proj2.peer.ui.swing.components;

import com.formdev.flatlaf.FlatClientProperties;
import net.miginfocom.swing.MigLayout;

import javax.swing.*;

public class UserPost extends JPanel {
    public UserPost(String username, String timestamp, String content) {
        super(new MigLayout("fillx"));

        putClientProperty(FlatClientProperties.STYLE,
                "[light]background: tint(@background,50%);" +
                        "[dark]background: shade(@background,15%);" +
                        "[light]border: 4,4,4,4,shade(@background,10%),,8;" +
                        "[dark]border: 4,4,4,4,tint(@background,10%),,8");

        JLabel usernameLabel = new JLabel(username);
        usernameLabel.setFont(UIManager.getFont("h4.font"));
        add(usernameLabel, "");

        JSeparator sep = new JSeparator(JSeparator.VERTICAL);
        add(sep, "growy, wmin 2px");

        JLabel timestampLabel = new JLabel(timestamp);
        timestampLabel.setEnabled(false);
        add(timestampLabel, "wrap");

        JLabel contentLabel = new JLabel(content);
        add(contentLabel, "span, growx, wrap, wmin 300px");
    }
}
