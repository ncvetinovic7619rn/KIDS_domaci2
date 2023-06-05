package servent.message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import app.AppConfig;
import app.CausalBroadcastShared;
import app.ServentInfo;
import app.snapshot_bitcake.manager.AcharyaBitcakeManager;
import app.snapshot_bitcake.manager.BitcakeManager;

/**
 * Represents a bitcake transaction. We are sending some bitcakes to another
 * node.
 * 
 * @author bmilojkovic
 *
 */
public class TransactionMessage extends BasicMessage {
	private static final long serialVersionUID = -333251402058492901L;
	private final boolean isRebroadcast;
	private transient BitcakeManager bitcakeManager;
	private final int originalReciverId;

	public TransactionMessage(ServentInfo sender, ServentInfo receiver, int amount, BitcakeManager bitcakeManager,
			Map<Integer, Integer> senderVectorClock, boolean isRebroadcast, int originalReciverId) {
		super(MessageType.TRANSACTION, sender, receiver, String.valueOf(amount), senderVectorClock);
		this.isRebroadcast = isRebroadcast;
		this.bitcakeManager = bitcakeManager;
		this.originalReciverId = originalReciverId;
	}

	public TransactionMessage(ServentInfo sender, ServentInfo receiver, List<ServentInfo> routeList, String messageText,
			int messageId, Map<Integer, Integer> senderVectorClock, boolean isRebroadcast, int originalReciverId) {
		super(MessageType.TRANSACTION, sender, receiver, routeList, messageText, messageId, senderVectorClock);
		this.isRebroadcast = isRebroadcast;
		this.originalReciverId = originalReciverId;
	}
	
	

	public int getOriginalReciverId() {
		return originalReciverId;
	}

	/**
	 * We want to take away our amount exactly as we are sending, so our snapshots
	 * don't mess up. This method is invoked by the sender just before sending, and
	 * with a lock that guarantees that we are white when we are doing this in
	 * Chandy-Lamport.
	 */
	@Override
	public Message makeMeASender() {
		ServentInfo myInfo = AppConfig.myServentInfo;
		List<ServentInfo> newRouteList = new ArrayList<>(getRoute());
		newRouteList.add(myInfo);
		return new TransactionMessage(getOriginalSenderInfo(), getReceiverInfo(), newRouteList, getMessageText(),
				getMessageId(), getSenderVectorClock(), true, getOriginalReciverId());
	}

	@Override
	public Message changeReceiver(Integer newReceiverId) {

		if (AppConfig.myServentInfo.getNeighbors().contains(newReceiverId)) {
			ServentInfo newReceiverInfo = AppConfig.getInfoById(newReceiverId);
			return new TransactionMessage(getOriginalSenderInfo(), newReceiverInfo, getRoute(), getMessageText(),
					getMessageId(), getSenderVectorClock(), true,  getOriginalReciverId());
		} else {
			AppConfig.timestampedErrorPrint(newReceiverId + " is not our neighbor!");
			return null;
		}
	}

	@Override
	public void sendEffect() {
		if (!isRebroadcast) {
			int amount = Integer.parseInt(getMessageText());
			bitcakeManager.takeSomeBitcakes(amount);
			CausalBroadcastShared.commitMessage(this);
			if (bitcakeManager instanceof AcharyaBitcakeManager) {
				((AcharyaBitcakeManager) bitcakeManager).addSentTransaction(getOriginalReciverId(), this);
			}
		}
	}
}
