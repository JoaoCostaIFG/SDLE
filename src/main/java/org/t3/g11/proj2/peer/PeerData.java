package org.t3.g11.proj2.peer;

import org.sqlite.SQLiteErrorCode;
import org.t3.g11.proj2.utils.Utils;

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
                  post_date INTEGER NOT NULL,
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

    public int getIdFromUsername(String username) throws SQLException {
        PreparedStatement pstmt = this.connection.prepareStatement("SELECT user_id FROM User WHERE user_username = ?");
        pstmt.setString(1, username);
        ResultSet res = pstmt.executeQuery();
        if (!res.next()) throw new SQLException("User " + username + " not found");
        int user_id = res.getInt("user_id");
        pstmt.close();

        return user_id;
    }

    public void addPost(int user_id, int guid, String content, String ciphered, long date) throws SQLException {
        PreparedStatement pstmt = this.connection.prepareStatement("INSERT INTO Post(post_id, post_content, post_ciphered, user_id, post_date) VALUES(?, ?, ?, ?, ?)");
        System.out.println("Content: " + content);
        pstmt.setInt(1, guid);
        pstmt.setString(2, content);
        pstmt.setString(3, ciphered);
        pstmt.setInt(4, user_id);
        pstmt.setLong(5, date);
        try {
            pstmt.executeUpdate();
        } catch (SQLException throwables) {
            // ignore exception on duplicated posts
            // TODO tirar isto daqui
            System.err.println(throwables.getErrorCode() + " " + SQLiteErrorCode.SQLITE_CONSTRAINT_PRIMARYKEY.code + " " + throwables.getMessage());
            if (throwables.getErrorCode() != SQLiteErrorCode.SQLITE_CONSTRAINT_PRIMARYKEY.code) {
                throw throwables;
            }
        }
        pstmt.close();
    }

    public void addPost(String user_username, int guid, String content, String ciphered, long date) throws SQLException {
        int user_id = this.getIdFromUsername(user_username);
        this.addPost(user_id, guid, content, ciphered, date);
    }

    public void addPostSelf(String content, String ciphered) throws SQLException {
        long timestamp = System.currentTimeMillis();
        String toHash = this.username + timestamp;
        this.addPost(this.username, Utils.IdFromName(toHash), content, ciphered, timestamp);
    }

    public List<HashMap<String, String>> getPosts(int user_id) throws SQLException {
        PreparedStatement pstmt =
                this.connection.prepareStatement("""
                        SELECT User.user_username, Post.post_id, Post.post_date, Post.post_content, Post.post_ciphered
                        FROM (Post INNER JOIN User ON Post.user_id = User.user_id)
                        WHERE Post.user_id = ?
                        """);
        pstmt.setInt(1, user_id);
        ResultSet res = pstmt.executeQuery();


        List<HashMap<String, String>> ret = new ArrayList<>();
        while (res.next()) {
            HashMap<String, String> elem = new HashMap<>();
            elem.put("guid", res.getString("post_id"));
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
        int user_id = this.getIdFromUsername(user_username);
        return this.getPosts(user_id);
    }

    public List<HashMap<String, String>> getPostsSelf() throws SQLException {
        return this.getPosts(this.username);
    }

    public long getLastUserPostDate(String user_username) throws SQLException {
        int user_id = this.getIdFromUsername(user_username);
        PreparedStatement pstmt =
                this.connection.prepareStatement("""
                        SELECT post_date
                        FROM Post
                        WHERE Post.user_id = ?
                        ORDER BY post_date DESC
                        LIMIT 1
                        """);
        pstmt.setInt(1, user_id);
        ResultSet res = pstmt.executeQuery();
        if (!res.next()) return 0;
        return res.getLong("post_date");
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
