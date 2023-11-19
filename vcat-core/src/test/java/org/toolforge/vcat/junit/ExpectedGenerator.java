package org.toolforge.vcat.junit;

import org.toolforge.vcat.VCatForCategoriesTest;
import org.toolforge.vcat.VCatForSubcategoriesTest;

/**
 * Run this to regenerate the cached API calls and results for some of the tests.
 * <p>
 * They may be generated in the wrong place! The files must be committed under the vcat-core/src directory of the Git
 * repository.
 */
public class ExpectedGenerator {

    public static void main(String... args) throws Exception {
        TestUtils.deleteAndRecreateDirectories(TestUtils.expectedDirectory, TestUtils.testApiClientCacheDirectory);
        TestUtils.generateExpected(
                VCatForCategoriesTest::new,
                VCatForSubcategoriesTest::new
        );
    }

}
