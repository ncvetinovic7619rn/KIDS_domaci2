package servent.message.snapshot;

import java.util.List;
import java.util.Map;

import app.AppConfig;
import app.ServentInfo;
import app.snapshot_bitcake.AcharyaSnapshotResult;
import servent.message.BasicMessage;
import servent.message.Message;
import servent.message.MessageType;

public class AcharyaTellMessage extends BasicMessage {
	private static final long serialVersionUID = 8688053275497701589L;
	private final AcharyaSnapshotResult acharyaSnapshotResult;

	public AcharyaTellMessage(ServentInfo senderInfo, ServentInfo receiverInfo, Map<Integer, Integer> senderVectorClock,
			AcharyaSnapshotResult acharyaSnapshotResult) {
		super(MessageType.ACHARYA_TELL_AMOUNT, senderInfo, receiverInfo, "", senderVectorClock);
		this.acharyaSnapshotResult = acharyaSnapshotResult;
	}

	private AcharyaTellMessage(ServentInfo originalSenderInfo, ServentInfo receiverInfo, List<ServentInfo> routeList,
			String messageText, int messageId, Map<Integer, Integer> senderVectorClock,
			AcharyaSnapshotResult acharyaSnapshotResult) {
		super(MessageType.ACHARYA_TELL_AMOUNT, originalSenderInfo, receiverInfo, routeList, messageText, messageId,
				senderVectorClock);
		this.acharyaSnapshotResult = acharyaSnapshotResult;
	}

	public AcharyaSnapshotResult getAcharyaSnapshotResult() {
		return acharyaSnapshotResult;
	}
	@Override
	public Message makeMeASender() {
		ServentInfo myInfo = AppConfig.myServentInfo;
		List<ServentInfo> newRouteList = getRoute();
		newRouteList.add(myInfo);
		return new AcharyaTellMessage(getOriginalSenderInfo(), getReceiverInfo(), newRouteList, getMessageText(),
				getMessageId(), getSenderVectorClock(), getAcharyaSnapshotResult());
	}

	@Override
	public Message changeReceiver(Integer newReceiverId) {
		if (AppConfig.myServentInfo.getNeighbors().contains(newReceiverId)) {
			ServentInfo newReceiverInfo = AppConfig.getInfoById(newReceiverId);
			return new AcharyaTellMessage(getOriginalSenderInfo(), newReceiverInfo, getRoute(), getMessageText(),
					getMessageId(), getSenderVectorClock(), getAcharyaSnapshotResult());
		} else {
			AppConfig.timestampedErrorPrint(newReceiverId + " is not our neighbor!");
			return null;
		}
	}
}
