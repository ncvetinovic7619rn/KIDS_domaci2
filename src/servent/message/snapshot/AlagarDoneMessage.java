package servent.message.snapshot;

import java.util.List;
import java.util.Map;

import app.ServentInfo;
import servent.message.BasicMessage;
import servent.message.MessageType;

public class AlagarDoneMessage extends BasicMessage {
	private static final long serialVersionUID = 1L;

	public AlagarDoneMessage(ServentInfo senderInfo, ServentInfo receiverInfo,
			Map<Integer, Integer> senderVectorClock) {
		super(MessageType.DONE, senderInfo, receiverInfo, "", senderVectorClock);
	}

	private AlagarDoneMessage(ServentInfo originalSenderInfo, ServentInfo receiverInfo, List<ServentInfo> routeList,
			String messageText, int messageId, Map<Integer, Integer> senderVectorClock) {
		super(MessageType.DONE, originalSenderInfo, receiverInfo, routeList, messageText, messageId, senderVectorClock);

	}
}
