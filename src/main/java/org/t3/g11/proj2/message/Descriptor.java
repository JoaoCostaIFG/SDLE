package org.t3.g11.proj2.message;

import org.zeromq.ZMsg;

import java.util.Collections;
import java.util.List;

public class Descriptor extends UnidentifiedMessage {
    public static final String PING = "PING";
    public static final String PONG = "PONG";
    public static final String PUSH = "PUSH";
    public static final String QUERY = "QUERY";
    public static final String QUERYHIT = "QUERYHIT";
    public static final String NEIGH = "NEIGH";
    public static final String NUMNEIGH = "NUMNEIGH";
    public static final String MYNEIGH = "MYNEIGH";
    public static final String NEIGHOK = "NEIGHOK";
    public static final String NEIGHERR = "NEIGHERR";
    public static final String DROPOK = "DROPOK";
    public static final String DROPERR = "DROPERR";
    public static final String DROP = "DROP";

    public static final int MAX_TTL = 4;
    public static final int PING_TTL = 1;

    public Descriptor(String cmd, List<String> args) {
        super(cmd, args);
    }

    public Descriptor(ZMsg msg) {
        super("");
        this.decomposeZMsg(msg);
    }

    public String getId() {
        return this.getArg(0);
    }

    public int getHops() {
        return Integer.parseInt(this.getArg(1));
    }

    public int getTtl() {
        return Integer.parseInt(this.getArg(2));
    }
}
