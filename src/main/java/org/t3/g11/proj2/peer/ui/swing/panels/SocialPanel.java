package org.t3.g11.proj2.peer.ui.swing.panels;

import net.miginfocom.swing.MigLayout;
import org.t3.g11.proj2.peer.ui.swing.SwingInterface;

import javax.swing.*;

public class SocialPanel extends JPanel {

    SwingInterface swi;

    public SocialPanel(SwingInterface swi) {
        super(new MigLayout("inset 20, fill"));

        this.swi = swi;

        JLabel header = new JLabel("Social");
        header.setFont(UIManager.getFont("h0.font"));
        add(header);

        JLabel userLabel = new JLabel(swi.peer.getPeerData().getSelfUsername());
        userLabel.setHorizontalAlignment(JLabel.RIGHT);
        userLabel.setFont(UIManager.getFont("large.font"));
        add(userLabel, "align right, wrap");

        JSeparator sep = new JSeparator(JSeparator.HORIZONTAL);
        add(sep, "span, growx, hmin 2px, wrap");

        add(new JLabel("Not yet implemented", JLabel.CENTER), "span, grow, pushy, wrap");
    }
}
