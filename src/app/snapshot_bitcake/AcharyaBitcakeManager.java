package app.snapshot_bitcake;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class AcharyaBitcakeManager implements BitcakeManager {

	private final AtomicInteger currentAmount = new AtomicInteger(1000);

	@Override
	public void takeSomeBitcakes(int amount) {
		currentAmount.getAndAdd(-amount);
	}

	@Override
	public void addSomeBitcakes(int amount) {
		currentAmount.getAndAdd(amount);
	}

	@Override
	public int getCurrentBitcakeAmount() {
		return currentAmount.get();
	}

	public void markerEvent(int id, SnapshotCollectorWorker snapshotCollectorWorker,
			ConcurrentHashMap concurrentHashMap) {

	}

}
