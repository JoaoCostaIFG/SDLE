package proxy;

import org.zeromq.*;
import org.zeromq.ZMQ.Socket;

public class Proxy {
    private Socket pubSocket, subSocket;

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
                this.handlePublish(zmsg);
            }
            // subscribers
            if (poller.pollin(1)) {

            }
        }
    }

    private void handlePublish(ZMsg zmsg) {
        ZFrame identity = zmsg.unwrap();

        ZFrame topic = zmsg.getFirst();
        ZFrame update = zmsg.getLast();
        System.out.printf("Pub %s - %s\n", topic, update);

        ZMsg replyZMsg = new ZMsg();
        replyZMsg.wrap(identity);
        replyZMsg.add("OK SUB");
        replyZMsg.send(this.pubSocket);
    }
}
