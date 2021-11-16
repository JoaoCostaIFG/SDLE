package proxy;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;

public class ProxyWorker implements Runnable {

    Socket pushSock;
    Socket pullSock;

    Proxy parent;

    public ProxyWorker(ZContext zctx, String endpointIn, String endpointOut, Proxy parent) {
        this.pushSock = zctx.createSocket(SocketType.PUSH);
        this.pushSock.connect(endpointOut);

        this.pullSock = zctx.createSocket(SocketType.PULL);
        this.pullSock.connect(endpointIn);

        this.parent = parent;
    }


    @Override
    public void run() {
        while (!Thread.interrupted()) {
            ZMsg zMsg = ZMsg.recvMsg(this.pullSock);

            System.out.println(zMsg);
            ZMsg replyZMsg;
            ZFrame target = zMsg.removeLast();
            switch (target.toString()) {
                case Proxy.PUBWORKER -> {
                    replyZMsg = this.parent.handlePublisher(zMsg);
                    replyZMsg.addLast(target);
                }
                case Proxy.SUBWORKER -> {
                    replyZMsg = this.parent.handleSubscriber(zMsg);
                    replyZMsg.addLast(target);
                }
                default -> {
                    System.out.println("Worker found some weird stuff");
                    return;
                }
            }

            replyZMsg.send(this.pushSock);
        }
        this.pushSock.close();
        this.pullSock.close();

    }


}
