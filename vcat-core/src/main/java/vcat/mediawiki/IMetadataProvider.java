package vcat.mediawiki;

import java.io.Serializable;

public interface IMetadataProvider extends Serializable {

	public abstract Metadata requestMetadata(IWiki wiki) throws ApiException;

}