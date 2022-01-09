package org.t3.g11.proj2.peer;

import org.t3.g11.proj2.nuttela.message.Result;
import org.t3.g11.proj2.nuttela.message.query.Query;

import java.util.List;

public interface PeerObserver {
    void handleNewResults(int guid, List<Result> results);

    List<Result> getUserResults(String username, long timestamp);

    List<Result> getTagResults(String queryString);

    List<Result> handleQuery(Query query);
}
