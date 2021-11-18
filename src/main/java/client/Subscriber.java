package client;

import message.UnidentifiedMessage;
import org.zeromq.ZContext;
import org.zeromq.ZMsg;
import proxy.Proxy;

import java.util.Collections;

public class Subscriber extends SocketHolder {
    public static final String GETCMD = "GET";
    public static final String SUBCMD = "SUB";
    public static final String UNSUBCMD = "UNSUB";

    public Subscriber(ZContext zctx, String id) {
        super(zctx, id);
    }

    public boolean subscribe(String topic) throws Exception {
        ZMsg reqZMsg = new UnidentifiedMessage(Subscriber.SUBCMD,
                Collections.singletonList(topic)).newZMsg();
        if (!reqZMsg.send(this.socket))
            return false;

        ZMsg replyZMsg = ZMsg.recvMsg(this.socket);
        UnidentifiedMessage reply = new UnidentifiedMessage(replyZMsg);
        return reply.getCmd().equals(SUBCMD) &&
                reply.getArg(0).equals(Proxy.OKREPLY);
    }

    public boolean unsubscribe(String topic) throws Exception {
        ZMsg reqZMsg = new UnidentifiedMessage(Subscriber.UNSUBCMD,
                Collections.singletonList(topic)).newZMsg();
        if (!reqZMsg.send(this.socket))
            return false;

        ZMsg replyZMsg = ZMsg.recvMsg(this.socket);
        UnidentifiedMessage reply = new UnidentifiedMessage(replyZMsg);
        return reply.getCmd().equals(UNSUBCMD) &&
                reply.getArg(0).equals(Proxy.OKREPLY);
    }

    public String get(String topic) throws Exception {
        String ret = null;

        while (!Thread.currentThread().isInterrupted()) {
            ZMsg reqZMsg = new UnidentifiedMessage(Subscriber.GETCMD,
                    Collections.singletonList(topic)).newZMsg();
            if (!reqZMsg.send(this.socket))
                return null;

            ZMsg replyZMsg = ZMsg.recvMsg(this.socket);
            UnidentifiedMessage reply = new UnidentifiedMessage(replyZMsg);
            if (!reply.getCmd().equals(GETCMD) || reply.getArg(0).equals(Proxy.ERRREPLY)) {
                System.out.println("Get failure");
                break;
            } else if (reply.getArg(0).equals(Proxy.EMPTYREPLY)) {
                System.out.println("Get no updates yet (wait a bit and try again)");
                Thread.sleep(1000);
            } else {
                ret = reply.getArg(1);
                break;
            }
        }

        return ret;
    }
}
