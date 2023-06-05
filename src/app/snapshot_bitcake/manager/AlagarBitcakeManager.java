package app.snapshot_bitcake.manager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import app.AppConfig;
import app.CausalBroadcastShared;
import app.snapshot_bitcake.AlagarSnapshotResult;
import servent.message.Message;
import servent.message.snapshot.AcharyaMarkerMessage;
import servent.message.snapshot.AlagarMarkerMessage;
import servent.message.util.MessageUtil;

public class AlagarBitcakeManager implements BitcakeManager {
	private final AtomicInteger currentAmount = new AtomicInteger(1000);
	private static Map<Integer, List<Message>> oldHistory = new ConcurrentHashMap<>();
	private static final Object oldLock = new Object();

	public AlagarBitcakeManager() {
		for (int i = 0; i < AppConfig.getServentCount(); i++) {
			oldHistory.put(i, new CopyOnWriteArrayList<>());
		}
	}

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

	public void markerEvent( Map<Integer, Integer> vectorClock) {
		Message markerMessage = new AlagarMarkerMessage(AppConfig.myServentInfo, null, vectorClock);
		for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
			markerMessage = markerMessage.changeReceiver(neighbor);
			MessageUtil.sendMessage(markerMessage);
		}
		Message markerCommitMessage = new AcharyaMarkerMessage(AppConfig.myServentInfo, AppConfig.myServentInfo,
				vectorClock);
		CausalBroadcastShared.commitMessage(markerCommitMessage);

//        String text = String.format("Got a result from %s [%s/%s]", id, colectedResult.size(), AppConfig.getServentCount());
//        AppConfig.timestampedStandardPrint(text);

	}
	public Map<Integer, List<Message>> getOldHistory() {
		Map<Integer, List<Message>> currentSentHistory = new ConcurrentHashMap<>();
		synchronized (oldLock) {
//			currentSentHistory.putAll(sentHistory);
			for (Map.Entry<Integer, List<Message>> m : oldHistory.entrySet()) {
				currentSentHistory.put(m.getKey(), new CopyOnWriteArrayList<>(m.getValue()));
			}
		}
		return currentSentHistory;
	}

	public void addOldTransaction(int serventId, Message message) {
		synchronized (oldLock) {
			oldHistory.get(serventId).add(message);
		}
	}
}
