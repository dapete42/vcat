package vcat.util;

/**
 * Dummy link provider which does not add a link.
 *
 * @author Peter Schlömer
 */
public class EmptyLinkProvider extends AbstractLinkProvider {

    private static final long serialVersionUID = 4130505369114014479L;

    @Override
    public String provideLink(final String title) {
        return null;
    }

}
