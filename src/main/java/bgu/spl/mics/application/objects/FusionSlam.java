package bgu.spl.mics.application.objects;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import com.google.gson.Gson;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Manages the fusion of sensor data for simultaneous localization and mapping (SLAM).
 * Combines data from multiple sensors (e.g., LiDAR, camera) to build and update a global map.
 * Implements the Singleton pattern to ensure a single instance of FusionSlam exists.
 */
public class FusionSlam {
    private static List<LandMark> landmarkList;
    private static List<Pose> poseList;
    private final AtomicInteger serviceCount;
    private static volatile boolean outputFileCreated;
    private final int duration;
    private final String configFilePath;

    FusionSlam(int initialServiceCount, int duration,String configFilePath) {
        landmarkList = new ArrayList<>();
        poseList = new ArrayList<>();
        this.serviceCount = new AtomicInteger(initialServiceCount);
        outputFileCreated = false;
        this.duration = duration;
        this.configFilePath=configFilePath;
    }

    public List<LandMark> getLandMarks() {
        return landmarkList;
    }

    private static class SingletonHolder {
        private static FusionSlam instance;

        private static FusionSlam getInstance(int initialServiceCount,int duration,String configFilePathString) {
            if (instance == null) {
                instance = new FusionSlam(initialServiceCount,duration,configFilePathString);
            }
            return instance;
        }
    }

    public static FusionSlam getInstance(int initialServiceCount, int duration, String configFilePathString) {
        return SingletonHolder.getInstance(initialServiceCount,duration,configFilePathString);
    }

    public void addLandmark(LandMark landmark) {
        landmarkList.add(landmark);
    }

    public void addPose(Pose pose) {
        poseList.add(pose);
    }

    public Pose getPoseByTime(int timestamp) {
        for (Pose pose : poseList) {
            if (pose.getTime() == timestamp) {
                return pose;
            }
        }
        return null;
    }

    public LandMark findLandmarkById(String id) {
        for (LandMark landmark : landmarkList) {
            if (landmark.getId().equals(id)) {
                return landmark;
            }
        }
        return null;
    }

    public void processTrackedObjects(List<TrackedObject> trackedObjects, int detectionTime) {
        Pose poseAtTime = getPoseByTime(detectionTime);
        if (poseAtTime == null) {
            System.out.println("Pose not found for time: " + detectionTime);
            return;
        }

        for (TrackedObject object : trackedObjects) {
            System.out.println("Processing object with ID: " + object.getId());
            List<CloudPoint> transformedCoordinates = new ArrayList<>();

            for (CloudPoint point : object.getCoordinates()) {
                double radYaw = Math.toRadians(poseAtTime.getYaw());
                double cosTheta = Math.cos(radYaw);
                double sinTheta = Math.sin(radYaw);

                double globalX = cosTheta * point.getX() - sinTheta * point.getY() + poseAtTime.getX();
                double globalY = sinTheta * point.getX() + cosTheta * point.getY() + poseAtTime.getY();

                transformedCoordinates.add(new CloudPoint(globalX, globalY));
            }

            LandMark existingLandmark = findLandmarkById(object.getId());
            if (existingLandmark == null) {
                System.out.println("Adding new landmark with ID: " + object.getId());
                addLandmark(new LandMark(object.getId(), object.getDescription(), transformedCoordinates));
                StatisticalFolder.getInstance().incrementNumLandmarks(1);
            } else {
                existingLandmark.updateCoordinates(transformedCoordinates);
                System.out.println("Updated existing landmark with ID: " + object.getId());
            }
        }
    }

    public boolean decreaseServiceCounter() {
        serviceCount.decrementAndGet();
        if(serviceCount.get() == 0 && !outputFileCreated) {
            return false;
        }
        return true;
    }

    public boolean shouldTerminateAtTime(int currentTime) {
        if (serviceCount.get() == 0 || isDurationPassed(currentTime)) {
            if (!outputFileCreated) {
                outputFileCreated = true;
                createOutputFile();
            }
            return true;
        }
        return false;
    }


    public boolean isDurationPassed(int currentTime) {
        if (currentTime >= duration ) {
            return true;
        }
        return false;
    }

    public String getOutputFilePath() {
        Path configFilePathObj = Paths.get(configFilePath);
        Path parentDirectory = configFilePathObj.getParent();
        return parentDirectory.resolve("output_file.json").toString();
    }

    public String getErrorOutputFilePath() {
        Path configFilePathObj = Paths.get(configFilePath);
        Path parentDirectory = configFilePathObj.getParent();
        return parentDirectory.resolve("OutputError.json").toString();
    }


    public void createOutputFile() {
        if (outputFileCreated) return;
        outputFileCreated = true;
        System.out.println("FusionSlam: Creating output file...");

        String outputFilePath = getOutputFilePath();
        if (outputFilePath == null) {
            System.err.println("FusionSlam: Could not determine output file path.");
            return;
        }

        try (FileWriter writer = new FileWriter(outputFilePath)) {
            writer.write("{\"systemRuntime\":" + StatisticalFolder.getInstance().getSystemRuntime() +
                    ",\"numDetectedObjects\":" + StatisticalFolder.getInstance().getNumDetectedObjects() +
                    ",\"numTrackedObjects\":" + StatisticalFolder.getInstance().getNumTrackedObjects() +
                    ",\"numLandmarks\":" + StatisticalFolder.getInstance().getNumLandmarks() + ",");

            writer.write("\n");
            writer.write("\"landMarks\":{\n");
            for (int i = 0; i < landmarkList.size(); i++) {
                LandMark landMark = landmarkList.get(i);
                writer.write("\"" + landMark.getId() + "\":{\"id\":\"" + landMark.getId() +
                        "\",\"description\":\"" + landMark.getDescription() +
                        "\",\"coordinates\":" + new Gson().toJson(landMark.getCoordinates()) + "}");

                if (i < landmarkList.size() - 1) {
                    writer.write(",\n");
                }
            }
            writer.write("\n}}");
            System.out.println("FusionSlam: Output file created successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void createErrorOutputFile(String errorMessage, String faultySensor) {
        if (outputFileCreated) return;
        outputFileCreated = true;
        System.out.println("FusionSlam: Creating error output file...");

        String outputFilePath = getErrorOutputFilePath();
        if (outputFilePath == null) {
            System.err.println("FusionSlam: Could not determine output file path.");
            return;
        }

        try (FileWriter writer = new FileWriter(outputFilePath)) {
            writer.write("{\n");

            writer.write("  \"{error\": \"" + errorMessage + "\",\n");
            writer.write("  \"faultySensor\": \"" + faultySensor + "\",\n");

            writer.write("  \"lastCamerasFrame\": {\n");
            Map<String, StampedDetectedObjects> cameraFrames = CameraFrameManager.getInstance().getCameraMap();
            int cameraCount = 0;
            for (Map.Entry<String, StampedDetectedObjects> entry : cameraFrames.entrySet()) {
                writer.write("    \"Camera" + entry.getKey() + "\": " + new Gson().toJson(entry.getValue()));
                if (++cameraCount < cameraFrames.size()) {
                    writer.write(",");
                }
                writer.write("\n");
            }
            writer.write("  },\n");

            writer.write("  \"lastLiDarWorkerTrackersFrame\": {\n");
            Map<String, List<TrackedObject>> lidarFrames = LiDarFrameManager.getInstance().getLiDarMap();
            int lidarCount = 0;
            for (Map.Entry<String, List<TrackedObject>> entry : lidarFrames.entrySet()) {
                writer.write("    \"LiDarWorkerTracker" + entry.getKey() + "\": " + new Gson().toJson(entry.getValue()));
                if (++lidarCount < lidarFrames.size()) {
                    writer.write(",");
                }
                writer.write("\n");
            }
            writer.write("  },\n");

            writer.write("  \"poses\": [");
            for (int i = 0; i < poseList.size(); i++) {
                Pose pose = poseList.get(i);
                writer.write("{\"time\": " + pose.getTime() +
                        ", \"x\": " + pose.getX() +
                        ", \"y\": " + pose.getY() +
                        ", \"yaw\": " + pose.getYaw() + "}");

                if (i < poseList.size() - 1) {
                    writer.write(",");
                }
            }
            writer.write("  ],\n");

            writer.write("\"statistics\":{\"systemRuntime\":" + StatisticalFolder.getInstance().getSystemRuntime() +
                    ",\"numDetectedObjects\":" + StatisticalFolder.getInstance().getNumDetectedObjects() +
                    ",\"numTrackedObjects\":" + StatisticalFolder.getInstance().getNumTrackedObjects() +
                    ",\"numLandmarks\":" + StatisticalFolder.getInstance().getNumLandmarks() + ",");

            writer.write("\"landMarks\":{");
            for (int i = 0; i < landmarkList.size(); i++) {
                LandMark landMark = landmarkList.get(i);
                writer.write("\"" + landMark.getId() + "\":" + new Gson().toJson(landMark));
                if (i < landmarkList.size() - 1) {
                    writer.write(",");
                }
            }
            writer.write("}}");

            writer.write("}");
            System.out.println("FusionSlam: Error output file created successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
