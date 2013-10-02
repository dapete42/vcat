package vcat.mediawiki;

public interface IMetadataProvider {

	public abstract Metadata requestMetadata(IWiki wiki) throws ApiException;

}