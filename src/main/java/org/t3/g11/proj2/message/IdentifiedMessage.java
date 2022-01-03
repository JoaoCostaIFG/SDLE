package org.t3.g11.proj2.message;

import org.zeromq.ZFrame;
import org.zeromq.ZMsg;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class IdentifiedMessage extends UnidentifiedMessage {
    private final ZFrame identity;

    public IdentifiedMessage(ZFrame identity, String cmd, List<String> args) {
        super(cmd, args);
        this.identity = identity;
    }

    public IdentifiedMessage(ZFrame identity, String cmd) {
        super(cmd, Collections.emptyList());
        this.identity = identity;
    }

    public IdentifiedMessage(ZMsg zMsg) {
        super("", new ArrayList<>());
        this.identity = zMsg.unwrap();
        this.decomposeZMsg(zMsg);
    }

    public ZFrame getIdentity() {
        return this.identity;
    }

    public ZMsg newZMsg() {
        ZMsg ret = super.newZMsg();
        ret.wrap(this.identity);
        return ret;
    }

    public String getIdentityStr() {
        return this.identity.getString(StandardCharsets.UTF_8);
    }
}
