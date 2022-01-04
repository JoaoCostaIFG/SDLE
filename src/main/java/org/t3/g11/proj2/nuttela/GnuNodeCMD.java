package org.t3.g11.proj2.nuttela;

import org.t3.g11.proj2.message.UnidentifiedMessage;
import org.zeromq.ZMsg;

import java.util.Collections;
import java.util.List;

public enum GnuNodeCMD {
    NEIGH, // id, address, n_neigh, capacity
    NUMNEIGH,
    MYNEIGH, // n_neigh
    NEIGHOK, // id, capacity
    NEIGHERR,
    DROPOK,
    DROPERR,
    DROP, // id, address
    PING,
    PONG,
    PUSH,
    QUERY,
    QUERYHIT;

    public ZMsg getMessage(List<String> args) {
        return new UnidentifiedMessage(this.toString(), args).newZMsg();
    }

    public ZMsg getMessage() {
        return this.getMessage(Collections.emptyList());
    }
}
