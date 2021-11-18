package client;

import destroyable.Destroyable;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;

import java.nio.charset.StandardCharsets;

public abstract class SocketHolder implements Destroyable {
    protected ZMQ.Socket socket;

    public SocketHolder(ZContext zctx, String id) {
        this.socket = zctx.createSocket(SocketType.REQ);
        this.socket.setIdentity(id.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public void destroy() {
        //this.socket.close();
        //System.err.println("Destroyed socket");
    }

    public boolean connect(String endpoint) {
        return this.socket.connect(endpoint);
    }
}
