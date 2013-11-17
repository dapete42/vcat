package vcat.params;

import vcat.AbstractVCat;
import vcat.Messages;
import vcat.VCatException;
import vcat.VCatForCategories;
import vcat.VCatForSubcategories;
import vcat.mediawiki.ICategoryProvider;
import vcat.mediawiki.IWiki;

public class VCatFactory<W extends IWiki> {

	private final ICategoryProvider<W> categoryProvider;

	public VCatFactory(ICategoryProvider<W> categoryProvider) {
		this.categoryProvider = categoryProvider;
	}

	public AbstractVCat<W> createInstance(AbstractAllParams<W> all) throws VCatException {
		Relation relation = all.getVCat().getRelation();
		switch (relation) {
		case Category:
			return new VCatForCategories<W>(all, this.categoryProvider);
		case Subcategory:
			return new VCatForSubcategories<W>(all, this.categoryProvider);
		default:
			throw new VCatException(String.format(Messages.getString("VCatFactory.Exception.RelationTypeNotSupported"),
					relation.name()));
		}
	}

}
