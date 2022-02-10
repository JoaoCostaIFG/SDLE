package org.t3.g11.proj2.peer;

public interface PeerStateObserver {
    void followCountUpdated(int followCount);
    void newPost(String username, long timestamp, String content);
}
