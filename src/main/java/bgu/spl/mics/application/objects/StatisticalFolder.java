package bgu.spl.mics.application.objects;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Holds statistical information about the system's operation.
 * This class aggregates metrics such as the runtime of the system,
 * the number of objects detected and tracked, and the number of landmarks identified.
 * <p>
 * Implements Singleton pattern to ensure only one instance of the class exists.
 */
public class StatisticalFolder {

    // Static instance of the class for Singleton pattern
    private static StatisticalFolder instance ;

    // AtomicInteger fields to track system statistics
    private final AtomicInteger systemRuntime = new AtomicInteger(0);
    private final AtomicInteger numDetectedObjects = new AtomicInteger(0);
    private final AtomicInteger numTrackedObjects = new AtomicInteger(0);
    private final AtomicInteger numLandmarks = new AtomicInteger(0);

    private static final List<LandMark> statisticalLandmarks = new CopyOnWriteArrayList<>();

    /**
     * Private constructor to prevent instantiation from outside the class.
     */
    private static class SingletonHolder {
        private static final StatisticalFolder instance = new StatisticalFolder();
    }

    private StatisticalFolder() {}

    public static StatisticalFolder getInstance() {
        return SingletonHolder.instance;
    }
    /**
     * Increments the system's runtime by 1.
     */
    public void incrementSystemRuntime() {
        systemRuntime.incrementAndGet() ;
    }

    /**
     * Increments the number of detected objects by the specified size.
     *
     * @param size The number of detected objects to add.
     */
    public void incrementNumDetectedObjects(int size) {
        numDetectedObjects.addAndGet(size);
        System.out.println("Number of detected objects: " + numDetectedObjects);
    }

    /**
     * Increments the number of tracked objects by the specified size.
     *
     * @param size The number of tracked objects to add.
     */
    public void incrementNumTrackedObjects(int size) {
        numTrackedObjects.addAndGet(size);
    }

    /**
     * Increments the number of landmarks identified by 1.
     */
    public void incrementNumLandmarks(int size)  {numLandmarks.addAndGet(size);}

    public int getSystemRuntime() {
        return systemRuntime.get();
    }

    public int getNumDetectedObjects() {
        return numDetectedObjects.get();
    }

    public int getNumTrackedObjects() {
        return numTrackedObjects.get();
    }

    public int getNumLandmarks() {
        return numLandmarks.get();
    }

}
