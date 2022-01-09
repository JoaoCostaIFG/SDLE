package org.t3.g11.proj2.peer.ui.swing.panels;

import net.miginfocom.swing.MigLayout;
import org.t3.g11.proj2.peer.ui.swing.SwingInterface;

import javax.swing.*;

public class DiscoveryPanel extends JPanel {

    SwingInterface swi;

    public DiscoveryPanel(SwingInterface swi) {
        super(new MigLayout("inset 20, fill"));

        this.swi = swi;

        JLabel header = new JLabel("Discovery");
        header.setFont(UIManager.getFont("h0.font"));
        add(header, "span, wrap");

        JSeparator sep = new JSeparator(JSeparator.HORIZONTAL);
        add(sep, "span, growx, hmin 2px, wrap");

        add(new JLabel("Not yet implemented", JLabel.CENTER), "span, grow, pushy, wrap");
    }
}
