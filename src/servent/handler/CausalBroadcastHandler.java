package servent.handler;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import app.AppConfig;
import app.CausalBroadcastShared;
import app.ServentInfo;
import servent.message.Message;
import servent.message.util.MessageUtil;

/**
 * Handles the CAUSAL_BROADCAST message. Fairly simple, as we assume that we are
 * in a clique. We add the message to a pending queue, and let the check on the
 * queue take care of the rest.
 * 
 * @author bmilojkovic
 *
 */
public class CausalBroadcastHandler implements MessageHandler {
	private static Set<Message> recievedBroadcasts = Collections.newSetFromMap(new ConcurrentHashMap<>());
	private final Message clientMessage;

	public CausalBroadcastHandler(Message clientMessage) {
		this.clientMessage = clientMessage;
	}

	@Override
	public void run() {
		// Sanity check.
		/*
		 * Same print as the one in BROADCAST handler. Kind of useless here, as we
		 * assume a clique.
		 */
		ServentInfo senderInfo = clientMessage.getOriginalSenderInfo();

//    			String text = String.format("Got %s from %s causally broadcast by %s\n", clientMessage.getMessageText(),
//    					lastSenderInfo, senderInfo);
//    			AppConfig.timestampedStandardPrint(text);

		if (senderInfo.getId() != AppConfig.myServentInfo.getId()) {
			if (recievedBroadcasts.add(clientMessage)) {
				ServentInfo lastSenderInfo = clientMessage.getRoute().isEmpty() ? clientMessage.getOriginalSenderInfo()
						: clientMessage.getRoute().get(clientMessage.getRoute().size() - 1);

				for (Integer neighbor : AppConfig.myServentInfo.getNeighbors()) {
					if (neighbor == lastSenderInfo.getId()) {
						continue;
					}
					MessageUtil.sendMessage(clientMessage.changeReceiver(neighbor).makeMeASender());
				}
				CausalBroadcastShared.addPendingMessage(clientMessage);
				CausalBroadcastShared.checkPendingMessages();
			}
		}
//    			CausalBroadcastShared.commitCausalMessage(clientMessage);
		/*
		 * 
		 * Uncomment the next line and comment out the two afterwards to see what
		 * happens when causality is broken.
		 */

	}

}
