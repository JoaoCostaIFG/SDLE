package org.t3.g11.proj2.nuttela.message;

public class Query {
    public final static int ALLDATE = 0;

    private final String queryString;
    private final int latestDate;

    public Query(String queryString, int latestDate) {
        this.queryString = queryString;
        this.latestDate = latestDate;
    }

    public Query(String queryString) {
        this(queryString, Query.ALLDATE);
    }

    public String getQueryString() {
        return this.queryString;
    }

    public int getLatestDate() {
        return this.latestDate;
    }
}
