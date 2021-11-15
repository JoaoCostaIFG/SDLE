import org.zeromq.ZContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Proj1 {
    ZContext zctx;
    String id;
    List<Destroyable> destroyables = new ArrayList<>();

    public Proj1(String id) {
        this.zctx = new ZContext();
        this.id = id;
    }

    public static void usage() {
        System.out.println("Usage: <id> <role>");
        System.exit(1);
    }

    public static void main(String[] args) {
        if (args.length < 2) usage();

        String id = args[0];
        Proj1 p1 = new Proj1(id);
        Runtime.getRuntime().addShutdownHook(new Thread(p1::destroy));

        switch (args[1]) {
            case "pub":
                p1.publisher("tcp://localhost:5559");
                break;
            case "sub":
                p1.subscriber("tcp://localhost:5560");
                break;
            case "proxy":
                p1.proxy(5559, 5560);
                break;
            default:
                usage();
                break;
        }

    }

    public void destroy() {
        this.zctx.close();

        for(Destroyable d : this.destroyables)
        {
            d.destroy();
        }
    }

    public void publisher(String endpoint) {
        Publisher p = new Publisher(this.zctx, this.id);
        this.destroyables.add(p);

        if (!p.connect(endpoint)) {
            p.destroy();
            return;
        }

        Random srandom = new Random(System.currentTimeMillis());
        for (; true; ) {
            int zipcode, temperature;
            zipcode = 10000 + srandom.nextInt(10);
            temperature = srandom.nextInt(50) - 20 + 1;

            String topic = String.format("%05d", zipcode);
            p.put(topic, String.valueOf(temperature));

        }

    }

    private void subscriber(String endpoint) {
        Subscriber s = new Subscriber(this.zctx, this.id);
        this.destroyables.add(s);


        if (!s.connect(endpoint)) {
            s.destroy();
            return;
        }

        String topic = "10001";
        s.subscribe(topic);


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
        this.destroyables.add(proxy);

        proxy.bind(pubPort, subPort);

        proxy.pollSockets(this.zctx);

    }
}