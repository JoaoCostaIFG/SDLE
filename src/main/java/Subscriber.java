import org.zeromq.ZContext;
import org.zeromq.ZMsg;

import java.util.Collections;

public class Subscriber extends SocketHolder {
    public static final String GETCMD = "GET";
    public static final String SUBCMD = "SUB";
    public static final String UNSUBCMD = "UNSUB";

    public Subscriber(ZContext zctx, String id) {
        super(zctx, id);
    }

    public boolean subscribe(String topic) {
        ZMsg reqZMsg = new UnidentifiedMessage(Subscriber.SUBCMD,
                Collections.singletonList(topic)).newZMsg();
        reqZMsg.send(this.socket);

        ZMsg replyZMsg = ZMsg.recvMsg(this.socket);
        UnidentifiedMessage reply = new UnidentifiedMessage(replyZMsg);
        if (reply.getCmd().equals(SUBCMD) &&
                reply.getArg(0).equals(Proxy.OKREPLY)) {
            System.out.println("Sub success");
            return true;
        } else {
            System.out.println("Sub failure");
            return false;
        }
    }

    public boolean unsubscribe(String topic) {
        ZMsg reqZMsg = new UnidentifiedMessage(Subscriber.UNSUBCMD,
                Collections.singletonList(topic)).newZMsg();
        reqZMsg.send(this.socket);

        ZMsg replyZMsg = ZMsg.recvMsg(this.socket);
        UnidentifiedMessage reply = new UnidentifiedMessage(replyZMsg);
        if (reply.getCmd().equals(UNSUBCMD) &&
                reply.getArg(0).equals(Proxy.OKREPLY)) {
            System.out.println("Unsub success");
            return true;
        } else {
            System.out.println("Unsub failure");
            return false;
        }
    }

    public String get(String topic) {
        String ret;

        while (true) {
            ZMsg reqZMsg = new UnidentifiedMessage(Subscriber.GETCMD,
                    Collections.singletonList(topic)).newZMsg();
            reqZMsg.send(this.socket);

            ZMsg replyZMsg = ZMsg.recvMsg(this.socket);
            UnidentifiedMessage reply = new UnidentifiedMessage(replyZMsg);
            if (!reply.getCmd().equals(GETCMD) || reply.getArg(0).equals(Proxy.ERRREPLY)) {
                System.out.println("Get failure");
                ret = null;
                break;
            } else if (reply.getArg(0).equals(Proxy.EMPTYREPLY)) {
                System.out.println("Get no updates yet (wait a bit and try again)");
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                ret = reply.getArg(1);
                System.out.printf("Get success: %s\n", ret);
                break;
            }
        }

        return ret;
    }
}
