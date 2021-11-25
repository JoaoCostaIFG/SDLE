package client;

import message.UnidentifiedMessage;
import org.zeromq.ZContext;
import org.zeromq.ZMsg;
import proxy.Proxy;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Subscriber extends SocketHolder {
    public static final String GETCMD = "GET";
    public static final String ACKGETCMD = "ACKGET";
    public static final String SUBCMD = "SUB";
    public static final String UNSUBCMD = "UNSUB";

    private final Map<String, Integer> lastMsgId;

    public Subscriber(ZContext zctx, String id, String endpoint) {
        super(zctx, id, endpoint);
        lastMsgId = new HashMap<>();
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
        this.lastMsgId.put(topic,-1);
        return reply.getCmd().equals(SUBCMD) && reply.getArg(0).equals(Proxy.OKREPLY);
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
        this.lastMsgId.remove(topic);
        return reply.getCmd().equals(UNSUBCMD) && reply.getArg(0).equals(Proxy.OKREPLY);
    }

    public String get(String topic) throws Exception {
        String content = null;

        while (!Thread.currentThread().isInterrupted()) {
            // send request
            int lastId = this.lastMsgId.get(topic);
            ZMsg reqZMsg = new UnidentifiedMessage(Subscriber.GETCMD,
                    Arrays.asList(topic, String.valueOf(lastId + 1))).newZMsg();
            ZMsg replyZMsg = this.sendAndReply(reqZMsg); // Throws exception
            UnidentifiedMessage reply = new UnidentifiedMessage(replyZMsg);

            if (!reply.getCmd().equals(GETCMD) || reply.getArg(0).equals(Proxy.ERRREPLY)) {
                System.out.println("Get failure");
                break;
            }

            String replyType = reply.getArg(0);
            if (replyType.equals(Proxy.EMPTYREPLY)) {
                System.out.println("Get no updates yet (wait a bit and try again)");
                Thread.sleep(1000);
                continue;
            }

            if (replyType.equals(Proxy.NEEDACK)) {
                int id = Integer.parseInt(reply.getArg(1));
                content = reply.getArg(2);

                if (lastId < 0) lastId = id - 1;
                System.out.printf("Got ID %d\n", id);

                if (id < lastId + 1) {
                    System.err.println("Get found out of sequence message. Ignoring...");
                } else {
                    this.lastMsgId.put(topic, id);
                    ZMsg ackZMsg = new UnidentifiedMessage(Subscriber.ACKGETCMD,
                            Arrays.asList(topic, this.lastMsgId.toString())).newZMsg();
                    ZMsg contentZMsg = this.sendAndReply(ackZMsg); // Throws exception
                    // TODO recheck ID?
                    break;
                }
            }
        }

        return content;
    }
}
