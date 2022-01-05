package org.t3.g11.proj2.peer.ui.swing;

import com.formdev.flatlaf.FlatDarkLaf;
import org.t3.g11.proj2.peer.Peer;
import org.t3.g11.proj2.peer.ui.PeerInterface;

import javax.swing.*;

public class SwingInterface implements PeerInterface {

    private Peer peer;

    public SwingInterface(Peer peer) {
        this.peer = peer;
    }

    @Override
    public void setup() {
        // https://stackoverflow.com/questions/18976990/best-practice-to-start-a-swing-application
        SwingUtilities.invokeLater(() -> {
            FlatDarkLaf.setup();

            MainFrame mf = new MainFrame();
        });
    }


    @Override
    public void clientLoop() {
    }
}
