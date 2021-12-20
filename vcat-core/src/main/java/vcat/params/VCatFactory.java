package vcat.params;

import java.io.Serializable;

import org.slf4j.helpers.MessageFormatter;

import vcat.AbstractVCat;
import vcat.Messages;
import vcat.VCatException;
import vcat.VCatForCategories;
import vcat.VCatForSubcategories;
import vcat.mediawiki.ICategoryProvider;
import vcat.mediawiki.IWiki;

public class VCatFactory<W extends IWiki> implements Serializable {

	private static final long serialVersionUID = 5190043989637851420L;

	private final ICategoryProvider<W> categoryProvider;

	public VCatFactory(final ICategoryProvider<W> categoryProvider) {
		this.categoryProvider = categoryProvider;
	}

	public AbstractVCat<W> createInstance(final AbstractAllParams<W> all) throws VCatException {
		final Relation relation = all.getVCat().getRelation();
		switch (relation) {
		case Category:
			return new VCatForCategories<>(all, this.categoryProvider);
		case Subcategory:
			return new VCatForSubcategories<>(all, this.categoryProvider);
		default:
			throw new VCatException(MessageFormatter
					.format(Messages.getString("VCatFactory.Exception.RelationTypeNotSupported"), relation.name())
					.getMessage());
		}
	}

}
