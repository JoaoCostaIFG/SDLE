package org.t3.g11.proj2.keyserver;

import org.sqlite.SQLiteErrorCode;
import org.t3.g11.proj2.message.IdentifiedMessage;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

public class KeyServer {
  public static final String ENDPOINT = "tcp://localhost:8080";

  private static final String DBRESOURCE = "keyserver.db";

  private final ZContext zctx;
  private final Connection keyDB;
  private final ZMQ.Socket socket;

  public KeyServer(ZContext zctx) {
    this.zctx = zctx;

    // connect to db
    Connection keyDB;
    String url = "jdbc:sqlite:" + Objects.requireNonNull(getClass().getClassLoader().getResource(KeyServer.DBRESOURCE)).getPath();
    try {
      keyDB = DriverManager.getConnection(url);
    } catch (SQLException throwables) {
      keyDB = null;
      System.err.println("Database file reading failed.");
    }
    this.keyDB = keyDB;

    // open socket
    this.socket = zctx.createSocket(SocketType.ROUTER);
    this.socket.bind(KeyServer.ENDPOINT);
  }

  public void serverLoop() {
    ZMQ.Poller poller = this.zctx.createPoller(1);
    poller.register(this.socket, ZMQ.Poller.POLLIN);

    while (poller.poll() >= 0) {
      if (poller.pollin(0)) {
        ZMsg zMsg = ZMsg.recvMsg(this.socket);
        if (zMsg == null) continue;
        IdentifiedMessage msg = new IdentifiedMessage(zMsg);
        if (!this.handleMsg(msg))
          System.err.printf("Failed command: [command=%s]\n", msg.getCmd());
      }
    }
  }

  private boolean register(String username, String pubkey) {
    System.out.printf("Registering user: [username=%s], [pubkey=%s]\n", username, pubkey);

    String sql = "INSERT INTO User(user_username, user_pubkey) VALUES(?,?)";
    try {
      PreparedStatement pstmt = this.keyDB.prepareStatement(sql);
      pstmt.setString(1, username);
      pstmt.setString(2, pubkey);
      pstmt.executeUpdate();
    } catch (SQLException throwables) {
      //SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE;
      return false;
    }

    return true;
  }

  private boolean handleMsg(IdentifiedMessage msg) {
    String cmd = msg.getCmd();
    if (cmd.equals(KeyServerCMD.REGISTER.toString())) {
      // register
      if (msg.getArgCount() != 2) return false;
      String username = msg.getArg(0);
      String pubkey = msg.getArg(1);
      return this.register(username, pubkey);
    } else if (cmd.equals(KeyServerCMD.LOOKUP.toString())) {
      // lookup

      return true;
    }

    // unrecognized command
    return false;
  }

  public static void main(String[] args) {
    ZContext zctx = new ZContext();
    KeyServer keyServer = new KeyServer(zctx);
    keyServer.serverLoop();
  }
}
