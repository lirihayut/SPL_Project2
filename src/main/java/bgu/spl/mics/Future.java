package bgu.spl.mics;

import java.util.concurrent.TimeUnit;

/**
 * A Future object represents a promised result - an object that will
 * eventually be resolved to hold a result of some operation. The class allows
 * retrieving the result once it is available.
 *
 * Only private methods may be added to this class.
 * No public constructor is allowed except for the empty constructor.
 */
public class Future<T> {
	private T result;
	private boolean isDone;

	/**
	 * This should be the only public constructor in this class.
	 */
	public Future() {
		this.result = null;
		this.isDone = false;
	}

	/**
	 * Retrieves the result the Future object holds if it has been resolved.
	 * This is a blocking method! It waits for the computation in case it has
	 * not been completed.
	 * <p>
	 * @return the result of type T if it is available, if not waits until it is available.
	 */
	public synchronized T get() {
		while (!isDone) {
			try {
				wait();
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt(); // Preserve interrupt status
			}
		}
		return result;
	}

	/**
	 * Resolves the result of this Future object.
	 * This method can only be called once.
	 */
	public synchronized void resolve(T result) {
		if (isDone) {
			throw new IllegalStateException("Future has already been resolved");
		}
		this.result = result;
		this.isDone = true;
		notifyAll();
	}

	/**
	 * @return true if this object has been resolved, false otherwise
	 */
	public synchronized boolean isDone() {
		return isDone;
	}

	/**
	 * Retrieves the result the Future object holds if it has been resolved.
	 * This method is non-blocking, and waits for a limited amount of time.
	 * <p>
	 * @param timeout the maximal amount of time units to wait for the result.
	 * @param unit the {@link TimeUnit} time units to wait.
	 * @return the result of type T if it is available, if not, waits for {@code timeout} TimeUnits {@code unit}.
	 *         If the time has elapsed and the result is not available, returns null.
	 */
	public synchronized T get(long timeout, TimeUnit unit) {
		if (isDone) {
			return result;
		}
		try {
			unit.timedWait(this, timeout);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt(); // Preserve interrupt status
		}

		if (isDone) {
			return result;
		} else {
			return null;
		}
	}
}
