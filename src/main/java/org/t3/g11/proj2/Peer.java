package org.t3.g11.proj2;

import org.t3.g11.proj2.keyserver.KeyServer;
import org.t3.g11.proj2.keyserver.KeyServerCMD;
import org.t3.g11.proj2.message.UnidentifiedMessage;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.util.Arrays;

public class Peer {
  public static void main(String[] args) {
    ZContext zctx = new ZContext();
    ZMQ.Socket ksSocket = zctx.createSocket(SocketType.REQ);

    ksSocket.connect(KeyServer.ENDPOINT);

    ZMsg zMsg = new UnidentifiedMessage(
            KeyServerCMD.REGISTER.toString(),
            Arrays.asList("a", "b")
    ).newZMsg();
    zMsg.send(ksSocket);

    ZMsg replyZMsg = ZMsg.recvMsg(ksSocket);
    System.out.println(replyZMsg);
  }
}
