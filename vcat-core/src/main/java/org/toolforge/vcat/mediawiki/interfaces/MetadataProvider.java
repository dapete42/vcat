package org.toolforge.vcat.mediawiki.interfaces;

import org.toolforge.vcat.mediawiki.ApiException;
import org.toolforge.vcat.mediawiki.Metadata;

import java.io.Serializable;

public interface MetadataProvider extends Serializable {

    Metadata requestMetadata(Wiki wiki) throws ApiException;

}