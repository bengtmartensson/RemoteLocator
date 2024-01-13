package org.harctoolbox.remotelocator;

import java.io.File;
import java.net.URL;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class Jp1ScrapNGTest {

    private static final File JP1_XML_FILE = new File("src/test/jp1/jp1-master-1.17.fods");

    @BeforeClass
    public void setUpClass() throws Exception {
    }

    @AfterClass
    public void tearDownClass() throws Exception {
    }

    public Jp1ScrapNGTest() {
    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
    }

    /**
     * Test of scrap method, of class Jp1Scrap.
     * @throws java.lang.Exception
     */
    @Test
    public void testScrap() throws Exception {
        System.out.println("scrap");
        RemoteDatabase result = Jp1Scrap.scrap(JP1_XML_FILE);
        result.print("output/jp1.xml");
    }

    /**
     * Test of getRemote method, of class Jp1Scrap.
     * @throws java.lang.Exception
     */
    @Test
    public void testGetUrl() throws Exception {
        System.out.println("getRemote");
        Jp1Scrap instance = new Jp1Scrap();
        instance.add(JP1_XML_FILE);
        String name = "Oppo Sonica DAC";
        URL url = instance.getUrl("Oppo", "digital stbs", name);
        String result = url.toString();
        String expResult = "http://www.hifi-remote.com/forums/dload.php?action=file&file_id=14576";
        assertEquals(result, expResult);
    }
}
