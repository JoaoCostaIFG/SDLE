package org.t3.g11.proj2.keyserver;

public enum KeyServerCMD {
    REGISTER, // REGISTER USERNAME PUBKEY -> success/failure
    LOOKUP,   // LOOKUP USERNAME          -> PUBKEY/failure
    STOPWORKER, // STOP WORKER
    // DELETE,
    // UPDATE,
}
