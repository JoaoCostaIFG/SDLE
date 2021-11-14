import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;

import java.util.Arrays;
import java.util.Collections;

public class Proxy {
    public static final String OKREPLY = "OK";

    private final Socket pubSocket;
    private final Socket subSocket;

    public Proxy(ZContext zctx) {
        this.pubSocket = zctx.createSocket(SocketType.ROUTER);
        this.subSocket = zctx.createSocket(SocketType.ROUTER);
    }

    public void destroy() {
        this.pubSocket.close();
        this.subSocket.close();
    }

    public boolean bind(int pubPort, int subPort) {
        if (!this.pubSocket.bind("tcp://*:" + pubPort))
            return false;
        return this.subSocket.bind("tcp://*:" + subPort);
    }

    public void pollSockets(ZContext zctx) {
        ZMQ.Poller poller = zctx.createPoller(2);
        poller.register(this.pubSocket, ZMQ.Poller.POLLIN);
        poller.register(this.subSocket, ZMQ.Poller.POLLIN);

        for (; true; ) {
            poller.poll();
            // publishers
            if (poller.pollin(0)) {
                ZMsg zmsg = ZMsg.recvMsg(this.pubSocket);
                this.handlePublisher(zmsg);
            }
            // subscribers
            if (poller.pollin(1)) {
                ZMsg zmsg = ZMsg.recvMsg(this.subSocket);
                this.handleSubscriber(zmsg);
            }
        }
    }

    private void handlePublisher(ZMsg zmsg) {
        IdentifiedMessage reqMsg = new IdentifiedMessage(zmsg);
        System.out.printf("Pub (%s) %s - %s\n", reqMsg.getCmd(), reqMsg.getArg(0), reqMsg.getArg(1));

        // TODO queue stuff

        ZMsg replyZMsg = new IdentifiedMessage(reqMsg.getIdentity(),
                Publisher.PUTCMD,
                Collections.singletonList(Proxy.OKREPLY)).newZMsg();
        replyZMsg.send(this.pubSocket);
    }

    private void handleSubscriber(ZMsg zmsg) {
        IdentifiedMessage reqMsg = new IdentifiedMessage(zmsg);
        System.out.printf("Sub (%s) %s - %s\n", reqMsg.getCmd(), reqMsg.getArg(0), reqMsg.getArg(1));

        // TODO cmd stuff

        ZMsg replyZMsg = new IdentifiedMessage(reqMsg.getIdentity(),
                Subscriber.GETCMD,
                Arrays.asList(Proxy.OKREPLY, "tchu tcha")).newZMsg();
        replyZMsg.send(this.subSocket);
    }
}
