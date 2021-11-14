import TopicQueue.TopicQueue;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Proxy {
    public static final String OKREPLY = "OK";
    public static final String ERRREPLY = "ERR";
    public static final String EMPTYREPLY = "EMPTY";

    private final Socket pubSocket;
    private final Socket subSocket;
    private final Map<String, TopicQueue> messageQueues;

    public Proxy(ZContext zctx) {
        this.pubSocket = zctx.createSocket(SocketType.ROUTER);
        this.subSocket = zctx.createSocket(SocketType.ROUTER);
        this.messageQueues = new HashMap<>();
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
                ZMsg zMsg = ZMsg.recvMsg(this.pubSocket);
                this.handlePublisher(zMsg);
            }
            // subscribers
            if (poller.pollin(1)) {
                ZMsg zMsg = ZMsg.recvMsg(this.subSocket);
                this.handleSubscriber(zMsg);
            }
        }
    }

    private void handlePublisher(ZMsg zMsg) {
        IdentifiedMessage reqMsg = new IdentifiedMessage(zMsg);
        String topic = reqMsg.getArg(0);
        String update = reqMsg.getArg(1);
        System.out.printf("Pub (%s) %s - %s\n", reqMsg.getCmd(), topic, update);

        // En-queue message. Silently ignore puts in case the message queue doesn't
        // exist (no one would get the message anyway).
        if (this.messageQueues.containsKey(topic)) {
            TopicQueue queue = this.messageQueues.get(topic);
            queue.push(update);
        }

        ZMsg replyZMsg = new IdentifiedMessage(reqMsg.getIdentity(),
                Publisher.PUTCMD,
                Collections.singletonList(Proxy.OKREPLY)).newZMsg();
        replyZMsg.send(this.pubSocket);
    }

    /**
     * SUB <topic>
     * (success)  -----> SUB OK
     * (repetida) -----> SUB ERROR
     *
     * @param reqMsg
     */
    private void handleSubCmd(IdentifiedMessage reqMsg) {
        String topic = reqMsg.getArg(0);

        // create queue if doesn't exist (worth storing messages now)
        TopicQueue queue;
        if (this.messageQueues.containsKey(topic)) {
            queue = this.messageQueues.get(topic);
        } else {
            queue = new TopicQueue();
            this.messageQueues.put(topic, queue);
        }

        String id = reqMsg.getIdentityStr();
        ZMsg replyZMsg;
        if (queue.sub(id)) {
            replyZMsg = new IdentifiedMessage(reqMsg.getIdentity(),
                    Subscriber.SUBCMD,
                    Collections.singletonList(Proxy.OKREPLY)).newZMsg();
        } else {
            replyZMsg = new IdentifiedMessage(reqMsg.getIdentity(),
                    Subscriber.SUBCMD,
                    Collections.singletonList(Proxy.ERRREPLY)).newZMsg();
        }

        replyZMsg.send(this.subSocket);
    }

    /**
     * UNSUB <topic>
     * (success)    -----> UNSUB OK
     * (not subbed) -----> UNSUB ERROR
     *
     * @param reqMsg
     */
    private void handleUnsubCmd(IdentifiedMessage reqMsg) {
        String topic = reqMsg.getArg(0);

        // if queue doesn't exist => no one has subscribed (including you)
        if (!this.messageQueues.containsKey(topic)) {
            ZMsg replyZMsg = new IdentifiedMessage(reqMsg.getIdentity(),
                    Subscriber.UNSUBCMD,
                    Collections.singletonList(Proxy.ERRREPLY)).newZMsg();
            replyZMsg.send(this.subSocket);
            return;
        }

        TopicQueue queue = this.messageQueues.get(topic);

        String id = reqMsg.getIdentityStr();
        ZMsg replyZMsg;
        if (queue.unsub(id)) {
            replyZMsg = new IdentifiedMessage(reqMsg.getIdentity(),
                    Subscriber.UNSUBCMD,
                    Collections.singletonList(Proxy.OKREPLY)).newZMsg();
        } else {
            replyZMsg = new IdentifiedMessage(reqMsg.getIdentity(),
                    Subscriber.UNSUBCMD,
                    Collections.singletonList(Proxy.ERRREPLY)).newZMsg();
        }

        replyZMsg.send(this.subSocket);
    }

    /**
     * GET <topic>
     * (success)    -----> GET OK <answer>
     * (not subbed) -----> GET ERROR
     *
     * @param reqMsg
     */
    private void handleGetCmd(IdentifiedMessage reqMsg) {
        String topic = reqMsg.getArg(0);

        // if queue doesn't exist => no one has subscribed (including you)
        if (!this.messageQueues.containsKey(topic)) {
            ZMsg replyZMsg = new IdentifiedMessage(reqMsg.getIdentity(),
                    Subscriber.GETCMD,
                    Collections.singletonList(Proxy.ERRREPLY)).newZMsg();
            replyZMsg.send(this.subSocket);
            return;
        }

        TopicQueue queue = this.messageQueues.get(topic);

        String id = reqMsg.getIdentityStr();
        if (!queue.isSubbed(id)) {
            ZMsg replyZMsg = new IdentifiedMessage(reqMsg.getIdentity(),
                    Subscriber.GETCMD,
                    Collections.singletonList(Proxy.ERRREPLY)).newZMsg();
            replyZMsg.send(this.subSocket);
            return;
        }

        // no content to send
        if (queue.size() == 0) {
            ZMsg replyZMsg = new IdentifiedMessage(reqMsg.getIdentity(),
                    Subscriber.GETCMD,
                    Collections.singletonList(Proxy.EMPTYREPLY)).newZMsg();
            replyZMsg.send(this.subSocket);
            return;
        }

        String content = queue.pop();
        ZMsg replyZMsg = new IdentifiedMessage(reqMsg.getIdentity(),
                Subscriber.GETCMD,
                Arrays.asList(Proxy.OKREPLY, content)).newZMsg();
        replyZMsg.send(this.subSocket);
    }

    private void handleSubscriber(ZMsg zMsg) {
        IdentifiedMessage reqMsg = new IdentifiedMessage(zMsg);
        System.out.printf("Sub (%s)\n", reqMsg.getCmd());

        switch (reqMsg.getCmd()) {
            case Subscriber.SUBCMD:
                this.handleSubCmd(reqMsg);
                break;
            case Subscriber.UNSUBCMD:
                this.handleUnsubCmd(reqMsg);
                break;
            case Subscriber.GETCMD:
                this.handleGetCmd(reqMsg);
                break;
            default:
                break;
        }

    }
}
