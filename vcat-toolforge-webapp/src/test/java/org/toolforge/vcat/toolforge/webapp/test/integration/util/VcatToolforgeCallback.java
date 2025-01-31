package org.toolforge.vcat.toolforge.webapp.test.integration.util;

import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.toolforge.vcat.toolforge.webapp.test.integration.docker.util.VcatToolforgeContainers;

public class VcatToolforgeCallback implements AfterAllCallback {

    @Override
    public void afterAll(ExtensionContext extensionContext) {
        VcatToolforgeContainers.instance().stop();
    }

}
