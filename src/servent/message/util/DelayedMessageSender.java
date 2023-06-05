package servent.message.util;

import app.AppConfig;
import app.ServentInfo;
import servent.message.Message;

import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

/**
 * This worker sends a message asynchronously. Doing this in a separate thread
 * has the added benefit of being able to delay without blocking main or somesuch.
 * 
 * @author bmilojkovic
 *
 */
public class DelayedMessageSender implements Runnable {

	private Message messageToSend;
	
	public DelayedMessageSender(Message messageToSend) {
		this.messageToSend = messageToSend;
	}
	
	public void run() {
		/*
		 * A random sleep before sending.
		 * It is important to take regular naps for health reasons.
		 */
		try {
			Thread.sleep((long)(Math.random() * 1000) + 500);
		} catch (InterruptedException e1) {
			e1.printStackTrace();
		}
		
		ServentInfo receiverInfo = messageToSend.getReceiverInfo();
		
		if (MessageUtil.MESSAGE_UTIL_PRINTING) {
			AppConfig.timestampedStandardPrint("Sending message " + messageToSend);
		}

		//Pokusavamo vise puta da posaljemo poruku
		int retryCount = 0;
		while (true) {
			try {
				Socket sendSocket = new Socket(receiverInfo.getIpAddress(), receiverInfo.getListenerPort());

				ObjectOutputStream oos = new ObjectOutputStream(sendSocket.getOutputStream());
				oos.writeObject(messageToSend);
				oos.flush();

				sendSocket.close();

				messageToSend.sendEffect();

				break;
			} catch (IOException e) {
				retryCount += 1;
				if (retryCount > MessageUtil.SEND_RETRY_LIMIT) {
					AppConfig.timestampedErrorPrint("Couldn't send message: " + messageToSend.toString() + ". No more retries.");
					break;
				} else {
					String text = String.format("Couldn't send message: %s. Retrying %s/%s.",
							messageToSend.toString(), retryCount, MessageUtil.SEND_RETRY_LIMIT);
					AppConfig.timestampedErrorPrint(text);
				}
			}
		}
	}
	
}
