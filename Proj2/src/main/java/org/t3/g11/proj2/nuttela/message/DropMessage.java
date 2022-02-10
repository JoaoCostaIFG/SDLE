package org.t3.g11.proj2.nuttela.message;

import org.t3.g11.proj2.nuttela.GnuNodeCMD;

import java.net.InetSocketAddress;

public class DropMessage extends GnuIdMessage {
    public DropMessage(InetSocketAddress addr, int id) {
        super(GnuNodeCMD.DROP, addr, id);
    }
}
