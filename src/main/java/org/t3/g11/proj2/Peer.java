package org.t3.g11.proj2;

import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ;
import org.zeromq.ZMsg;

import java.util.Collections;

public class Peer {
  public static void main(String[] args) {
    ZContext zctx = new ZContext();
    ZMQ.Socket ksSocket = zctx.createSocket(SocketType.REQ);

    ksSocket.connect(KeyServer.ENDPOINT);

    ZMsg zMsg = new message.UnidentifiedMessage("TCHU", Collections.singletonList("TCHA")).newZMsg();
    zMsg.send(ksSocket);

    ZMsg replyZMsg = ZMsg.recvMsg(ksSocket);
    System.out.println(replyZMsg);
  }
}
