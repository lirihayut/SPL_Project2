package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents an object tracked by the LiDAR.
 * This object includes information about the tracked object's ID, description,
 * time of tracking, and coordinates in the environment.
 */
public class TrackedObject {
    private final String id;
    private final int time;
    private final String description;
    private final  List<CloudPoint> coordinates;

    public TrackedObject(DetectedObject detectedObject, int time,  List<CloudPoint> coordinates) {
        this.id = detectedObject.getId();
        this.time = time;
        this.description = detectedObject.getDescription();
        if (coordinates != null) {
            this.coordinates = coordinates;
        } else {
            this.coordinates = new ArrayList<>();
        }
    }

    public TrackedObject(String id, int time, String description, List<CloudPoint> coordinates) {
        this.id = id;
        this.time = time;
        this.description = description;
        if (coordinates != null) {
            this.coordinates = coordinates;
        } else {
            this.coordinates = new ArrayList<>();
        }
    }

    public String getId() {
        return id;
    }

    public int getTime() {
        return time;
    }

    public String getDescription() {
        return description;
    }

    public List<CloudPoint> getCoordinates() {
        return coordinates;
    }

    public void setId(String id) {
    }

    public void setTime(int time) {
    }

    @Override
    public String toString() {
        return "TrackedObject{" +
                "id='" + id + '\'' +
                ", time=" + time +
                ", description='" + description + '\'' +
                ", coordinates=" + coordinates +
                '}';
    }
}

