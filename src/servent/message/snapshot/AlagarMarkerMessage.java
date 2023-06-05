package servent.message.snapshot;

import java.util.List;
import java.util.Map;

import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.MessageType;

public class AlagarMarkerMessage extends BasicMessage {
	private static final long serialVersionUID = 1L;

	public AlagarMarkerMessage(ServentInfo senderInfo, ServentInfo receiverInfo,
			Map<Integer, Integer> senderVectorClock) {
		super(MessageType.ALAGAR_MARKER, senderInfo, receiverInfo, "", senderVectorClock);
	}

	private AlagarMarkerMessage(ServentInfo originalSenderInfo, ServentInfo receiverInfo, List<ServentInfo> routeList,
			String messageText, int messageId, Map<Integer, Integer> senderVectorClock) {
		super(MessageType.ALAGAR_MARKER, originalSenderInfo, receiverInfo, routeList, messageText, messageId,
				senderVectorClock);

	}
}
