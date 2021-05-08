/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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

/**
 *
 * @author bengt
 */
public class IrdbNGTest {

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
     * Test of getName method, of class Irdb.
     */
    @Test
    public void testGetName() {
        System.out.println("getName");
        String expResult = "irdb";
        String result = Irdb.getName();
        assertEquals(result, expResult);
    }

    /**
     * Test of parse method, of class Irdb.
     * @throws java.io.IOException
     */
    @Test
    public void testParse() throws IOException {
        System.out.println("parse");
        File path = new File("../irdb/codes/Yamaha/Unknown_RX-V850/122,-1.csv");
        String manufacturer = "Trabbi";
        String deviceType = "Tractor";
        //Remote expResult = null;
        Remote result = Irdb.parse(path, manufacturer, deviceType);
        XmlUtils.printDOM(new File ("yama.girr"), result.toDocument(null, null, null, false, true, true, false, false));
    }
}
