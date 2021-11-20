import client.Publisher;
import client.Subscriber;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import proxy.Proxy;

import java.util.Random;

public class Proj1 {
    private static final int PUBPORT = 5559;
    private static final int SUBPORT = 5560;
    private static final String PUBENDPOINT = "tcp://localhost:" + PUBPORT;
    private static final String SUBENDPOINT = "tcp://localhost:" + SUBPORT;
    private static final String CTRLENDPOINT = "inproc://control";

    ZContext zctx;
    String id;

    public Proj1(String id) {
        this.zctx = new ZContext();
        this.id = id;
    }

    public static void usage() {
        System.out.println("Usage: <id> <put|get|proxy> [arg1 [arg2]]");
        System.exit(1);
    }

    public static void main(String[] args) {
        if (args.length < 2) usage();

        String id = args[0];
        Proj1 p1 = new Proj1(id);

        switch (args[1]) {
            case "put":
                if (args.length != 4) usage();
                p1.doput(Proj1.PUBENDPOINT, args[2], Integer.parseInt(args[3]));
                break;
            case "get":
                if (args.length == 3) {
                    p1.doget(Proj1.SUBENDPOINT, args[2]);
                } else if (args.length == 4) {
                    p1.doget(Proj1.SUBENDPOINT, args[2], Integer.parseInt(args[3]));
                } else {
                    usage();
                }
                break;
            case "proxy":
                p1.proxy(5559, 5560);
                break;
            default:
                usage();
                break;
        }

        System.exit(0);
    }

    public void doput(String endpoint, String topic, int n) {
        Publisher p = new Publisher(this.zctx, this.id, endpoint);

        if (!p.connect()) {
            System.err.printf("Failed connection to proxy: [endpoint=%s]\n", endpoint);
            return;
        }

        Random srandom = new Random(System.currentTimeMillis());
        for (int i = 0; i < n; ++i) {
            int temperature = srandom.nextInt(50) - 20;
            try {
                if (!p.put(topic, String.valueOf(temperature))) {
                    System.err.printf("Put failed: [topic=%s], [update=%s]\n", topic, temperature);
                } else {
                    System.out.printf("Published a topic update: [topic=%s], [update=%s]\n", topic, temperature);
                }
            } catch (Exception e) {
                // context closed => leave
                break;
            }
        }
    }

    private void doget(String endpoint, String topic, int n) {
        Subscriber s = new Subscriber(this.zctx, this.id, endpoint);

        if (!s.connect()) {
            System.err.printf("Failed connection to endpoint: [endpoint=%s]\n", endpoint);
            return;
        }

        try {
            if (!s.subscribe(topic)) {
                System.err.printf("Failed to sub topic (already subbed): [topic=%s]\n", topic);
            } else {
                System.err.printf("Subscribed to topic: [topic=%s]\n", topic);
            }
        } catch (Exception e) {
            // context closed => leave
            return;
        }

        for (int i = 0; i < n || n < 0; ++i) {
            try {
                String update = s.get(topic);
                if (update == null) {
                    System.err.printf("Get failed: [topic=%s]\n", topic);
                } else {
                    System.out.printf("Got topic update: [topic=%s], [update=%s]\n", topic, update);
                }
            } catch (Exception e) {
                // context closed => leave
                return;
            }
        }

        try {
            if (!s.unsubscribe(topic)) {
                System.err.printf("Failed to unsub topic: [topic=%s]\n", topic);
            } else {
                System.err.printf("Unsubscribed from topic: [topic=%s]\n", topic);
            }
        } catch (Exception e) {
            // context closed => leave
            return;
        }
    }

    private void doget(String endpoint, String topic) {
        this.doget(endpoint, topic, -1);
    }

    public void proxy(int pubPort, int subPort) {
        Proxy proxy = new Proxy(this.zctx, Proj1.CTRLENDPOINT);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ZMQ.Socket ctrlSocket = this.zctx.createSocket(SocketType.PAIR);
            ctrlSocket.connect(Proj1.CTRLENDPOINT);
            ctrlSocket.send("", 0);

            proxy.waitWorkers();
        }));

        System.out.println("Proxy ready!");
        if (!proxy.bind(pubPort, subPort)) {
            System.err.printf("Bind failed: [pubPort=%d], [subPort=%d]\n", pubPort, subPort);
            return;
        }

        proxy.pollSockets(this.zctx);
        System.err.println("Proxy done");
    }
}