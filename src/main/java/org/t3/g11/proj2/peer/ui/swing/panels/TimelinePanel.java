package org.t3.g11.proj2.peer.ui.swing.panels;

import net.miginfocom.swing.MigLayout;
import org.t3.g11.proj2.peer.ui.swing.SwingInterface;

import javax.swing.*;
import java.awt.*;

class TimelinePost extends JPanel {
    public TimelinePost(String username, String timestamp, String content) {
        super(new MigLayout("fill"));

        JLabel usernameLabel = new JLabel(username);
        add(usernameLabel, "");

        JSeparator sep = new JSeparator();
        sep.setOrientation(JSeparator.VERTICAL);
        add(sep, "growy");

        JLabel timestampLabel = new JLabel(timestamp);
        timestampLabel.setEnabled(false);
        add(timestampLabel, "wrap");

        JLabel contentLabel = new JLabel(content);
        add(contentLabel, "span, width 100%, wrap");
    }
}

public class TimelinePanel extends JPanel {

    SwingInterface swi;
    private final JPanel postsPanel;

    public TimelinePanel(SwingInterface swi) {
        super(new MigLayout("inset 20"));

        this.swi = swi;

        JLabel header = new JLabel("Timeline");
        header.setHorizontalAlignment(JLabel.CENTER);
        header.setFont( UIManager.getFont( "h0.font" ) );
        add(header, "wrap");
        add(new JSeparator(), "wrap");

        this.postsPanel = new JPanel(new MigLayout(""));
        addPost("souto", "12 jan 2021", "Não há report para ninguém amiguinhos");
        addPost("baquero", "12 jan 2021", "O meu barril é baril");
        addPost("souto", "12 jan 2021", "Não há report para ninguém amiguinhos");
        addPost("baquero", "12 jan 2021", "Guys tenho um barril no jardim");
        addPost("baquero", "12 jan 2021", "O meu barril é baril");
        addPost("souto", "12 jan 2021", "Não há report para ninguém amiguinhos");
        addPost("baquero", "12 jan 2021", "Guys tenho um barril no jardim");
        addPost("baquero", "12 jan 2021", "O meu barril é baril");
        addPost("souto", "12 jan 2021", "Não há report para ninguém amiguinhos");
        addPost("baquero", "12 jan 2021", "Guys tenho um barril no jardim");
        addPost("baquero", "12 jan 2021", "O meu barril é baril");


        JScrollPane scrollPane = new JScrollPane(postsPanel);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, "span, grow, wrap");

        scrollPane.setBorder(BorderFactory.createEmptyBorder());
    }

    private void addPost(String username, String timestamp, String content) {
        this.postsPanel.add(new TimelinePost(username, timestamp, content), "span, wrap");
    }
}
