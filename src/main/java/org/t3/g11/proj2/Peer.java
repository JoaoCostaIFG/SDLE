package org.t3.g11.proj2;

import org.t3.g11.proj2.keyserver.KeyServer;
import org.t3.g11.proj2.keyserver.KeyServerCMD;
import org.t3.g11.proj2.message.UnidentifiedMessage;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.util.Arrays;
import java.util.Collections;

public class Peer {
  public static void register(ZContext zctx, String username, String pubkey) {
    ZMQ.Socket ksSocket = zctx.createSocket(SocketType.REQ);

    ksSocket.connect(KeyServer.ENDPOINT);

    ZMsg zMsg = new UnidentifiedMessage(
            KeyServerCMD.REGISTER.toString(),
            Arrays.asList(username, pubkey)
    ).newZMsg();
    zMsg.send(ksSocket);

    ZMsg replyZMsg = ZMsg.recvMsg(ksSocket);
    System.out.println(replyZMsg);
  }

  public static void lookup(ZContext zctx, String username) {
    ZMQ.Socket ksSocket = zctx.createSocket(SocketType.REQ);

    ksSocket.connect(KeyServer.ENDPOINT);

    ZMsg zMsg = new UnidentifiedMessage(
            KeyServerCMD.LOOKUP.toString(),
            Collections.singletonList(username)
    ).newZMsg();
    zMsg.send(ksSocket);

    ZMsg replyZMsg = ZMsg.recvMsg(ksSocket);
    System.out.println(replyZMsg);
  }

  public static void main(String[] args) {
    ZContext zctx = new ZContext();
    register(zctx, "example_user", "example_pubkey");
    lookup(zctx, "example_user");
  }
}
