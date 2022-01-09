package org.t3.g11.proj2.nuttela.message;

import java.io.Serializable;
import java.util.Objects;

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
    public String toString() {
        return String.format("Result: %d - %d - %s", this.guid, this.date, this.author);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Result result = (Result) o;
        return guid == result.guid;
    }

    @Override
    public int hashCode() {
        return Objects.hash(guid);
    }
}
