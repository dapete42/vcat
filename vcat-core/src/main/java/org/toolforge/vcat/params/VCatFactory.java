package org.toolforge.vcat.params;

import org.toolforge.vcat.AbstractVCat;
import org.toolforge.vcat.VCatForCategories;
import org.toolforge.vcat.VCatForSubcategories;
import org.toolforge.vcat.mediawiki.interfaces.CategoryProvider;

import java.io.Serial;
import java.io.Serializable;

public class VCatFactory implements Serializable {

    @Serial
    private static final long serialVersionUID = 5190043989637851420L;

    private final CategoryProvider categoryProvider;

    public VCatFactory(final CategoryProvider categoryProvider) {
        this.categoryProvider = categoryProvider;
    }

    public AbstractVCat createInstance(final AbstractAllParams all) {
        final Relation relation = all.getVCat().getRelation();
        return switch (relation) {
            case Category -> new VCatForCategories(all, this.categoryProvider);
            case Subcategory -> new VCatForSubcategories(all, this.categoryProvider);
        };
    }

}
