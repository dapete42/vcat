package vcat.mediawiki;

import vcat.mediawiki.interfaces.Wiki;

import java.io.Serial;

public class SimpleWikimediaWiki implements Wiki {

    @Serial
    private static final long serialVersionUID = -7792081905551487036L;

    private final String host;

    public SimpleWikimediaWiki(String host) {
        this.host = host;
    }

    @Override
    public String getApiUrl() {
        return "https://" + this.host + "/w/api.php";
    }

    @Override
    public String getDisplayName() {
        return this.host;
    }

    @Override
    public String getName() {
        return this.host;
    }

}
