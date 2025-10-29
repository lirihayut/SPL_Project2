package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.List;

/**
 * LiDarWorkerTracker is responsible for managing a LiDAR worker.
 * It processes DetectObjectsEvents and generates TrackedObjectsEvents by using data from the LiDarDataBase.
 * Each worker tracks objects and sends observations to the FusionSlam service.
 */
public class LiDarWorkerTracker {
    private String id;
    private int frequency;
    private STATUS status;
    private List<TrackedObject> lastTrackedObjects;


    public LiDarWorkerTracker(String id, int frequency) {
        this.id = id;
        this.frequency = frequency;
        this.status = STATUS.UP;
        this.lastTrackedObjects = new ArrayList<>();
    }

    public List<TrackedObject> processDetectedObjects(List<DetectedObject> detectedObjects, int detectionTime, LiDarDataBase dataBase) {
        if (detectedObjects == null || dataBase == null) {
            throw new NullPointerException("Detected objects or database is null");
        }
        List<TrackedObject> trackedObjects = new ArrayList<>();

        for (DetectedObject detected : detectedObjects) {
            List<StampedCloudPoints> stampedPoints = dataBase.getStampedCloudPointsAtTime(detectionTime);
            for (StampedCloudPoints stampedCloudPoint : stampedPoints) {
                if (stampedCloudPoint.getId().equals(detected.getId())) {
                    TrackedObject trackedObject = new TrackedObject(
                            detected,
                            detectionTime,
                            stampedCloudPoint.getCloudPoints()
                    );
                    trackedObjects.add(trackedObject);
                }
            }
        }
        lastTrackedObjects = trackedObjects;

        return trackedObjects;
    }

    public boolean detectError(int currentTime, LiDarDataBase dataBase) {
        List<StampedCloudPoints> stampedCloudPointsList = dataBase.getStampedCloudPointsAtTime(currentTime);
        for (StampedCloudPoints stampedCloudPoints : stampedCloudPointsList) {
            if ("ERROR".equals(stampedCloudPoints.getId())) {
                System.out.println("LiDAR Worker " + id + ": ERROR detected at time " + currentTime);
                return true;
            }
        }

        return false;
    }



    public String getId() {
        return id;
    }

    public int getFrequency() {
        return frequency;
    }

    public void setStatus(STATUS status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "LiDarWorkerTracker [id=" + id + ", frequency=" + frequency + ", status=" + status + "]";
    }

    public List<TrackedObject> getLastTrackedObjects() {
        return lastTrackedObjects;
    }

}

