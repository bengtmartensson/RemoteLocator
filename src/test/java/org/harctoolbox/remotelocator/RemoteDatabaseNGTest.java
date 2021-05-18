package org.harctoolbox.remotelocator;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import static org.harctoolbox.remotelocator.RemoteDatabase.UNKNOWN;
import org.harctoolbox.xml.XmlUtils;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

/**
 *
 * @author bengt
 */
public class RemoteDatabaseNGTest {
    //private static final File LOCAL_LIRC_BASEDIR = new File("../../lirc/lirc-remotes/remotes");
    private static final File LOCAL_IRDB_BASEDIR = new File("../irdb/codes");
    private static final File LOCAL_GIRRLIB_BASEDIR = new File("../GirrLib/Girr");
    //private static final File LOCAL_GIRRTEST_BASEDIR = new File("../Girr/src/test/girr");
    private static final File JP1_XML_FILE = new File("src/test/jp1/jp1-master-1.16.fods");

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    private final RemoteDatabase remoteDatabase;

    public RemoteDatabaseNGTest() throws IOException, SAXException {
        remoteDatabase = new RemoteDatabase();
        new GirrScrap(remoteDatabase).add(LOCAL_GIRRLIB_BASEDIR);
        //new LircScrap(remoteDatabase).add(LOCAL_LIRC_BASEDIR);
        new IrdbScrap(remoteDatabase).add(LOCAL_IRDB_BASEDIR);
        new Jp1Scrap(remoteDatabase).add(JP1_XML_FILE);
        remoteDatabase.sort();
        Document document = remoteDatabase.toDocument();
        XmlUtils.printDOM(new File("output/all.xml"), document);
    }

    @BeforeMethod
    public void setUpMethod() throws Exception {
    }

    @AfterMethod
    public void tearDownMethod() throws Exception {
    }

    /**
     * Test of get method, of class RemoteDatabase.
     * @throws org.harctoolbox.remotelocator.NotFoundException
     * @throws java.io.IOException
     */
    @Test
    public void testGet() throws Exception {
        System.out.println("get");
        String manufacturer = "Philips";
        String deviceClass = "tv";
        String remoteName = "philips_37pfl9603";
        RemoteDatabase instance = new RemoteDatabase();
        GirrScrap girr = new GirrScrap(instance);
        girr.add(new File("../GirrLib/Girr"));
        RemoteLink result = instance.get(manufacturer, deviceClass, remoteName);
        assertEquals(result.getComment(), "dfdklfkd");
        try {
            instance.get(manufacturer, deviceClass, "sfmlsfsd");
            fail();
        } catch (NotFoundException ex) {
        }
    }

    /**
     * Test of iterator method, of class RemoteDatabase.
     * @throws java.io.IOException
     */
    @Test
    public void testIterator() throws Exception {
        System.out.println("iterator");
        RemoteDatabase instance = new RemoteDatabase();
        GirrScrap girr = new GirrScrap(instance);
        girr.add(new File("../Girr/src/test/girr"));
        int cnt = 0;
        for (Iterator<ManufacturerDeviceClasses> it = instance.iterator(); it.hasNext();) {
            it.next();
            cnt++;
        }
        assertEquals(cnt, 4);
    }

    /**
     * Test of mkKey method, of class RemoteDatabase.
     */
    @Test
    public void testMkKey() {
        System.out.println("mkKey");
        String string = "";
        String expResult = UNKNOWN;
        String result = RemoteDatabase.mkKey(string);
        assertEquals(result, expResult);
        result = RemoteDatabase.mkKey("xYz");
        assertEquals(result, "xyz");
    }

    /**
     * Test of getManufacturers method, of class RemoteDatabase.
     */
    @Test
    public void testGetManufacturers() {
        System.out.println("getManufacturers");
        List<String> result = remoteDatabase.getManufacturers(null);
        assertEquals(result.size(), 1775);
        result = remoteDatabase.getManufacturers(ScrapKind.girr);
        assertEquals(result.size(), 37);
    }

    /**
     * Test of getDeviceTypes method, of class RemoteDatabase.
     * @throws org.harctoolbox.remotelocator.NotFoundException
     */
    @Test
    public void testGetDeviceTypes() throws NotFoundException {
        System.out.println("getDeviceTypes");
        String manufacturer = "Philips";
        List<String> result = remoteDatabase.getDeviceTypes(null, manufacturer);
        assertEquals(result.size(), 87);
        result = remoteDatabase.getDeviceTypes(ScrapKind.girr, manufacturer);
        assertEquals(result.size(), 3);
        try {
            remoteDatabase.getDeviceTypes(null, "dfdfdfdf");
            fail();
        } catch (NotFoundException ex) {
        }
    }

    /**
     * Test of getRemotes method, of class RemoteDatabase.
     * @throws org.harctoolbox.remotelocator.NotFoundException
     */
    @Test
    public void testGetRemotes() throws NotFoundException {
        System.out.println("getRemotes");
        String manufacturer = "Philips";
        String deviceType = "TV";
        List result = remoteDatabase.getRemotes(manufacturer, deviceType);
        assertEquals(result.size(), 38);
        try {
            remoteDatabase.getRemotes(manufacturer, "sdfsdfsdfdsf");
            fail();
        } catch (NotFoundException ex) {
        }
        assertEquals(result.size(), 38);
    }

    /**
     * Test of RemoteDatabase(File).
     * @throws Exception
     */
    @Test
    public void testRemoteDatabase_File() throws Exception {
        System.out.println("getRemoteDatabaseFile");
        RemoteDatabase db = new RemoteDatabase(new File("output/all.xml"));
        db.sort();
        db.print("output/aller.xml");
    }
}
