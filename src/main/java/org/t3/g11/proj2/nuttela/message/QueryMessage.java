package org.t3.g11.proj2.nuttela.message;

import org.t3.g11.proj2.nuttela.GnuNodeCMD;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class QueryMessage extends GnuMessage {
    public static final int STARTTTL = 1024;

    private static volatile Integer seqNum = 0;

    private final Query query;
    private final int guid;

    private int ttl;

    public QueryMessage(InetSocketAddress addr, int id, Query query) {
        super(GnuNodeCMD.QUERY, addr);
        this.query = query;
        this.ttl = QueryMessage.STARTTTL;

        synchronized (QueryMessage.class) {
            this.guid = this.genGUID(id + ":" + QueryMessage.seqNum);
            ++QueryMessage.seqNum;
        }
    }


    public QueryMessage(InetSocketAddress addr, int id, String queryString) {
        this(addr, id, new Query(queryString));
    }

    private int genGUID(String guidStr) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(guidStr.getBytes());
            ByteBuffer wrapped = ByteBuffer.wrap(hash);
            return wrapped.getInt();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return -1;
        }
    }

    public int getGuid() {
        return this.guid;
    }

    public Query getQuery() {
        return this.query;
    }

    public int getTtl() {
        return this.ttl;
    }

    public int decreaseTtl() {
        return --this.ttl;
    }
}
