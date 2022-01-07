package org.t3.g11.proj2.keyserver;

import org.t3.g11.proj2.nuttela.GnuNode;
import org.t3.g11.proj2.nuttela.GnuNodeCMD;
import org.t3.g11.proj2.nuttela.message.GnuMessage;
import org.t3.g11.proj2.nuttela.message.QueryHitMessage;
import org.t3.g11.proj2.nuttela.message.QueryMessage;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.InetSocketAddress;

public class BootstrapGnuNode extends GnuNode {
    public static final int BOOTSTRAPID = 0;
    public static final InetSocketAddress NODEENDPOINT = new InetSocketAddress("localhost", 8080);
    public static final int MAX_NEIGH = 50;

    public BootstrapGnuNode() throws IOException {
        super(null, BootstrapGnuNode.BOOTSTRAPID, BootstrapGnuNode.NODEENDPOINT,
                BootstrapGnuNode.MAX_NEIGH);
    }

    @Override
    protected void bootstrap() {
        // do nothing
    }

    @Override
    protected void handleQuery(ObjectOutputStream oos, QueryMessage reqMsg) {
        try {
            GnuMessage ackMsg = GnuNodeCMD.ACK.getMessage(this.addr);
            oos.writeObject(ackMsg);
        } catch (Exception e) {
            System.err.println("Couldn't connect to query relayer peer");
            e.printStackTrace();
            return;
        }

        if (reqMsg.decreaseTtl() != 0) {
            reqMsg.setAddr(this.addr); // update source address
            this.query(reqMsg);
        }
    }

    @Override
    protected void handleQueryHit(QueryHitMessage reqMsg) {
        // do nothing
    }
}
