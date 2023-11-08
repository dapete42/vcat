package vcat.toolforge.webapp;

import lombok.Getter;
import vcat.mediawiki.interfaces.Wiki;

import java.io.Serial;

public class ToolforgeWiki implements Wiki {

    @Serial
    private static final long serialVersionUID = -8039078925472549881L;

    @Getter
    private final String dbname;

    private final String name;

    private final String url;

    protected ToolforgeWiki(final String dbname, final String name, final String url) {
        this.dbname = dbname;
        this.name = name;
        // Since June 2015, WMF have started to make wikis HTTPS-only, with all HTTP requests redirecting. All wikis
        // support HTTPS, so using it for the API would already have made sense before, and will now also avoid
        // unnecessary requests.
        if (url != null && url.startsWith("http://")) {
            this.url = "https" + url.substring(4);
        } else {
            this.url = url;
        }
    }

    @Override
    public String getApiUrl() {
        return this.url + "/w/api.php";
    }

    @Override
    public String getDisplayName() {
        return this.name + " (" + this.dbname + ')';
    }

    @Override
    public String getName() {
        return this.dbname;
    }

}
