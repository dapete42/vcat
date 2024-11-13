package org.toolforge.vcat.mediawiki;

import jakarta.ws.rs.core.UriBuilder;
import org.toolforge.vcat.mediawiki.interfaces.Wiki;

import java.io.Serial;
import java.net.URI;

public class SimpleWikimediaWiki implements Wiki {

    @Serial
    private static final long serialVersionUID = -7792081905551487036L;

    private final String host;

    public SimpleWikimediaWiki(String host) {
        this.host = host;
    }

    @Override
    public String getApiUrl() {
        // Security: use an UriBuilder to escape the host name
        final var uri = URI.create("https://HOST/w/api.php");
        return UriBuilder.fromUri(uri).host(host).toString();
    }

    @Override
    public String getDisplayName() {
        return host;
    }

    @Override
    public String getName() {
        return host;
    }

}
