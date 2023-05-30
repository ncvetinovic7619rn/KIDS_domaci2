package app.snapshot_bitcake;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.omg.PortableInterceptor.ACTIVE;

import app.AppConfig;
import app.CausalBroadcastShared;
import servent.message.Message;
import servent.message.util.MessageUtil;

/**
 * Main snapshot collector class. Has support for Naive, Chandy-Lamport and
 * Lai-Yang snapshot algorithms.
 * 
 * @author bmilojkovic
 *
 */
public class SnapshotCollectorWorker implements SnapshotCollector {

	private volatile boolean working = true;

	private AtomicBoolean collecting = new AtomicBoolean(false);

	private Map<Integer, AcharyaSnapshotResult> collectedAcharyaValues = new ConcurrentHashMap<>();
	private Map<Integer, AlagarSnapshotResult> collectedAlagarValues = new ConcurrentHashMap<>();

	private SnapshotType snapshotType = SnapshotType.ACHARYA;

	private BitcakeManager bitcakeManager;

	public SnapshotCollectorWorker(SnapshotType snapshotType) {
		this.snapshotType = snapshotType;

		switch (snapshotType) {

		case ACHARYA:
			bitcakeManager = new AcharyaBitcakeManager();
			break;
		case ALAGAR:
			bitcakeManager = new AlagarBitcakeManager();
			break;
		case NONE:
			AppConfig.timestampedErrorPrint("Making snapshot collector without specifying type. Exiting...");
			System.exit(0);
		}

	}

	@Override
	public BitcakeManager getBitcakeManager() {
		return bitcakeManager;
	}

	@Override
	public void run() {
		while (working) {

			/*
			 * Not collecting yet - just sleep until we start actual work, or finish
			 */
			while (collecting.get() == false) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				if (working == false) {
					return;
				}
			}

			/*
			 * Collecting is done in three stages: 1. Send messages asking for values 2.
			 * Wait for all the responses 3. Print result
			 */

			// 1 send asks
			switch (snapshotType) {
			case ACHARYA:
				((AcharyaBitcakeManager) bitcakeManager).markerEvent(AppConfig.myServentInfo.getId(), this, new ConcurrentHashMap<>(CausalBroadcastShared.getVectorClock()));
				break;
			case ALAGAR:
				((AlagarBitcakeManager) bitcakeManager).markerEvent(AppConfig.myServentInfo.getId(), this, new ConcurrentHashMap<>(CausalBroadcastShared.getVectorClock()));
				break;
			case NONE:
				// Shouldn't be able to come here. See constructor.
				break;
			}

			// 2 wait for responses or finish
			boolean waiting = true;
			while (waiting) {
				switch (snapshotType) {
				case ACHARYA:
					if (collectedAcharyaValues.size() == AppConfig.getServentCount()) {
						waiting = false;
					}
					break;
				case ALAGAR:
					if (collectedAlagarValues.size() == AppConfig.getServentCount()) {
						waiting = false;
					}
					break;
				case NONE:
					// Shouldn't be able to come here. See constructor.
					break;
				}

				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}

				if (working == false) {
					return;
				}
			}

			// print
			int sum;
			switch (snapshotType) {
			case ACHARYA:
				sum = 0;
				for (Entry<Integer, AcharyaSnapshotResult> nodeResult : collectedAcharyaValues.entrySet()) {
					sum += nodeResult.getValue().getRecordedAmount();
					AppConfig.timestampedStandardPrint(
							"Recorded bitcake amount for " + nodeResult.getKey() + " = " + nodeResult.getValue().getRecordedAmount());
				}
				for(int i = 0; i < AppConfig.getServentCount(); i++) {
					for (int j = 0; j < AppConfig.getServentCount(); j++) {
						if (i != j) {
							if (AppConfig.getInfoById(i).getNeighbors().contains(j) &&
								AppConfig.getInfoById(j).getNeighbors().contains(i)) {
								int ijAmount = collectedAcharyaValues.get(i).getGiveHistory().get(j);
								int jiAmount = collectedAcharyaValues.get(j).getGetHistory().get(i);
								
								if (ijAmount != jiAmount) {
									String outputString = String.format(
											"Unreceived bitcake amount: %d from servent %d to servent %d",
											ijAmount - jiAmount, i, j);
									AppConfig.timestampedStandardPrint(outputString);
									sum += ijAmount - jiAmount;
								}
							}
						}
					}
				}
				
				AppConfig.timestampedStandardPrint("System bitcake count: " + sum);
				
				collectedAcharyaValues.clear(); //reset for next invocation
				break;
			case ALAGAR:
				sum = 0;
				for (Entry<Integer, AlagarSnapshotResult> nodeResult : collectedAlagarValues.entrySet()) {
					sum += nodeResult.getValue().getRecordedAmount();
					AppConfig.timestampedStandardPrint(
							"Recorded bitcake amount for " + nodeResult.getKey() + " = " + nodeResult.getValue().getRecordedAmount());
				}
				for(int i = 0; i < AppConfig.getServentCount(); i++) {
					for (int j = 0; j < AppConfig.getServentCount(); j++) {
						if (i != j) {
							if (AppConfig.getInfoById(i).getNeighbors().contains(j) &&
								AppConfig.getInfoById(j).getNeighbors().contains(i)) {
								int ijAmount = collectedAlagarValues.get(i).getGiveHistory().get(j);
								int jiAmount = collectedAlagarValues.get(j).getGetHistory().get(i);
								
								if (ijAmount != jiAmount) {
									String outputString = String.format(
											"Unreceived bitcake amount: %d from servent %d to servent %d",
											ijAmount - jiAmount, i, j);
									AppConfig.timestampedStandardPrint(outputString);
									sum += ijAmount - jiAmount;
								}
							}
						}
					}
				}
				
				AppConfig.timestampedStandardPrint("System bitcake count: " + sum);
				
				collectedAcharyaValues.clear(); //reset for next invocation
				break;

			case NONE:
				// Shouldn't be able to come here. See constructor.
				break;
			}
			collecting.set(false);
		}

	}

	@Override
	public void startCollecting() {
		boolean oldValue = this.collecting.getAndSet(true);

		if (oldValue == true) {
			AppConfig.timestampedErrorPrint("Tried to start collecting before finished with previous.");
		}
	}

	@Override
	public void stop() {
		working = false;
	}

	@Override
	public void addAcharyaSnapshotInfo(int id, AcharyaSnapshotResult acharyaSnapshotResult) {
		collectedAcharyaValues.put(id, acharyaSnapshotResult);
	}

	@Override
	public void addAlagarSnapshotInfo(int id, AlagarSnapshotResult alagarSnapshotResult) {
		collectedAlagarValues.put(id, alagarSnapshotResult);
	}

}
