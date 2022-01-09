package org.t3.g11.proj2.peer;

import org.t3.g11.proj2.peer.ui.PeerInterface;
import org.t3.g11.proj2.peer.ui.cmd.CmdInterface;
import org.t3.g11.proj2.peer.ui.swing.SwingInterface;
import org.zeromq.ZContext;

import java.util.Arrays;

public class Main {

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: Peer <address> <port> [--gui]");
            return;
        }

        ZContext zctx = new ZContext();
        Peer peer;
        try {
            peer = new Peer(zctx, args[0], Integer.parseInt(args[1]));
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
            return;
        }


        PeerInterface peerInterface;
        if (Arrays.asList(args).contains("--gui")) {
            peerInterface = new SwingInterface(peer);
        } else {
            peerInterface = new CmdInterface(peer);
        }

        peerInterface.setup();
    }
}
