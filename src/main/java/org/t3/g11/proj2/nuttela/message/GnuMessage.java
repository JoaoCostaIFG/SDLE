package org.t3.g11.proj2.nuttela.message;

import org.t3.g11.proj2.nuttela.GnuNodeCMD;

import java.net.InetSocketAddress;

public class GnuMessage {
    protected final GnuNodeCMD cmd;
    protected final InetSocketAddress addr;

    public GnuMessage(GnuNodeCMD cmd, InetSocketAddress addr) {
        this.cmd = cmd;
        this.addr = addr;
    }

    public GnuNodeCMD getCmd() {
        return this.cmd;
    }

    public InetSocketAddress getAddr() {
        return addr;
    }
}
