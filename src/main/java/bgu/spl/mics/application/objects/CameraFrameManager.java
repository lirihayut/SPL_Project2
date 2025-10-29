package bgu.spl.mics.application.objects;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class CameraFrameManager {
    private static CameraFrameManager instance;

    private static Map<String, StampedDetectedObjects> cameraMap;

    private CameraFrameManager() {
        if (cameraMap == null) {
            this.cameraMap = new ConcurrentHashMap<>();
        }
    }

    public static CameraFrameManager getInstance() {
        if (instance == null) {
            instance = new CameraFrameManager();
        }
        return instance;
    }

    public static void updateCameraMap(String cameraId, StampedDetectedObjects stampedDetectedObject) {
        if (cameraMap == null) {
            cameraMap= new ConcurrentHashMap<>();
        }
        cameraMap.put(cameraId, stampedDetectedObject);
    }

    public Map<String, StampedDetectedObjects> getCameraMap() {
        return cameraMap;
    }
}
