import TopicQueue.TopicQueue;
import org.zeromq.*;
import org.zeromq.ZMQ.Socket;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Proxy {
    public static final String OKREPLY = "OK";
    public static final String ERRREPLY = "ERR";
    public static final String EMPTYREPLY = "EMPTY";

    // used for internal routing
    private static final String SUBWORKER = "WSUB";
    private static final String PUBWORKER = "WPUB";

    private final Map<String, TopicQueue> messageQueues;

    private final ZContext zctx;

    private final Socket pubSocket;
    private final Socket subSocket;

    private final String workersEndpoint = "inproc://workers";
    private final Socket workersDealer;

    private final ExecutorService pubThreadPool;
    private final ExecutorService subThreadPool;

    public Proxy(ZContext zctx) {
        this.zctx = zctx;
        this.messageQueues = new ConcurrentHashMap<>();

        // yar har fiddle dee dee! (Simple Pirate Pattern)
        this.pubSocket = zctx.createSocket(SocketType.ROUTER);
        this.subSocket = zctx.createSocket(SocketType.ROUTER);

        // internal communication between threads to distribute work
        this.workersDealer = zctx.createSocket(SocketType.DEALER);
        this.workersDealer.bind(this.workersEndpoint);

        int maxThreads = Runtime.getRuntime().availableProcessors() + 1;
        this.pubThreadPool = Executors.newFixedThreadPool(maxThreads / 2);
        this.subThreadPool = Executors.newFixedThreadPool((int) Math.ceil(maxThreads / 2.0));
    }

    public void destroy() {
        this.pubSocket.close();
        this.subSocket.close();

        this.pubThreadPool.shutdownNow();
        this.subThreadPool.shutdownNow();
    }

    public boolean bind(int pubPort, int subPort) {
        if (!this.pubSocket.bind("tcp://*:" + pubPort))
            return false;
        return this.subSocket.bind("tcp://*:" + subPort);
    }

    public void pollSockets(ZContext zctx) {
        ZMQ.Poller poller = zctx.createPoller(3);
        poller.register(this.pubSocket, ZMQ.Poller.POLLIN);
        poller.register(this.subSocket, ZMQ.Poller.POLLIN);
        poller.register(this.workersDealer, ZMQ.Poller.POLLIN);

        while (true) {
            poller.poll();
            // publishers
            if (poller.pollin(0)) {
                ZMsg zMsg = ZMsg.recvMsg(this.pubSocket);
                //zMsg.addLast(Proxy.PUBWORKER);
                this.pubThreadPool.execute(this::spawnWorker);
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                zMsg.send(this.workersDealer);
            }
            // subscribers
            if (poller.pollin(1)) {
                ZMsg zMsg = ZMsg.recvMsg(this.subSocket);
                zMsg.addLast(Proxy.SUBWORKER);
                this.subThreadPool.execute(this::spawnWorker);
                zMsg.send(this.workersDealer);
            }
            // workers
            if (poller.pollin(2)) {
                ZMsg zMsg = ZMsg.recvMsg(this.workersDealer);
                zMsg.send(this.pubSocket);
                /*
                String route = zMsg.removeLast().getString(StandardCharsets.UTF_8);
                switch (route) {
                    case Proxy.PUBWORKER -> zMsg.send(this.pubSocket);
                    case Proxy.SUBWORKER -> zMsg.send(this.subSocket);
                }
                 */
            }
        }
    }

    private void spawnWorker() {
        Socket repSocket = this.zctx.createSocket(SocketType.REP);
        repSocket.setHWM(1);
        repSocket.connect(this.workersEndpoint);
        ZMsg zMsg = ZMsg.recvMsg(repSocket);

        /*
        ZMsg replyZMsg;
        ZFrame target = zMsg.removeLast();
        switch (target.toString()) {
            case Proxy.PUBWORKER -> {
                replyZMsg = this.handlePublisher(zMsg);
                replyZMsg.addLast(target);
            }
            case Proxy.SUBWORKER -> {
                replyZMsg = this.handleSubscriber(zMsg);
                replyZMsg.addLast(target);
            }
            default -> {
                System.out.println("Worker found some weird stuff");
                return;
            }
        }
         */

        ZMsg replyZMsg = this.handlePublisher(zMsg);

        replyZMsg.send(repSocket);
        repSocket.close();
    }

    /**
     * PUT <topic> <update>
     * (success) -----> PUT OK
     */
    private ZMsg handlePublisher(ZMsg zMsg) {
        UnidentifiedMessage reqMsg = new UnidentifiedMessage(zMsg);
        String topic = reqMsg.getArg(0);
        String update = reqMsg.getArg(1);
        System.out.printf("Pub (%s) %s - %s\n", reqMsg.getCmd(), topic, update);

        // En-queue message. Silently ignore puts in case the message queue doesn't
        // exist (no one would get the message anyway).
        if (this.messageQueues.containsKey(topic)) {
            TopicQueue queue = this.messageQueues.get(topic);
            queue.push(update);
        }

        return new UnidentifiedMessage(Publisher.PUTCMD,
                Collections.singletonList(Proxy.OKREPLY)).newZMsg();
    }

    /**
     * SUB <topic>
     * (success)  -----> SUB OK
     * (repetida) -----> SUB ERROR
     */
    private ZMsg handleSubCmd(IdentifiedMessage reqMsg) {
        String topic = reqMsg.getArg(0);
        String id = reqMsg.getIdentityStr();

        // create queue if it doesn't exist (worth storing messages now)
        boolean subSuccess;
        synchronized (this.messageQueues) {
            TopicQueue queue;
            if (this.messageQueues.containsKey(topic)) {
                queue = this.messageQueues.get(topic);
            } else {
                queue = new TopicQueue();
                this.messageQueues.put(topic, queue);
            }
            subSuccess = queue.sub(id);
        }

        if (subSuccess) {
            return new UnidentifiedMessage(Subscriber.SUBCMD,
                    Collections.singletonList(Proxy.OKREPLY)).newZMsg();
        }
        return new UnidentifiedMessage(Subscriber.SUBCMD,
                Collections.singletonList(Proxy.ERRREPLY)).newZMsg();
    }

    /**
     * UNSUB <topic>
     * (success)    -----> UNSUB OK
     * (not subbed) -----> UNSUB ERROR
     */
    private ZMsg handleUnsubCmd(IdentifiedMessage reqMsg) {
        String topic = reqMsg.getArg(0);

        // if queue doesn't exist => no one has subscribed (including you)
        if (!this.messageQueues.containsKey(topic)) {
            return new IdentifiedMessage(reqMsg.getIdentity(),
                    Subscriber.UNSUBCMD,
                    Collections.singletonList(Proxy.ERRREPLY)).newZMsg();
        }

        TopicQueue queue = this.messageQueues.get(topic);

        String id = reqMsg.getIdentityStr();
        if (queue.unsub(id)) {
            return new IdentifiedMessage(reqMsg.getIdentity(),
                    Subscriber.UNSUBCMD,
                    Collections.singletonList(Proxy.OKREPLY)).newZMsg();
        } else {
            return new IdentifiedMessage(reqMsg.getIdentity(),
                    Subscriber.UNSUBCMD,
                    Collections.singletonList(Proxy.ERRREPLY)).newZMsg();
        }
    }

    /**
     * GET <topic>
     * (success)    -----> GET OK <answer>
     * (not subbed) -----> GET ERROR
     */
    private ZMsg handleGetCmd(IdentifiedMessage reqMsg) {
        String topic = reqMsg.getArg(0);

        // if queue doesn't exist => no one has subscribed (including you)
        if (!this.messageQueues.containsKey(topic)) {
            return new UnidentifiedMessage(Subscriber.GETCMD,
                    Collections.singletonList(Proxy.ERRREPLY)).newZMsg();
        }

        TopicQueue queue = this.messageQueues.get(topic);

        // TODO is a synchronized block needed for the next 2 queue usages

        String id = reqMsg.getIdentityStr();
        if (!queue.isSubbed(id)) {
            return new UnidentifiedMessage(Subscriber.GETCMD,
                    Collections.singletonList(Proxy.ERRREPLY)).newZMsg();
        }

        // no content to send
        String content = queue.retrieveUpdate(id);
        if (content == null) {
            return new UnidentifiedMessage(
                    Subscriber.GETCMD,
                    Collections.singletonList(Proxy.EMPTYREPLY)).newZMsg();
        }

        return new UnidentifiedMessage(Subscriber.GETCMD,
                Arrays.asList(Proxy.OKREPLY, content)).newZMsg();
    }

    private ZMsg handleSubscriber(ZMsg zMsg) {
        IdentifiedMessage reqMsg = new IdentifiedMessage(zMsg);
        System.out.printf("Sub (%s)\n", reqMsg.getCmd());

        return switch (reqMsg.getCmd()) {
            case Subscriber.SUBCMD -> this.handleSubCmd(reqMsg);
            case Subscriber.UNSUBCMD -> this.handleUnsubCmd(reqMsg);
            case Subscriber.GETCMD -> this.handleGetCmd(reqMsg);
            default -> new UnidentifiedMessage(Subscriber.GETCMD,
                    Collections.singletonList(Proxy.ERRREPLY)).newZMsg();
        };
    }
}
