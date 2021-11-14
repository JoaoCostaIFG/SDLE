package TopicQueue;

public class QueueNode {
    private final String content;
    protected QueueNode next;

    public QueueNode(String content, QueueNode prev) {
        this.content = content;
        prev.next = this;
    }

    public QueueNode(String content) {
        this.content = content;
        this.next = null;
    }

    public String getContent() {
        return content;
    }
}
