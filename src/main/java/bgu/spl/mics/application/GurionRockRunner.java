package bgu.spl.mics.application;

import java.util.List;
import bgu.spl.mics.MessageBusImpl;
import bgu.spl.mics.application.input.CameraConfiguration;
import bgu.spl.mics.application.input.Configuration;
import bgu.spl.mics.application.input.LidarConfig;
import bgu.spl.mics.application.objects.*;
import bgu.spl.mics.application.services.*;

/**
 * The main entry point for the GurionRock Pro Max Ultra Over 9000 simulation.
 * This class initializes the system and starts the simulation by setting up
 * services, objects, and configurations.
 */
public class GurionRockRunner {

    /**
     * The main method of the simulation.
     * Sets up the necessary components, parses configuration files,
     * initializes services, and starts the simulation.
     *
     * @param args Command-line arguments. The first argument is expected to be the path to the configuration file.
     */
    public static void main(String[] args) {
        System.out.println("Starting simulation...");

        if (args.length < 1) {
            System.err.println("Error: Base path for configuration file is required as the first argument.");
            return;
        }

        // Build configuration file path
        String configFilePath = args[0];
        if (args.length > 1) {
            configFilePath = configFilePath  +" "+ args[1];
        }
        System.out.println("Using configuration file path: " + configFilePath);

        int microServicesCnt = 0;

        try {
            // Load configuration
            System.out.println("Loading configuration...");
            Configuration config = Configuration.getInstance(configFilePath);
            System.out.println("Configuration loaded: " + config);


            // Initialize LiDAR data
            config.initializeLiDarDataBase();
            System.out.println("LiDAR data initialized successfully.");

            // Initialize MessageBus
            System.out.println("Initializing MessageBus...");
            MessageBusImpl messageBus = MessageBusImpl.getInstance();
            System.out.println("MessageBus initialized.");

            // Initialize PoseService
            System.out.println("Initializing PoseService...");
            GPSIMU gpsimu = new GPSIMU();
            PoseService poseService = new PoseService(gpsimu);
            Thread poseThread = new Thread(poseService, "PoseService");
            poseThread.start();
            microServicesCnt++;
            System.out.println("PoseService started.");

            // Initialize Cameras and Camera Services
            System.out.println("Initializing Camera Services...");
            List<Camera> cameras = CameraConfiguration.getCameras();
            for (Camera camera : cameras) {
                System.out.println("Initializing CameraService for Camera ID: " + camera.getId());
                CameraService cameraService = new CameraService(camera);
                Thread cameraThread = new Thread(cameraService, "CameraService-" + camera.getId());
                cameraThread.start();
                microServicesCnt++;
                System.out.println("CameraService started for Camera ID: " + camera.getId());
            }

            // Initialize LiDAR Worker Services
            System.out.println("Initializing LiDAR Worker Services...");
            List<LidarConfig> lidarConfigs = config.getLidarWorkers().getLidarConfigurations();
            for (LidarConfig lidarConfig : lidarConfigs) {
                System.out.println("Initializing LiDAR Worker for ID: " + lidarConfig.getId());
                LiDarWorkerTracker lidarTracker = new LiDarWorkerTracker(lidarConfig.getId(), lidarConfig.getFrequency());
                LiDarService lidarService = new LiDarService(lidarTracker);
                Thread lidarThread = new Thread(lidarService, "LiDarService-" + lidarConfig.getId());
                lidarThread.start();
                microServicesCnt++;
                System.out.println("LiDAR Worker started for ID: " + lidarConfig.getId());
            }

            // Initialize Fusion-SLAM
            System.out.println("Initializing Fusion-SLAM...");
            FusionSlam fusionSlam = FusionSlam.getInstance(microServicesCnt , config.getDuration(), configFilePath);
            FusionSlamService fusionSlamService = new FusionSlamService(fusionSlam);
            Thread fusionThread = new Thread(fusionSlamService, "FusionSlamService");
            fusionThread.start();
            System.out.println("Fusion-SLAM initialized and service started.");

            // Initialize TimeService
            System.out.println("Initializing TimeService...");
            TimeService timeService = new TimeService(config.getTickTime(), config.getDuration());
            Thread timeThread = new Thread(timeService, "TimeService");
            timeThread.start();
            System.out.println("TimeService started.");


            // Wait for TimeService to complete
            System.out.println("Waiting for TimeService to finish...");
            timeThread.join();
            System.out.println("TimeService completed.");

            //fusionSlam.createOutputFile();
            //System.out.println("Output file generated successfully.");

        } catch (RuntimeException e) {
            System.err.println("Error occurred during simulation: " + e.getMessage());
            e.printStackTrace();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Simulation interrupted by interruption: " + e.getMessage());
            e.printStackTrace();
        } finally {
            System.out.println("Simulation finished.");
        }
    }
}
