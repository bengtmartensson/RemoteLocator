package org.harctoolbox.remotelocator;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
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
public class IrdbScrapNGTest {
    private static final File LOCAL_IRDB_BASEDIR = new File("../irdb/codes");

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    public IrdbScrapNGTest() {
    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
    }

    /**
     * Test of scrap method, of class IrdbScrap.
     * @throws java.io.IOException
     * @throws org.xml.sax.SAXException
     */
    @Test
    public void testScrap() throws IOException, SAXException {
        System.out.println("scrap");
        RemoteDatabase instance = IrdbScrap.scrap(LOCAL_IRDB_BASEDIR);
        instance.print("output/irdb.xml");
    }

    /**
     * Test of parse method, of class IrdbScrap.
     * @throws java.io.IOException
     */
    @Test
    public void testParse_3args_1() throws IOException {
        System.out.println("parse");
        File path = new File("../irdb/codes/Yamaha/Unknown_RX-V850/122,-1.csv");
        //File path = new File("../irdb/codes/BnK Components/Tuner_preamp/11,79.csv");
        String manufacturer = "Trabbi";
        String deviceType = "Tractor";
        Remote result = IrdbScrap.parse(path, manufacturer, deviceType);
        XmlUtils.printDOM(new File ("output/yamaha.girr"), result.toDocument(null, false, true, false, false));
    }

    /**
     * Test of getRemote method, of class IrdbScrap.
     * @throws java.lang.Exception
     */
    @Test
    public void testGetRemote() throws Exception {
        System.out.println("getRemote");
        IrdbScrap instance = new IrdbScrap();
        instance.add(LOCAL_IRDB_BASEDIR);
        String name = IrdbScrap.mkName("RC6", 4, 0);
        Remote remote = instance.getRemote("Yamaha", "DVD", name);
        remote.print("output/yamaha-dvd.girr");
        String firstcommand = remote.iterator().next().getCommands().iterator().next().getName();
        String expResult = "0";
        assertEquals(firstcommand, expResult);
        assertEquals(remote.getManufacturer(), "Yamaha");
        assertEquals(remote.getDeviceClass(), "DVD");
    }

    /**
     * Test of splitCSV method, of class IrdbScrap.
     * @throws java.text.ParseException
     */
    @Test
    public void testSplitCSV_String() throws ParseException {
        System.out.println("splitCSV");
        String input = "1997,Ford,E350";
        List<String> expResult = new ArrayList<>(3);
        expResult.add("1997");
        expResult.add("Ford");
        expResult.add("E350");
        List<String> result = IrdbScrap.splitCSV(input);
        assertEquals(result, expResult);

        input = "  \"1997\",\"Ford\"    ,    \"E350\" ";
        result = IrdbScrap.splitCSV(input);
        assertEquals(result, expResult);

        input = "1997,Ford,E350, \"Super, luxurious truck\" ";
        expResult.add("Super, luxurious truck");
        result = IrdbScrap.splitCSV(input);
        assertEquals(result, expResult);

        input = "1997,Ford,E350, \"Super, luxurious truck "; // unbalanced quotes
        try {
            IrdbScrap.splitCSV(input);
            fail();
        } catch (ParseException ex) {
        }

        input = "1997,   Ford ,E350,\"Super, luxurious truck\""; // whitespaces outside of "" may not be removed-
        expResult.set(1, "   Ford ");
        result = IrdbScrap.splitCSV(input);
        assertEquals(result, expResult);
    }
}
