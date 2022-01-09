package org.t3.g11.proj2.nuttela.message.query;

import org.t3.g11.proj2.utils.Utils;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public abstract class Query implements Serializable {
    public static final int STARTTTL = 1024;
    public final static long ALLDATE = 0;

    private static volatile Integer seqNum = 0;

    protected final QueryType queryType;
    protected final InetSocketAddress sourceAddr;
    protected final int sourceId;
    protected final int guid;
    protected int neededHits;
    protected final String queryString;
    protected int ttl;

    public Query(InetSocketAddress sourceAddr, int sourceId, int neededHits, int ttl, String queryString, QueryType type) {
        this.sourceAddr = sourceAddr;
        this.sourceId = sourceId;
        this.neededHits = neededHits;
        this.ttl = ttl;
        this.queryString = queryString;
        this.queryType = type;

        synchronized (Query.class) {
            this.guid = Utils.IdFromName(sourceId + ":" + Query.seqNum);
            ++Query.seqNum;
        }
    }

    public Query(InetSocketAddress sourceAddr, int sourceId, int neededHits, String queryString, QueryType type) {
        this(sourceAddr, sourceId, neededHits, STARTTTL, queryString, type);
    }

    public InetAddress getSourceAddr() {
        return this.sourceAddr.getAddress();
    }

    public int getSourcePort() {
        return this.sourceAddr.getPort();
    }

    public int getSourceId() {
        return this.sourceId;
    }

    public String getQueryString() {
        return this.queryString;
    }

    public int getGuid() {
        return this.guid;
    }

    public void decreaseNeededHits(int amount) {
        this.neededHits -= amount;
    }

    public int getNeededHits() {
        return this.neededHits;
    }

    public int decreaseTtl() {
        return --this.ttl;
    }

    public int getSize() {
        return this.queryString.length();
    }

    @Override
    public String toString() {
        return String.format("Query(%d - %s)", this.guid, this.queryString);
    }

    public QueryType getQueryType() {
        return queryType;
    }
}
