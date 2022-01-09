package org.t3.g11.proj2.peer;

import org.t3.g11.proj2.nuttela.message.Result;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.FutureTask;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class QueryTaskRunnable implements Runnable {
    private final int neededHits;
    private final Set<Result> results = new HashSet();

    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();

    public QueryTaskRunnable(int neededHits) {
        this.neededHits = neededHits;
    }

    @Override
    public void run() {
        this.lock.lock();
        try {
            while (results.size() < this.neededHits) {
                this.condition.await();
            }
        } catch (InterruptedException e) {
            // ignored (timeout)
            return;
        } finally {
            this.lock.unlock();
        }
    }

    public void addResult(Result result) {
        this.lock.lock();
        this.results.add(result);
        this.condition.signal();
        this.lock.unlock();
    }

    public void addResults(List<Result> results) {
        this.lock.lock();
        this.results.addAll(results);
        this.condition.signal();
        this.lock.unlock();
    }
}

public class QueryTask extends FutureTask<Set<Result>> {
    private final QueryTaskRunnable runnable;

    private QueryTask(QueryTaskRunnable runnable, Set<Result> result) {
        super(runnable, result);
        this.runnable = runnable;
    }

    public QueryTask(int neededHits, Set<Result> result) {
        this(new QueryTaskRunnable(neededHits), result);
    }

    public void addResult(Result result) {
        this.runnable.addResult(result);
    }

    public void addResults(List<Result> results) {
        this.runnable.addResults(results);
    }
}
