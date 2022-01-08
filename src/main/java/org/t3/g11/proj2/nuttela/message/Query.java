package org.t3.g11.proj2.nuttela.message;

import org.t3.g11.proj2.utils.Utils;

import java.io.Serializable;
import java.net.InetAddress;
import java.net.InetSocketAddress;

public class Query implements Serializable {
    public final static long ALLDATE = 0;

    private static volatile Integer seqNum = 0;

    private final InetSocketAddress sourceAddr;
    private final int sourceId;
    private final int guid;
    private final String queryString;
    private final long latestDate;

    public Query(InetSocketAddress sourceAddr, int sourceId, String queryString, long latestDate) {
        this.sourceAddr = sourceAddr;
        this.sourceId = sourceId;
        this.queryString = queryString;
        this.latestDate = latestDate;

        synchronized (Query.class) {
            this.guid = Utils.IdFromName(sourceId + ":" + Query.seqNum);
            ++Query.seqNum;
        }
    }

    public Query(InetSocketAddress sourceAddr, int sourceId, String queryString) {
        this(sourceAddr, sourceId, queryString, Query.ALLDATE);
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
}
