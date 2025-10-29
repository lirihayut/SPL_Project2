package bgu.spl.mics.application.services;

import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.CrashedBroadcast;
import bgu.spl.mics.application.messages.PoseEvent;
import bgu.spl.mics.application.messages.TerminatedBroadcast;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.GPSIMU;
import bgu.spl.mics.application.objects.Pose;
import bgu.spl.mics.application.objects.STATUS;

/**
 * PoseService is responsible for maintaining the robot's current pose (position and orientation)
 * and broadcasting PoseEvents at every tick.
 */
public class PoseService extends MicroService {
    private final GPSIMU gpsimu;
    private Pose currentPose;

    public PoseService(GPSIMU gpsimu) {
        super("PoseService");
        this.gpsimu = gpsimu;
        this.currentPose = new Pose(0, 0, 0, 0); // Default initial pose.
    }

    @Override
    protected void initialize() {
        System.out.println(getName() + " started.");

        subscribeBroadcast(TerminatedBroadcast.class, this::handleTerminatedBroadcast);
        subscribeBroadcast(TickBroadcast.class, this::handleTickBroadcast);
        subscribeBroadcast(CrashedBroadcast.class, this::handleCrashedBroadcast);
    }

    private void handleTickBroadcast(TickBroadcast tickBroadcast) {
        int currentTime = tickBroadcast.getTime();

        gpsimu.setCurrentTick(currentTime);

        if (gpsimu.isLastTick(currentTime)) {
            System.out.println(getName() + ":GPSIMU terminating...");
            gpsimu.setStatus(STATUS.DOWN);
            sendBroadcast(new TerminatedBroadcast(getName()));
            terminate();
            return;
        }

        else {
            updateCurrentPose(currentTime);
            sendPoseEvent(currentTime);
        }
    }

    private void updateCurrentPose(int time) {
        Pose newPose = gpsimu.getPoseByTime(time);
        if (newPose != null) {
            currentPose = newPose;
        }
    }

    private void sendPoseEvent(int time) {
        PoseEvent poseEvent = new PoseEvent(time, currentPose);
        sendEvent(poseEvent);
        System.out.println(getName() + ": Sent PoseEvent for time " + time + ".");
    }

    private void handleCrashedBroadcast(CrashedBroadcast crashedBroadcast) {
        System.out.println(getName() + ": Received CrashedBroadcast, terminating.");
        gpsimu.setStatus(STATUS.ERROR);
        terminate();
    }

    private void handleTerminatedBroadcast(TerminatedBroadcast terminatedBroadcast) {
        if (terminatedBroadcast.getMessage() == "TimeService") {
            terminate();
        }
    }
}
