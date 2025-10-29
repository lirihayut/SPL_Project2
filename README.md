## GurionRock Pro Max Ultra Over 9000 - Perception and Mapping System

This project is an implementation of a multi-threaded microservice framework designed to simulate the perception and mapping system of an autonomous vacuum robot, the **GurionRock Pro Max Ultra Over 9000**. The system integrates sensor data from cameras and LiDAR to construct an environmental map using a **Fusion-SLAM** (Simultaneous Localization and Mapping) algorithm.

### Key Features

  * **Java Concurrency and Synchronization:** Deep implementation of Java's concurrency principles, including multi-threading, thread pools, and advanced synchronization mechanisms.
  * **Microservices Framework:** Utilizes a custom-built microservices architecture with a dedicated **MessageBus** for asynchronous, decoupled communication.
  * **Sensor Data Processing:** Handles complex data streams from simulated cameras and LiDAR, tracking detected objects and converting their local sensor coordinates into a global coordinate system.
  * **Fusion-SLAM:** Integrates and fuses heterogeneous sensor data (visual and spatial) to build a consistent and accurate environmental map.
  * **Configuration-driven Execution:** System settings, simulation parameters, and input data files are read from external JSON configuration files.
  * **Robust Error Handling:** Implements a crash detection and error-reporting mechanism to ensure a safe system shutdown upon critical failures.

-----

## System Components and Architecture

The system operates on a custom microservices framework where each component runs on its own thread(s) and communicates exclusively via the MessageBus.

### Microservices

| Microservice | Role |
| :--- | :--- |
| `TimeService` | Manages the system's global clock (`system ticks`) and synchronizes operations across all components. |
| `CameraService` | Detects objects from simulated camera input and sends raw detection data as `DetectObjectsEvent`s. |
| `LiDarWorkerService` | Processes raw detection events, performs spatial coordinate transformation, and sends refined object data as `TrackedObjectsEvent`s. |
| `FusionSlamService` | The core mapping unit. It integrates tracked objects and robot pose data to construct and update the global environmental map. |
| `PoseService` | Monitors and broadcasts the robot's current position and orientation (`PoseEvent`), crucial for SLAM. |

### Messages and Events

Communication between services is achieved through three primary event types and system-wide broadcast messages.

| Message/Event | Sender | Handler(s) | Description |
| :--- | :--- | :--- | :--- |
| `DetectObjectsEvent` | `CameraService` | `LiDarWorkerService` | Raw object detection from the camera. |
| `TrackedObjectsEvent` | `LiDarWorkerService` | `FusionSlamService` | Objects with calculated global spatial coordinates. |
| `PoseEvent` | `PoseService` | `FusionSlamService` | The robot's current location and orientation data. |
| `TickBroadcast` | `TimeService` | *All Services* | Synchronization signal to advance system state. |
| `Termination and Crash Broadcasts` | *Various* | *All Services* | Signals a safe and coordinated system shutdown. |

### MessageBus Implementation

The `MessageBusImpl` manages all message passing:

  * **Round-Robin Event Distribution:** Ensures fair load distribution by dispatching events to subscribing services in a cyclical manner.
  * **Broadcast Messaging:** Allows the transmission of system-wide synchronization and termination signals.
  * **Future-based Event Handling:** Supports events that require an asynchronous result or confirmation from the handling service.

-----

## Setup and Execution

### Prerequisites

  * **Java 8+**
  * **Maven**

### Project Structure

```
├── src
│   ├── main/java/bgu/spl/mics/             # Custom Microservices Framework (MessageBus, Service, etc.)
│   ├── main/java/bgu/spl/mics/application/  # Main application logic and execution
│   ├── main/java/bgu/spl/mics/application/objects/  # Data Structures (Objects, Messages, Events)
│   └── main/java/bgu/spl/mics/application/configuration/ # Configuration Parsing Logic
├── resources/
│   ├── configuration_file.json   # Simulation settings (tick time, duration, data files)
│   ├── camera_data.json          # Simulated Camera input data
│   ├── lidar_data.json           # Simulated LiDAR input data
│   ├── pose_data.json            # Simulated Robot movement data
│   └── output_file.json          # Generated map and system statistics (output)
├── pom.xml                       # Maven dependency file
└── README.md                     # Project documentation (this file)
```

### Running the Simulation

1.  **Compile the project:**

    ```bash
    mvn clean install
    ```

2.  **Run the application:** Execute the JAR file, passing the configuration file path as an argument.

    ```bash
    java -jar target/assignment2.jar resources/configuration_file.json
    ```

3.  **View Results:** The final environmental map and simulation statistics will be stored in `resources/output_file.json`.

### Configuration

The simulation is configured via the `configuration_file.json`, which specifies:

  * Camera and LiDAR sensor settings.
  * The time unit for a simulation tick and the total simulation duration.
  * Paths to the input data files (`camera_data.json`, etc.).

-----

## Testing

The project includes a comprehensive suite of **JUnit-based unit tests**.

To run the tests:

```bash
mvn test
```

The tests validate:

  * **MessageBus Functionality:** Event dispatching logic and synchronization primitives.
  * **Sensor Data Processing:** Correctness of object detection and coordinate transformations.
  * **Error Handling:** The system's ability to gracefully halt and report errors using the `CrashedBroadcast` mechanism.

-----

## Authors

**Course:** SPL 225 - Concurrent Programming in Java

**Institution:** Ben-Gurion University of the Negev (BGU) Computer Science Department
