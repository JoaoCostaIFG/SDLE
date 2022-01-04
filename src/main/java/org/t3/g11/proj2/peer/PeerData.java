package org.t3.g11.proj2.peer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class PeerData {
    private final Connection connection;
    private final String username;

    public PeerData(String username) throws SQLException {
        this.username = username;
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + username + ".db");
    }

    public void reInitDB() throws SQLException {
        Statement stmt = this.connection.createStatement();
        stmt.execute("DROP TABLE IF EXISTS User");
        stmt.execute("""
                CREATE TABLE User (
                  user_id INTEGER PRIMARY KEY ASC,
                  user_username TEXT UNIQUE NOT NULL,
                  user_pubkey TEXT NOT NULL
                )
                """);
        stmt.execute("DROP TABLE IF EXISTS Post");
        stmt.execute("""
                CREATE TABLE Post (
                  post_id INTEGER PRIMARY KEY ASC,
                  post_date INTEGER NOT NULL DEFAULT (strftime('%s', CURRENT_TIMESTAMP)),
                  post_ciphered TEXT NOT NULL,
                  post_content TEXT NOT NULL,
                  user_id INTEGER NOT NULL,
                  FOREIGN KEY(user_id) REFERENCES User
                )
                """);
        stmt.close();
    }
}
