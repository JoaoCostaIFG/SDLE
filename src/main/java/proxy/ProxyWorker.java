package proxy;

import org.zeromq.*;
import org.zeromq.ZMQ.Socket;

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
        try {
            while (!Thread.currentThread().isInterrupted()) {
                ZMsg zMsg;
                zMsg = ZMsg.recvMsg(this.pullSock);
                if (zMsg == null) continue;

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
        } catch (ZMQException ignored) {
        }

        this.pushSock.close();
        this.pullSock.close();
    }
}
