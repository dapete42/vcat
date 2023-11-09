package vcat.util;

import java.io.Serial;

/**
 * Dummy link provider which does not add a link.
 *
 * @author Peter Schlömer
 */
public class EmptyLinkProvider extends AbstractLinkProvider {

    @Serial
    private static final long serialVersionUID = 4130505369114014479L;

    @Override
    public String provideLink(final String title) {
        return null;
    }

}
