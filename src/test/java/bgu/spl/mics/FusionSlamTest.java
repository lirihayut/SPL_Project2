package bgu.spl.mics;

import bgu.spl.mics.application.objects.*;
import org.junit.jupiter.api.*;

import java.io.*;
import java.util.List;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

class FusionSlamTest {

    private FusionSlam fusionSlamInstance;
    private final ByteArrayOutputStream outputStreamCaptor = new ByteArrayOutputStream();

    @BeforeEach
    void setUp() {
        // Initialize FusionSlam instance
        fusionSlamInstance = FusionSlam.getInstance(5, 15, null);

        // Set up System.out to capture the logs
        System.setOut(new PrintStream(outputStreamCaptor));
    }

    @Test
    void testPoseNotFound_ErrorHandling() {
        // Simulate the case where there is no pose available for a given detection time.
        // Expectation: The system should handle the error appropriately and log a message.

        // Create tracked objects with some points
        List<TrackedObject> trackedObjects = new ArrayList<>();
        trackedObjects.add(new TrackedObject("Window_1", 4, "Window", List.of(
                new CloudPoint(3.0, 4.0),
                new CloudPoint(5.0, 6.0)
        )));

        // Process tracked objects at time 5, when no pose exists for that time
        fusionSlamInstance.processTrackedObjects(trackedObjects, 5);

        // Capture the output and assert on it
        String capturedOutput = outputStreamCaptor.toString().trim();
        assertTrue(capturedOutput.contains("Pose not found for time: 5"), "Expected log not found.");
    }

    @Test
    void testProcessTrackedObjects_NewLandmarksAdded() {
        // Simulate the case where no landmarks exist in the system yet.
        // Expectation: New landmarks should be created when processing tracked objects.

        // Create tracked objects with some points
        List<TrackedObject> trackedObjects = new ArrayList<>();
        trackedObjects.add(new TrackedObject("Lamp_1", 2, "Lamp", List.of(
                new CloudPoint(1.5, 2.5),
                new CloudPoint(3.5, 4.5)
        )));
        trackedObjects.add(new TrackedObject("Table_1", 2, "Table", List.of(
                new CloudPoint(4.5, 5.5),
                new CloudPoint(6.5, 7.5)
        )));

        // Add a pose for detection time 2
        Pose poseAtTime2 = new Pose(0, 0, 45, 2); // Robot's pose at time 2
        fusionSlamInstance.addPose(poseAtTime2);

        // Process the tracked objects at time 2
        fusionSlamInstance.processTrackedObjects(trackedObjects, 2);

        // Verify that the landmarks were added
        List<LandMark> landmarks = fusionSlamInstance.getLandMarks();
        assertEquals(2, landmarks.size(), "Two landmarks should be added.");

        assertEquals("Lamp_1", landmarks.get(0).getId(), "First landmark ID should be 'Lamp_1'.");
        assertEquals("Table_1", landmarks.get(1).getId(), "Second landmark ID should be 'Table_1'.");
    }

    @Test
    void testProcessTrackedObjects_WithRotation() {
        // This test case simulates a pose with a rotation of 180 degrees at detection time 3.
        // Expectation: The coordinates of the landmarks should be transformed accordingly.

        // Clear existing landmarks before starting the test
        fusionSlamInstance.getLandMarks().clear();

        // Create tracked objects with coordinates that will be transformed
        List<TrackedObject> trackedObjects = new ArrayList<>();
        trackedObjects.add(new TrackedObject("Sofa_1", 3, "Sofa", List.of(
                new CloudPoint(2.0, 3.0),
                new CloudPoint(4.0, 5.0)
        )));
        trackedObjects.add(new TrackedObject("Chair_2", 3, "Chair", List.of(
                new CloudPoint(6.0, 7.0),
                new CloudPoint(8.0, 9.0)
        )));

        // Add a pose for detection time 3 with 180 degrees rotation
        Pose poseAtTime3 = new Pose(0, 0, 180, 3); // Robot's pose at time 3 with 180-degree rotation
        fusionSlamInstance.addPose(poseAtTime3);

        // Process the tracked objects at time 3
        fusionSlamInstance.processTrackedObjects(trackedObjects, 3);

        // Capture the landmarks after processing
        List<LandMark> landmarks = fusionSlamInstance.getLandMarks();

        // Check the number of landmarks after processing
        assertEquals(2, landmarks.size(), "Two landmarks should be added.");

        LandMark sofaLandmark = landmarks.get(0);
        assertEquals("Sofa_1", sofaLandmark.getId(), "The ID of the first landmark should match.");
        assertTrue(Math.abs(sofaLandmark.getCoordinates().get(0).getX() + 2.0) < 0.001, "Sofa's X coordinate should be transformed.");
        assertTrue(Math.abs(sofaLandmark.getCoordinates().get(0).getY() + 3.0) < 0.001, "Sofa's Y coordinate should be transformed.");

        LandMark chairLandmark = landmarks.get(1);
        assertEquals("Chair_2", chairLandmark.getId(), "The ID of the second landmark should match.");
        // Expected transformation: (-6.0, -7.0) and (-8.0, -9.0) after 180-degree rotation
        assertTrue(Math.abs(chairLandmark.getCoordinates().get(0).getX() + 6.0) < 0.001, "Chair's first X coordinate should be transformed.");
        assertTrue(Math.abs(chairLandmark.getCoordinates().get(0).getY() + 7.0) < 0.001, "Chair's first Y coordinate should be transformed.");
        assertTrue(Math.abs(chairLandmark.getCoordinates().get(1).getX() + 8.0) < 0.001, "Chair's second X coordinate should be transformed.");
        assertTrue(Math.abs(chairLandmark.getCoordinates().get(1).getY() + 9.0) < 0.001, "Chair's second Y coordinate should be transformed.");
    }


    @Test
    void testServiceCounter_ZeroCounterTerminating() {
        // Test the behavior when the service count reaches zero. Expectation: The system should terminate and output the file.

        // Simulate processing where the service count is decremented to zero
        fusionSlamInstance.decreaseServiceCounter();  // Decrease service count to 4
        fusionSlamInstance.decreaseServiceCounter();  // Decrease service count to 3
        fusionSlamInstance.decreaseServiceCounter();  // Decrease service count to 2
        fusionSlamInstance.decreaseServiceCounter();  // Decrease service count to 1

        // Check that it should not terminate yet
        assertFalse(fusionSlamInstance.shouldTerminateAtTime(5), "System should not terminate yet.");

        // Decrease service count to 0, expecting termination
        fusionSlamInstance.decreaseServiceCounter();
        assertTrue(fusionSlamInstance.shouldTerminateAtTime(10), "System should terminate when service count is 0.");
    }
}
