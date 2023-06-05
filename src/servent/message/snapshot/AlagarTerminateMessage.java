package servent.message.snapshot;

import java.util.List;
import java.util.Map;

import app.AppConfig;
import app.ServentInfo;
import app.snapshot_bitcake.AcharyaSnapshotResult;
import app.snapshot_bitcake.AlagarSnapshotResult;
import servent.message.BasicMessage;
import servent.message.Message;
import servent.message.MessageType;

public class AlagarTerminateMessage extends BasicMessage {
	private static final long serialVersionUID = -7105930611836478136L;

	public AlagarTerminateMessage(ServentInfo senderInfo, ServentInfo receiverInfo,
			Map<Integer, Integer> senderVectorClock) {
		super(MessageType.TERMINATE, senderInfo, receiverInfo, "", senderVectorClock);
	}

	private AlagarTerminateMessage(ServentInfo originalSenderInfo, ServentInfo receiverInfo,
			List<ServentInfo> routeList, String messageText, int messageId, Map<Integer, Integer> senderVectorClock) {
		super(MessageType.TERMINATE, originalSenderInfo, receiverInfo, routeList, messageText, messageId,
				senderVectorClock);
	}

}
