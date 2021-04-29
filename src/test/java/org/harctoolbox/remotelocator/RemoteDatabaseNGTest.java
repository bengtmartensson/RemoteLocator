/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.harctoolbox.remotelocator;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.fail;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 *
 * @author bengt
 */
public class RemoteDatabaseNGTest {
    private static final String lircBaseDir = "../../lirc/lirc-remotes/remotes";
    private static final String irdbBaseDir = "../irdb/codes";
    private static final String girrLibBaseDir = "../GirrLib/Girr";
    private static final String girrTestBaseDir = "../Girr/src/test/girr";

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
     * Test of print method, of class RemoteDatabase.
     * @throws java.io.FileNotFoundException
     */
    @Test
    public void testPrint() throws IOException {
        System.out.println("print");
        RemoteDatabase.setBaseDir("..");
        RemoteDatabase instance = new RemoteDatabase();
        instance.add(girrLibBaseDir);
//        instance.add(RemoteKind.cvs, irdbBaseDir);
//        instance.add(RemoteKind.lirc, lircBaseDir);
        instance.sort();

        instance.print(new File("out.xml"));
    }

    /**
     * Test of get method, of class RemoteDatabase.
     * @throws org.harctoolbox.remotelocator.NotFoundException
     */
    @Test
    public void testGet() throws NotFoundException {
        System.out.println("get");
        String manufacturer = "Philips";
        String deviceClass = "tv";
        String remoteName = "philips_37pfl9603";
        RemoteDatabase.setBaseDir("..");
        RemoteDatabase instance = new RemoteDatabase();
        instance.add(RemoteKind.girr, "../Girr/src/test/girr", "../GirrLib/Girr");
        RemoteLink result = instance.get(manufacturer, deviceClass, remoteName);
        assertEquals(result.getComment(), "Full HD");
        try {
            instance.get(manufacturer, deviceClass, "sfmlsfsd");
            fail();
        } catch (NotFoundException ex) {
        }
    }

    /**
     * Test of addCsv method, of class RemoteDatabase.
     * @throws java.io.IOException
     */
    @Test
    public void testAddCsv() throws IOException {
        System.out.println("addCsv");
        RemoteDatabase.setBaseDir(irdbBaseDir);
        RemoteDatabase instance = new RemoteDatabase();
        //instance.add("Girr/src/test/girr", "GirrLib/Girr");
        instance.add(RemoteKind.cvs, irdbBaseDir);
        instance.sort();
        instance.print("irdb.xml");
    }

    /**
     * Test of addLirc method, of class RemoteDatabase.
     * @throws java.io.IOException
     */
    @Test
    public void testAddLirc() throws IOException {
        System.out.println("addLirc");

        RemoteDatabase.setBaseDir(lircBaseDir);
        RemoteDatabase instance = new RemoteDatabase();
        //instance.add("Girr/src/test/girr", "GirrLib/Girr");
        instance.add(RemoteKind.lirc, lircBaseDir);
        instance.sort();
        instance.print("lirc.xml");
    }

    /**
     * Test of iterator method, of class RemoteDatabase.
     */
    @Test
    public void testIterator() {
        System.out.println("iterator");
        RemoteDatabase instance = new RemoteDatabase("../Girr/src/test/girr");
        int cnt = 0;
        for (Iterator<ManufacturerDeviceClasses> it = instance.iterator(); it.hasNext();) {
            it.next();
            cnt++;
        }
        assertEquals(cnt, 4);
    }

    /**
     * Test of addRecursive method, of class RemoteDatabase.
     */
    @Test
    public void testAddRecursive() {
        System.out.println("addRecursive");
        File file = new File("../Girr/src/test/girr");
        RemoteDatabase instance = new RemoteDatabase();
        instance.addRecursive(file);

    }
}
