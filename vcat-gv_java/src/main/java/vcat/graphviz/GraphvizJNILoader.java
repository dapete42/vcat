/*
 * Copyright (C) 2013 Peter Schlömer
 * Released unter the Eclipse Public License - v 1.0.
 */

package vcat.graphviz;

/**
 * Loader for the native <code>gv_java</code> library used by the Graphviz JNI.
 * 
 * @author Peter Schlömer
 */
public abstract class GraphvizJNILoader {

	/** Remember if the library has already been loadad. */
	private static boolean initialized = false;

	/**
	 * Load the Graphviz JNI library, <code>gv_java</code>.
	 */
	public static synchronized void init() {
		if (!initialized) {
			// TODO These are just some hard coded paths, should be handled better
			try {
				System.load("/usr/lib/graphviz/java/libgv_java.so");
			} catch (UnsatisfiedLinkError e1) {
				try {
					System.load("./libgv_java.so");
				} catch (UnsatisfiedLinkError e2) {
					System.loadLibrary("gv_java");
				}
			}
			initialized = true;
		}
	}

}
