package org.harctoolbox.remotelocator;

import java.io.File;
import java.io.IOException;
import org.harctoolbox.girr.Remote;
import org.harctoolbox.xml.XmlUtils;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

/**
 *
 * @author bengt
 */
public class IrdbNGTest {
    private static final File localIrdbBaseDir = new File("../irdb/codes");

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    public IrdbNGTest() {
    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
    }

    /**
     * Test of scrap method, of class Irdb.
     * @throws java.io.IOException
     * @throws org.xml.sax.SAXException
     */
    @Test
    public void testScrap() throws IOException, SAXException {
        System.out.println("scrap");
        RemoteDatabase instance = Irdb.scrap(localIrdbBaseDir);
        instance.print("output/irdb.xml");
    }

    /**
     * Test of parse method, of class Irdb.
     * @throws java.io.IOException
     */
    @Test
    public void testParse_3args_1() throws IOException {
        System.out.println("parse");
        File path = new File("../irdb/codes/Yamaha/Unknown_RX-V850/122,-1.csv");
        //File path = new File("../irdb/codes/BnK Components/Tuner_preamp/11,79.csv");
        String manufacturer = "Trabbi";
        String deviceType = "Tractor";
        Remote result = Irdb.parse(path, manufacturer, deviceType);
        XmlUtils.printDOM(new File ("output/yamaha.girr"), result.toDocument(null, null, null, false, true, true, false, false));
    }

    /**
     * Test of getRemote method, of class Irdb.
     * @throws java.lang.Exception
     */
    @Test
    public void testGetRemote() throws Exception {
        System.out.println("getRemote");
        Irdb instance = new Irdb();
        instance.add(localIrdbBaseDir);
        String name = Irdb.mkName("RC6", 4, 0);
        Remote remote = instance.getRemote("Yamaha", "DVD", name);
        remote.print("output/yamaha-dvd.girr");
        String firstcommand = remote.getCommands().keySet().iterator().next();
        String expResult = "0";
        assertEquals(firstcommand, expResult);
    }
}
