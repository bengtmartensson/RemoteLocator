package org.harctoolbox.remotelocator;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import org.harctoolbox.girr.Command;
import org.harctoolbox.girr.CommandSet;
import org.harctoolbox.girr.GirrException;
import org.harctoolbox.ircore.IrCoreException;
import org.harctoolbox.irp.IrpException;
import org.harctoolbox.irp.NameEngine;
import static org.testng.Assert.*;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class FlipperParserNGTest {
    private static final File LOCAL_FLIPPER_BASEDIR = new File("../Flipper-IRDB");

    public FlipperParserNGTest() {
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

    private void parseCheck(String filename, String commandName, String protocol, String  expected) throws IOException, IrpException, IrCoreException, GirrException, ParseException {
        File file = new File(LOCAL_FLIPPER_BASEDIR, filename);
        FileReader reader = new FileReader(file);
        CommandSet cmdSet = FlipperParser.parse(reader);
        Command command = cmdSet.getCommand(commandName);
        assertEquals(command.getProtocolName(), protocol);
        NameEngine actualParameters = new NameEngine(command.getParameters());
        NameEngine expectedParameters = new NameEngine(expected);
        assertTrue(actualParameters.equals(expectedParameters));
    }

    /**
     * Test of parse method, of class FlipperParser.
     */
    @Test
    public void testParse() throws Exception {
        System.out.println("parse");
        parseCheck("Audio_and_Video_Receivers/Aiwa/AIWA_XR-EM300.ir", "Mute", "Aiwa", "{D=110,S=0,F=76}");
        parseCheck("Blu-Ray/Oppo/OPPO_BDP93.ir", "POWER", "NEC1", "{D=73,F=26}");
        parseCheck("Audio_and_Video_Receivers/Yamaha/Yamaha_RAS5.ir", "Dock", "NEC1-f16", "{D=127,S=1,F=74,E=180}");
        parseCheck("Blu-Ray/LG/LG_UBK80.ir", "Power", "NECx1", "{D=45,S=45,F=48}");
        parseCheck("TVs/Philips/Philips_32PFL7962D12.ir", "Power", "RC5", "{D=0,F=12}");
        parseCheck("TVs/Philips/Philips_32PFL7962D12.ir", "MenuDigital", "RC5", "{D=3,F=82}");
        parseCheck("TVs/Philips/Philips_TV_Universal.ir", "Power", "RC6", "{D=0,F=12}");
        parseCheck("VCR/Sony/Sony_SLVM88.ir", "Power", "Sony12", "{D=11,F=21}");
        parseCheck("VCR/Sony/Sony_SLVM88.ir", "Ok", "Sony15", "{D=186,F=24}");
        parseCheck("Projectors/Sony/Sony_RM_PJ24.ir", "Zoom", "Sony20", "{D=26,S=42,F=98}");
        parseCheck("Audio_and_Video_Receivers/Denon/Denon_RC1253.ir", "CBL/SAT", "Denon-K", "{D=4,S=1,F=723}");
        parseCheck("Blu-Ray/Panasonic/Panasonic_DPUB820.ir", "Power", "Panasonic", "{D=176,S=0,F=61}");
        parseCheck("Blu-Ray/Panasonic/Panasonic_DPUB820.ir", "Up", "Panasonic", "{D=176,S=0,F=133}");
        parseCheck("Blu-Ray/JVC/JVC_XVBP1.ir", "Fast_ba", "JVC-48", "{D=32,S=26,F=64}"); // ??
        parseCheck("Blu-Ray/Pioneer/Pioneer_BDP150.ir", "Power", "Pioneer", "{D=175,F=188}");
        parseCheck("TVs/RCA/RCA_CRK50A.ir", "Power", "RCA", "{D=15,F=84}");
    }
}
