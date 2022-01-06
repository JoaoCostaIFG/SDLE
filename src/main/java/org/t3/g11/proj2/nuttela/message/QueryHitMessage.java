package org.t3.g11.proj2.nuttela.message;

import org.t3.g11.proj2.nuttela.GnuNodeCMD;

import java.net.InetSocketAddress;
import java.util.List;

public class QueryHitMessage extends GnuMessage {
    private final int guid;
    private final List<Result> results;

    public QueryHitMessage(InetSocketAddress addr, int guid, List<Result> results) {
        super(GnuNodeCMD.QUERYHIT, addr);
        this.guid = guid;
        this.results = results;
    }

    public int getGuid() {
        return this.guid;
    }

    public List<Result> getResultSet() {
        return this.results;
    }

}
