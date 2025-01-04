/*
Copyright (C) 2025 Bengt Martensson.

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 3 of the License, or (at
your option) any later version.

This program is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
General Public License for more details.

You should have received a copy of the GNU General Public License along with
this program. If not, see http://www.gnu.org/licenses/.
*/

package org.harctoolbox.remotelocator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.harctoolbox.girr.Command;
import org.harctoolbox.girr.CommandSet;
import org.harctoolbox.girr.GirrException;
import org.harctoolbox.girr.Remote;
import org.harctoolbox.ircore.ThisCannotHappenException;
import org.harctoolbox.irp.IrpDatabase;
import org.harctoolbox.irp.IrpParseException;
import org.xml.sax.SAXException;

public final class FlipperScrap extends Girrable {

    private static final Logger logger = Logger.getLogger(FlipperScrap.class.getName());

    /**
     * There is to my knowledge not an official character set for Flipper;
     * This is "be liberal in what you accept".
     */
    public static final String FLIPPER_CHARSET = "WINDOWS-1252"; // ?
    //public static final String FLIPPER_BASE  = "https://raw.githubusercontent.com/probonopd/flipper/master/codes/";
    // produces error message: "Package size exceeded the configured limit of 50 MB"
    //public static final String FLIPPER_BASE    = "https://cdn.jsdelivr.net/gh/Lucaslhm/Flipper-IRDB@main/";
    public static final String FLIPPER_BASE    = "https://raw.githubusercontent.com/Lucaslhm/Flipper-IRDB/refs/heads/main/";
    public static final String FLIPPER_URL     = "https://github.com/Lucaslhm/Flipper-IRDB";
    public static final URI FLIPPER_BASE_URI   = URI.create(FLIPPER_BASE);
    private static final String FLIPPER_NAME   = "flipper";
    private static final IrpDatabase irpDatabase;
    private static final String FLIPPER_REMOTE_ENDING = ".ir";
    private static final int FLIPPER_REMOTE_ENDING_LENGTH = FLIPPER_REMOTE_ENDING.length();

    static {
        // Unfortunately, this forces IrpProtocol.xml to be parsed twice :-(
        try {
            irpDatabase = new IrpDatabase((String) null);
            Command.setIrpDatabase(irpDatabase);
        } catch (IOException | IrpParseException | SAXException ex) {
            throw new ThisCannotHappenException(ex);
        }
    }

    public static RemoteDatabase scrap(File baseDir) throws IOException, SAXException {
        FlipperScrap flipper = new FlipperScrap();
        return flipper.scrapSort(baseDir);
    }

    public static Remote parse(File file, String manufacturer, String deviceClass) throws IOException, ParseException, GirrException {
        if (!(file.isFile() && file.canRead())) {
            logger.log(Level.WARNING, "{0} is not a regular and readable file, ignored.", new Object[]{file});
            return null;
        }
        String remoteName = file.getName();
        if (remoteName.endsWith(FLIPPER_REMOTE_ENDING)) {
            remoteName = remoteName.substring(0, remoteName.length() - FLIPPER_REMOTE_ENDING_LENGTH);
        }
        else {
            logger.log(Level.WARNING, "File {0} does not end with \"" + FLIPPER_REMOTE_ENDING + "\", ignored", file.getCanonicalPath());
            return null;
        }
        //System.err.println("$$$$$$$$$$$$$ " + file.getCanonicalPath());
        return parse(new InputStreamReader(new FileInputStream(file), FLIPPER_CHARSET), manufacturer, deviceClass, remoteName, file.getPath());
    }

    public static Remote parse(RemoteLink remoteLink, String manufacturer, String deviceClass, String remoteName) throws IOException, ParseException, GirrException {
        if (remoteLink.getKind() != ScrapKind.flipper)
            return null;

        try (InputStream inputStream = remoteLink.getUrl().openStream(); InputStreamReader inputStreamReader = new InputStreamReader(inputStream, FLIPPER_CHARSET)) {
            return parse(inputStreamReader, manufacturer, deviceClass, remoteName, remoteLink.getUrl().toString());
        }
    }
    
    public static Remote parse(URL url, String manufacturer, String deviceClass, String remoteName) throws IOException, ParseException, GirrException {
        try (InputStream inputStream = url.openStream(); InputStreamReader inputStreamReader = new InputStreamReader(inputStream, FLIPPER_CHARSET)) {
            return parse(inputStreamReader, manufacturer, deviceClass, remoteName, url.toString());
        }
    }

    public static Remote parse(Reader reader, String manufacturer, String deviceClass, String remoteName, String source) throws IOException, ParseException, GirrException {
        CommandSet cmdSet = FlipperParser.parse(reader);
        Remote.MetaData metadata = new Remote.MetaData(remoteName, null, manufacturer, null, deviceClass, null);
        Collection<CommandSet> commandSets = new ArrayList<>(4);
        commandSets.add(cmdSet);
        return new Remote(metadata, source, null /* comment */, null /* notes */, commandSets, null /* applicationParameters */);
    }

    FlipperScrap(RemoteDatabase remoteDatabase) {
        super(remoteDatabase);
    }

    FlipperScrap() {
        super();
    }

    @Override
    public String getName() {
        return FLIPPER_NAME;
    }

    @Override
    public void add(File dir) throws IOException {
        remoteDatabase.addKind(ScrapKind.flipper);
        addDeviceTypesDirectory(FLIPPER_BASE_URI, dir);
    }

    private void addDeviceTypesDirectory(URI uriBase, File baseDir) throws IOException {
        assertReadableDirectory(baseDir);

        String[] deviceTypes = baseDir.list((File dir, String name) -> Character.isAlphabetic(name.charAt(0)));
        for (String devType : deviceTypes) {
            addDeviceType(uriBase, baseDir, devType, new File(baseDir, devType));
        }
    }

    private void addDeviceType(URI uriBase, File baseDir, String deviceType, File dir) throws IOException {
        if (!isReadableDirectory(dir)) {
            logger.log(Level.WARNING, "File {0} is not a readable directory, skipping", dir.getCanonicalPath());
            return;
        }
        
        String[] manufacturers = dir.list();
        for (String manufacturer : manufacturers) {
            addManifacturer(uriBase, baseDir, manufacturer, deviceType, new File(dir, manufacturer));
        }
    }

    private void addManifacturer(URI uriBase, File baseDir, String manifacturer, String deviceType, File dir) throws IOException {
        if (!isReadableDirectory(dir)) {
            logger.log(Level.WARNING, "Non-directory: {0}, ignored.", dir);
            return;
        }
//        if (manifacturer.equals("Yarra"))
//            System.err.println("sdfsdsd");
        ManufacturerDeviceClasses manufacturerTypes = remoteDatabase.getOrCreate(manifacturer);
        DeviceClassRemotes devType = manufacturerTypes.getOrCreate(deviceType);
        String[] remotes = dir.list();
        for (String remoteFileName : remotes) {
            addRemote(uriBase, baseDir, devType, manifacturer, /*deviceType,*/ remoteFileName, new File(dir, remoteFileName), "");
//            remoteDatabase.removeIfEmpty(manufacturerTypes);    
        }
    }

    @SuppressWarnings({"BroadCatchBlock", "TooBroadCatch"})
    private void addRemote(URI uriBase, File baseDir, DeviceClassRemotes devices, String manifacturer, String remoteFileName, File file, String prefix) throws IOException {
        if (isReadableDirectory(file)) {
            // Can be junk file; non-fatal
            //logger.log(Level.WARNING, "File {0} is not a readable directory, ignored.", dir);
            //return;
            //}

            String[] array = file.list();
            for (String remoteName : array) {
                if (remoteName.endsWith("~")) {
                    continue;
                }

                File f = new File(file, remoteName);
                addRemote(uriBase, baseDir, devices, manifacturer, remoteFileName, f, prefix + f.getName() + "$");
            }
        } else {
            try {
                Remote remote = parse(file, manifacturer, devices.getName());
                if (remote != null) {
                    RemoteLink remoteLink = new RemoteLink(ScrapKind.flipper, remote, uriBase, baseDir, file);
                    devices.add(remoteLink);
                }
            } catch (IOException | ParseException | GirrException ex) {
                logger.log(Level.SEVERE, "Parse error in file {0}, ignored", file.toString());
            }
        }
    }

    @Override
    public Remote getRemote(InputStreamReader reader, String source, String xpath, String manufacturer, String deviceClass, String remoteName) throws IOException {
        try {
            return parse(reader, manufacturer, deviceClass, remoteName/*source*/, null);
        } catch (ParseException | GirrException ex) {
            throw new IOException(ex);
        }
    }
}
