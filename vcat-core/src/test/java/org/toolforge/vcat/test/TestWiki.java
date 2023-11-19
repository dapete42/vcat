package org.toolforge.vcat.test;

import org.toolforge.vcat.mediawiki.interfaces.Wiki;

import java.io.Serial;

public class TestWiki implements Wiki {

    @Serial
    private static final long serialVersionUID = 6023746140482931907L;

    @Override
    public String getApiUrl() {
        return "http://api.url";
    }

    @Override
    public String getDisplayName() {
        return "Test";
    }

    @Override
    public String getName() {
        return "test";
    }

}