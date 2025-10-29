package bgu.spl.mics;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import bgu.spl.mics.Message;
import bgu.spl.mics.MessageBusImpl;
import bgu.spl.mics.MicroService;
import bgu.spl.mics.application.messages.PoseEvent;
import bgu.spl.mics.application.messages.TickBroadcast;
import bgu.spl.mics.application.objects.Pose;

import static org.junit.jupiter.api.Assertions.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

class MessageBusImplTest {

    private MessageBusImpl messageBus;
    private MicroService testMicroService1;
    private MicroService testMicroService2;

    @BeforeEach
    public void setUp() {
        // Initialize the MessageBus and MicroServices for each test
        messageBus = MessageBusImpl.getInstance();
        testMicroService1 = new TestMicroService("TestMicroService1");
        testMicroService2 = new TestMicroService("TestMicroService2");
    }

    @Test
    public void testServiceRegisterAndRemove() {
        // ** Test that a MicroService can be registered and unregistered correctly **
        messageBus.register(testMicroService1);
        assertTrue(messageBus.getMicroServiceQueues().containsKey(testMicroService1),
                "TestMicroService1 should be registered and have a queue.");

        messageBus.unregister(testMicroService1);
        assertFalse(messageBus.getMicroServiceQueues().containsKey(testMicroService1),
                "TestMicroService1 should be unregistered and its queue should be removed.");
    }

    @Test
    public void testMicroServiceEventSubscription() {
        // ** Test subscribing a MicroService to an event **
        messageBus.register(testMicroService1);
        messageBus.subscribeEvent(PoseEvent.class, testMicroService1);
        assertTrue(messageBus.getEventSubscribers().get(PoseEvent.class).contains(testMicroService1),
                "TestMicroService1 should be subscribed to PoseEvent.");
    }

    @Test
    public void testBroadcastMessageDelivery() throws InterruptedException {
        // ** Test that both MicroServices receive a broadcast **
        messageBus.register(testMicroService1);
        messageBus.register(testMicroService2);
        messageBus.subscribeBroadcast(TickBroadcast.class, testMicroService1);
        messageBus.subscribeBroadcast(TickBroadcast.class, testMicroService2);

        TickBroadcast broadcastMessage = new TickBroadcast(25);
        messageBus.sendBroadcast(broadcastMessage);

        BlockingQueue<Message> queue1 = messageBus.getMicroServiceQueues().get(testMicroService1);
        BlockingQueue<Message> queue2 = messageBus.getMicroServiceQueues().get(testMicroService2);

        assertEquals(broadcastMessage, queue1.poll(300, TimeUnit.MILLISECONDS),
                "TestMicroService1 should have received the broadcast message.");
        assertEquals(broadcastMessage, queue2.poll(300, TimeUnit.MILLISECONDS),
                "TestMicroService2 should have received the broadcast message.");
    }

    @Test
    public void testPoseEventCompletion() throws InterruptedException {
        // ** Test sending a PoseEvent and completing it **
        messageBus.register(testMicroService1);
        messageBus.subscribeEvent(PoseEvent.class, testMicroService1);

        Pose samplePose = new Pose(25, 15, 45, 10);
        PoseEvent poseEvent = new PoseEvent(30, samplePose);

        Future<Pose> poseFuture = (Future<Pose>) messageBus.sendEvent(poseEvent);
        assertNotNull(poseFuture, "A Future should be returned for the PoseEvent.");

        messageBus.complete(poseEvent, samplePose);
        assertEquals(samplePose, poseFuture.get(), "The result of the event should match the completed Pose.");
    }

    @Test
    public void testServiceAwaitMessage() throws InterruptedException {
        // ** Test awaiting a broadcast message **
        messageBus.register(testMicroService1);
        messageBus.subscribeBroadcast(TickBroadcast.class, testMicroService1);

        TickBroadcast tickMessage = new TickBroadcast(15);
        messageBus.sendBroadcast(tickMessage);

        Message receivedMessage = messageBus.awaitMessage(testMicroService1);
        assertEquals(tickMessage, receivedMessage, "The awaited message should be the TickBroadcast.");
    }

    @Test
    public void testExceptionOnAwaitMessageForUnregisteredService() {
        // ** Test awaiting a message for an unregistered MicroService **
        assertThrows(IllegalStateException.class, () -> messageBus.awaitMessage(testMicroService1),
                "Awaiting a message for an unregistered MicroService should throw an exception.");
    }

    @Test
    public void testMultipleMicroServiceEventSubscriptions() {
        // ** Test subscribing multiple MicroServices to the same event **
        messageBus.register(testMicroService1);
        messageBus.register(testMicroService2);

        messageBus.subscribeEvent(PoseEvent.class, testMicroService1);
        messageBus.subscribeEvent(PoseEvent.class, testMicroService2);

        assertTrue(messageBus.getEventSubscribers().get(PoseEvent.class).contains(testMicroService1),
                "TestMicroService1 should be subscribed to PoseEvent.");
        assertTrue(messageBus.getEventSubscribers().get(PoseEvent.class).contains(testMicroService2),
                "TestMicroService2 should be subscribed to PoseEvent.");
    }

    // Mock MicroService class for testing purposes
    private class TestMicroService extends MicroService {
        public TestMicroService(String name) {
            super(name);
        }

        @Override
        protected void initialize() {
            // No special initialization required for the test
        }
    }
}
