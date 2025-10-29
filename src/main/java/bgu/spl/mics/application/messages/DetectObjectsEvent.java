package bgu.spl.mics.application.messages;

import bgu.spl.mics.Event;
import bgu.spl.mics.application.objects.StampedDetectedObjects;

public class DetectObjectsEvent implements Event<Boolean> {
    private final StampedDetectedObjects detectedObjects;

    public DetectObjectsEvent(StampedDetectedObjects detectedObjects) {
        if (detectedObjects == null) {
            throw new IllegalArgumentException("DetectedObjects cannot be null");
        }
        this.detectedObjects = detectedObjects;
    }

    public StampedDetectedObjects getDetectedObjects() {
        return detectedObjects;
    }
}
