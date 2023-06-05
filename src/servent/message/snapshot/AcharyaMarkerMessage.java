package servent.message.snapshot;

import java.util.List;
import java.util.Map;

import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.MessageType;

public class AcharyaMarkerMessage extends BasicMessage {
	private static final long serialVersionUID = -5619135560170169205L;

	public AcharyaMarkerMessage(ServentInfo senderInfo, ServentInfo receiverInfo,
			Map<Integer, Integer> senderVectorClock) {
		super(MessageType.ACHARYA_MARKER, senderInfo, receiverInfo, "", senderVectorClock);

	}

	private AcharyaMarkerMessage(ServentInfo originalSenderInfo, ServentInfo receiverInfo, List<ServentInfo> routeList,
			String messageText, int messageId, Map<Integer, Integer> senderVectorClock) {
		super(MessageType.ACHARYA_MARKER, originalSenderInfo, receiverInfo, routeList, messageText, messageId,
				senderVectorClock);
	}
}
