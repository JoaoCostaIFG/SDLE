package org.t3.g11.proj2.keyserver;

import org.t3.g11.proj2.message.IdentifiedMessage;
import org.zeromq.SocketType;
import org.zeromq.ZContext;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMsg;

import java.util.Collections;

public class KeyServerWorker implements Runnable {
  private final KeyServer keyServer;
  private final Socket outSock;
  private final Socket inSock;

  public KeyServerWorker(ZContext zctx, KeyServer keyServer, String endpointIn, String endpointOut) {
    this.keyServer = keyServer;

    this.outSock = zctx.createSocket(SocketType.PUSH);
    this.outSock.connect(endpointOut);

    this.inSock = zctx.createSocket(SocketType.PULL);
    this.inSock.connect(endpointIn);
  }

  @Override
  public void run() {
    while (!Thread.currentThread().isInterrupted()) {
      ZMsg zMsg;
      try {
        zMsg = ZMsg.recvMsg(this.inSock);
      } catch (Exception e) {
        break;
      }
      if (zMsg == null) continue;

      ZMsg replyZMsg = this.handleMsg(zMsg);
      try {
        replyZMsg.send(this.outSock);
      } catch (Exception e) {
        break;
      }

      /*
      if (target.toString().equals(Proxy.STOPWORKER)) {
        System.err.println("Worker got kill signal. Quitting...");
        break;
      }
       */
    }

    this.inSock.close();
    this.outSock.close();
  }

  private ZMsg handleMsg(ZMsg zMsg) {
    IdentifiedMessage msg = new IdentifiedMessage(zMsg);

    KeyServerCMD cmd;
    try {
      cmd = KeyServerCMD.valueOf(msg.getCmd());
    } catch (IllegalArgumentException e) {
      // unrecognized command
      System.err.printf("Unknown command: [command=%s]\n", msg.getCmd());
      return KeyServerReply.UNKNOWN.getMessage(msg);
    }

    // TODO: Having this here would send an UNIMPLEMENTED back to the key server, maybe do this in another method
    if (cmd == KeyServerCMD.STOPWORKER)
      Thread.currentThread().interrupt();

    return switch (cmd) {
      case REGISTER -> {
        // register
        if (msg.getArgCount() != 2) {
          System.err.printf("Incorrect argument count: [argCount=%d]\n", msg.getArgCount());
          yield KeyServerReply.UNKNOWN.getMessage(msg);
        }

        String username = msg.getArg(0);
        String pubkey = msg.getArg(1);
        if (this.keyServer.register(username, pubkey)) yield KeyServerReply.SUCCESS.getMessage(msg);
        else yield KeyServerReply.FAILURE.getMessage(msg);
      }
      case LOOKUP -> {
        // lookup
        if (msg.getArgCount() != 1) {
          System.err.printf("Incorrect argument count: [argCount=%d]\n", msg.getArgCount());
          yield KeyServerReply.UNKNOWN.getMessage(msg);
        }

        String res = this.keyServer.lookup(msg.getArg(0));
        if (res == null) yield KeyServerReply.FAILURE.getMessage(msg);
        else yield new IdentifiedMessage(msg.getIdentity(),
                KeyServerReply.SUCCESS.toString(),
                Collections.singletonList(res)).newZMsg();
      }
      default -> KeyServerReply.UNIMPLEMENTED.getMessage(msg);
    };
  }
}
