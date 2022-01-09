package org.t3.g11.proj2.nuttela.message;

import java.io.Serializable;

public class Result implements Serializable {
    public final int guid;
    public final long date;
    public final String ciphered;
    public final String author;

    public Result(int guid, long date, String ciphered, String author) {
        this.guid = guid;
        this.date = date;
        this.ciphered = ciphered;
        this.author = author;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(this.guid);
    }
}
