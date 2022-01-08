package org.t3.g11.proj2.nuttela.message;

import org.t3.g11.proj2.nuttela.GnuNodeCMD;

import java.net.InetSocketAddress;

public class QueryMessage extends GnuIdMessage {
    private final Query query;

    public QueryMessage(InetSocketAddress addr, int id, Query query) {
        super(GnuNodeCMD.QUERY, addr, id);
        this.query = query;
    }

    public Query getQuery() {
        return this.query;
    }

    public int getGuid() {
        return this.query.getGuid();
    }

    public int decreaseTtl() {
        return this.query.decreaseTtl();
    }

    public void decreaseNeededHits(int amount) {
        this.query.decreaseNeededHits(amount);
    }

    public int getNeededHits() {
        return this.query.getNeededHits();
    }

    @Override
    public String toString() {
        return super.toString() + " " + this.query;
    }
}
