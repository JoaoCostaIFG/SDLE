package proxy;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZFrame;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;

public class ProxyWorker implements Runnable {
    private final Socket pushSock;
    private final Socket pullSock;
    private final Proxy parent;

    public ProxyWorker(ZContext zctx, String endpointIn, String endpointOut, Proxy parent) {
        this.pushSock = zctx.createSocket(SocketType.PUSH);
        this.pushSock.connect(endpointOut);

        this.pullSock = zctx.createSocket(SocketType.PULL);
        this.pullSock.connect(endpointIn);

        this.parent = parent;
    }

    @Override
    public void run() {
        while (!Thread.currentThread().isInterrupted()) {
            ZMsg zMsg;
            try {
                zMsg = ZMsg.recvMsg(this.pullSock);
            } catch (Exception e) {
                break;
            }
            if (zMsg == null) continue;

            ZMsg replyZMsg;
            ZFrame target = zMsg.removeLast();
            if (target.toString().equals(Proxy.STOPWORKER)) {
                System.err.println("Worker got kill signal. Quitting...");
                break;
            }
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
                    continue;
                }
            }

            try {
                replyZMsg.send(this.pushSock);
            } catch (Exception e) {
                break;
            }
        }

        this.pullSock.close();
        this.pushSock.close();
    }
}
