package org.toolforge.vcat.renderer;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.file.Path;

@AllArgsConstructor
@Getter
public class RenderedFileInfo {

    private final Path file;

    private final String mimeType;

}
