package app;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class VectorClock {
	private static final Object cLock = new Object();
	private static Map<Integer, Integer> clock = new ConcurrentHashMap<>();

	public static void initializeVectorClock(int serventCount) {
		synchronized (cLock) {
			for (int i = 0; i < serventCount; i++) {
				clock.put(i, 0);
			}
		}
	} 

	public static Map<Integer, Integer> getClock() {
		Map<Integer, Integer> toReturn = new HashMap<>();
		synchronized (cLock) {
			for (Map.Entry<Integer, Integer> m : clock.entrySet()) {
				toReturn.put(m.getKey(), m.getValue());
			}
		}
		return toReturn;
	}

	public static void incrementClock(int serventId) {
		synchronized (cLock) {
			clock.computeIfPresent(serventId, (key, oldValue) -> oldValue + 1);
		}
	}

	public static boolean otherClockGreater(Map<Integer, Integer> clock1, Map<Integer, Integer> clock2) {
		if (clock1.size() != clock2.size()) {
			throw new IllegalArgumentException("Clocks are not same!");
		}
		for (int i = 0; i < clock1.size(); i++) {
			if (clock2.get(i) > clock1.get(i)) {
				return true;
			}
		}
		return false;
	}
}
