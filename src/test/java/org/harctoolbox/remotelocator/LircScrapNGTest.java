// Note: This test requires the project https://sourceforge.net/projects/lirc-remotes/
// to be locally cloned to the location given by LOCAL_LIRCB_ASEDIR.

package org.harctoolbox.remotelocator;

import java.io.File;
import org.harctoolbox.girr.Command;
import org.harctoolbox.girr.Remote;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class LircScrapNGTest {
    private static final File LOCAL_LIRC_BASEDIR = new File("src/test/lirc-remotes/remotes");

    public LircScrapNGTest() {
    }

    @BeforeClass
    public void setUpClass() throws Exception {
    }

    @AfterClass
    public void tearDownClass() throws Exception {
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

    /**
     * Test of getRemote method, of class LircScrap.
     * @throws java.lang.Exception
     */
    @Test
    public void testGetRemote() throws Exception {
        System.out.println("getRemote");
        RemoteDatabase instance = LircScrap.scrap(LOCAL_LIRC_BASEDIR);
        Remote remote = instance.getRemote("Yamaha", "unknown", "yamaha-amp");
        Command firstCommand = remote.iterator().next().iterator().next();
        String expResult = "KEY_MUTE";
        assertEquals(firstCommand.getName(), expResult);
        assertEquals(remote.getManufacturer(), "yamaha");
        assertEquals(remote.getDeviceClass(), "unknown");
    }
}