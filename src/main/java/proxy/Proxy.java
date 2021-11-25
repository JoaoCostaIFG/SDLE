package proxy;

import client.Publisher;
import client.Subscriber;
import message.IdentifiedMessage;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;
import proxy.TopicQueue.TopicQueue;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Proxy {
    public static final String OKREPLY = "OK";
    public static final String NEEDACK = "NEEDACK";
    public static final String ERRREPLY = "ERR";
    public static final String EMPTYREPLY = "EMPTY";
    // used for internal routing
    protected static final String SUBWORKER = "WSUB";
    protected static final String PUBWORKER = "WPUB";
    protected static final String STOPWORKER = "WSTOP";
    // frequency of the auto saving of messages
    private static final int SAVERATE = 50;
    private final String workersPushEndpoint = "inproc://workersPush";
    private final String workersPullEndpoint = "inproc://workersPull";
    private final Socket workersPush;
    private final Socket workersPull;
    private final List<Thread> workers;

    // client communication
    private final Socket pubSocket;
    private final Socket subSocket;
    private final Socket ctrlSocket;
    private final Map<String, TopicQueue> messageQueues;

    public Proxy(ZContext zctx, String ctrlendpoint) {
        this.ctrlSocket = zctx.createSocket(SocketType.PAIR);
        this.ctrlSocket.bind(ctrlendpoint);

        // import state
        this.messageQueues = this.importState();

        // yar har fiddle dee dee! (Simple Pirate Pattern)
        this.pubSocket = zctx.createSocket(SocketType.ROUTER);
        this.subSocket = zctx.createSocket(SocketType.ROUTER);

        // internal communication between threads to distribute work
        this.workersPush = zctx.createSocket(SocketType.PUSH);
        this.workersPush.bind(this.workersPushEndpoint);

        this.workersPull = zctx.createSocket(SocketType.PULL);
        this.workersPull.bind(this.workersPullEndpoint);

        int maxThreads = Runtime.getRuntime().availableProcessors() + 1;
        this.workers = new ArrayList<>();
        for (int i = 0; i < maxThreads; ++i) {
            ProxyWorker w = new ProxyWorker(zctx,
                    this.workersPushEndpoint,
                    this.workersPullEndpoint,
                    this);
            Thread t = new Thread(w);
            this.workers.add(t);
            t.start();
        }
    }

    private Map<String, TopicQueue> importState() {
        Map<String, TopicQueue> messageQueues = new ConcurrentHashMap<>();

        File stateFolder = new File("queues_state");
        File[] listOfFiles = stateFolder.listFiles();

        if (listOfFiles == null || listOfFiles.length < 1) return messageQueues;

        for (File file : stateFolder.listFiles()) {
            if (!file.isFile()) continue;

            String topicName = file.getName();
            try {
                FileInputStream state = new FileInputStream(file);
                ObjectInputStream ois = new ObjectInputStream(state);
                TopicQueue topicQueue = (TopicQueue) ois.readObject();
                messageQueues.put(topicName, topicQueue);
            } catch (Exception e) {
                e.printStackTrace();
                messageQueues = new ConcurrentHashMap<>();
            }
        }
        return messageQueues;
    }

    private void exportState() {
        File stateDirectory = new File("queues_state");
        if (!stateDirectory.exists()) stateDirectory.mkdir();

        for (Map.Entry<String, TopicQueue> entry : this.messageQueues.entrySet()) {
            TopicQueue queue = entry.getValue();
            if (!queue.isChanged()) continue;

            try {
                File stateFile = new File(stateDirectory.getName() + "/" + entry.getKey());
                FileOutputStream fos = new FileOutputStream(stateFile); // file named after topic
                ObjectOutputStream oos = new ObjectOutputStream(fos);
                oos.writeObject(entry.getValue());
                oos.close();
                fos.close();
                queue.resetChange();
            } catch (Exception e) {
                System.err.println("Topic " + entry.getKey() + " state saving failed");
            }
        }
    }

    public boolean bind(int pubPort, int subPort) {
        if (!this.pubSocket.bind("tcp://*:" + pubPort))
            return false;
        return this.subSocket.bind("tcp://*:" + subPort);
    }

    public void pollSockets(ZContext zctx) {
        ZMQ.Poller poller = zctx.createPoller(4);
        poller.register(this.pubSocket, ZMQ.Poller.POLLIN);
        poller.register(this.subSocket, ZMQ.Poller.POLLIN);
        poller.register(this.workersPull, ZMQ.Poller.POLLIN);
        poller.register(this.ctrlSocket, ZMQ.Poller.POLLIN);

        int msgCounter = 0;

        try {
            while (poller.poll() >= 0) {
                // publishers
                if (poller.pollin(0)) {
                    ++msgCounter;
                    ZMsg zMsg = ZMsg.recvMsg(this.pubSocket);
                    if (zMsg != null) {
                        zMsg.addLast(Proxy.PUBWORKER);
                        zMsg.send(this.workersPush);
                    }
                }
                // subscribers
                if (poller.pollin(1)) {
                    ++msgCounter;
                    ZMsg zMsg = ZMsg.recvMsg(this.subSocket);
                    if (zMsg != null) {
                        zMsg.addLast(Proxy.SUBWORKER);
                        zMsg.send(this.workersPush);
                    }
                }
                // workers
                if (poller.pollin(2)) {
                    ++msgCounter;
                    ZMsg zMsg = ZMsg.recvMsg(this.workersPull);
                    if (zMsg != null) {
                        String route = zMsg.removeLast().getString(StandardCharsets.UTF_8);
                        switch (route) {
                            case Proxy.PUBWORKER -> zMsg.send(this.pubSocket);
                            case Proxy.SUBWORKER -> zMsg.send(this.subSocket);
                        }
                    }
                }
                // ctrl socket (die)
                if (poller.pollin(3)) {
                    // we just die
                    ZMsg.recvMsg(this.ctrlSocket);
                    break;
                }
                // save program state to physical memory every SAVERATE handled messages
                if (msgCounter >= Proxy.SAVERATE) {
                    this.exportState();
                    msgCounter = 0;
                }
            }
        } catch (Exception ignored) {
        }

        this.destroy();
    }

    private void destroy() {
        // clean up
        System.out.println("Cleaning up");
        this.pubSocket.close();
        this.subSocket.close();

        this.exportState();
        System.out.println("Saved state");

        // clean up threads
        for (int i = 0; i < this.workers.size(); ++i) {
            ZMsg killMsg = new ZMsg();
            killMsg.add(Proxy.STOPWORKER);
            killMsg.send(this.workersPush);
        }
    }

    public void waitWorkers() {
        for (Thread t : this.workers) {
            try {
                //t.interrupt();
                t.join();
            } catch (InterruptedException ignored) {
            }
        }

        this.workersPull.close();
        this.workersPush.close();
        System.out.println("Finished waiting workers");
    }

    /**
     * PUT <topic> <update>
     * (success) -----> PUT OK
     */
    protected ZMsg handlePublisher(ZMsg zMsg) {
        IdentifiedMessage reqMsg = new IdentifiedMessage(zMsg);
        String topic = reqMsg.getArg(0);
        String update = reqMsg.getArg(1);
        String msgId = reqMsg.getArg(2);
        System.out.printf("Put: %s - %s\n", topic, update);
        // En-queue message. Silently ignore puts in case the message queue doesn't
        // exist (no one would get the message anyway).
        if (this.messageQueues.containsKey(topic)) {
            TopicQueue queue = this.messageQueues.get(topic);
            if (!queue.push(update, msgId, reqMsg.getIdentityStr()))
                return new IdentifiedMessage(reqMsg.getIdentity(), Publisher.PUTCMD,
                        Collections.singletonList(Proxy.ERRREPLY)).newZMsg();
        }

        return new IdentifiedMessage(reqMsg.getIdentity(), Publisher.PUTCMD,
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
            return new IdentifiedMessage(reqMsg.getIdentity(), Subscriber.SUBCMD,
                    Collections.singletonList(Proxy.OKREPLY)).newZMsg();
        }
        return new IdentifiedMessage(reqMsg.getIdentity(), Subscriber.SUBCMD,
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
        int requestedId = Integer.parseInt(reqMsg.getArg(1));

        // if queue doesn't exist => no one has subscribed (including you)
        if (!this.messageQueues.containsKey(topic)) {
            System.out.println("ola");
            return new IdentifiedMessage(reqMsg.getIdentity(), Subscriber.GETCMD,
                    Collections.singletonList(Proxy.ERRREPLY)).newZMsg();
        }
        TopicQueue queue = this.messageQueues.get(topic);

        String id = reqMsg.getIdentityStr();
        if (!queue.isSubbed(id)) {
            System.out.println("adeus");
            return new IdentifiedMessage(reqMsg.getIdentity(), Subscriber.GETCMD,
                    Collections.singletonList(Proxy.ERRREPLY)).newZMsg();
        }

        // check if there are updates for the given user
        int lastId = queue.hasUpdate(id);
        // discard messages
        while (lastId > 0 && lastId < requestedId) {
            queue.retrieveUpdate(id);
            lastId = queue.hasUpdate(id);
        }

        if (lastId > 0) {
            return new IdentifiedMessage(
                    reqMsg.getIdentity(),
                    Subscriber.GETCMD,
                    Arrays.asList(
                            Proxy.NEEDACK,
                            Integer.toString(lastId),
                            queue.getContent(id)
                    )).newZMsg();
        } else {
            return new IdentifiedMessage(
                    reqMsg.getIdentity(),
                    Subscriber.GETCMD,
                    Collections.singletonList(Proxy.EMPTYREPLY)).newZMsg();
        }
    }

    protected ZMsg handleAckGetCmd(IdentifiedMessage reqMsg) {
        String topic = reqMsg.getArg(0);
        TopicQueue queue = this.messageQueues.get(topic);
        String id = reqMsg.getIdentityStr();
        List<String> content = queue.retrieveUpdate(id);
        // send content
        content.add(0, Proxy.OKREPLY);
        return new IdentifiedMessage(reqMsg.getIdentity(), Subscriber.GETCMD,
                content).newZMsg();
    }

    protected ZMsg handleSubscriber(ZMsg zMsg) {
        IdentifiedMessage reqMsg = new IdentifiedMessage(zMsg);
        System.out.printf("Sub (%s)\n", reqMsg.getCmd());

        return switch (reqMsg.getCmd()) {
            case Subscriber.SUBCMD -> this.handleSubCmd(reqMsg);
            case Subscriber.UNSUBCMD -> this.handleUnsubCmd(reqMsg);
            case Subscriber.GETCMD -> this.handleGetCmd(reqMsg);
            case Subscriber.ACKGETCMD -> this.handleAckGetCmd(reqMsg);
            default -> new IdentifiedMessage(reqMsg.getIdentity(), Subscriber.GETCMD,
                    Collections.singletonList(Proxy.ERRREPLY)).newZMsg();
        };
    }
}
