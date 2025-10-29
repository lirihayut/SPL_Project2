package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a group of cloud points corresponding to a specific timestamp.
 * Used by the LiDAR system to store and process point cloud data for tracked objects.
 */
public class StampedCloudPoints {
    private String id;
    private int time;
    private List<CloudPoint> cloudPoints;

    /**
     * Constructor for StampedCloudPoints.
     *
     * @param id    The ID of the cloud point group.
     * @param time  The timestamp associated with this group of cloud points.
     */
    public StampedCloudPoints(String id, int time, List<CloudPoint> cloudpoints) {
        this.id = id;
        this.time = time;
        this.cloudPoints = cloudpoints;
    }

    public String getId() {
        return id;
    }

    public int getTime() {
        return time;
    }

    public List<CloudPoint> getCloudPoints() {
        return cloudPoints;
    }
}

