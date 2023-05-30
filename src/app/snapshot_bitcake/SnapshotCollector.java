package app.snapshot_bitcake;

import app.Cancellable;

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

	void startCollecting();

}