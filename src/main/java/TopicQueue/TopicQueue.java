package TopicQueue;

import java.util.HashSet;
import java.util.Set;

public class TopicQueue {
    // first node
    private QueueNode head;
    // last node
    private QueueNode tail;
    // queue size
    private int size;
    private Set<String> subs;

    public TopicQueue() {
        this.head = null;
        this.tail = null;
        this.size = 0;
        this.subs = new HashSet<>();
    }

    public void push(String content) {
        if (this.size == 0) this.head = this.tail = new QueueNode(content);
        else this.tail = new QueueNode(content, this.tail);

        ++this.size;
    }

    public String pop() {
        if (this.size == 0) return null;
        --this.size;

        String ret = this.head.getContent();
        this.head = this.head.next;
        return ret;
    }

    public int size() {
        return size;
    }

    public boolean sub(String subId) {
        if (this.subs.contains(subId)) return false;
        this.subs.add(subId);
        return true;
    }

    public boolean unsub(String subId) {
        if (!this.subs.contains(subId)) return false;
        this.subs.remove(subId);
        return true;
    }

    public boolean isSubbed(String subId) {
        return this.subs.contains(subId);
    }
}
