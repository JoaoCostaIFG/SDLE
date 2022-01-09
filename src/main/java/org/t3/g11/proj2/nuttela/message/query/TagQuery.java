package org.t3.g11.proj2.nuttela.message.query;

import java.net.InetSocketAddress;

public class TagQuery extends Query {
    public TagQuery(InetSocketAddress sourceAddr, int sourceId, int neededHits, int ttl, String queryString) {
        super(sourceAddr, sourceId, neededHits, ttl, queryString, QueryType.TAG);
    }

    public TagQuery(InetSocketAddress sourceAddr, int sourceId, int neededHits, String queryString) {
        super(sourceAddr, sourceId, neededHits, queryString, QueryType.TAG);
    }
}
