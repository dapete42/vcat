package vcat.mediawiki.interfaces;

import java.io.Serializable;

public interface Wiki extends Serializable {

    String getApiUrl();

    String getDisplayName();

    String getName();

}
