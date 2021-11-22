package client;

import message.UnidentifiedMessage;
import org.zeromq.ZContext;
import org.zeromq.ZMsg;
import proxy.Proxy;

import java.util.Arrays;
import java.util.Collections;

public class Subscriber extends SocketHolder {
    public static final String GETCMD = "GET";
    public static final String ACKGETCMD = "ACKGET";
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

    public ZMsg sendAndReply(ZMsg reqZMsg) throws Exception {
        ZMsg replyZMsg = null;

        while (replyZMsg == null) {
            if (!reqZMsg.send(this.socket)) return null;
            replyZMsg = this.receiveMsg();
        }
        return replyZMsg;
    }

    public String get(String topic) throws Exception {
        String content = null;

        while (!Thread.currentThread().isInterrupted()) {
            // send request
            ZMsg reqZMsg = new UnidentifiedMessage(Subscriber.GETCMD,
                    Arrays.asList(topic, String.valueOf(this.lastMsgId + 1))).newZMsg();
            ZMsg replyZMsg = this.sendAndReply(reqZMsg); // Throws exception
            UnidentifiedMessage reply = new UnidentifiedMessage(replyZMsg);

            if (!reply.getCmd().equals(GETCMD) || reply.getArg(0).equals(Proxy.ERRREPLY)) {
                System.out.println("Get failure");
                break;
            } else {
                String replyType = reply.getArg(0);
                if (replyType.equals(Proxy.EMPTYREPLY)) {
                    System.out.println("Get no updates yet (wait a bit and try again)");
                    Thread.sleep(1000);
                } else if (replyType.equals(Proxy.NEEDACK)) {
                    int id = Integer.parseInt(reply.getArg(1));
                    if (this.lastMsgId < 0) this.lastMsgId = id - 1;
                    System.out.printf("Got ID %d\n", id);

                    if (id < this.lastMsgId + 1) {
                        System.err.println("Get found out of sequence message. Ignoring...");
                    } else {
                        this.lastMsgId = id;
                        ZMsg ackZMsg = new UnidentifiedMessage(Subscriber.ACKGETCMD,
                                Arrays.asList(topic, this.lastMsgId.toString())).newZMsg();
                        ZMsg contentZMsg = this.sendAndReply(ackZMsg); // Throws exception
                        UnidentifiedMessage contentMsg = new UnidentifiedMessage(contentZMsg);
                        // TODO recheck ID?
                        id = Integer.parseInt(contentMsg.getArg(1));
                        content = contentMsg.getArg(2);
                        break;
                    }
                }
            }
        }

        return content;
    }
}
