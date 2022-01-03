package org.t3.g11.proj2.keyserver;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.ArrayList;
import java.util.List;

public class KeyServer {
  public static final String ENDPOINT = "tcp://localhost:8080";

  private static final String workersPushEndpoint = "inproc://workersPush";
  private static final String workersPullEndpoint = "inproc://workersPull";
  private static final String DBRESOURCE = "keyserver.db";

  private final ZContext zctx;
  private final Connection keyDB;
  private final ZMQ.Socket socket;
  private final ZMQ.Socket workersPush;
  private final ZMQ.Socket workersPull;

  private final List<Thread> workers;

  public KeyServer(ZContext zctx) throws Exception {
    this.zctx = zctx;

    // connect to db
    String url = "jdbc:sqlite:";
    URL resourcePath = getClass().getClassLoader().getResource(KeyServer.DBRESOURCE);
    if (resourcePath == null) {
      System.err.println("Couldn't find db file.");
      throw new Exception("Couldn't find db file.");
    } else {
      url += URLDecoder.decode(resourcePath.getPath(), StandardCharsets.UTF_8);
      this.keyDB = DriverManager.getConnection(url);
    }

    // open socket
    this.socket = zctx.createSocket(SocketType.ROUTER);
    this.socket.bind(KeyServer.ENDPOINT);

    // internal communication between threads to distribute work
    this.workersPush = zctx.createSocket(SocketType.PUSH);
    this.workersPush.bind(workersPushEndpoint);

    this.workersPull = zctx.createSocket(SocketType.PULL);
    this.workersPull.bind(workersPullEndpoint);

    int maxThreads = Runtime.getRuntime().availableProcessors() + 1;
    this.workers = new ArrayList<>();
    for (int i = 0; i < maxThreads; ++i) {
      KeyServerWorker w = new KeyServerWorker(
              zctx,
              this.keyDB,
              workersPushEndpoint,
              workersPullEndpoint
      );
      Thread t = new Thread(w);
      this.workers.add(t);
      t.start();
    }
  }

  public void serverLoop() {
    ZMQ.Poller poller = this.zctx.createPoller(2);
    poller.register(this.socket, ZMQ.Poller.POLLIN);
    poller.register(this.workersPull, ZMQ.Poller.POLLIN);

    while (poller.poll() >= 0) {
      if (poller.pollin(0)) {
        ZMsg zMsg = ZMsg.recvMsg(this.socket);
        if (zMsg == null) continue;
        zMsg.send(this.workersPush);
      }
      if (poller.pollin(1)) {
        ZMsg zMsg = ZMsg.recvMsg(this.workersPull);
        if (zMsg == null) continue;
        zMsg.send(this.socket);
      }
    }
  }

  public static void main(String[] args) {
    ZContext zctx = new ZContext();

    // init server
    KeyServer keyServer = null;
    try {
      keyServer = new KeyServer(zctx);
    } catch (Exception e) {
      e.printStackTrace();
      System.err.println("Database file reading failed.");
    }

    keyServer.serverLoop();
  }
}
