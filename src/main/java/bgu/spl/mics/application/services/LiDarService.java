package bgu.spl.mics.application.services;

import bgu.spl.mics.Broadcast;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.*;
import bgu.spl.mics.application.objects.*;

import java.util.ArrayList;
import java.util.List;

/**
 * LiDarService is responsible for processing data from the LiDAR sensor and
 * sending TrackedObjectsEvents to the FusionSLAM service.
 */
public class LiDarService extends MicroService {
    private final LiDarWorkerTracker liDarWorkerTracker;
    private final LiDarDataBase liDarDataBase;
    private final List<TrackedObject> waitingTrackedObjects;
    private int currentTick;

    public LiDarService(LiDarWorkerTracker LiDarWorkerTracker) {
        super("LiDarService" + LiDarWorkerTracker.getId());
        this.liDarWorkerTracker = LiDarWorkerTracker;
        this.liDarDataBase = LiDarDataBase.getInstance();
        this.waitingTrackedObjects = new ArrayList<>();
        this.currentTick = 0;

        System.out.println("LiDarService initialized for LiDarWorkerTracker ID: " + liDarWorkerTracker.getId());
    }

    @Override
    protected void initialize() {
        System.out.println(getName() + " started");

        subscribeBroadcast(TerminatedBroadcast.class, this::handleTerminatedBroadcast);
        subscribeBroadcast(CrashedBroadcast.class, this::handleCrashedBroadcast);
        subscribeBroadcast(TickBroadcast.class, this::handleTickBroadcast);
        subscribeEvent(DetectObjectsEvent.class, this::handleDetectObjectsEvent);
    }

    private void handleTerminatedBroadcast(TerminatedBroadcast terminated) {
        if(terminated.getMessage() == "TimeService") {
            terminate();
        }
        System.out.println(getName() + ": Received TerminatedBroadcast - " + terminated.getMessage());
    }

    private void handleCrashedBroadcast(CrashedBroadcast crashed) {
        System.out.println(getName() + ": Received CrashedBroadcast from " + crashed.getMessage());
        liDarWorkerTracker.setStatus(STATUS.ERROR);
        terminate();
    }

    private void handleTickBroadcast(TickBroadcast tick) {
        currentTick = tick.getTime();

        if (liDarDataBase.getLastTime() + liDarWorkerTracker.getFrequency() < currentTick) {
            liDarWorkerTracker.setStatus(STATUS.DOWN);
            sendBroadcast(new TerminatedBroadcast(getName()));
            terminate();
            return;
        }

        if (detectAndHandleErrors()) return;

        processWaitingTrackedObjects();
    }


    private boolean detectAndHandleErrors() {
        if (liDarWorkerTracker.detectError(currentTick, liDarDataBase)) {
            System.out.println(getName() + ": ERROR detected. Sending CrashedBroadcast and terminating.");
            sendBroadcast(new CrashedBroadcast(getName(), "Sensor " + getName() + " disconnected"));
            terminate();
            return true;
        }
        return false;
    }

    private void processWaitingTrackedObjects() {
        List<TrackedObject> toSend = new ArrayList<>();
        for (TrackedObject object : waitingTrackedObjects) {
            if (currentTick >= object.getTime() + liDarWorkerTracker.getFrequency()) {
                toSend.add(object);
            }
        }

        if (!toSend.isEmpty()) {
            sendTrackedObjectsEvent(toSend);
            waitingTrackedObjects.removeAll(toSend);
        }
    }

    private void handleDetectObjectsEvent(DetectObjectsEvent event) {
        int detectionTime = event.getDetectedObjects().getTime();
        List<TrackedObject> trackedObjects = liDarWorkerTracker.processDetectedObjects(
                event.getDetectedObjects().getDetectedObjects(),
                detectionTime,
                liDarDataBase
        );

        if (!trackedObjects.isEmpty()) {
            if (currentTick >= detectionTime + liDarWorkerTracker.getFrequency()) {
                sendTrackedObjectsEvent(trackedObjects);
            } else {
                waitingTrackedObjects.addAll(trackedObjects);
            }
        }
    }

    private void sendTrackedObjectsEvent(List<TrackedObject> trackedObjects) {
        int detectionTime = trackedObjects.get(0).getTime();
        StatisticalFolder.getInstance().incrementNumTrackedObjects(trackedObjects.size());
        TrackedObjectsEvent trackedEvent = new TrackedObjectsEvent(detectionTime, trackedObjects);
        sendEvent(trackedEvent);
        LiDarFrameManager.updateLiDarMap(liDarWorkerTracker.getId(), liDarWorkerTracker.getLastTrackedObjects());
        System.out.println(getName() + ": Sent TrackedObjectsEvent at tick " + currentTick);
    }
}
