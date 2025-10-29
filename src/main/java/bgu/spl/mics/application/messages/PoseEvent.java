package bgu.spl.mics.application.messages;

import bgu.spl.mics.Event;
import bgu.spl.mics.application.objects.Pose;

public class PoseEvent implements Event<Pose> {
    private final int time;
    private final Pose pose;

    public PoseEvent(int time, Pose pose) {
        this.time = time;
        this.pose = pose;
    }

    public int getTime() {
        return time;
    }

    public Pose getPose() {
        return pose;
    }
}
