import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.util.Random;

public class Proj1 {
    ZContext zctx;
    String id;

    public Proj1(String id) {
        this.zctx = new ZContext();
        this.id = id;
    }

    public void destroy() {
        this.zctx.close();
    }

    public void publisher(String endpoint) {
        Publisher p = new Publisher(this.zctx, this.id);
        if (!p.connect(endpoint)) {
            p.destroy();
            return;
        }

        Random srandom = new Random(System.currentTimeMillis());
        for (; true; ) {
            int zipcode, temperature;
            zipcode = 10000 + srandom.nextInt(10000);
            temperature = srandom.nextInt(50) - 20 + 1;

            String topic = String.format("%05d", zipcode);
            p.put(topic, String.valueOf(temperature));
        }

        //p.destroy();
    }

    private void subscriber(String endpoint) {
        Subscriber s = new Subscriber(this.zctx, this.id);
        if (!s.connect(endpoint)) {
            s.destroy();
            return;
        }

        String topic = "10001";
        if (!s.subscribe(topic)) {
            System.out.println("Sub failure");
            return;
        }

        for (; true; ) {
            String update = s.get(topic);

            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        //s.destroy();
    }

    public void proxy(int pubPort, int subPort) {
        Proxy proxy = new Proxy(this.zctx);
        proxy.bind(pubPort, subPort);

        proxy.pollSockets(this.zctx);

        proxy.destroy();
    }

    public static void usage() {
        System.out.println("Usage: <id> <role>");
        System.exit(1);
    }

    public static void main(String[] args) {
        if (args.length < 2) usage();

        String id = args[0];

        switch (args[1]) {
            case "pub":
                new Proj1(id).publisher("tcp://localhost:5559");
                break;
            case "sub":
                new Proj1(id).subscriber("tcp://localhost:5560");
                break;
            case "proxy":
                new Proj1(id).proxy(5559, 5560);
                break;
            default:
                usage();
                break;
        }

    }
}