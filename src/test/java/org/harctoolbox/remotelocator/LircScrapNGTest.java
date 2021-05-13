package org.harctoolbox.remotelocator;

import java.io.File;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 *
 * @author bengt
 */
public class LircScrapNGTest {
    private static final File LOCAL_LIRC_BASEDIR = new File("../../lirc/lirc-remotes/remotes");

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    public LircScrapNGTest() {
    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
    }

    /**
     * Test of scrap method, of class LircScrap.
     * @throws java.lang.Exception
     */
    // Takes pretty long (24s) (and consumes pretty much momory), so disabled by default
    @Test(enabled = false)
    public void testScrap() throws Exception {
        System.out.println("scrap");
        RemoteDatabase result = LircScrap.scrap(LOCAL_LIRC_BASEDIR);
        result.print("output/lirc.xml");
    }
}