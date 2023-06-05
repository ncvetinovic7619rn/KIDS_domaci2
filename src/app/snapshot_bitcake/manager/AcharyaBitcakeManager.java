package app.snapshot_bitcake.manager;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

import app.AppConfig;
import app.CausalBroadcastShared;
import app.snapshot_bitcake.AcharyaSnapshotResult;
import servent.message.Message;
import servent.message.snapshot.AcharyaMarkerMessage;
import servent.message.util.MessageUtil;

public class AcharyaBitcakeManager implements BitcakeManager {
	private final AtomicInteger currentAmount = new AtomicInteger(1000);
	private static Map<Integer, List<Message>> sentHistory = new ConcurrentHashMap<>();
	private static Map<Integer, List<Message>> recievedHistory = new ConcurrentHashMap<>();
	private static final Object sentLock = new Object();
	private static final Object recievedLock = new Object();

	public AcharyaBitcakeManager() {
		for (int i = 0; i < AppConfig.getServentCount(); i++) {
			sentHistory.put(i, new CopyOnWriteArrayList<>());
			recievedHistory.put(i, new CopyOnWriteArrayList<>());
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

	public void markerEvent(Map<Integer, AcharyaSnapshotResult> colectedResult, Map<Integer, Integer> vectorClock) {

		Message markerMessage = new AcharyaMarkerMessage(AppConfig.myServentInfo, null, vectorClock);
		for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
			markerMessage = markerMessage.changeReceiver(neighbor);
			MessageUtil.sendMessage(markerMessage);
		}

		AcharyaSnapshotResult snapshotResult = new AcharyaSnapshotResult(AppConfig.myServentInfo.getId(),
				getCurrentBitcakeAmount(), getSentHistory(), getRecievedHistory());

		Message markerCommitMessage = new AcharyaMarkerMessage(AppConfig.myServentInfo, AppConfig.myServentInfo,
				vectorClock);
		CausalBroadcastShared.commitMessage(markerCommitMessage);

		colectedResult.put(AppConfig.myServentInfo.getId(), snapshotResult);
//        String text = String.format("Got a result from %s [%s/%s]", id, colectedResult.size(), AppConfig.getServentCount());
//        AppConfig.timestampedStandardPrint(text);

	}

	public Map<Integer, List<Message>> getSentHistory() {
		Map<Integer, List<Message>> currentSentHistory = new ConcurrentHashMap<>();
		synchronized (sentLock) {
//			currentSentHistory.putAll(sentHistory);
			for (Map.Entry<Integer, List<Message>> m : sentHistory.entrySet()) {
				currentSentHistory.put(m.getKey(), new CopyOnWriteArrayList<>(m.getValue()));
			}
		}
		return currentSentHistory;
	}

	public Map<Integer, List<Message>> getRecievedHistory() {
		Map<Integer, List<Message>> currentRecievedHistory = new ConcurrentHashMap<>();
		synchronized (recievedLock) {
//			currentRecievedHistory.putAll(recievedHistory);
			for (Map.Entry<Integer, List<Message>> m : recievedHistory.entrySet()) {
				currentRecievedHistory.put(m.getKey(), new CopyOnWriteArrayList<>(m.getValue()));
			}
		}
		return currentRecievedHistory;
	}

	public void addSentTransaction(int serventId, Message message) {
		synchronized (sentLock) {
			sentHistory.get(serventId).add(message);
		}
	}

	public void addRecievedTransaction(int serventId, Message message) {
		synchronized (recievedLock) {
			recievedHistory.get(serventId).add(message);
		}
	}

}
