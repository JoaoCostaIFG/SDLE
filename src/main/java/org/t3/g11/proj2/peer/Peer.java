package org.t3.g11.proj2.peer;

import org.t3.g11.proj2.keyserver.KeyServer;
import org.t3.g11.proj2.keyserver.KeyServerCMD;
import org.t3.g11.proj2.keyserver.KeyServerReply;
import org.t3.g11.proj2.keyserver.message.UnidentifiedMessage;
import org.t3.g11.proj2.nuttela.GnuNode;
import org.t3.g11.proj2.nuttela.message.Result;
import org.t3.g11.proj2.utils.Utils;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.InvalidKeySpecException;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class Peer implements PeerObserver {
    public static final int UPDATE_FREQ = 5;

    private final ZMQ.Socket ksSocket;
    private final KeyHolder keyHolder;
    private final InetSocketAddress nodeAddr; // for late intialization

    private PeerData peerData;
    private boolean authenticated;
    private GnuNode node; // initialized late
    private Thread nodeT; // initialized late

    public Peer(ZContext zctx, String address, int port) throws Exception {
        this.ksSocket = zctx.createSocket(SocketType.REQ);
        if (!this.ksSocket.connect(KeyServer.KEYENDPOINT)) {
            System.err.println("Failed to connect to keyserver.");
            throw new Exception("Failed to connect to keyserver.");
        }

        this.authenticated = false;
        this.keyHolder = new KeyHolder(KeyServer.KEYINSTANCE, KeyServer.KEYSIZE);
        this.nodeAddr = new InetSocketAddress(address, port);
    }

    public PeerData getPeerData() {
        return peerData;
    }

    /**
     * Called when authenticated (register/login).
     */
    public void startNode() {
        try {
            this.node = new GnuNode(Utils.IdFromName(this.peerData.getSelfUsername()), this.nodeAddr);
            this.node.setObserver(this);
            this.nodeT = new Thread(this.node);
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(1);
        }

        try {
            this.node.buildBloom(this.peerData.getSubs());
            this.node.addToBloom(this.peerData.getSelfUsername());
        } catch (SQLException throwables) {
            System.out.println("Problem getting subscriptions");
            throwables.printStackTrace();
        }
        this.nodeT.start();
        // TODO make data member
        ScheduledExecutorService queryScheduler = Executors.newSingleThreadScheduledExecutor();
        queryScheduler.scheduleAtFixedRate(this::fetchSubPosts, 1, UPDATE_FREQ, TimeUnit.SECONDS);
    }

    public void fetchSubPosts() {
        for (String sub : this.getSubs()) {
            try {
                // we're okay with 1
                this.node.query(1, sub, this.peerData.getLastUserPostDate(sub));
            } catch (Exception e) {
                System.err.println("Problem getting info about user: " + sub);
                e.printStackTrace();
            }
        }
    }

    public boolean register(String username) {
        // keys stuff
        try {
            this.keyHolder.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Failed to initialize key generator.");
            return false;
        }

        PublicKey publicKey = this.keyHolder.getPublicKey();
        PrivateKey privateKey = this.keyHolder.getPrivateKey();

        ZMsg zMsg = new UnidentifiedMessage(
                KeyServerCMD.REGISTER.toString(),
                Arrays.asList(username, KeyHolder.encodeKey(publicKey))
        ).newZMsg();
        zMsg.send(this.ksSocket);

        ZMsg replyZMsg = ZMsg.recvMsg(this.ksSocket);
        UnidentifiedMessage replyMsg = new UnidentifiedMessage(replyZMsg);
        if (replyMsg.getCmd().equals(KeyServerReply.SUCCESS.toString())) {
            // success
            try {
                KeyHolder.writeKeyToFile(privateKey, username);
            } catch (IOException e) {
                System.err.printf("Failed to save user's private key to a file. Here it is:\n%s\n",
                        KeyHolder.encodeKey(privateKey));
            }

            try {
                KeyHolder.writeKeyToFile(publicKey, username);
            } catch (IOException e) {
                System.err.printf("Failed to save user's public key to a file. Here it is:\n%s\n",
                        KeyHolder.encodeKey(publicKey));
            }

            try {
                this.peerData = new PeerData(username);
                this.peerData.reInitDB();
                this.peerData.addUserSelf(KeyHolder.encodeKey(publicKey));
            } catch (SQLException throwables) {
                throwables.printStackTrace();
                System.err.println("Failed to create database.");
                System.exit(1);
            }
            this.startNode();
            this.authenticated = true;
        } else {
            // failure
            this.authenticated = false;
            this.keyHolder.clear();
        }

        return this.authenticated;
    }

    public boolean authenticate(String username) {
        this.authenticated = false;

        try {
            this.keyHolder.importKeysFromFile(username);
        } catch (IOException | InvalidKeySpecException e) {
            System.err.println("Authentication failed");
            return false;
        }

        try {
            this.peerData = new PeerData(username);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
            System.err.println("Failed to open user database.");
            this.keyHolder.clear();
            return false;
        }

        this.startNode();
        this.authenticated = true;
        return true;
    }

    public PublicKey lookup(String username) {
        ZMsg zMsg = new UnidentifiedMessage(
                KeyServerCMD.LOOKUP.toString(),
                Collections.singletonList(username)
        ).newZMsg();
        zMsg.send(this.ksSocket);

        ZMsg replyZMsg = ZMsg.recvMsg(this.ksSocket);
        UnidentifiedMessage replyMsg = new UnidentifiedMessage(replyZMsg);
        if (replyMsg.getCmd().equals(KeyServerReply.SUCCESS.toString())) {
            // success
            try {
                return this.keyHolder.genPubKey(replyMsg.getArg(0));
            } catch (InvalidKeySpecException e) {
                e.printStackTrace();
                return null;
            }
        } else {
            // failure
            return null;
        }
    }

    public boolean newPost(String content) {
        byte[] cipherBuffer = content.getBytes();
        String ciphered;
        try {
            ciphered = this.keyHolder.encryptStr(cipherBuffer);
        } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
            System.err.println("Failed to encrypt post content.");
            return false;
        }

        try {
            this.peerData.addPostSelf(content, ciphered);
        } catch (SQLException throwables) {
            System.err.println(throwables.getMessage());
            return false;
        }
        return true;
    }

    public List<HashMap<String, String>> getSelfPeerPosts() {
        try {
            return peerData.getPostsSelf();
        } catch (SQLException throwables) {
            System.err.println(throwables.getMessage());
            return Collections.emptyList();
        }
    }

    private PublicKey getUserKey(String username) {
        // fetch from cache
        String pubKeyStr;
        try {
            pubKeyStr = this.peerData.getUserKey(username);
        } catch (SQLException throwables) {
            pubKeyStr = null;
        }

        if (pubKeyStr != null) {
            // key cached
            try {
                return this.keyHolder.genPubKey(pubKeyStr);
            } catch (InvalidKeySpecException e) {
                e.printStackTrace();
                return null;
            }
        }

        // key not cached
        return this.lookup(username);
    }

    public String decypherText(String ciphered, String username) throws Exception {
        PublicKey publicKey = this.getUserKey(username);
        if (publicKey == null) throw new Exception("User " + username + " not found.");
        return this.keyHolder.decryptStr(ciphered, publicKey);
    }

    public void subscribe(String username) throws Exception {
        if (this.peerData.getSelfUsername().equals(username)) throw new Exception("Can't subscribe to self.");

        PublicKey publicKey = this.lookup(username);
        if (publicKey == null) throw new Exception("User " + username + " not found.");
        this.peerData.addUser(username, KeyHolder.encodeKey(publicKey));
        // update node bloom filter
        this.node.addToBloom(username);
    }

    public void unsubscribe(String username) throws Exception {
        if (this.peerData.getSelfUsername().equals(username)) throw new Exception("Can't subscribe to self.");
        this.peerData.removeUser(username);
        // update node bloom filter
        this.node.buildBloom(this.peerData.getSubs());
        this.node.addToBloom(this.peerData.getSelfUsername());
    }

    public Set<String> getSubs() {
        try {
            return this.peerData.getSubs();
        } catch (Exception e) {
            System.out.println("There was a problem getting subscribed users");
            return new HashSet<>();
        }
    }

    public List<HashMap<String, String>> getUserPosts(String username) {
        try {
            return this.peerData.getPosts(username);
        } catch (SQLException throwables) {
            System.err.println(throwables.getMessage());
            return null;
        }
    }

    public SortedSet<HashMap<String, String>> getPosts() throws Exception {
        Comparator<HashMap<String, String>> comparator =
                Comparator.comparingLong(e -> Long.parseLong(e.get("timestamp")));
        SortedSet<HashMap<String, String>> posts = new TreeSet<>(comparator.reversed());
        for (String usrnm : this.peerData.getSubs())
            posts.addAll(this.getUserPosts(usrnm));
        posts.addAll(this.getSelfPeerPosts());
        return posts;
    }

    @Override
    public List<Result> getResults(String username, long timestamp) {
        List<Result> results = new ArrayList<>();
        var userPosts = this.getUserPosts(username);
        if (userPosts == null) return results;

        for (HashMap<String, String> post : userPosts) {
            long postTimestamp = Long.parseLong(post.get("timestamp"));
            if (postTimestamp > timestamp) {
                results.add(new Result(Integer.parseInt(post.get("guid")),
                        Long.parseLong(post.get("timestamp")),
                        post.get("ciphered"), post.get("author")));
            }
        }

        return results;
    }

    @Override
    public void newPeerPost(int guid, long date, String ciphered, String author) throws Exception {
        String content = this.decypherText(ciphered, author);
        this.getPeerData().addPost(author, guid, content, ciphered, date);
    }

    public void shutdown() {
    }
}
