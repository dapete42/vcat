package vcat.params;

import vcat.AbstractVCat;
import vcat.Messages;
import vcat.VCatException;
import vcat.VCatForCategories;
import vcat.VCatForSubcategories;
import vcat.mediawiki.ICategoryProvider;
import vcat.mediawiki.IWiki;
import vcat.util.AbstractLinkProvider;

public class VCatFactory<W extends IWiki> {

	private final ICategoryProvider<W> categoryProvider;

	public VCatFactory(final ICategoryProvider<W> categoryProvider) {
		this.categoryProvider = categoryProvider;
	}

	public AbstractVCat<W> createInstance(final AbstractAllParams<W> all) throws VCatException {
		final Relation relation = all.getVCat().getRelation();
		final AbstractLinkProvider linkProvider = AbstractLinkProvider.fromParams(all);
		switch (relation) {
		case Category:
			return new VCatForCategories<W>(all, this.categoryProvider, linkProvider);
		case Subcategory:
			return new VCatForSubcategories<W>(all, this.categoryProvider, linkProvider);
		default:
			throw new VCatException(String.format(Messages.getString("VCatFactory.Exception.RelationTypeNotSupported"),
					relation.name()));
		}
	}

}
