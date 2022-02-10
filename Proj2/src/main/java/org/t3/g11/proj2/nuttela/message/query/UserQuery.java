package org.t3.g11.proj2.nuttela.message.query;

import java.net.InetSocketAddress;

public class UserQuery extends Query {
    private final long latestDate;

    public UserQuery(InetSocketAddress sourceAddr, int sourceId, int neededHits, int ttl, String queryString, long latestDate) {
        super(sourceAddr, sourceId, neededHits, ttl, queryString, QueryType.USER);
        this.latestDate = latestDate;
    }

    public UserQuery(InetSocketAddress sourceAddr, int sourceId, int neededHits, String queryString, long latestDate) {
        super(sourceAddr, sourceId, neededHits, queryString, QueryType.USER);
        this.latestDate = latestDate;
    }

    public long getLatestDate() {
        return this.latestDate;
    }

    @Override
    public int getSize() {
        return super.getSize() + 8; // 8 bytes from the latestDate
    }
}
