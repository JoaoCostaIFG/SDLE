package org.t3.g11.proj2.keyserver;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class KeyServer {
    public static final String KEYINSTANCE = "RSA";
    public static final int KEYSIZE = 4096;
    public static final String KEYENDPOINT = "tcp://localhost:8079";
    private static final String workersPushEndpoint = "inproc://workersPush";
    private static final String workersPullEndpoint = "inproc://workersPull";
    private static final String DBRESOURCE = "keyserver.db";

    private final Connection keyDB;
    private final BootstrapGnuNode gnuNode;
    private final Thread gnuNodeThread;
    private final ZContext zctx;
    private final ZMQ.Socket socket;
    private final ZMQ.Socket workersPush;
    private final ZMQ.Socket workersPull;
    private final List<Thread> workers;

    public KeyServer(ZContext zctx) throws Exception {
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

        this.zctx = zctx;
        // open socket
        this.socket = zctx.createSocket(SocketType.ROUTER);
        this.socket.bind(KeyServer.KEYENDPOINT);

        // internal communication between threads to distribute work
        this.workersPush = zctx.createSocket(SocketType.PUSH);
        this.workersPush.bind(workersPushEndpoint);
        this.workersPull = zctx.createSocket(SocketType.PULL);
        this.workersPull.bind(workersPullEndpoint);
        // workers
        int maxThreads = Runtime.getRuntime().availableProcessors() + 1;
        this.workers = new ArrayList<>();
        for (int i = 0; i < maxThreads; ++i) {
            KeyServerWorker w = new KeyServerWorker(
                    zctx,
                    this,
                    workersPushEndpoint,
                    workersPullEndpoint
            );
            Thread t = new Thread(w);
            this.workers.add(t);
            t.start();
        }

        this.gnuNode = new BootstrapGnuNode();
        this.gnuNodeThread = new Thread(this.gnuNode);

        System.out.println("Key server ready!");
    }

    public void serverLoop() {
        this.gnuNodeThread.start();

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

        this.gnuNodeThread.interrupt();
        try {
            this.gnuNodeThread.join();
        } catch (InterruptedException e) {
            System.out.println("Failed to join GnuNode thread with stacktrace:");
            e.printStackTrace();
        }
    }

    protected boolean register(String username, String pubkey) {
        System.out.printf("Registering user: [username=%s], [pubkey=%s]\n",
                username, pubkey.substring(0, 12));

        String sql = "INSERT INTO User(user_username, user_pubkey) VALUES(?, ?)";
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

    protected String lookup(String username) {
        System.out.printf("Looking-up user: [username=%s]\n", username);

        String sql = "SELECT user_pubkey FROM User WHERE user_username = ?";
        try {
            PreparedStatement pstmt = this.keyDB.prepareStatement(sql);
            pstmt.setString(1, username);
            ResultSet res = pstmt.executeQuery();
            if (!res.next()) return null;
            return res.getString("user_pubkey");
        } catch (SQLException throwables) {
            //SQLiteErrorCode.SQLITE_CONSTRAINT_UNIQUE;
            return null;
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
