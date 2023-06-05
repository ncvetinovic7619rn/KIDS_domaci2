package app.snapshot_bitcake;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import servent.message.Message;

/**
 * Snapshot result for servent with id serventId. The amount of bitcakes on that
 * servent is written in recordedAmount. The channel messages are recorded in
 * giveHistory and getHistory. In Lai-Yang, the initiator has to reconcile the
 * differences between individual nodes, so we just let him know what we got and
 * what we gave and let him do the rest.
 * 
 * @author bmilojkovic
 *
 */
public class AlagarSnapshotResult implements Serializable {
	private static final long serialVersionUID = 8939516333227254439L;
	private final int serventId;
	private final int recordedAmount;
	private final Map<Integer, List<Message>> oldHistory;

	public AlagarSnapshotResult(int serventId, int recordedAmount, Map<Integer, List<Message>> oldHistory) {
		this.serventId = serventId;
		this.recordedAmount = recordedAmount;
		this.oldHistory = new ConcurrentHashMap<>(oldHistory);
	}

	public int getServentId() {
		return serventId;
	}

	public int getRecordedAmount() {
		return recordedAmount;
	}

	public Map<Integer, List<Message>> getOldHistory() {
		return oldHistory;
	}

}
