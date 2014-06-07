package vcat.util;

/**
 * Dummy link provider which does not add a link.
 * 
 * @author Peter Schlömer
 */
public class EmptyLinkProvider extends AbstractLinkProvider {

	@Override
	public String provideLink(final String title) {
		return null;
	}

}
