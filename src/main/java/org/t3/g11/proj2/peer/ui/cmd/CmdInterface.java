package org.t3.g11.proj2.peer.ui.cmd;

import org.t3.g11.proj2.peer.Peer;
import org.t3.g11.proj2.peer.ui.PeerInterface;

import java.util.*;

public class CmdInterface implements PeerInterface {
    private final Peer peer;
    private final Scanner scanner;

    private Stack<CmdPage> pages;
    private boolean exit = false;

    public CmdInterface(Peer peer) {
        this.peer = peer;
        this.pages = new Stack<>();
        this.scanner = new Scanner(System.in);

        this.pushPage(new GuestPage(this));
    }

    public Scanner getScanner() {
        return scanner;
    }

    public Peer getPeer() {
        return peer;
    }

    @Override
    public void setup() {
        while (!this.exit)
            this.pages.peek().show();

        peer.shutdown();
    }

    public void exit() {
        this.exit = true;
    }

    public void pushPage(CmdPage page) {
        this.pages.push(page);
    }

    public void popPage() {
        this.pages.pop();
    }
}
