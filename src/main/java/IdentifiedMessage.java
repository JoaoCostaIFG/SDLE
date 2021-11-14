import org.zeromq.ZFrame;
import org.zeromq.ZMsg;

import java.util.ArrayList;
import java.util.List;

public class IdentifiedMessage extends UnidentifiedMessage {
    private final ZFrame identity;

    public IdentifiedMessage(ZFrame identity, String cmd, List<String> args) {
        super(cmd, args);
        this.identity = identity;
    }

    public IdentifiedMessage(ZMsg zMsg) {
        super("", new ArrayList<>());
        this.identity = zMsg.unwrap();
        this.decomposeZMsg(zMsg);
    }

    public ZFrame getIdentity() {
        return identity;
    }

    public ZMsg newZMsg() {
        ZMsg ret = super.newZMsg();
        ret.wrap(this.identity);
        return ret;
    }
}
