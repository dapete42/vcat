/*
 * Copyright (C) 2013 Peter Schlömer
 * Released unter the Eclipse Public License - v 1.0.
 */

package vcat.gv_java;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Loader for the native <code>gv_java</code> library used by the Graphviz JNI.
 * 
 * @author Peter Schlömer
 */
public abstract class GraphvizJNILoader {

	private static String[] defaultLocations = { "/usr/lib/graphviz/java/libgv_java.so", "./libgv_java.so" };

	/** Remember if the library has already been loadad. */
	private static boolean initialized = false;

	/**
	 * Try to load the Graphviz JNI library, <code>gv_java</code>, from default locations using
	 * {@link System#load(String) load} or using {@link System#loadLibrary(String) loadLibrary}. This is called by new
	 * {@link GraphvizJNI} instances automatically.
	 * 
	 * @return Whether loading has been successful.
	 */
	public static synchronized boolean init() {
		if (initialized) {
			return true;
		} else {
			return init(new ArrayList<String>(0));
		}
	}

	/**
	 * Try to load the Graphviz JNI library, <code>gv_java</code>, from default locations and additional custom
	 * locations using {@link System#load(String) load}, or using {@link System#loadLibrary(String) loadLibrary}. If the
	 * library is not installed system-wide, this should be called with the appropriate paths before trying to use
	 * {@link GraphvizJNI}.
	 * 
	 * @param customLocations
	 *            List of locations (full file path) where the <code>gv_java</code> library can be found.
	 * @return Whether loading has been successful.
	 */
	public static synchronized boolean init(List<String> customLocations) {
		if (initialized) {
			return true;
		} else {
			// Build a list of all locations. Search custom locations first, then default ones.
			ArrayList<String> locations = new ArrayList<>(customLocations);
			locations.addAll(Arrays.asList(defaultLocations));
			loadLoop: for (String location : locations) {
				try {
					System.load(location);
					initialized = true;
					break loadLoop;
				} catch (UnsatisfiedLinkError e1) {
					// Do nothing
				}
			}
			if (!initialized) {
				// If this did not work, try loadLibrary instead
				try {
					System.loadLibrary("gv_java");
					initialized = true;
				} catch (UnsatisfiedLinkError e1) {
					// Do nothing
				}
			}
			return initialized;
		}
	}

}
