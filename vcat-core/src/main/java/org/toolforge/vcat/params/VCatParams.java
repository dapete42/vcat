package org.toolforge.vcat.params;

import lombok.Getter;
import lombok.Setter;
import org.toolforge.vcat.AbstractVCat;
import org.toolforge.vcat.mediawiki.interfaces.Wiki;
import org.toolforge.vcat.util.AbstractLinkProvider;

import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;

/**
 * Parameters for the graph generation used by the {@link AbstractVCat VCat} class.
 *
 * @author Peter Schlömer
 */
@Getter
@Setter
public class VCatParams implements Serializable {

    @Serial
    private static final long serialVersionUID = -7181473469976962467L;

    private Integer depth;

    private Integer limit;

    /**
     * Link provider. Included here because different linking requires different cache entries.
     */
    private AbstractLinkProvider linkProvider;

    private Links links = Links.None;

    private Relation relation = Relation.Category;

    private boolean showhidden = false;

    private Collection<TitleNamespaceParam> titleNamespaceParams;

    private Wiki wiki;

}
