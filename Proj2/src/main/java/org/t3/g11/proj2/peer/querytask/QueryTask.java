package org.t3.g11.proj2.peer.querytask;

import org.t3.g11.proj2.nuttela.GnuNode;
import org.t3.g11.proj2.nuttela.message.Result;
import org.t3.g11.proj2.nuttela.message.query.Query;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

class QueryTaskCallable implements Callable<Set<Result>> {
    private final int neededHits;
    private final GnuNode node;
    private final Query query;
    private final Set<Result> results = new HashSet<>();

    private final Lock lock = new ReentrantLock();
    private final Condition condition = lock.newCondition();
    private AtomicBoolean isAlive = new AtomicBoolean(false);

    public QueryTaskCallable(GnuNode node, Query q) {
        this.node = node;
        this.query = q;
        this.neededHits = q.getNeededHits();
    }

    @Override
    public Set<Result> call() throws Exception {
        this.isAlive.set(true);
        this.node.query(this.query); // quwey

        this.lock.lock();
        try {
            while (results.size() < this.neededHits) {
                this.condition.await();
            }
        } catch (InterruptedException e) {
            // ignored (timeout)
        } finally {
            this.lock.unlock();
        }
        this.isAlive.set(false);
        return this.results;
    }

    public void addResults(List<Result> results) {
        if (!this.isAlive.get()) return;

        this.lock.lock();
        this.results.addAll(results);
        this.condition.signal();
        this.lock.unlock();
    }

    public Set<Result> getResults() {
        return this.results;
    }
}

public class QueryTask extends FutureTask<Set<Result>> implements QueryTaskInteface {
    private final QueryTaskCallable callable;

    private QueryTask(QueryTaskCallable callable) {
        super(callable);
        this.callable = callable;
    }

    public QueryTask(GnuNode node, Query q) {
        this(new QueryTaskCallable(node, q));
    }

    @Override
    public void addResults(List<Result> results) {
        this.callable.addResults(results);
    }

    @Override
    public Set<Result> getResults() {
        return this.callable.getResults();
    }
}
