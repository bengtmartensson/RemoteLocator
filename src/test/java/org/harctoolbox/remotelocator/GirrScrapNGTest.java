package org.harctoolbox.remotelocator;

import java.io.File;
import org.harctoolbox.girr.Remote;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class GirrScrapNGTest {
    private static final File localGirrLibBaseDir = new File("../GirrLib/Girr");
    private static final File localGirrTestBaseDir = new File("../Girr/src/test/girr");

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    public GirrScrapNGTest() {
    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
    }

    /**
     * Test of scrap method, of class GirrScrap.
     * @throws java.lang.Exception
     */
    @Test
    public void testScrap() throws Exception {
        System.out.println("scrap");
        RemoteDatabase result = GirrScrap.scrap(localGirrLibBaseDir);
        result.print("output/girr.xml");
    }

    /**
     * Test of add method, of class GirrScrap.
     * @throws java.lang.Exception
     */
    @Test
    public void testAdd_File() throws Exception {
        System.out.println("add");
        RemoteDatabase remoteDatabase = new RemoteDatabase();
        GirrScrap instance = new GirrScrap(remoteDatabase);
        instance.add(localGirrLibBaseDir);
        instance.add(localGirrTestBaseDir);
        remoteDatabase.print("output/allgirr.xml");
    }

    /**
     * Test of getRemote method, of class GirrScrap.
     * @throws java.lang.Exception
     */
    @Test
    public void testGetRemote() throws Exception {
        System.out.println("getRemote");
        GirrScrap instance = new GirrScrap();
        instance.add(localGirrLibBaseDir);
        String name = "coolstream_neo";
        Remote remote = instance.getRemote("Coolstream", "sat", name);
        remote.print("output/coolstream.girr");
        String firstcommand = remote.getCommands().keySet().iterator().next();
        String expResult = "power_toggle";
        assertEquals(firstcommand, expResult);
    }
}