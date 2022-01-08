package org.t3.g11.proj2.peer;

import org.t3.g11.proj2.peer.ui.PeerInterface;
import org.t3.g11.proj2.peer.ui.cmd.CmdInterface;
import org.t3.g11.proj2.peer.ui.swing.SwingInterface;
import org.zeromq.ZContext;

import java.util.Arrays;

public class Main {

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: Peer <id> <routerIP> <routerPort>");
            return;
        }

        ZContext zctx = new ZContext();
        Peer peer;
        try {
            peer = new Peer(zctx, Integer.parseInt(args[0]), args[1], Integer.parseInt(args[2]));
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
