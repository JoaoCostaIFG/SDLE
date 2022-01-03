import org.zeromq.*;

import java.nio.charset.StandardCharsets;

public class GnuNode {
    private final String id;
    private final ZContext zctx;
    private final ZMQ.Socket dealerSock;
    private final ZMQ.Socket routerSock;


    public GnuNode(String id, String dealerIP, String routerIP) {
        this.id = id;
        this.zctx = new ZContext();
        this.dealerSock = zctx.createSocket(SocketType.REQ);
        this.routerSock = zctx.createSocket(SocketType.ROUTER);


//        this.routerSock.setIdentity(id.getBytes(StandardCharsets.UTF_8));
        if(id.equals("1")) {
//            this.dealerSock.setIdentity(id.getBytes(StandardCharsets.UTF_8));
            this.dealerSock.connect("tcp://localhost:8080");
        }
        else
            this.routerSock.bind("tcp://localhost:8080");
    }

    public void send(String toSend) {
        ZMsg msg = new ZMsg();
        msg.add("Ola");
        System.out.println("sending");
        msg.send(this.dealerSock);
        System.out.println("sent");
    }

    public String receive() {
        System.out.println("waiting message");

        ZMQ.Poller plr = zctx.createPoller(1);
        plr.register(this.routerSock, ZMQ.Poller.POLLIN);

        while(plr.poll() >= 0){
            if(plr.pollin(0)){
                ZMsg msg = ZMsg.recvMsg(this.routerSock);
                System.out.println("message: " + msg.toString());
            }
        }
        return "";
    }
}
