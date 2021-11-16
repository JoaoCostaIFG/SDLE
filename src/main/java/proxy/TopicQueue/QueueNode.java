package proxy.TopicQueue;

import java.io.Serializable;

public class QueueNode implements Serializable {
    private final String content;
    protected QueueNode next;
    protected int refCount;

    public QueueNode(String content) {
        this.content = content;
        this.next = null;
        this.refCount = 0;
    }

    public QueueNode(String content, QueueNode prev) {
        this(content);
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
