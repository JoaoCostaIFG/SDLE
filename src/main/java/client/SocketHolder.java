package client;

import destroyable.Destroyable;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.nio.charset.StandardCharsets;

public abstract class SocketHolder implements Destroyable {
    private static final int RECEIVETIMEOUT = 10000;

    protected ZMQ.Socket socket;
    protected String id;
    protected String endpoint;
    protected ZContext zctx;

    protected void setUpSocket(){
        this.socket = zctx.createSocket(SocketType.REQ);
        this.socket.setIdentity(id.getBytes(StandardCharsets.UTF_8));
        this.socket.setReceiveTimeOut(SocketHolder.RECEIVETIMEOUT);
    }

    public SocketHolder(ZContext zctx, String id, String endpoint) {
        this.zctx = zctx;
        this.endpoint = endpoint;
        this.id = id;
        this.setUpSocket();
    }

    public void reconnect()
    {
        this.socket.close();
        this.setUpSocket();
        this.connect();
    }

    @Override
    public void destroy() {
        //this.socket.close();
        //System.err.println("Destroyed socket");
    }

    public boolean connect() {
        return this.socket.connect(this.endpoint);
    }

    public ZMsg receiveMsg()
    {
        ZMsg replyZMsg = ZMsg.recvMsg(this.socket);
        if(replyZMsg == null)
        {
            System.out.println("Couldn't receive message. Reconnecting socket.");
            this.reconnect();
        }
        return replyZMsg;
    }
}
