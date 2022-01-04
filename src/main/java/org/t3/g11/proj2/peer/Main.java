package org.t3.g11.proj2.peer;

import org.t3.g11.proj2.peer.ui.PeerInterface;
import org.t3.g11.proj2.peer.ui.cmd.CmdInterface;
import org.zeromq.ZContext;

import java.util.Arrays;

public class Main {
    private final ZContext zctx;
    private final Peer peer;

    private PeerInterface peerInterface;

    public Main() throws Exception {
        this.zctx = new ZContext();
        this.peer = new Peer(this.zctx);
    }

    public void setInterface(PeerInterface peerInterface) {
        this.peerInterface = peerInterface;
    }

    public static void main(String[] args) {
        Main main;
        try {
            main = new Main();
        } catch (Exception e) {
            e.printStackTrace();
            System.exit(1);
            return;
        }

        if (Arrays.asList(args).contains("--gui")) {
            System.err.println("Not implemented");
            System.exit(1);
        } else {
            main.setInterface(new CmdInterface(main.peer));
        }

        main.peerInterface.clientLoop();
    }
}
