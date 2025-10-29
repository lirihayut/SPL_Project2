package bgu.spl.mics;

import bgu.spl.mics.application.objects.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * LiDarWorkerTrackerTest simulates the behavior of the LiDarWorkerTracker by providing mock data
 * and testing its functionality.
 */
public class LiDarWorkerTrackerTest {

    private LiDarWorkerTracker lidarWorker;
    private LiDarDataBase lidarDatabase;

    @BeforeEach
    void setUp() {
        // Initialize the LiDarWorkerTracker
        lidarWorker = new LiDarWorkerTracker("worker1", 5);

        // Initialize a mock LiDarDataBase with predefined data
        lidarDatabase = LiDarDataBase.getInstance();

        // Adding mock data to the LiDarDatabase
        addMockDataToDatabase();
    }

    private void addMockDataToDatabase() {
        // Create some mock CloudPoints for the objects
        List<CloudPoint> cloudPointsForChair = List.of(
                new CloudPoint(1.0, 2.0),
                new CloudPoint(1.5, 2.5)
        );
        List<CloudPoint> cloudPointsForTable = List.of(
                new CloudPoint(3.0, 4.0),
                new CloudPoint(3.5, 4.5)
        );

        // Create StampedCloudPoints with mock data
        StampedCloudPoints chairPoints = new StampedCloudPoints("Chair_1", 2, cloudPointsForChair);
        StampedCloudPoints tablePoints = new StampedCloudPoints("Table_1", 3, cloudPointsForTable);

        // Add the StampedCloudPoints objects to the database
        lidarDatabase.addDetectedObject(chairPoints);
        lidarDatabase.addDetectedObject(tablePoints);

        // Add the error StampedCloudPoints at time 5
        List<CloudPoint> cloudPointsForError = new ArrayList<>(); // No cloud points for error
        StampedCloudPoints errorPoints = new StampedCloudPoints("ERROR", 5, cloudPointsForError);
        lidarDatabase.addDetectedObject(errorPoints);
    }

    @Test
    void testProcessDetectedObjects() {
        List<DetectedObject> detectedObjects = List.of(
                new DetectedObject("Chair_1", "Chair"), // This object is present in the LiDarDataBase at time 2
                new DetectedObject("Table_1", "Table") // This object is present at time 3
        );

        List<TrackedObject> trackedObjects = lidarWorker.processDetectedObjects(detectedObjects, 2, lidarDatabase);

        // Validate that objects are processed correctly
        assertNotNull(trackedObjects, "Tracked objects should not be null.");
        assertEquals(1, trackedObjects.size(), "There should be 1 tracked object (Chair_1).");

        // Validate tracked object details
        TrackedObject trackedChair = trackedObjects.get(0);
        assertEquals("Chair_1", trackedChair.getId(), "Tracked object's ID should match.");
        assertEquals("Chair", trackedChair.getDescription(), "Tracked object's description should match.");
        assertEquals(2, trackedChair.getTime(), "Tracked object's time should match.");
        assertEquals(2, trackedChair.getCoordinates().size(), "Tracked object should have 2 cloud points.");
    }

    @Test
    void testNoObjectsFound() {
        // Test when no objects are found in the database at time 0
        List<DetectedObject> detectedObjects = new ArrayList<>();
        List<TrackedObject> trackedObjects = lidarWorker.processDetectedObjects(detectedObjects, 0, lidarDatabase);

        // Validate that no objects are tracked
        assertNotNull(trackedObjects, "Tracked objects should not be null.");
        assertTrue(trackedObjects.isEmpty(), "There should be no tracked objects.");
    }

    @Test
    void testDatabaseError() {
        // Simulate error detection in the database
        assertFalse(lidarWorker.detectError(3, lidarDatabase), "No error should be found at time 3.");

        // Check error at time 5
        assertTrue(lidarWorker.detectError(5, lidarDatabase), "An error should be detected at time 5.");
    }

    @Test
    void testNullValues() {
        // Test null parameters in processDetectedObjects
        assertThrows(NullPointerException.class, () ->
                        lidarWorker.processDetectedObjects(null, 5, lidarDatabase),
                "Should throw NullPointerException when detected objects list is null."
        );

        assertThrows(NullPointerException.class, () ->
                        lidarWorker.processDetectedObjects(new ArrayList<>(), 5, null),
                "Should throw NullPointerException when database is null."
        );
    }
}
