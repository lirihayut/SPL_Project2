package bgu.spl.mics.application.objects;

import bgu.spl.mics.application.input.Configuration;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Represents the robot's GPS and IMU system.
 * Provides information about the robot's position and movement.
 */
public class GPSIMU {
    private int currentTick;
    private STATUS status;
    private List<Pose> poseList;

    public GPSIMU() {
        this.currentTick = 0;
        this.status = STATUS.UP;
        this.poseList = new ArrayList<>();
        initializePoseList();
    }


    public Pose getPoseByTime(int time) {
        for (Pose pose : poseList) {
            if (pose.getTime() == time) {
                return pose;
            }
        }
        return null;
    }


    public boolean isLastTick(int currentTick) {
        if (poseList.isEmpty()) {
            return false;
        }
        int lastPose = poseList.get(poseList.size() - 1).getTime();
        return currentTick >= lastPose;
    }


    /**
     * Loads pose data from a JSON file.
     * Clears the existing pose list before loading new data.
     *
     */
    private void initializePoseList() {
        try {
            Configuration config = Configuration.getInstance(null);
            this.poseList = config.loadPoseList();
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize PoseList in GPSIMU", e);
        }
    }


    public void setStatus(STATUS status) {
        this.status = status;
    }

    public void setCurrentTick(int currentTick) {
        this.currentTick = currentTick;
    }

}