package org.t3.g11.proj2.nuttela.message;

import java.io.Serializable;
import java.net.InetSocketAddress;

public class Query implements Serializable {
    public final static int ALLDATE = 0;

    private final InetSocketAddress sourceAddr;
    private final String queryString;
    private final int latestDate;

    public Query(InetSocketAddress sourceAddr, String queryString, int latestDate) {
        this.sourceAddr = sourceAddr;
        this.queryString = queryString;
        this.latestDate = latestDate;
    }

    public Query(InetSocketAddress sourceAddr, String queryString) {
        this(sourceAddr, queryString, Query.ALLDATE);
    }

    public String getQueryString() {
        return this.queryString;
    }

    public int getLatestDate() {
        return this.latestDate;
    }
}
