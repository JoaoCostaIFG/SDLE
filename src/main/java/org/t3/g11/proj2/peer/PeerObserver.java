package org.t3.g11.proj2.peer;

import org.t3.g11.proj2.nuttela.message.Result;

import java.util.List;

public interface PeerObserver {
    List<Result> getResults(String username, long timestamp);

    void newPeerPost(int guid, long date, String ciphered, String author) throws Exception;
}
