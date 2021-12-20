package vcat.renderer;

import java.io.Serializable;

import vcat.VCatException;
import vcat.mediawiki.IWiki;
import vcat.params.AbstractAllParams;

public interface IVCatRenderer<W extends IWiki> extends Serializable {

	public abstract RenderedFileInfo render(AbstractAllParams<W> all) throws VCatException;

}