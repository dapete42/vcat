package vcat.renderer;

import java.nio.file.Path;

public class RenderedFileInfo {

    private final Path file;

    private final String mimeType;

    public RenderedFileInfo(Path file, String mimeType) {
        this.file = file;
        this.mimeType = mimeType;
    }

    public Path getFile() {
        return file;
    }

    public String getMimeType() {
        return mimeType;
    }

}