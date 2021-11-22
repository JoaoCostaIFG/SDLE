package proxy.TopicQueue;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TopicQueue implements Serializable {
    // people that subscribed to this queue
    private final Map<String, QueueNode> subs;
    // first node
    private QueueNode head;
    // last node
    private QueueNode tail;
    // queue size
    private int size;

    private Integer msgId;

    private boolean saved;
    private boolean wasChanged;

    public TopicQueue() {
        this.head = null;
        this.tail = null;
        this.size = 0;
        this.subs = new HashMap<>();
        this.msgId = 0;
        this.saved = false;
        this.wasChanged = false;
    }

    public synchronized void push(String content) {
        this.wasChanged = true;

        ++this.msgId;
        if (this.size == 0) this.head = this.tail = new QueueNode(content, msgId);
        else this.tail = new QueueNode(content, this.tail, msgId);

        // TODO could be optimized with a second data struct with O(1) appends
        // set next message for those that don't have any
        for (Map.Entry<String, QueueNode> e : this.subs.entrySet()) {
            if (e.getValue() == null) {
                this.subs.put(e.getKey(), this.tail);
                this.tail.increaseRef();
            }
        }

        ++this.size;
    }

    public synchronized List<String> retrieveUpdate(String subId) {
        if (!this.subs.containsKey(subId)) return null;

        QueueNode qn = this.subs.get(subId);
        if (qn == null) return null;

        this.wasChanged = true;

        // retrieve content and advance pointer
        String content = qn.getContent();
        Integer msgId = qn.id;
        qn.decreaseRef();
        this.subs.put(subId, qn.next);
        if (qn.next != null) qn.next.increaseRef();

        this.garbageCollector();

        List<String> ret = new ArrayList<>();
        ret.add(msgId.toString());
        ret.add(content);

        return ret;
    }

    private void garbageCollector() {
        while (this.head != null && this.head.refCount == 0) {
            this.pop();
        }
    }

    private void pop() {
        if (this.size == 0) return;
        --this.size;
        this.head = this.head.next;
    }

    public synchronized boolean sub(String subId) {
        if (this.isSubbed(subId)) return false;
        this.wasChanged = true;
        this.subs.put(subId, null);
        return true;
    }

    public synchronized boolean unsub(String subId) {
        if (!this.isSubbed(subId)) return false;

        this.wasChanged = true;

        QueueNode n = this.subs.get(subId);
        if (n != null) {
            n.decreaseRef();
            this.garbageCollector();
        }

        this.subs.remove(subId);
        return true;
    }

    public synchronized boolean isSubbed(String subId) {
        return this.subs.containsKey(subId);
    }

    public void resetChange() { this.wasChanged = false; }

    public void setSaved(boolean saved) { this.saved = saved; }

    public boolean isSaved() { return this.saved; }

    public boolean isChanged() { return this.wasChanged; }
}
