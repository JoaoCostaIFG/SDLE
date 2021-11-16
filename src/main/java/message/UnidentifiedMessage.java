package message;

import org.zeromq.ZMsg;

import java.util.ArrayList;
import java.util.List;

public class UnidentifiedMessage {
    private final List<String> args;
    private String cmd;

    public UnidentifiedMessage(String cmd, List<String> args) {
        this.cmd = cmd;
        this.args = args;
    }

    public UnidentifiedMessage(String cmd) {
        this(cmd, new ArrayList<>());
    }

    public UnidentifiedMessage(ZMsg zMsg) {
        this("");
        decomposeZMsg(zMsg);
    }

    protected void decomposeZMsg(ZMsg zMsg) {
        this.args.clear();

        if (zMsg.size() > 0) this.cmd = zMsg.popString();
        else this.cmd = "";

        while (zMsg.size() > 0) {
            this.args.add(zMsg.popString());
        }
    }

    public String getCmd() {
        return cmd;
    }

    public String getArg(int i) {
        if (i >= this.args.size()) return null;
        return this.args.get(i);
    }

    public int getArgCount() {
        return this.args.size();
    }

    public ZMsg newZMsg() {
        ZMsg ret = new ZMsg();
        ret.add(this.cmd);
        for (String arg : this.args)
            ret.add(arg);
        return ret;
    }
}
