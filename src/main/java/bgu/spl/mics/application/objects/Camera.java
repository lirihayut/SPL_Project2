package bgu.spl.mics.application.objects;

import bgu.spl.mics.application.messages.DetectObjectsEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Represents a camera sensor on the robot.
 * Responsible for detecting objects in the environment.
 */
public class Camera {
    private final String id;
    private int frequency;
    private STATUS status;
    private List<StampedDetectedObjects> detectedObjectsList;
    private final Map<Integer, StampedDetectedObjects> eventLog;

    public Camera(String id, int frequency) {
        this.id = id;
        this.frequency = frequency;
        this.status = STATUS.UP;
        this.detectedObjectsList = new ArrayList<>();
        eventLog= new ConcurrentHashMap<>();
    }

    public String getId() {
        return id;
    }

    public void setStatus(STATUS status) {
        this.status = status;
    }

    public StampedDetectedObjects getObjectsAtTime(int currentTime) {
        for (StampedDetectedObjects sDetectedObjects : detectedObjectsList) {
            if (sDetectedObjects.getTime() == currentTime) {
                return sDetectedObjects;
            }
        }
        return null;
    }

    public List<DetectedObject> getDetectedObjectsAtTime(int currentTime) {
        for (StampedDetectedObjects sDetectedObjects : detectedObjectsList) {
            if (sDetectedObjects.getTime() == currentTime) {
                return sDetectedObjects.getDetectedObjects();
            }
        }
        return null;
    }

    public Map<Integer, StampedDetectedObjects> getEventLog() {
        return eventLog;
    }

    public void updateEventLog(int currentTime) {
        StampedDetectedObjects detectedObjects = getObjectsAtTime(currentTime);

        if (detectedObjects != null) {
            int eventTime = currentTime + frequency;
            eventLog.put(eventTime, detectedObjects);
        }
    }

    public String detectError(int currentTime) {
        List<DetectedObject> detectedObjects = getDetectedObjectsAtTime(currentTime);
        if (detectedObjects != null) {
            for (DetectedObject object : detectedObjects) {
                if ("ERROR".equals(object.getId())) {
                    return object.getDescription();
                }
            }
        }
        return null;
    }

    public boolean shouldTerminateAtTime(int currentTime) {
        int last = detectedObjectsList.get(detectedObjectsList.size() - 1).getTime() + frequency;
        return currentTime > last;
    }

    public void addDetectedObject(StampedDetectedObjects stampedObject) {
        detectedObjectsList.add(stampedObject);
    }
}
