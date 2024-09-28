package org.toolforge.vcat.renderer.interfaces;

import org.toolforge.vcat.VCatException;
import org.toolforge.vcat.params.AbstractAllParams;
import org.toolforge.vcat.renderer.RenderedFileInfo;

import java.io.Serializable;

public interface VCatRenderer extends Serializable {

    RenderedFileInfo render(AbstractAllParams all) throws VCatException;

}
