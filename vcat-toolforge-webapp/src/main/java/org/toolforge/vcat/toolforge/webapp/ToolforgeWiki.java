package org.toolforge.vcat.toolforge.webapp;

import lombok.Getter;
import org.jspecify.annotations.Nullable;
import org.toolforge.vcat.mediawiki.interfaces.Wiki;

import java.io.Serial;
import java.util.Objects;

public class ToolforgeWiki implements Wiki {

    @Serial
    private static final long serialVersionUID = -8039078925472549881L;

    @Getter
    private final String dbname;

    private final String name;

    private final String url;

    protected ToolforgeWiki(@Nullable String dbname, @Nullable String name, @Nullable String url) {
        this.dbname = Objects.requireNonNull(dbname);
        this.name = Objects.requireNonNull(name);
        this.url = url;
    }

    @Override
    public String getApiUrl() {
        return url + "/w/api.php";
    }

    @Override
    public String getDisplayName() {
        return name + " (" + dbname + ')';
    }

    @Override
    public String getName() {
        return dbname;
    }

}
