package org.t3.g11.proj2.nuttela.message;

import org.t3.g11.proj2.nuttela.GnuNodeCMD;

import java.net.InetSocketAddress;

public class QueryMessage extends GnuIdMessage {
    public static final int STARTTTL = 5;// 1024;

    private final Query query;

    private int ttl;

    public QueryMessage(InetSocketAddress addr, int id, int ttl, Query query) {
        super(GnuNodeCMD.QUERY, addr, id);
        this.query = query;
        this.ttl = ttl;
    }

    public QueryMessage(InetSocketAddress addr, int id, Query query) {
        this(addr, id, QueryMessage.STARTTTL, query);
    }

    public Query getQuery() {
        return this.query;
    }

    public int getGuid() {
        return this.query.getGuid();
    }

    public int getTtl() {
        return this.ttl;
    }

    public int decreaseTtl() {
        return --this.ttl;
    }
}
