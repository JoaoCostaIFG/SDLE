import org.zeromq.ZContext;
import org.zeromq.ZMsg;

import java.util.Arrays;

public class Publisher extends SocketHolder {

    public static final String PUTCMD = "PUT";

    public Publisher(ZContext zctx, String id) {
        super(zctx, id);
    }

    public boolean put(String topic, String content) {
        ZMsg zmsg = new UnidentifiedMessage(Publisher.PUTCMD, Arrays.asList(topic, content)).newZMsg();
        if (!zmsg.send(this.socket)) {
            System.out.println("Sending failure");
            return false;
        }
        zmsg.destroy();

        ZMsg replyZMsg = ZMsg.recvMsg(this.socket);
        UnidentifiedMessage reply = new UnidentifiedMessage(replyZMsg);

        if (reply.getCmd().equals(Publisher.PUTCMD) &&
                reply.getArg(0).equals(Proxy.OKREPLY)) {
            System.out.println("Put success");
            return true;
        } else {
            System.out.println("Put failure");
            return false;
        }
    }
}
