package org.harctoolbox.remotelocator;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import org.harctoolbox.girr.GirrException;
import org.harctoolbox.girr.Remote;
import org.harctoolbox.xml.XmlUtils;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.xml.sax.SAXException;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class FlipperScrapNGTest {
    private static final File LOCAL_FLIPPER_BASEDIR = new File("../Flipper-IRDB");

    public FlipperScrapNGTest() {
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
     * Test of scrap method, of class FlipperScrap.
     * @throws java.io.IOException
     * @throws org.xml.sax.SAXException
     */
    @Test
    public void testScrap() throws IOException, SAXException {
        System.out.println("scrap");
        RemoteDatabase instance = FlipperScrap.scrap(LOCAL_FLIPPER_BASEDIR);
        instance.print("output/flipper.xml");
    }

    /**
     * Test of parse method, of class FlipperScrap.
     * @throws java.io.IOException
     */
    @Test
    public void testParse_3args_1() throws IOException, ParseException, GirrException {
        System.out.println("parse");
        File path = new File("src/test/flipper/OPPO_BDP93.ir");
        String manufacturer = "Trabbi";
        String deviceType = "Tractor";
        Remote result = FlipperScrap.parse(path, manufacturer, deviceType);
        XmlUtils.printDOM(new File ("output/tractor.girr"), result.toDocument(null, false, true, false, false));
    }

    /**
     * Test of parse method, of class FlipperScrap.
     * @throws java.io.IOException
     */
    @Test
    public void testParse_3args_1_1() throws IOException, ParseException, GirrException {
        System.out.println("parse sony");
        File path = new File("../Flipper-IRDB/VCR/Sony/Sony_SLV-SE610B.ir");
        String manufacturer = "Trabbi";
        String deviceType = "Tractor";
        Remote result = FlipperScrap.parse(path, manufacturer, deviceType);
        XmlUtils.printDOM(new File ("output/sony.girr"), result.toDocument(null, false, true, false, false));
    }

    /**
     * Test of getRemote method, of class FlipperScrap.
     * @throws java.lang.Exception
     */
    @Test
    public void testGetRemote() throws Exception {
        System.out.println("getRemote");
        FlipperScrap instance = new FlipperScrap();
        instance.add(LOCAL_FLIPPER_BASEDIR);
        String name = "OPPO_BDP93";
        Remote remote = instance.getRemote("Oppo", "Blu-Ray", name);
        remote.print("output/oppo.girr");
        String firstcommand = remote.iterator().next().getCommands().iterator().next().getName();
        String expResult = "POWER";
        assertEquals(firstcommand, expResult);
        assertEquals(remote.getManufacturer(), "Oppo");
        assertEquals(remote.getDeviceClass(), "Blu-Ray");
    }
}
