package bgu.spl.mics.application.objects;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

/**
 * DetectedObject represents an object detected by the camera.
 * It contains information such as the object's ID and description.
 */
public class DetectedObject {
    private String id;
    private String description;

    public DetectedObject(String id,String description) {
        this.id = id;
        this.description=description;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public String toString() {
        return "DetectedObject{id='" + id + "', description='" + description + "'}";
    }

    public void setId(String id) {
        this.id = id;
    }
}
