package org.t3.g11.proj2;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
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
        System.out.println(zMsg);
        if (zMsg == null) continue;
        zMsg.send(this.socket);
      }
    }
  }

  public static void main(String[] args) {
    ZContext zctx = new ZContext();
    KeyServer keyServer = new KeyServer(zctx);
    keyServer.serverLoop();
  }
}
