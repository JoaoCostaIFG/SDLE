package org.t3.g11.proj2.peer.ui.swing.panels;

import com.formdev.flatlaf.FlatClientProperties;
import com.formdev.flatlaf.ui.FlatLineBorder;
import net.miginfocom.swing.MigLayout;
import org.t3.g11.proj2.peer.ui.swing.SwingInterface;

import javax.swing.*;
import java.awt.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.SortedSet;

class TimelinePost extends JPanel {
    public TimelinePost(String username, String timestamp, String content) {
        super(new MigLayout("fillx"));

//        putClientProperty(FlatClientProperties.STYLE,
//                "[light]background: tint(@background,50%);" +
//                        "[dark]background: shade(@background,15%);" +
//                        "[light]border: 4,4,4,4,shade(@background,10%),,8;" +
//                        "[dark]border: 4,4,4,4,tint(@background,10%),,8");

        JLabel usernameLabel = new JLabel(username);
        usernameLabel.setFont(UIManager.getFont("h4.font"));
        add(usernameLabel, "");

        JSeparator sep = new JSeparator(JSeparator.VERTICAL);
        add(sep, "growy, wmin 2px");

        JLabel timestampLabel = new JLabel(timestamp);
        timestampLabel.setEnabled(false);
        add(timestampLabel, "wrap");

        JLabel contentLabel = new JLabel(content);
        add(contentLabel, "span, growx, wrap");
    }
}

public class TimelinePanel extends JPanel {

    SwingInterface swi;
    private final JPanel postsPanel;

    public TimelinePanel(SwingInterface swi) {
        super(new MigLayout("inset 20, fill"));

        this.swi = swi;

        JLabel header = new JLabel("Timeline");
        header.setFont(UIManager.getFont("h0.font"));
        add(header);

        JLabel userLabel = new JLabel(swi.peer.getPeerData().getSelfUsername());
        userLabel.setHorizontalAlignment(JLabel.RIGHT);
        userLabel.setFont(UIManager.getFont("large.font"));
        add(userLabel, "align right, wrap");

        JSeparator sep = new JSeparator(JSeparator.HORIZONTAL);
        add(sep, "span, growx, hmin 2px, wrap");

        this.postsPanel = new JPanel(new MigLayout(""));

        SortedSet<HashMap<String, String>> posts = null;
        DateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        try {
            posts = swi.peer.getPosts();
        } catch (Exception e) {
            e.printStackTrace();
        }
        for (var post: posts) {
            Date d = new Date(Long.parseLong(post.get("timestamp")));
            addPost(post.get("author"), format.format(d), post.get("content"));
        }


        JScrollPane scrollPane = new JScrollPane(postsPanel);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, "span, grow, pushy, wrap");

        scrollPane.setBorder(BorderFactory.createEmptyBorder());
    }

    private void addPost(String username, String timestamp, String content) {
        this.postsPanel.add(new TimelinePost(username, timestamp, content), "span, wrap");
    }
}
