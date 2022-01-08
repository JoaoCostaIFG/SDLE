package org.t3.g11.proj2.nuttela.message;

import org.t3.g11.proj2.utils.Utils;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class Query implements Serializable {
    public static final int STARTTTL = 1024;
    public final static long ALLDATE = 0;

    private static volatile Integer seqNum = 0;

    private final InetSocketAddress sourceAddr;
    private final int sourceId;
    private final int guid;
    private int neededHits;
    private int ttl;

    private final String queryString;
    private final long latestDate;

    public Query(InetSocketAddress sourceAddr, int sourceId, int neededHits, int ttl, String queryString, long latestDate) {
        this.sourceAddr = sourceAddr;
        this.sourceId = sourceId;
        this.neededHits = neededHits;
        this.ttl = ttl;

        this.queryString = queryString;
        this.latestDate = latestDate;

        synchronized (Query.class) {
            this.guid = Utils.IdFromName(sourceId + ":" + Query.seqNum);
            ++Query.seqNum;
        }
    }

    public Query(InetSocketAddress sourceAddr, int sourceId, int neededHits, String queryString, long latestDate) {
        this(sourceAddr, sourceId, neededHits, Query.STARTTTL, queryString, latestDate);
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

    public int getGuid() {
        return this.guid;
    }

    public String getQueryString() {
        return this.queryString;
    }

    public long getLatestDate() {
        return this.latestDate;
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
        return this.queryString.length() + 8; // 8 bytes from the latestDate
    }

    @Override
    public String toString() {
        return String.format("Query(%d - %s, %d)", this.guid, this.queryString, this.latestDate);
    }
}
