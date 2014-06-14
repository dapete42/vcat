package vcat.renderer;

import vcat.VCatException;
import vcat.mediawiki.IWiki;
import vcat.params.AbstractAllParams;

public interface IVCatRenderer<W extends IWiki> {

	public abstract RenderedFileInfo render(AbstractAllParams<W> all) throws VCatException;

}