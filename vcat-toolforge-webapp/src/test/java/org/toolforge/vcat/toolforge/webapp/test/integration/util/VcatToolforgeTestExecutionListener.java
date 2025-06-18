package org.toolforge.vcat.toolforge.webapp.test.integration.util;

import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestPlan;

public class VcatToolforgeTestExecutionListener implements TestExecutionListener {

    @Override
    public void testPlanExecutionFinished(TestPlan testPlan) {
        VcatToolforgeContainers.instance().stop();
    }

}
