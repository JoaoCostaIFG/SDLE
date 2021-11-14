package publisher;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;

public class Publisher {
    private Socket socket;

    public Publisher(ZContext zctx) {
        this.socket = zctx.createSocket(SocketType.REQ);
    }

    public void destroy() {
        this.socket.close();
    }

    public boolean connect(String endpoint) {
        return this.socket.connect(endpoint);
    }

    public boolean put(String topic, String msg) {
        ZMsg zmsg = new ZMsg();
        zmsg.add(topic);
        zmsg.add(msg);

        if (!zmsg.send(this.socket))
            return false;
        zmsg = ZMsg.recvMsg(this.socket);

        System.out.printf("Put success: %s", zmsg.getLast());
        return true;
    }
}
