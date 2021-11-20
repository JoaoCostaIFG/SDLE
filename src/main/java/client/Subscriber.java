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

    private Integer lastMsgId = -1;

    public Subscriber(ZContext zctx, String id, String endpoint) {
        super(zctx, id, endpoint);
    }

    public boolean subscribe(String topic) throws Exception {
        ZMsg replyZMsg = null;

        ZMsg reqZMsg = new UnidentifiedMessage(Subscriber.SUBCMD,
                Collections.singletonList(topic)).newZMsg();
        while (replyZMsg == null) {
            if (!reqZMsg.send(this.socket)) return false;
            replyZMsg = this.receiveMsg();
        }

        UnidentifiedMessage reply = new UnidentifiedMessage(replyZMsg);
        return reply.getCmd().equals(SUBCMD) &&
                reply.getArg(0).equals(Proxy.OKREPLY);
    }

    public boolean unsubscribe(String topic) throws Exception {
        ZMsg replyZMsg = null;

        ZMsg reqZMsg = new UnidentifiedMessage(Subscriber.UNSUBCMD,
                Collections.singletonList(topic)).newZMsg();
        while (replyZMsg == null) {
            if (!reqZMsg.send(this.socket)) return false;
            replyZMsg = this.receiveMsg();
        }

        UnidentifiedMessage reply = new UnidentifiedMessage(replyZMsg);
        return reply.getCmd().equals(UNSUBCMD) &&
                reply.getArg(0).equals(Proxy.OKREPLY);
    }

    public String get(String topic) throws Exception {
        String content = null;
        Integer id = -1;

        while (!Thread.currentThread().isInterrupted()) {
            ZMsg reqZMsg = new UnidentifiedMessage(Subscriber.GETCMD,
                    Collections.singletonList(topic)).newZMsg();

            ZMsg replyZMsg = null;
            while (replyZMsg == null) {
                if (!reqZMsg.send(this.socket)) return null;
                replyZMsg = this.receiveMsg();
            }

            UnidentifiedMessage reply = new UnidentifiedMessage(replyZMsg);
            if (!reply.getCmd().equals(GETCMD) || reply.getArg(0).equals(Proxy.ERRREPLY)) {
                System.out.println("Get failure");
                break;
            } else if (reply.getArg(0).equals(Proxy.EMPTYREPLY)) {
                System.out.println("Get no updates yet (wait a bit and try again)");
                Thread.sleep(1000);
            } else {
                id = Integer.parseInt(reply.getArg(1));
                if (lastMsgId < 0) lastMsgId = id - 1;
                content = reply.getArg(2);

                System.out.printf("Got ID %d\n", id);

                if (id != lastMsgId + 1) {
                    System.err.println("Get found out of sequence message. Trying again...");
                } else {
                    lastMsgId = id;
                    break;
                }
            }
        }

        return content;
    }
}
