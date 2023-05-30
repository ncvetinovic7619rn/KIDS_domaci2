package app.snapshot_bitcake;

/**
 * This class is used if the user hasn't specified a snapshot type in config.
 * 
 * @author bmilojkovic
 *
 */
public class NullSnapshotCollector implements SnapshotCollector {

	@Override
	public void run() {
	}

	@Override
	public void stop() {
	}

	@Override
	public BitcakeManager getBitcakeManager() {
		return null;
	}

	@Override
	public void startCollecting() {
	}

	@Override
	public void addAcharyaSnapshotInfo(int id, AcharyaSnapshotResult acharyaSnapshotResult) {
		// TODO Auto-generated method stub
	}

	@Override
	public void addAlagarSnapshotInfo(int id, AlagarSnapshotResult alagarSnapshotResult) {
		// TODO Auto-generated method stub
	}

}
