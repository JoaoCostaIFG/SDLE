package org.t3.g11.proj2.peer.querytask;

import org.t3.g11.proj2.nuttela.message.Result;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public class QueryTaskMock implements QueryTaskInteface {
    @Override
    public void addResults(List<Result> results) {
        // do nothing
    }

    @Override
    public Set<Result> getResults() {
        return Collections.emptySet();
    }
}
