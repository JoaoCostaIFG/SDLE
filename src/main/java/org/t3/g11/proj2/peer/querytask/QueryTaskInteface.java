package org.t3.g11.proj2.peer.querytask;

import org.t3.g11.proj2.nuttela.message.Result;

import java.util.List;
import java.util.Set;

public interface QueryTaskInteface {
    void addResults(List<Result> results);

    Set<Result> getResults();
}
