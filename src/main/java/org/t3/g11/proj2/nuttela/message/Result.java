package org.t3.g11.proj2.nuttela.message;

public class Result {
    public final int id;
    public final int date;
    public final String ciphered;
    public final int author;

    public Result(int id, int date, String ciphered, int author) {
        this.id = id;
        this.date = date;
        this.ciphered = ciphered;
        this.author = author;
    }
}
