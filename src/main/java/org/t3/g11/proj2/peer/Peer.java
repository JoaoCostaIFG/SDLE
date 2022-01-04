package org.t3.g11.proj2.peer;

import org.t3.g11.proj2.keyserver.KeyServer;
import org.t3.g11.proj2.keyserver.KeyServerCMD;
import org.t3.g11.proj2.keyserver.KeyServerReply;
import org.t3.g11.proj2.message.UnidentifiedMessage;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.Scanner;

public class Peer {
  private final ZContext zctx;
  private final ZMQ.Socket ksSocket;

  private boolean authenticated;
  private String username;
  private final KeyHolder keyHolder;

  public Peer(ZContext zctx) throws Exception {
    this.zctx = zctx;
    this.ksSocket = zctx.createSocket(SocketType.REQ);
    if (!this.ksSocket.connect(KeyServer.ENDPOINT)) {
      System.err.println("Failed to connect to keyserver.");
      throw new Exception("Failed to connect to keyserver.");
    }

    this.authenticated = false;
    this.keyHolder = new KeyHolder(KeyServer.KEYINSTANCE);
  }

  public boolean register(String username) {
    // keys stuff
    KeyPair keyPair;
    try {
      SecureRandom secureRandom = SecureRandom.getInstanceStrong();
      KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance(KeyServer.KEYINSTANCE);
      keyPairGenerator.initialize(KeyServer.KEYSIZE, secureRandom);
      keyPair = keyPairGenerator.generateKeyPair();
    } catch (NoSuchAlgorithmException e) {
      System.err.println("Failed to initialize key generator.");
      return false;
    }

    PublicKey publicKey = keyPair.getPublic();
    PrivateKey privateKey = keyPair.getPrivate();

    ZMsg zMsg = new UnidentifiedMessage(
            KeyServerCMD.REGISTER.toString(),
            Arrays.asList(username, KeyHolder.encodeKey(publicKey))
    ).newZMsg();
    zMsg.send(this.ksSocket);

    ZMsg replyZMsg = ZMsg.recvMsg(this.ksSocket);
    UnidentifiedMessage replyMsg = new UnidentifiedMessage(replyZMsg);
    if (replyMsg.getCmd().equals(KeyServerReply.SUCCESS.toString())) {
      // success
      this.username = username;
      this.keyHolder.setPrivateKey(privateKey);
      this.keyHolder.setPublicKey(publicKey);
      try {
        KeyHolder.writeKeyToFile(privateKey, username);
      } catch (IOException e) {
        System.err.printf("Failed to save user's private key to a file. Here it is:\n%s\n",
                KeyHolder.encodeKey(privateKey));
      }
      this.authenticated = true;
    } else {
      // failure
      this.authenticated = false;
      this.keyHolder.clear();
      this.username = null;
    }

    return this.authenticated;
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

  public void testEncryption() {
    byte[] buffer = "Padoru".getBytes(StandardCharsets.UTF_8);
    try {
      byte[] encrypted = this.keyHolder.encrypt(buffer);
      System.out.println(new String(this.keyHolder.decrypt(encrypted)));
    } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException e) {
      e.printStackTrace();
    }
  }

  public static void main(String[] args) {
    ZContext zctx = new ZContext();

    Peer peer;
    try {
      peer = new Peer(zctx);
    } catch (Exception e) {
      e.printStackTrace();
      return;
    }

    Scanner sc = new Scanner(System.in);
    event_loop:
    while (true) {
      if (!peer.authenticated) {
        System.out.print("""
                r - Register
                l - Login
                q - quit
                """);
        System.out.flush();
        String nextLine = sc.nextLine();
        if (nextLine.length() <= 0) continue;
        char cmd = nextLine.charAt(0);

        switch (cmd) {
          case 'r', 'R':
            // TODO this username is random each time!!!!!!!
            byte[] buf = new byte[12];
            new Random().nextBytes(buf);
            String username = new String(buf, StandardCharsets.UTF_8);
            if (!peer.register(username)) {
              System.out.println("Register failed");
            } else {
              System.out.println("Authenticated as " + username);
            }
            break;
          case 'l', 'L':
            break;
          case 'q', 'Q':
            System.err.println("Quitting...");
            break event_loop;
          default:
            System.err.println("Unknown command...");
            continue event_loop;
        }
      } else {
        System.out.print("""
                n - New post
                q - quit
                """);
        System.out.flush();
        String nextLine = sc.nextLine();
        if (nextLine.length() <= 0) continue;
        char cmd = nextLine.charAt(0);

        switch (cmd) {
          case 'n', 'N':
            peer.testEncryption();
            break;
          case 'q', 'Q':
            System.err.println("Quitting...");
            break event_loop;
          default:
            System.err.println("Unknown command...");
            continue event_loop;
        }
      }
    }
  }
}
