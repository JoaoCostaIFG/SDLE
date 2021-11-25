package client;

import message.UnidentifiedMessage;
import org.zeromq.ZContext;
import org.zeromq.ZMsg;
import proxy.Proxy;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.Arrays;

public class Publisher extends SocketHolder {
    public static final String PUTCMD = "PUT";
    private final MessageDigest digest;

    public Publisher(ZContext zctx, String id, String endpoint) throws NoSuchAlgorithmException {
        super(zctx, id, endpoint);
        digest = MessageDigest.getInstance("SHA-1");
    }

    public boolean put(String topic, String content) throws Exception {
        do {
            long timeNow = Instant.now().getEpochSecond();
            this.digest.reset();
            this.digest.update((timeNow + content).getBytes(StandardCharsets.UTF_8));
            String msgId = String.format("%040x", new BigInteger(1, digest.digest()));
            ZMsg zmsg = new UnidentifiedMessage(Publisher.PUTCMD, Arrays.asList(topic, content, msgId)).newZMsg();
            if (!zmsg.send(this.socket)) return false;

            ZMsg replyZMsg = this.sendAndReply(zmsg);
            UnidentifiedMessage reply = new UnidentifiedMessage(replyZMsg);

            if (!reply.getCmd().equals(Publisher.PUTCMD)) return false;
            if (reply.getArg(0).equals(Proxy.OKREPLY)) return true;

        } while (this.nTimeouts == 0);

        return true;
    }
}
