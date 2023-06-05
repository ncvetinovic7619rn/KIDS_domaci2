package app.snapshot_bitcake;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import app.AppConfig;
import app.VectorClock;
import app.snapshot_bitcake.manager.AcharyaBitcakeManager;
import app.snapshot_bitcake.manager.AlagarBitcakeManager;
import app.snapshot_bitcake.manager.BitcakeManager;
import servent.message.Message;
import servent.message.TransactionMessage;
import servent.message.snapshot.AlagarDoneMessage;

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
	private SnapshotType snapshotType;
	private BitcakeManager bitcakeManager;

	private Map<Integer, AcharyaSnapshotResult> collectedAcharyaValues = new ConcurrentHashMap<>();
	private Map<Integer, AlagarSnapshotResult> collectedAlagarValues = new ConcurrentHashMap<>();
	private Map<Integer, AlagarDoneMessage> collectedDoneMessages = new ConcurrentHashMap<>();


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
			AppConfig.timestampedStandardPrint("Sending token to everyone.");
			Map<Integer, Integer> vectorClock = VectorClock.getClock();
			// 1 send asks
			switch (snapshotType) {
			case ACHARYA:
				((AcharyaBitcakeManager) bitcakeManager).markerEvent(collectedAcharyaValues, vectorClock);
				break;
			case ALAGAR:
				((AlagarBitcakeManager) bitcakeManager).markerEvent(vectorClock);
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
					if (collectedDoneMessages.size() == AppConfig.getServentCount()) {
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
			if(snapshotType == SnapshotType.ALAGAR) {
				if (collectedAlagarValues.size() == AppConfig.getServentCount()) {
					waiting = false;
				}
			}
			switch (snapshotType) {
			case ACHARYA:
				printAcharyaValues();
				break;
			case ALAGAR:
				printAlagarValues();
				break;
			case NONE:
				// Shouldn't be able to come here. See constructor.
				break;
			}
			collecting.set(false);
		}

	}

	private void printAcharyaValues() {
		int sum;
		sum = 0;
		for (Entry<Integer, AcharyaSnapshotResult> nodeResult : collectedAcharyaValues.entrySet()) {
			sum += nodeResult.getValue().getRecordedAmount();
			AppConfig.timestampedStandardPrint("Recorded bitcake amount for " + nodeResult.getKey() + " = "
					+ nodeResult.getValue().getRecordedAmount() + " bitcake");
		}
		for (int i = 0; i < AppConfig.getServentCount(); i++) {
			for (int j = 0; j < AppConfig.getServentCount(); j++) {
				if (i != j) {
					if (AppConfig.getInfoById(i).getNeighbors().contains(j)
							&& AppConfig.getInfoById(j).getNeighbors().contains(i)) {
						List<Message> ijMessages = collectedAcharyaValues.get(i).getSentHistory().get(j);
						List<Message> jiMessages = collectedAcharyaValues.get(j).getRecievedHistory().get(i);

						int ijAmount = getAmount(ijMessages);
						int jiAmount = getAmount(jiMessages);

						if (ijAmount != jiAmount) {
							String outputString = String.format(
									"Unreceived bitcake amount: %d from servent %d to servent %d", ijAmount - jiAmount,
									i, j);
							AppConfig.timestampedStandardPrint(outputString);
							sum += ijAmount - jiAmount;
						}
					}
				}
			}
		}
		AppConfig.timestampedStandardPrint("System bitcake count: " + sum);
		collectedAcharyaValues.clear(); // reset for next invocation
	}

	private void printAlagarValues() {
		int sum;
		sum = 0;
		for (Entry<Integer, AlagarSnapshotResult> nodeResult : collectedAlagarValues.entrySet()) {
			sum += nodeResult.getValue().getRecordedAmount();
			AppConfig.timestampedStandardPrint("Recorded bitcake amount for " + nodeResult.getKey() + " = "
					+ nodeResult.getValue().getRecordedAmount() + " bitcake");
		}
		for (int i = 0; i < AppConfig.getServentCount(); i++) {
			for (int j = 0; j < AppConfig.getServentCount(); j++) {
				if (i != j) {
					if (AppConfig.getInfoById(i).getNeighbors().contains(j)
							&& AppConfig.getInfoById(j).getNeighbors().contains(i)) {
						List<Message> messages = collectedAlagarValues.get(i).getOldHistory().get(j);

						int amount = getAmount(messages);

						String outputString = String
								.format("Old bitcake amount added: %d from servent %d to servent %d", amount, i, j);
						AppConfig.timestampedStandardPrint(outputString);
						sum += amount;
					}
				}
			}
		}
		AppConfig.timestampedStandardPrint("System bitcake count: " + sum);
		collectedAcharyaValues.clear(); // reset for next invocation

	}

	private int getAmount(List<Message> messages) {
		int amount = 0;
		for (Message message : messages) {
			try {
				if (message instanceof TransactionMessage)
					amount += Integer.parseInt(((TransactionMessage) message).getMessageText());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return amount;
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
		String text = String.format("Got a result from %s [%s/%s]", id, collectedAcharyaValues.size(),
				AppConfig.getServentCount());
		AppConfig.timestampedStandardPrint(text);
	}

	@Override
	public void addAlagarSnapshotInfo(int id, AlagarSnapshotResult alagarSnapshotResult) {
		collectedAlagarValues.put(id, alagarSnapshotResult);
		String text = String.format("Got a result from %s [%s/%s]", id, collectedAlagarValues.size(),
				AppConfig.getServentCount());
		AppConfig.timestampedStandardPrint(text);
	}

	@Override
	public SnapshotType getSnapshotType() {
		return snapshotType;
	}

	@Override
	public boolean isCollecting() {
		return collecting.get();
	}

	@Override
	public void addAlagarDoneMessage(int id, AlagarDoneMessage alagarDoneMessage) {
		collectedDoneMessages.put(id, alagarDoneMessage);
		String text = String.format("Got a done message from %s [%s/%s]", id, collectedDoneMessages.size(),
				AppConfig.getServentCount());
		AppConfig.timestampedStandardPrint(text);		
	}

}
