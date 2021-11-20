package client;

import message.UnidentifiedMessage;
import org.zeromq.ZContext;
import org.zeromq.ZMsg;
import proxy.Proxy;

import java.util.Arrays;

public class Publisher extends SocketHolder {
    public static final String PUTCMD = "PUT";

    public Publisher(ZContext zctx, String id, String endpoint) {
        super(zctx, id, endpoint);
    }

    public boolean put(String topic, String content) throws Exception {
        ZMsg zmsg = new UnidentifiedMessage(Publisher.PUTCMD, Arrays.asList(topic, content)).newZMsg();
        if (!zmsg.send(this.socket)) return false;

        ZMsg replyZMsg = this.receiveMsg();
        if (replyZMsg == null) return false;
        UnidentifiedMessage reply = new UnidentifiedMessage(replyZMsg);

        return reply.getCmd().equals(Publisher.PUTCMD) &&
                reply.getArg(0).equals(Proxy.OKREPLY);
    }
}
