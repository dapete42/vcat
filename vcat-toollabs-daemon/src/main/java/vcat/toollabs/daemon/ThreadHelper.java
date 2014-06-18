package vcat.toollabs.daemon;

public class ThreadHelper {

	public static void sleep(long millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			// ignore
		}
	}

}
