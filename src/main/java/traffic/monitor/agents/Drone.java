package traffic.monitor.agents;

import java.util.PriorityQueue;
import java.util.Queue;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;

import traffic.monitor.communication.Channel;
import traffic.monitor.communication.Envelope;
import traffic.monitor.communication.Envelope.MessageType;
import traffic.monitor.data.Location;
import traffic.monitor.data.TrafficReport;
import traffic.monitor.data.WayPoint;

public class Drone {

    private static final Logger   LOG = Logger.getLogger(Drone.class);

    private Long                  id;

    @Value("#{environment['dispatcher.id']}")
    private Long                  dispatcherId;

    @Value("#{environment['drone.speed']}")
    private Double                speed;

    @Value("#{environment['max.drone.mem.capacity']}")
    private Integer               maxDroneMemCapacity;

    @Resource
    private Channel               channel;

    private final Queue<WayPoint> wayPoints;

    private Location              currentLocation;

    private boolean               listenToIncomingMessages;

    public Drone(Long id) {
        this.id = id;
        wayPoints = new PriorityQueue<>();
        currentLocation = new Location(0.0, 0.0);
    }

    @PostConstruct
    private void init() {
        Thread thread = new Thread(new Runnable() {
            public void run() {
                listenToIncomingMessages = true;
                LOG.info(Drone.this.id + " ready to intercept messages.");
                while (listenToIncomingMessages || !wayPoints.isEmpty()) {
                    if (wayPoints.isEmpty()) {
                        handleIncomingMessage(channel.retreiveMessage(id));
                    } else {
                        WayPoint wayPoint = wayPoints.poll();
                        Location location = wayPoint.getLocation();
                        goTo(location);
                        if (assertNearByTubeProximity(location)) {
                            sendTrafficReport(wayPoint);
                        }
                    }
                }
                // after the loop we know that the dispatcher requested the
                // shutdown and that no waypoints remain to be visited
                channel.sendMessage(new Envelope(id, dispatcherId, MessageType.DRONE_INACTIVE));
            }
        });
        thread.setName("Drone :: " + id + " Thread");
        thread.start();
    }

    private void handleIncomingMessage(Envelope envelope) {
        switch (envelope.getType()) {
        case ACTIVATE:
            LOG.info(id + " received activation signal. Awaiting way points...");
            channel.sendMessage(new Envelope(id, dispatcherId, MessageType.DRONE_ACTIVE));
            break;
        case WAYPOINT:
            Queue<WayPoint> newWayPoints = envelope.getMessage();
            LOG.info(id + ", " + newWayPoints.size() + " way points received");
            if (wayPoints.size() + newWayPoints.size() <= maxDroneMemCapacity) {
                wayPoints.addAll(newWayPoints);
            } else {
                // TODO keep as many waypoints as possible instead of rejecting
                // the entire message
                // TODO implement a notification mechanism using the channel to
                // let the dispatcher know that some waypoints were rejected
                // instead of relying on it to keep track of the drone memory
                // load
                LOG.warn(id + " out of memory, last message will be discarted");
            }
        case SHUTDOWN:
            LOG.info(id + " inactivation signal received. Stoping activity after consuming last waypoint.");
            listenToIncomingMessages = false;
        default:
            break;
        }
    }

    private void sendTrafficReport(WayPoint wayPoint) {
        LOG.info(id + " sending traffic report");
        channel.sendMessage(new Envelope(id, dispatcherId, MessageType.TRAFFIC_REPORT, TrafficReport.randomInstance(id, wayPoint.getDate())));
    }

    private boolean assertNearByTubeProximity(Location location) {
        LOG.info(id + " requesting confirmation of nearby tube station.");
        channel.sendMessage(new Envelope(id, dispatcherId, MessageType.ASSERT_TUBE_NEARBY_REQUEST, location));
        Boolean isOkToSendReport = null;
        while (isOkToSendReport == null) {
            Envelope envelope = channel.retreiveMessage(id);
            if (envelope.getType() == MessageType.ASSERT_TUBE_NEARBY_RESPONSE) {
                isOkToSendReport = envelope.getMessage();
            } else {
                handleIncomingMessage(envelope);
            }
        }
        LOG.info(id + " assertion of nearby tube station is :: " + isOkToSendReport);
        return isOkToSendReport;
    }

    private void goTo(Location newLocation) {
        LOG.info(id + " moving to :: " + newLocation);
        Double distance = newLocation.distanceTo(currentLocation);
        LOG.info(id + " distance to cover is :: " + distance);
        Long travelTime = Double.valueOf(distance / speed).longValue();
        LOG.info(id + " estimated travel time :: " + travelTime);
        try {
            // simulate flight delay
            Thread.sleep(travelTime);
        } catch (InterruptedException e) {
            LOG.info(id + " unable to travel to :: " + newLocation);
            throw new RuntimeException(e);
        }
        currentLocation = newLocation;
        LOG.info(id + " arrived at :: " + currentLocation);
    }
}
