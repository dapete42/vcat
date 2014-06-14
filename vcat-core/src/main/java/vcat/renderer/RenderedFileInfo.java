package vcat.renderer;

import java.io.File;

public class RenderedFileInfo {

	private final File file;

	private final String mimeType;

	public RenderedFileInfo(File file, String mimeType) {
		this.file = file;
		this.mimeType = mimeType;
	}

	public File getFile() {
		return file;
	}

	public String getMimeType() {
		return mimeType;
	}

}