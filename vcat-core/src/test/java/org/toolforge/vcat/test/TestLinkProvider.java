package org.toolforge.vcat.test;

import org.jspecify.annotations.Nullable;
import org.toolforge.vcat.util.AbstractLinkProvider;

import java.io.Serial;

public class TestLinkProvider extends AbstractLinkProvider {

    @Serial
    private static final long serialVersionUID = 4438603179157010925L;

    @Override
    @Nullable
    public String provideLink(String title) {
        return "link:" + title;
    }

}