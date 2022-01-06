package org.t3.g11.proj2.nuttela.message;

import org.t3.g11.proj2.nuttela.GnuNodeCMD;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class GnuMessage implements Serializable {
    protected final GnuNodeCMD cmd;
    protected InetSocketAddress addr; // hop source address

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

    public void setAddr(InetSocketAddress addr) {
        this.addr = addr;
    }

    public InetAddress getInetAddr() {
        return this.addr.getAddress();
    }

    public int getPort() {
        return this.addr.getPort();
    }
}
