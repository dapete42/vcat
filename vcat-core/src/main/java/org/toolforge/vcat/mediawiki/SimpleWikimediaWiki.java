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
        final var escapedHost = UriBuilder.fromUri("http://" + host + "/").host(host).build().toString();
        return "https://" + escapedHost + "/w/api.php";
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
