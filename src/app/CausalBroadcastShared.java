package app;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;

import app.snapshot_bitcake.AcharyaSnapshotResult;
import app.snapshot_bitcake.AlagarSnapshotResult;
import app.snapshot_bitcake.SnapshotCollector;
import app.snapshot_bitcake.SnapshotType;
import app.snapshot_bitcake.manager.AcharyaBitcakeManager;
import app.snapshot_bitcake.manager.AlagarBitcakeManager;
import servent.message.BasicMessage;
import servent.message.Message;
import servent.message.MessageType;
import servent.message.TransactionMessage;
import servent.message.snapshot.AcharyaTellMessage;
import servent.message.snapshot.AlagarDoneMessage;
import servent.message.snapshot.AlagarTerminateMessage;
import servent.message.util.MessageUtil;

/**
 * This class contains shared data for the Causal Broadcast implementation:
 * <ul>
 * <li>Vector clock for current instance
 * <li>Commited message list
 * <li>Pending queue
 * </ul>
 * As well as operations for working with all of the above.
 *
 * @author bmilojkovic
 *
 */
public class CausalBroadcastShared {
	private static final Object pendingMessagesLock = new Object();
	private static Queue<Message> pendingMessages = new ConcurrentLinkedQueue<>();
	private static List<Message> commitedMessages = new CopyOnWriteArrayList<>();
	private static AtomicBoolean alagarSnapshotStarted = new AtomicBoolean(false);
	private static AtomicBoolean alagarDone = new AtomicBoolean(false);

	private static Map<Integer, Integer> alagarSnapshotClock;
	public static SnapshotCollector snapshotCollector;

	public static void addPendingMessage(Message msg) {
		pendingMessages.add(msg);
	}

	public static void commitMessage(Message newMessage) {
		commitedMessages.add(newMessage);
		VectorClock.incrementClock(newMessage.getOriginalSenderInfo().getId());
		checkPendingMessages();
	}

	public static boolean isAlagarSnapshotStarted() {
		return alagarSnapshotStarted.get();
	}

	public static Map<Integer, Integer> getAlagarSnapshotClock() {
		return alagarSnapshotClock;
	}

	public static void checkPendingMessages() {
		boolean gotWork = true;

		while (gotWork) {
			gotWork = false;

			synchronized (pendingMessagesLock) {
				Iterator<Message> iterator = pendingMessages.iterator();
				Map<Integer, Integer> vectorClock = VectorClock.getClock();

				while (iterator.hasNext()) {
					Message pendingMessage = iterator.next();
					BasicMessage causalPendingMessage = (BasicMessage) pendingMessage;

					if (!VectorClock.otherClockGreater(vectorClock, causalPendingMessage.getSenderVectorClock())) {
						gotWork = true;

						switch (causalPendingMessage.getMessageType()) {
						case TRANSACTION:
							handleTransaction((TransactionMessage) causalPendingMessage);
							break;
						case ACHARYA_MARKER:
							handleAcharyaToken(vectorClock);
							break;
						case ACHARYA_TELL_AMOUNT:
							tellAcharyaResult((AcharyaTellMessage) causalPendingMessage);
							break;
						case ALAGAR_MARKER:
							handleAlagarMarker(causalPendingMessage, vectorClock);
							break;
						case DONE:
							handleDoneMessage(vectorClock);
							break;
						case TERMINATE:
							handleTerminateMessage(causalPendingMessage);
							break;
						default:
							throw new IllegalStateException("Advanacement made: How did we get here?");
						}
						commitedMessages.add(causalPendingMessage);
						VectorClock.incrementClock(causalPendingMessage.getOriginalSenderInfo().getId());
						iterator.remove();
						break;
					} else if (causalPendingMessage.getMessageType() == MessageType.ALAGAR_MARKER) {
						handleAlagarMarker(causalPendingMessage, vectorClock);
					} else if (snapshotCollector.getSnapshotType() == SnapshotType.ALAGAR && !VectorClock
							.otherClockGreater(causalPendingMessage.getSenderVectorClock(), alagarSnapshotClock)) {
						if (causalPendingMessage.getMessageType() == MessageType.TRANSACTION) {
							handleTransaction((TransactionMessage) causalPendingMessage);
						}
					}
				}
			}
		}

	}

	private static void handleTerminateMessage(BasicMessage causalPendingMessage) {
		AlagarBitcakeManager bitcakeManager = (AlagarBitcakeManager) snapshotCollector.getBitcakeManager();
		AlagarSnapshotResult snapshotResult = new AlagarSnapshotResult(AppConfig.myServentInfo.getId(),
				bitcakeManager.getCurrentBitcakeAmount(), bitcakeManager.getOldHistory());
		if (snapshotCollector.isCollecting()) {
			snapshotCollector.addAlagarSnapshotInfo(causalPendingMessage.getOriginalSenderInfo().getId(),
					snapshotResult);
		}
	}

	private static void handleDoneMessage(Map<Integer, Integer> vectorClock) {
		Message terminateMessage = new AlagarTerminateMessage(AppConfig.myServentInfo, null, vectorClock);
		for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
			terminateMessage = terminateMessage.changeReceiver(neighbor);
			MessageUtil.sendMessage(terminateMessage);
		}
		Message selfTerminateMessage = new AlagarTerminateMessage(AppConfig.myServentInfo, AppConfig.myServentInfo,
				vectorClock);
		commitedMessages.add(selfTerminateMessage);
		VectorClock.incrementClock(selfTerminateMessage.getOriginalSenderInfo().getId());
		alagarSnapshotStarted.getAndSet(false);
	}

	private static void handleAlagarMarker(BasicMessage causalPendingMessage, Map<Integer, Integer> vectorClock) {
		if (!alagarSnapshotStarted.get()) {
			alagarSnapshotClock = vectorClock;
			alagarSnapshotStarted.getAndSet(true);
		}
		Message message = new AlagarDoneMessage(AppConfig.myServentInfo, null, vectorClock);
		for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
			message = message.changeReceiver(neighbor);
			MessageUtil.sendMessage(message);
		}
		Message selfCommitMessage = new AlagarDoneMessage(AppConfig.myServentInfo, AppConfig.myServentInfo,
				vectorClock);
		snapshotCollector.addAlagarDoneMessage(AppConfig.myServentInfo.getId(), (AlagarDoneMessage) message);
		commitedMessages.add(selfCommitMessage);
		VectorClock.incrementClock(selfCommitMessage.getOriginalSenderInfo().getId());
	}

	/*
	 * Sends result to the neighbors
	 */
	private static void handleAcharyaToken(Map<Integer, Integer> vectorClock) {
		AcharyaBitcakeManager bitcakeManager = (AcharyaBitcakeManager) snapshotCollector.getBitcakeManager();
		AcharyaSnapshotResult snapshotResult = new AcharyaSnapshotResult(AppConfig.myServentInfo.getId(),
				bitcakeManager.getCurrentBitcakeAmount(), bitcakeManager.getSentHistory(),
				bitcakeManager.getRecievedHistory());

		Message message = new AcharyaTellMessage(AppConfig.myServentInfo, null, vectorClock, snapshotResult);
		for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
			message = message.changeReceiver(neighbor);
			MessageUtil.sendMessage(message);
		}
		Message selfCommitMessage = new AcharyaTellMessage(AppConfig.myServentInfo, AppConfig.myServentInfo,
				vectorClock, snapshotResult);
		commitedMessages.add(selfCommitMessage);
		VectorClock.incrementClock(selfCommitMessage.getOriginalSenderInfo().getId());
	}

	private static void handleTransaction(TransactionMessage causalPendingMessage) {
		if (causalPendingMessage.getOriginalReciverId() == AppConfig.myServentInfo.getId()) {
			String value = causalPendingMessage.getMessageText();
			int amountNumber = 0;
			try {
				amountNumber = Integer.parseInt(value);
			} catch (NumberFormatException e) {
				AppConfig.timestampedErrorPrint("Couldn't parse amount: " + value);
				return;
			}
			snapshotCollector.getBitcakeManager().addSomeBitcakes(amountNumber);
			switch (snapshotCollector.getSnapshotType()) {
			case ACHARYA:
				handleAcharyaTransaction(causalPendingMessage);
				break;
			case ALAGAR:
				handleAlagarTransaction(causalPendingMessage);
				break;
			default:
				break;
			}
		}
	}

	private static void handleAcharyaTransaction(TransactionMessage causalPendingMessage) {
		AcharyaBitcakeManager bitcakeManager = (AcharyaBitcakeManager) snapshotCollector.getBitcakeManager();
		bitcakeManager.addRecievedTransaction(causalPendingMessage.getOriginalSenderInfo().getId(),
				causalPendingMessage);
	}

	private static void handleAlagarTransaction(TransactionMessage causalPendingMessage) {
		AlagarBitcakeManager alagarBitcakeManager = (AlagarBitcakeManager) snapshotCollector.getBitcakeManager();
		if (!VectorClock.otherClockGreater(causalPendingMessage.getSenderVectorClock(), alagarSnapshotClock)
				&& isAlagarSnapshotStarted()) {
			alagarBitcakeManager.addOldTransaction(causalPendingMessage.getOriginalSenderInfo().getId(),
					causalPendingMessage);
		}
	}

	private static void tellAcharyaResult(AcharyaTellMessage message) {
		if (snapshotCollector.isCollecting()) {
			snapshotCollector.addAcharyaSnapshotInfo(message.getOriginalSenderInfo().getId(),
					message.getAcharyaSnapshotResult());
		}
	}
}
