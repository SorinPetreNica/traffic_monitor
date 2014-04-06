package traffic.monitor.communication;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.log4j.Logger;

import traffic.monitor.util.DateUtil;

public class InMemoryChannel implements Channel {

    private static final Logger                                LOG = Logger.getLogger(DateUtil.class);

    private final ConcurrentMap<Long, BlockingQueue<Envelope>> pipes;

    public InMemoryChannel() {
        pipes = new ConcurrentHashMap<>();
    }

    @Override
    public void sendMessage(Envelope envelope) {
        createReceiverPipeIfNeeded(envelope.getReceiverId());
        try {
            pipes.get(envelope.getReceiverId()).put(envelope);
        } catch (InterruptedException e) {
            LOG.error("Unable to send message :: " + envelope, e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Envelope retreiveMessage(Long receiverId) {
        createReceiverPipeIfNeeded(receiverId);
        try {
            return pipes.get(receiverId).take();
        } catch (InterruptedException e) {
            LOG.error("Unable to retreive message for receiver :: " + receiverId, e);
            throw new RuntimeException(e);
        }
    }

    private void createReceiverPipeIfNeeded(Long receiverId) {
        if (!pipes.containsKey(receiverId)) {
            synchronized (pipes) {
                // the entry could have been added after the unsynchronized
                // check
                if (!pipes.containsKey(receiverId)) {
                    pipes.put(receiverId, new LinkedBlockingQueue<Envelope>());
                }
            }
        }
    }

}
