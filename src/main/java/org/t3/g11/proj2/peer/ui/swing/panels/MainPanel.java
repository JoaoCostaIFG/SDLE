package org.t3.g11.proj2.peer.ui.swing.panels;

import com.sun.tools.jconsole.JConsoleContext;
import net.miginfocom.swing.MigLayout;
import org.t3.g11.proj2.peer.ui.swing.SwingInterface;
import org.t3.g11.proj2.peer.ui.swing.panels.LoginPanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class MainPanel extends JTabbedPane {
    public MainPanel(SwingInterface swi) {
        super();

        setTabPlacement(JTabbedPane.LEFT);
        setFont( UIManager.getFont( "h2.font" ) );

        JComponent timelinePanel = new TimelinePanel(swi);
        addTab("Timeline", null, timelinePanel, "See your current posts");

        JComponent discoveryPanel = new DiscoveryPanel(swi);
        addTab("Discover", null, discoveryPanel, "Find new content within the network");
    }
}
