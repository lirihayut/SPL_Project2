package bgu.spl.mics;

import java.util.*;
import java.util.concurrent.*;

public class MessageBusImpl implements MessageBus {
	private final Map<Class<? extends Message>, List<MicroService>> broadcastSubscribers;
	private final Map<Class<? extends Event<?>>, Queue<MicroService>> eventSubscribers;
	final Map<MicroService, LinkedBlockingQueue<Message>> microServiceQueues;
	private final Map<Event<?>, Future<?>> futureMap;

	private static MessageBusImpl instance = null;

	private MessageBusImpl() {
		microServiceQueues = new ConcurrentHashMap<>();
		eventSubscribers = new ConcurrentHashMap<>();
		broadcastSubscribers = new ConcurrentHashMap<>();
		futureMap = new ConcurrentHashMap<>();
	}

	@Override
	public <T> void subscribeEvent(Class<? extends Event<T>> type, MicroService m) {
		Queue<MicroService> subscribers = eventSubscribers.get(type);
		if (subscribers == null) {
			subscribers = new ConcurrentLinkedQueue<>();
			eventSubscribers.put(type, subscribers);
		}
		subscribers.add(m);
	}

	@Override
	public void subscribeBroadcast(Class<? extends Broadcast> type, MicroService m) {
		List<MicroService> subscribers = broadcastSubscribers.get(type);
		if (subscribers == null) {
			subscribers = new CopyOnWriteArrayList<>();
			broadcastSubscribers.put(type, subscribers);
		}
		subscribers.add(m);
	}

	@Override
	public <T> void complete(Event<T> e, T result) {
		Future<T> future = (Future<T>) futureMap.remove(e);
		if (future != null) {
			future.resolve(result);  // Resolve the future with the event result
		}
	}

	@Override
	public void sendBroadcast(Broadcast b) {
		List<MicroService> subscribers = broadcastSubscribers.get(b.getClass());
		if (subscribers != null) {
			for (MicroService m : subscribers) {
				LinkedBlockingQueue<Message> queue = microServiceQueues.get(m);
				if (queue != null) {
					queue.add(b);  // Send the broadcast message to each subscriber's queue
				}
			}
		}
	}

	@Override
	public <T> Future<T> sendEvent(Event<T> e) {
		Queue<MicroService> subscribers = eventSubscribers.get(e.getClass());
		if (subscribers == null || subscribers.isEmpty()) {
			return null;
		}
		MicroService m = subscribers.poll();  // Round-robin: poll the next subscriber
		subscribers.add(m);  // Re-insert back to the queue for round-robin

		BlockingQueue<Message> queue = microServiceQueues.get(m);
		if (queue != null) {
			Future<T> future = new Future<>();
			futureMap.put(e, future);
			queue.add(e);  // Place the event in the microservice's message queue
			return future;
		}
		return null;
	}

	@Override
	public void register(MicroService m) {
		if (!microServiceQueues.containsKey(m)) {
			microServiceQueues.put(m, new LinkedBlockingQueue<>());
		}
	}

	@Override
	public void unregister(MicroService m) {
		microServiceQueues.remove(m);
		for (List<MicroService> subscribers : broadcastSubscribers.values()) {
			subscribers.remove(m);  // Safely remove the MicroService from each broadcast subscriber list
		}

		for (Queue<MicroService> subscribers : eventSubscribers.values()) {
			subscribers.remove(m);
		}
	}

	@Override
	public Message awaitMessage(MicroService m) {
		if (!microServiceQueues.containsKey(m)) {
			throw new IllegalStateException("MicroService " + m.getName() + " is not registered.");
		}

		try {
			return microServiceQueues.get(m).take();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			return null;
		}
	}


	public static MessageBusImpl getInstance() {
		if (instance == null) {
			instance = new MessageBusImpl();
		}
		return instance;
	}

	public Map<MicroService, LinkedBlockingQueue<Message>> getMicroServiceQueues() {
		return microServiceQueues;
	}

	public Map<Class<? extends Event<?>>, Queue<MicroService>> getEventSubscribers() {
		return eventSubscribers;
	}
}
