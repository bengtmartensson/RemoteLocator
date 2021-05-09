package org.harctoolbox.remotelocator;

import java.io.File;
import java.io.FileNotFoundException;
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
    private static final File localLircBaseDir = new File("../../lirc/lirc-remotes/remotes");
    private static final File localIrdbBaseDir = new File("../irdb/codes");
    private static final File localGirrLibBaseDir = new File("../GirrLib/Girr");
    private static final File localGirrTestBaseDir = new File("../Girr/src/test/girr");

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    private final RemoteDatabase remoteDatabase;

    public RemoteDatabaseNGTest() throws IOException, SAXException {
        remoteDatabase = new RemoteDatabase();
        new GirrScrap(remoteDatabase).add(localGirrLibBaseDir);
        new LircScrap(remoteDatabase).add(localLircBaseDir);
        new IrdbScrap(remoteDatabase).add(localIrdbBaseDir);
        remoteDatabase.sort();
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
     * Test of toDocument method, of class RemoteDatabase.
     * @throws java.io.FileNotFoundException
     */
    @Test
    public void testToDocument() throws FileNotFoundException {
        System.out.println("toDocument");
        Document document = remoteDatabase.toDocument();
        XmlUtils.printDOM(new File("output/all.xml"), document);
    }

    /**
     * Test of getManufacturers method, of class RemoteDatabase.
     */
    @Test
    public void testGetManufacturers() {
        System.out.println("getManufacturers");
        List<String> result = remoteDatabase.getManufacturers();
        assertEquals(result.size(), 833);
    }

    /**
     * Test of getDeviceTypes method, of class RemoteDatabase.
     */
    @Test
    public void testGetDeviceTypes() {
        System.out.println("getDeviceTypes");
        String manufacturer = "Philips";
        List<String> result = remoteDatabase.getDeviceTypes(manufacturer);
        assertEquals(result.size(), 73);
    }

    /**
     * Test of getRemotes method, of class RemoteDatabase.
     */
    @Test
    public void testGetRemotes() {
        System.out.println("getRemotes");
        String manufacturer = "Philips";
        String deviceType = "TV";
        List result = remoteDatabase.getRemotes(manufacturer, deviceType);
        assertEquals(result.size(), 6);
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
