package bgu.spl.mics;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The MicroService is an abstract class that any micro-service in the system
 * must extend. It is responsible for interacting with the {@link MessageBus}.
 */
public abstract class MicroService implements Runnable {

    private boolean terminated = false;
    private final String name;
    private final MessageBus MBinstance;
    private final Map<Class<? extends Message>, Callback<? extends Message>> messPerCB;

    /**
     * Constructor
     *
     * @param name the micro-service name (used mainly for debugging purposes)
     */
    public MicroService(String name) {
        this.name = name;
        this.MBinstance = MessageBusImpl.getInstance();
        this.messPerCB = new ConcurrentHashMap<>();
    }

    /**
     * Subscribes to events of type {@code type} with the callback {@code callback}.
     */
    protected final <T, E extends Event<T>> void subscribeEvent(Class<E> type, Callback<E> callback) {
        MBinstance.subscribeEvent(type, this);
        messPerCB.put(type, callback);
    }

    /**
     * Subscribes to broadcast messages of type {@code type} with the callback {@code callback}.
     */
    protected final <B extends Broadcast> void subscribeBroadcast(Class<B> type, Callback<B> callback) {
        MBinstance.subscribeBroadcast(type, this);
        messPerCB.put(type, callback);
    }

    /**
     * Sends an event {@code e} using the message bus and receives a {@link Future<T>} object.
     */
    protected final <T> Future<T> sendEvent(Event<T> e) {
        return MBinstance.sendEvent(e);
    }

    /**
     * Sends a broadcast message {@code b} to all services subscribed to it.
     */
    protected final void sendBroadcast(Broadcast b) {
        MBinstance.sendBroadcast(b);
    }

    /**
     * Completes the received request {@code e} with the result {@code result}.
     */
    protected final <T> void complete(Event<T> e, T result) {
        MBinstance.complete(e, result);
    }

    /**
     * This method is called once when the event loop starts.
     */
    protected abstract void initialize();

    /**
     * Signals the event loop to terminate after handling the current message.
     */
    protected final void terminate() {
        this.terminated = true;
    }

    /**
     * @return the name of the service
     */
    public final String getName() {
        return name;
    }

    /**
     * The entry point of the micro-service. Registers the service to the {@link MessageBus},
     * initializes it, and processes messages until termination.
     */
    @Override
    public void run() {
        MBinstance.register(this);
        initialize();

        try {
            while (!terminated) {
                Message message = MBinstance.awaitMessage(this);
                Callback<? extends Message> callback = messPerCB.get(message.getClass());

                if (callback != null) {
                    @SuppressWarnings("unchecked")
                    Callback<Message> castedCallback = (Callback<Message>) callback;
                    castedCallback.call(message);
                } else {
                    System.err.println(getName() + " - No callback found for message type: " + message.getClass().getSimpleName());
                }
            }
        } catch (InterruptedException e) {
            System.out.println("Service interrupted: " + getName());
            Thread.currentThread().interrupt();
        } finally {
            MBinstance.unregister(this);
        }
    }
}
