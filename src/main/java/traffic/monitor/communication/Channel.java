package traffic.monitor.communication;


public interface Channel {

    void sendMessage(Envelope envelope);

    Envelope retreiveMessage(Long receiverId);

}
