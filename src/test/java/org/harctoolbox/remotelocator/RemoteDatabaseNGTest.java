/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.harctoolbox.remotelocator;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import org.harctoolbox.girr.Remote;
import org.harctoolbox.xml.XmlUtils;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.w3c.dom.Document;

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

    public RemoteDatabaseNGTest() {
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
    public void testGet() throws NotFoundException, IOException {
        System.out.println("get");
        String manufacturer = "Philips";
        String deviceClass = "tv";
        String remoteName = "philips_37pfl9603";
        RemoteDatabase instance = new RemoteDatabase();
        instance.add(RemoteKind.girr, null, null, localGirrTestBaseDir);
        instance.print("testgirr.xml");
        RemoteLink result = instance.get(manufacturer, deviceClass, remoteName);
        assertEquals(result.getComment(), "Full HD");
        try {
            instance.get(manufacturer, deviceClass, "sfmlsfsd");
            fail();
        } catch (NotFoundException ex) {
        }
    }

    @Test
    public void testGetRemote() throws NotFoundException, IOException {
        System.out.println("getRemote");
        String manufacturer = "Yamaha";
        String deviceClass = "unknown_rx-cx800";
        String remoteName = "Protocol=nec1,device=122";
        RemoteDatabase instance = RemoteDatabase.scrapIrdb(localIrdbBaseDir);
        Remote result = instance.getRemote(manufacturer, deviceClass, remoteName);
        Document doc = result.toDocument(null, null, null, false, true, true, false, false);
        XmlUtils.printDOM(new File("yamaha.girr"), doc);
    }

    @Test
    public void testGetRemoteGirr() throws NotFoundException, IOException {
        System.out.println("getRemoteGirr");
        String manufacturer = "unknown";
        String deviceClass = "unknown";
        String remoteName = "oppo";
        RemoteDatabase instance = new RemoteDatabase();// RemoteDatabase.scrapGirr(localGirrLibBaseDir);
        instance.add(RemoteKind.girr, null, null, localGirrLibBaseDir);
        instance.print("x.xml");
        Remote result = instance.getRemote(manufacturer, deviceClass, remoteName);
        Document doc = result.toDocument(null, null, null, false, true, true, false, false);
        XmlUtils.printDOM(new File("oppo.girr"), doc);
    }

    @Test
    public void testGetRemoteLirc() throws IOException, NotFoundException {
        System.out.println("getRemoteLirc");
        String manufacturer = "Yamaha";
        String deviceClass = "unknown";
        String remoteName = "RX-V995";
        RemoteDatabase instance = new RemoteDatabase();// RemoteDatabase.scrapGirr(localGirrLibBaseDir);
        instance.add(RemoteKind.lirc, null, null, localLircBaseDir);
        instance.sort();
        instance.print("l.xml");
        Remote result = instance.getRemote(manufacturer, deviceClass, remoteName);
        Document doc = result.toDocument(null, null, null, false, true, true, false, false);
        XmlUtils.printDOM(new File("y.girr"), doc);
    }

    /**
     * Test of addCsv method, of class RemoteDatabase.
     * @throws java.io.IOException
     */
    @Test
    public void testScrapIrdb() throws IOException {
        System.out.println("scrapTrdb");
        RemoteDatabase instance = RemoteDatabase.scrapIrdb(localIrdbBaseDir);
        instance.print("irdb.xml");
    }

    /**
     * Test of addLirc method, of class RemoteDatabase.
     * @throws java.io.IOException
     */
    @Test
    public void testAddLirc() throws IOException {
        System.out.println("addLirc");
        RemoteDatabase instance = RemoteDatabase.scrapLirc(localLircBaseDir);
        instance.print("lirc.xml");
    }

    /**
     * Test of iterator method, of class RemoteDatabase.
     * @throws java.io.IOException
     */
    @Test
    public void testIterator() throws IOException {
        System.out.println("iterator");
        RemoteDatabase instance = new RemoteDatabase();
        instance.add(RemoteKind.girr, localGirrTestBaseDir);
        int cnt = 0;
        for (Iterator<ManufacturerDeviceClasses> it = instance.iterator(); it.hasNext();) {
            it.next();
            cnt++;
        }
        assertEquals(cnt, 4);
    }

    /**
     * Test of addRecursive method, of class RemoteDatabase.
     * @throws java.io.IOException
     */
    @Test
    public void testScrapGirr() throws IOException {
        System.out.println("addRecursive");
        RemoteDatabase instance = RemoteDatabase.scrapGirr(localGirrLibBaseDir);
        instance.print("girr.xml");
    }
}
