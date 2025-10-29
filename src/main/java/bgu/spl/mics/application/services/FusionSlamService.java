package bgu.spl.mics.application.services;

import bgu.spl.mics.Broadcast;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.*;
import bgu.spl.mics.application.objects.*;

import java.util.List;

public class FusionSlamService extends MicroService {
    private final FusionSlam fusionSlam;
    private final String outputFilePath = "output_file.json";

    /**
     * Constructor for FusionSlamService.
     *
     * @param fusionSlam The FusionSLAM object responsible for managing the global map.
     */
    public FusionSlamService(FusionSlam fusionSlam) {
        super("FusionSlam");
        this.fusionSlam = fusionSlam;
    }

    @Override
    protected void initialize() {
        System.out.println(getName() + " initialized.");

        subscribeBroadcast(TickBroadcast.class, this::handleTickBroadcast);

        subscribeEvent(TrackedObjectsEvent.class, this::handleTrackedObjectsEvent);

        subscribeEvent(PoseEvent.class, this::handlePoseEvent);

        subscribeBroadcast(CrashedBroadcast.class, this::handleCrashedBroadcast);

        subscribeBroadcast(TerminatedBroadcast.class, this::handleTerminatedBroadcast);
    }

    private void handleTickBroadcast(TickBroadcast tickBroadcast) {
        int currentTime = tickBroadcast.getTime();
        if (fusionSlam.shouldTerminateAtTime(currentTime)) {
            System.out.println("FusionSlamService: Terminating Fusion Slam...");
            sendBroadcast(new TerminatedBroadcast("FusionSlamService"));
            terminate();
        }
    }

    private void handleTrackedObjectsEvent(TrackedObjectsEvent event) {
        int detectionTime = event.getTime();
        List<TrackedObject> trackedObjects = event.getTrackedObjects();
        System.out.println("FusionSlamService: Processing " + trackedObjects.size() + " tracked objects at time " + detectionTime);
        fusionSlam.processTrackedObjects(trackedObjects, detectionTime);
    }

    private void handlePoseEvent(PoseEvent event) {
        Pose currentPose = event.getPose();
        if (currentPose != null) {
            System.out.println("FusionSlamService: Received PoseEvent at time " + currentPose.getTime());
            fusionSlam.addPose(currentPose);
        }
    }

    private void handleCrashedBroadcast(CrashedBroadcast crashed) {
        System.out.println("FusionSlamService: Received CrashedBroadcast from " + crashed.getMessage());
        fusionSlam.createErrorOutputFile(crashed.getMessage(), crashed.getId());
        sendBroadcast(new TerminatedBroadcast("FusionSlamService"));
        terminate();
    }

    private void handleTerminatedBroadcast(TerminatedBroadcast terminated) {
        synchronized (this) {
            if (fusionSlam.decreaseServiceCounter()) {
                System.out.println("FusionSlamService: All services have completed. Ending simulation...");
                fusionSlam.createOutputFile();
                sendBroadcast(new TerminatedBroadcast("FusionSlamService"));
                terminate();
            }
        }
    }
}
