import client.Publisher;
import client.Subscriber;
import destroyable.Destroyable;
import org.zeromq.ZContext;
import proxy.Proxy;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Proj1 extends Thread {
    private static final int PUBPORT = 5559;
    private static final int SUBPORT = 5560;
    private static final String PUBENDPOINT = "tcp://localhost:" + PUBPORT;
    private static final String SUBENDPOINT = "tcp://localhost:" + SUBPORT;

    ZContext zctx;
    String id;
    List<Destroyable> destroyables = new ArrayList<>();

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
        Runtime.getRuntime().addShutdownHook(p1);

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

    /**
     * This is a work-around for Java's shutdown hook. The ZContext needs
     * to be destroyed on the Thread it was created on.
     */
    @Override
    public void start() {
        System.err.println("Shutting down");

        this.zctx.destroy();
        System.err.println("Closed context");

        System.err.println("Waiting destroyables");
        for (Destroyable d : this.destroyables) {
            d.destroy();
        }
    }

    public void doput(String endpoint, String topic, int n) {
        Publisher p = new Publisher(this.zctx, this.id, endpoint);
        this.destroyables.add(p);

        if (!p.connect()) {
            System.err.printf("Failed connection to proxy: [endpoint=%s]\n", endpoint);
            return;
        }

        Random srandom = new Random(System.currentTimeMillis());
        for (int i = 0; i < n; ++i) {
            int temperature = srandom.nextInt(50) - 20;
            try {
                if (!p.put(topic, String.valueOf(temperature))) {
                    System.err.println("Put failed. Try again...");
                } else {
                    System.out.printf("Published a topic update: [topic=%s], [update=%s]\n", topic, temperature);
                }
            } catch (Exception e) {
                System.err.println("Socket exception. Closing..");
                return;
            }
        }
    }

    private void doget(String endpoint, String topic, int n) {
        Subscriber s = new Subscriber(this.zctx, this.id, endpoint);
        this.destroyables.add(s);

        if (!s.connect()) {
            System.err.printf("Failed connection to endpoint: [endpoint=%s]\n", endpoint);
            return;
        }

        try {
            if (!s.subscribe(topic)) {
                System.err.printf("Failed to sub topic (already subbed): [topic=%s]\n", topic);
            }
        } catch (Exception e) {
            return;
        }

        for (int i = 0; i < n || n < 0; ++i) {
            try {
                String update = s.get(topic);
                if (update == null) {
                    System.err.println("Get failed");
                } else {
                    System.out.printf("Got topic update: [topic=%s], [update=%s]\n", topic, update);
                }
            } catch (Exception e) {
                return;
            }
        }

        try {
            if (!s.unsubscribe(topic)) {
                System.err.printf("Failed to unsub topic: [topic=%s]\n", topic);
            }
        } catch (Exception ignored) {
        }
    }

    private void doget(String endpoint, String topic) {
        this.doget(endpoint, topic, -1);
    }

    public void proxy(int pubPort, int subPort) {
        Proxy proxy = new Proxy(this.zctx);
        this.destroyables.add(proxy);

        if (!proxy.bind(pubPort, subPort)) {
            System.err.println("Bind failed");
            return;
        }

        proxy.pollSockets(this.zctx);
    }
}