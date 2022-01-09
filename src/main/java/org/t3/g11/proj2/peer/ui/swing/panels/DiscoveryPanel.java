package org.t3.g11.proj2.peer.ui.swing.panels;

import net.miginfocom.swing.MigLayout;
import org.t3.g11.proj2.peer.ui.swing.SwingInterface;

import javax.swing.*;

public class DiscoveryPanel extends JPanel {

    SwingInterface swi;

    public DiscoveryPanel(SwingInterface swi) {
        super(new MigLayout("inset 20"));

        this.swi = swi;

        JLabel header = new JLabel("Discovery");
        header.setHorizontalAlignment(JLabel.CENTER);
        header.setFont( UIManager.getFont( "h0.font" ) );
        add(header, "wrap");
        add(new JSeparator(), "wrap");

        add(new JLabel("Not yet implemented", JLabel.CENTER), "span, wrap");
    }
}
