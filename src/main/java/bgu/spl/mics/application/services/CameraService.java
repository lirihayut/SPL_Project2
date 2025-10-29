package bgu.spl.mics.application.services;

import bgu.spl.mics.Broadcast;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.DetectObjectsEvent;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.*;

import java.util.List;

/**
 * CameraService is responsible for processing data from the camera and
 * sending DetectObjectsEvents to LiDAR workers.
 *
 * This service interacts with the Camera object to detect objects and updates
 * the system's StatisticalFolder upon sending its observations.
 */
public class CameraService extends MicroService {
    private final Camera camera;

    public CameraService(Camera camera) {
        super("camera " + camera.getId());
        this.camera = camera;
    }

    @Override
    protected void initialize() {
        System.out.println(getName() + " is now active.");

        subscribeBroadcast(TickBroadcast.class, tick -> processTick(tick));

        subscribeBroadcast(TerminatedBroadcast.class, terminated -> {
            System.out.println(getName() + ": Received TerminatedBroadcast from " + terminated.getMessage());
            if(terminated.getMessage() == "TimeService") {
                terminate();
            }
        });

        subscribeBroadcast(CrashedBroadcast.class, crashed -> {
            System.out.println(getName() + ": CrashedBroadcast detected from " + crashed.getMessage());
            camera.setStatus(STATUS.ERROR);
            terminate();
        });
    }

    private void processTick(TickBroadcast tick) {
        int currentTime = tick.getTime();

        if (camera.shouldTerminateAtTime(currentTime)) {
            handleTermination();
            return;
        }

        String error = camera.detectError(currentTime);
        if (error != null) {
            System.out.println(getName() + ": Error detected in camera. Error details: " + error);
            sendBroadcast(new CrashedBroadcast(getName(), error));
            camera.setStatus(STATUS.ERROR);
            terminate();
            return;
        }

        camera.updateEventLog(currentTime);
        dispatchDetectionEvent(currentTime);
    }

    private void handleTermination() {
        camera.setStatus(STATUS.DOWN);
        sendBroadcast(new TerminatedBroadcast(getName()));
        terminate();
    }


    private void dispatchDetectionEvent(int currentTime) {
        StampedDetectedObjects detectedEvent = camera.getEventLog().remove(currentTime);
        if (detectedEvent != null) {
            int detectedCount = detectedEvent.getDetectedObjects().size();
            StatisticalFolder.getInstance().incrementNumDetectedObjects(detectedCount);
            sendEvent(new DetectObjectsEvent(detectedEvent));
            CameraFrameManager.updateCameraMap(camera.getId(), detectedEvent);

            System.out.println(getName() + ": DetectObjectsEvent dispatched. Time: " + currentTime + ", Detection time: " + detectedEvent.getTime());
        }
    }
}
