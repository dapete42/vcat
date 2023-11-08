package vcat.mediawiki.interfaces;

import vcat.mediawiki.ApiException;
import vcat.mediawiki.Metadata;

import java.io.Serializable;

public interface MetadataProvider extends Serializable {

    Metadata requestMetadata(Wiki wiki) throws ApiException;

}