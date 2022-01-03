package org.t3.g11.proj2.keyserver;

import org.t3.g11.proj2.message.IdentifiedMessage;
import org.t3.g11.proj2.message.UnidentifiedMessage;
import org.zeromq.ZMsg;

public enum KeyServerReply {
  SUCCESS,
  FAILURE,
  UNIMPLEMENTED,
  UNKNOWN;

  public ZMsg getMessage(IdentifiedMessage msg) {
    return new IdentifiedMessage(msg.getIdentity(), this.toString()).newZMsg();
  }

  public ZMsg getMessage() {
    return new UnidentifiedMessage(this.toString()).newZMsg();
  }
}
