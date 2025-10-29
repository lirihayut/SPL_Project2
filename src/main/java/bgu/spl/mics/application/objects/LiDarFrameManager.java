package bgu.spl.mics.application.objects;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class LiDarFrameManager {
    private static LiDarFrameManager instance;
    private static final Lock lidarLock = new ReentrantLock();
    private static Map<String, List<TrackedObject>> lidarMap;

    private LiDarFrameManager() {
        if (lidarMap == null) {
            this.lidarMap = new ConcurrentHashMap<>();
        }
    }

    public static LiDarFrameManager getInstance() {
        if (instance == null) {
            instance = new LiDarFrameManager();
        }
        return instance;
    }

    public Map<String, List<TrackedObject>> getLiDarMap() {
        return lidarMap;
    }

    public static void updateLiDarMap(String lidarId, List<TrackedObject> trackedObjects) {
        lidarLock.lock();
        try {
            if (lidarMap == null) {
                lidarMap = new ConcurrentHashMap<>();
            }
            lidarMap.put(lidarId, trackedObjects);
        } finally {
            lidarLock.unlock();
        }
    }
}
