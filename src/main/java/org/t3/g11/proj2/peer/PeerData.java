package org.t3.g11.proj2.peer;

import java.sql.*;
import java.util.*;

public class PeerData {
    private final Connection connection;
    private final String username;

    public PeerData(String username) throws SQLException {
        this.username = username;
        this.connection = DriverManager.getConnection("jdbc:sqlite:" + username + ".db");
    }

    public String getSelfUsername() {
        return this.username;
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
        stmt.execute("CREATE INDEX post_idx ON Post(post_date)");
        stmt.close();
    }

    public void addUser(String username, String pubkey) throws SQLException {
        PreparedStatement pstmt = this.connection.prepareStatement("INSERT INTO User(user_username, user_pubkey) VALUES(?, ?)");
        pstmt.setString(1, username);
        pstmt.setString(2, pubkey);
        pstmt.executeUpdate();
        pstmt.close();
    }

    public void removeUser(String username) throws SQLException {
        PreparedStatement pstmt = this.connection.prepareStatement("DELETE FROM User WHERE user_username = ?");
        pstmt.setString(1, username);
        pstmt.executeUpdate();
        pstmt.close();
    }

    public void addUserSelf(String pubkey) throws SQLException {
        this.addUser(this.username, pubkey);
    }

    public void addPost(int user_id, String content, String ciphered, long date) throws SQLException {
        PreparedStatement pstmt = this.connection.prepareStatement("INSERT INTO Post(post_content, post_ciphered, user_id, post_date) VALUES(?, ?, ?, ?)");
        System.out.println("Content: " + content);
        pstmt.setString(1, content);
        pstmt.setString(2, ciphered);
        pstmt.setInt(3, user_id);
        pstmt.setLong(4, date);
        pstmt.executeUpdate();
        pstmt.close();
    }

    public void addPost(String user_username, String content, String ciphered, long date) throws SQLException {
        PreparedStatement pstmt = this.connection.prepareStatement("SELECT user_id FROM User WHERE user_username = ?");
        pstmt.setString(1, user_username);
        ResultSet res = pstmt.executeQuery();
        if (!res.next()) throw new SQLException("User " + user_username + " not found");
        int user_id = res.getInt("user_id");
        pstmt.close();

        this.addPost(user_id, content, ciphered, date);
    }

    public List<HashMap<String, String>> getPosts(int user_id) throws SQLException {
        PreparedStatement pstmt =
                this.connection.prepareStatement("""
                        SELECT User.user_username, Post.post_date, Post.post_content, Post.post_ciphered
                        FROM (Post INNER JOIN User ON Post.user_id = User.user_id)
                        WHERE Post.user_id = ?
                        """);
        pstmt.setInt(1, user_id);
        ResultSet res = pstmt.executeQuery();


        List<HashMap<String, String>> ret = new ArrayList<>();
        while (res.next()) {
            HashMap<String, String> elem = new HashMap<>();
            elem.put("author", res.getString("user_username"));
            elem.put("timestamp", res.getString("post_date"));
            elem.put("content", res.getString("post_content"));
            elem.put("ciphered", res.getString("post_ciphered"));
            ret.add(elem);
        }

        pstmt.close();
        return ret;
    }

    public List<HashMap<String, String>> getPosts(String user_username) throws SQLException {
        PreparedStatement pstmt = this.connection.prepareStatement("SELECT user_id FROM User WHERE user_username = ?");
        pstmt.setString(1, user_username);
        ResultSet res = pstmt.executeQuery();
        if (!res.next()) throw new SQLException("User " + user_username + " not found");
        int user_id = res.getInt("user_id");
        pstmt.close();

        return this.getPosts(user_id);
    }

    public int getIdFromUsername(String username) throws SQLException{
        PreparedStatement pstmt = this.connection.prepareStatement("SELECT user_id FROM User WHERE user_username = ?");
        pstmt.setString(1, username);
        ResultSet res = pstmt.executeQuery();
        if (!res.next()) throw new SQLException("User " + username + " not found");
        int user_id = res.getInt("user_id");
        pstmt.close();

        return user_id;
    }

    public List<HashMap<String, String>> getPostsSelf() throws SQLException {
        return this.getPosts(this.username);
    }

    public void addPostSelf(String content, String ciphered) throws SQLException {
        this.addPost(this.username, content, ciphered, System.currentTimeMillis() / 1000L);
    }

    public String getUserKey(String user_username) throws SQLException {
        PreparedStatement pstmt = this.connection.prepareStatement("SELECT user_pubkey FROM User WHERE user_username = ?");
        pstmt.setString(1, user_username);
        ResultSet res = pstmt.executeQuery();
        if (!res.next()) return null;
        String user_pubkey = res.getString("user_pubkey");
        pstmt.close();
        return user_pubkey;
    }

    public Set<String> getSubs() throws SQLException {
        PreparedStatement pstmt =
                this.connection.prepareStatement("""
                        SELECT user_username FROM User
                        WHERE user_username <> ?
                        """);
        pstmt.setString(1, this.username);
        ResultSet res = pstmt.executeQuery();

        Set<String> ret = new HashSet<>();
        while (res.next()) {
            ret.add(res.getString("user_username"));
        }

        pstmt.close();
        return ret;
    }
}
