package vcat.renderer.interfaces;

import vcat.VCatException;
import vcat.mediawiki.interfaces.Wiki;
import vcat.params.AbstractAllParams;
import vcat.renderer.RenderedFileInfo;

import java.io.Serializable;

public interface VCatRenderer<W extends Wiki> extends Serializable {

    public abstract RenderedFileInfo render(AbstractAllParams<W> all) throws VCatException;

}