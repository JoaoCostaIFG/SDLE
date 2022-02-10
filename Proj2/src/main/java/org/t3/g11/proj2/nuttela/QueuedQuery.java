package org.t3.g11.proj2.nuttela;

import org.t3.g11.proj2.nuttela.message.query.Query;

import java.util.Comparator;

public class QueuedQuery implements Comparable<QueuedQuery> {
    private final Query query;
    private final double startTag;
    private final double finishTag;
    private final int hopId;

    public QueuedQuery(Query query, double virtTime, double prevFinishTag, int weight, int hopId) {
        this.query = query;
        this.startTag = Math.max(virtTime, prevFinishTag);
        this.finishTag = this.startTag + ((double) query.getSize() / weight);
        this.hopId = hopId;
    }

    public Query getQuery() {
        return query;
    }

    public double getStartTag() {
        return startTag;
    }

    public double getFinishTag() {
        return finishTag;
    }

    @Override
    public int compareTo(QueuedQuery o) {
        return Comparator.comparingDouble(QueuedQuery::getStartTag).compare(this, o);
    }

    public int getHopId() {
        return hopId;
    }
}
