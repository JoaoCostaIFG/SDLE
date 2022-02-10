package org.t3.g11.proj2.peer.ui.swing.panels;

import net.miginfocom.swing.MigLayout;
import org.t3.g11.proj2.peer.PeerStateObserver;
import org.t3.g11.proj2.peer.ui.swing.SwingInterface;
import org.t3.g11.proj2.peer.ui.swing.components.UserPost;

import javax.sound.sampled.*;
import javax.swing.*;
import java.io.IOException;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.SortedSet;

public class TimelinePanel extends JPanel implements PeerStateObserver {

    private final SwingInterface swi;
    private final JPanel postsPanel;
    private final DateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public TimelinePanel(SwingInterface swi) {
        super(new MigLayout("inset 20, fill"));

        this.swi = swi;
        swi.peer.addObserver(this);

        JLabel header = new JLabel("Timeline");
        header.setFont(UIManager.getFont("h0.font"));
        add(header);

        JLabel userLabel = new JLabel(swi.peer.getPeerData().getSelfUsername());
        userLabel.setHorizontalAlignment(JLabel.RIGHT);
        userLabel.setFont(UIManager.getFont("large.font"));
        add(userLabel, "align right, wrap");

        JSeparator sep = new JSeparator(JSeparator.HORIZONTAL);
        add(sep, "span, growx, hmin 2px, wrap");

        this.postsPanel = new JPanel(new MigLayout("fillx"));

        SortedSet<HashMap<String, String>> posts = null;
        try {
            posts = swi.peer.getPosts();
        } catch (Exception e) {
            addNewPost("Molater Team", System.currentTimeMillis(), "[Error] Failed to read local posts, please reload app");
        }

        if (posts.isEmpty())
            addNewPost("Molater Team", System.currentTimeMillis(), "This timeline is looking quite empty, follow other people and start posting yourself!");
        for (var post: posts) {
            addPost(post.get("author"), post.get("timestamp"), post.get("content"));
        }

        JScrollPane scrollPane = new JScrollPane(postsPanel);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        add(scrollPane, "span, grow, pushy, wrap");
    }

    private void addPost(String username, String timestamp, String content) {
        Date d = new Date(Long.parseLong(timestamp));
        this.postsPanel.add(new UserPost(username, format.format(d), content), "span, wrap");
    }

    private void addNewPost(String username, long timestamp, String content) {
        Date d = new Date(timestamp);
        this.postsPanel.add(new UserPost(username, format.format(d), content), "span, wrap", 0);
    }

    @Override
    public void followCountUpdated(int followCount) {}

    @Override
    public void newPost(String username, long timestamp, String content) {
        SwingUtilities.invokeLater(() -> {
            addNewPost(username, timestamp, content);
            invalidate();
            repaint();
        });

        try {
            // Open an audio input stream.
            URL url = this.getClass().getClassLoader().getResource("brown_headed_cowbird_one_call.wav");
            AudioInputStream audioIn = AudioSystem.getAudioInputStream(url);
            // Get a sound clip resource.
            Clip clip = AudioSystem.getClip();
            // Open audio clip and load samples from the audio input stream.
            clip.open(audioIn);
            clip.start();
        } catch (UnsupportedAudioFileException | IOException | LineUnavailableException e) {
            e.printStackTrace();
        }
    }
}
