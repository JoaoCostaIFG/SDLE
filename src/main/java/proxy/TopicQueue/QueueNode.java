package proxy.TopicQueue;

import java.io.Serializable;

public class QueueNode implements Serializable {
    private final String content;
    protected transient QueueNode next;
    protected int refCount;
    protected Integer id;

    public QueueNode(String content, Integer id) {
        this.content = content;
        this.next = null;
        this.refCount = 0;
        this.id = id;
    }

    public QueueNode(String content, QueueNode prev, Integer id) {
        this(content, id);
        prev.next = this;
    }

    public String getContent() {
        return content;
    }

    public void increaseRef() {
        ++this.refCount;
    }

    public void decreaseRef() {
        --this.refCount;
    }
}
