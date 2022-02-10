package org.t3.g11.proj2.peer.ui.swing.panels;

import net.miginfocom.swing.MigLayout;
import org.t3.g11.proj2.nuttela.message.Result;
import org.t3.g11.proj2.peer.PeerStateObserver;
import org.t3.g11.proj2.peer.ui.swing.SwingInterface;
import org.t3.g11.proj2.peer.ui.swing.components.UserPost;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.Future;

public class DiscoveryPanel extends JPanel {

    private final SwingInterface swi;
    private final JPanel postsPanel;
    private final DateFormat format = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    public DiscoveryPanel(SwingInterface swi) {
        super(new MigLayout("inset 20, fill", "", "[][][nogrid][]"));

        this.swi = swi;

        JLabel header = new JLabel("Discovery");
        header.setFont(UIManager.getFont("h0.font"));
        add(header);

        JLabel userLabel = new JLabel(swi.peer.getPeerData().getSelfUsername());
        userLabel.setHorizontalAlignment(JLabel.RIGHT);
        userLabel.setFont(UIManager.getFont("large.font"));
        add(userLabel, "align right, wrap");

        JSeparator sep = new JSeparator(JSeparator.HORIZONTAL);
        add(sep, "span, growx, hmin 2px, wrap");

        JPanel searchBox = new JPanel(new MigLayout("fillx", "[24]10[100:400:]push"));
        add(searchBox, "span, wrap, growx");

        ImageIcon icon = swi.createImageIcon("search_icon.png",
                "Search button");
        Image image = icon.getImage(); // transform it
        Image newimg = image.getScaledInstance(20, 20,  java.awt.Image.SCALE_SMOOTH); // scale it the smooth way
        icon = new ImageIcon(newimg);
        JButton searchButton = new JButton(icon);
        searchButton.setBorder(BorderFactory.createEmptyBorder());
        searchButton.setBackground(null);
        searchBox.add(searchButton);

        JTextField queryField = new JTextField();
        searchBox.add(queryField, "growx");

        JPanel searched = new JPanel(new MigLayout("nogrid"));
        searched.setVisible(false);
        JLabel searchedForLabel = new JLabel("Query: ");
        searchedForLabel.setEnabled(false);
        searched.add(searchedForLabel, "");
        JLabel currentQuery = new JLabel();
        searched.add(currentQuery, "wrap");
        add(searched, "wrap");

        this.postsPanel = new JPanel(new MigLayout("fillx"));

        JScrollPane scrollPane = new JScrollPane(postsPanel);
        scrollPane.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        scrollPane.setBorder(BorderFactory.createEmptyBorder());
        scrollPane.setVisible(false);
        add(scrollPane, "span, grow, pushy, wrap, hidemode 3");

        // Search elements
        ImageIcon loadingIcon = swi.createImageIcon("loading.gif",
                "loading");
        JLabel loadingLabel = new JLabel(loadingIcon, JLabel.CENTER);
        loadingLabel.setVisible(false);
        add(loadingLabel, "span, grow, pushy, align center, wrap, hidemode 3");

        JLabel noResultsFound = new JLabel("", JLabel.CENTER);
        add(noResultsFound, "span, grow, pushy, align center, wrap, hidemode 3");

        ActionListener querySubmit = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                String q = queryField.getText().trim();
                if (q.isBlank())
                    return;
                queryField.setText("");

                currentQuery.setText(q);
                searched.setVisible(true);

                noResultsFound.setText("");
                noResultsFound.setVisible(false);

                loadingLabel.setVisible(true);
                scrollPane.setVisible(false);
                postsPanel.removeAll();
                invalidate();
                repaint();

                Thread t = new Thread(() -> {
                    Set<Result> results = swi.peer.search(q);

                    SwingUtilities.invokeLater(() -> {
                        loadingLabel.setVisible(false);

                        if (results.isEmpty()) {
                            noResultsFound.setText("No results found");
                            noResultsFound.setVisible(true);
                        } else {
                            scrollPane.setVisible(true);

                            // handle save query results
                            for (Result post : results) {
                                System.out.println(post.author);
                                String content;
                                try {
                                    content = swi.peer.decypherText(post.ciphered, post.author);
                                    addNewPost(post.author, post.date, content);
                                } catch (Exception exception) {
                                    System.err.println("Failed to add post with stacktrace:");
                                    exception.printStackTrace();
                                }
                            }
                        }

                        revalidate();
                        repaint();
                    });
                });

                t.start();
            }
        };


        searchButton.addActionListener(querySubmit);
        queryField.addActionListener(querySubmit);
    }

    private void addNewPost(String username, long timestamp, String content) {
        Date d = new Date(timestamp);
        this.postsPanel.add(new UserPost(username, format.format(d), content), "span, wrap");
    }
}
