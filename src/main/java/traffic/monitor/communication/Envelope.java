package traffic.monitor.communication;

public class Envelope {

    public enum MessageType {
        ACTIVATE,
        DRONE_ACTIVE,
        SHUTDOWN,
        DRONE_INACTIVE,
        WAYPOINT,
        TRAFFIC_REPORT,
        ASSERT_TUBE_NEARBY_REQUEST,
        ASSERT_TUBE_NEARBY_RESPONSE;
    }

    private final MessageType type;

    private final Object      message;

    private final Long        senderId;

    private final Long        receiverId;

    public <M> Envelope(Long senderId, Long receiverId, MessageType messageType, M message) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.type = messageType;
        this.message = message;
    }

    public <M> Envelope(Long sender, Long receiver, MessageType messageType) {
        this(sender, receiver, messageType, null);
    }

    public MessageType getType() {
        return type;
    }

    /**
     * Returns the message contained in the envelope. It is the caller
     * responsibility to know the type of the content.
     * 
     * @return
     */
    @SuppressWarnings("unchecked")
    public <M> M getMessage() {
        // TODO Additional type checks
        // could be enforced by mapping each entry of the MessageType eunum to a
        // content class, and checking the message on it's way in through the
        // constructor.

        return (M) message;
    }

    public Long getSenderId() {
        return senderId;
    }

    public Long getReceiverId() {
        return receiverId;
    }

}
