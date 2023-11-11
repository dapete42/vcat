package vcat.renderer.interfaces;

import vcat.VCatException;
import vcat.params.AbstractAllParams;
import vcat.renderer.RenderedFileInfo;

import java.io.Serializable;

public interface VCatRenderer extends Serializable {

    RenderedFileInfo render(AbstractAllParams all) throws VCatException;

}