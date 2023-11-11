package vcat.junit;

import vcat.VCatForCategoriesTest;
import vcat.VCatForSubcategoriesTest;

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
