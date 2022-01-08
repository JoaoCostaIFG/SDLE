package org.t3.g11.proj2.peer;

import org.t3.g11.proj2.nuttela.GnuNode;
import org.t3.g11.proj2.peer.ui.PeerInterface;
import org.t3.g11.proj2.peer.ui.cmd.CmdInterface;
import org.t3.g11.proj2.peer.ui.swing.SwingInterface;
import org.zeromq.ZContext;

import java.util.Arrays;

public class Main {

    public static void main(String[] args) {

        if (args.length < 2) {
            System.out.println("Usage: Peer <id> <routerAdd>");
            return;
        }

        ZContext zctx = new ZContext();
        Peer peer;
        try {
            peer = new Peer(zctx, new GnuNode(zctx, Integer.parseInt(args[0]), args[1]));
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
