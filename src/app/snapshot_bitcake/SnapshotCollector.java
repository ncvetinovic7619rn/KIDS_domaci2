package app.snapshot_bitcake;

import app.Cancellable;
import app.snapshot_bitcake.manager.BitcakeManager;
import servent.message.snapshot.AlagarDoneMessage;

/**
 * Describes a snapshot collector. Made not-so-flexibly for readability.
 * 
 * @author bmilojkovic
 *
 */
public interface SnapshotCollector extends Runnable, Cancellable {

	BitcakeManager getBitcakeManager();
	void addAcharyaSnapshotInfo(int id, AcharyaSnapshotResult acharyaSnapshotResult);
	void addAlagarSnapshotInfo(int id, AlagarSnapshotResult alagarSnapshotResult);
	void addAlagarDoneMessage(int id, AlagarDoneMessage alagarDoneMessage);
	SnapshotType getSnapshotType();
	void startCollecting();
	boolean isCollecting();

}