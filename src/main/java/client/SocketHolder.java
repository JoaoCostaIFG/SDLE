package client;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.nio.charset.StandardCharsets;

public abstract class SocketHolder {
    private static final int RECEIVETIMEOUT = 10000;

    protected ZMQ.Socket socket;
    protected String id;
    protected String endpoint;
    protected ZContext zctx;

    public SocketHolder(ZContext zctx, String id, String endpoint) {
        this.zctx = zctx;
        this.endpoint = endpoint;
        this.id = id;
        this.setUpSocket();
    }

    protected void setUpSocket() {
        this.socket = zctx.createSocket(SocketType.REQ);
        this.socket.setIdentity(id.getBytes(StandardCharsets.UTF_8));
        this.socket.setReceiveTimeOut(SocketHolder.RECEIVETIMEOUT);
    }

    public void reconnect() {
        this.socket.close();
        this.setUpSocket();
        this.connect();
    }

    public boolean connect() {
        return this.socket.connect(this.endpoint);
    }

    public ZMsg receiveMsg() {
        ZMsg replyZMsg = ZMsg.recvMsg(this.socket);
        if (replyZMsg == null) {
            System.err.println("Receive timed out. Reconnecting...");
            this.reconnect();
        }

        return replyZMsg;
    }
}
