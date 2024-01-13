/*
Copyright (C) 2021 Bengt Martensson.

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
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.harctoolbox.girr.Remote;
import org.harctoolbox.girr.RemoteSet;
import static org.harctoolbox.ircore.IrCoreUtils.EXTENDED_LATIN1_NAME;
import org.harctoolbox.jirc.ConfigFile;
import static org.harctoolbox.remotelocator.RemoteDatabase.UNKNOWN;
import org.xml.sax.SAXException;

public class LircScrap extends Girrable {
    private static final Logger logger = Logger.getLogger(GirrScrap.class.getName());

    public static final String LIRC_BASE    = "https://sourceforge.net/p/lirc-remotes/code/ci/master/tree/remotes/";
    public static final URI LIRC_BASE_URI   = URI.create(LIRC_BASE);

    public static RemoteDatabase scrap(File dir) throws IOException, SAXException {
        LircScrap lirc = new LircScrap();
        return lirc.scrapSort(dir);
    }

    private LircScrap() {
        super();
    }

    LircScrap(RemoteDatabase remoteDatabase) {
        super(remoteDatabase);
    }

    @Override
    public String getName() {
        return "lirc";
    }

    @Override
    public void add(File dir) throws IOException {
        add(LIRC_BASE_URI, dir, dir);
    }

    private void add(URI uriBase, File baseDir, File file) throws IOException {
        assertReadableDirectory(file);

        String[] manufacturerArray = file.list();
        for (String manufacturer : manufacturerArray) {
            ManufacturerDeviceClasses manufacturerTypes = remoteDatabase.getOrCreate(manufacturer);
            add(manufacturerTypes, uriBase, baseDir, new File(file, manufacturer));
        }
    }

    private void add(ManufacturerDeviceClasses manufacturerTypes, URI uri, File baseDir, File dir) throws IOException {
        if (!isReadableDirectory(dir)) {
            // Non-fatal; there may lie junk files around
            logger.log(Level.WARNING, "{0} is not a readable directory", dir);
            return;
        }

        DeviceClassRemotes devices = manufacturerTypes.getOrCreate(UNKNOWN);
        add(devices, uri, baseDir, dir);
    }

    private void add(DeviceClassRemotes devices, URI uri, File baseDir, File dir) {
        if (!isReadableDirectory(dir)) {
            // Can be junk file, non-fatal
            logger.log(Level.WARNING, "File {0} not a readable directory, ignored.", dir);
            return;
        }

        String[] array = dir.list();
        for (String filename : array) {
            File path = new File(dir, filename);
            try {
                RemoteSet remoteSet = ConfigFile.parseConfig(path, EXTENDED_LATIN1_NAME, true, null);
                for (Remote remote : remoteSet) {
                    RemoteLink remoteLink = new RemoteLink(ScrapKind.lirc, remote, uri, baseDir, path);
                    devices.add(remoteLink);
                }
            } catch (IOException ex) {
                logger.log(Level.WARNING, ex.getLocalizedMessage());
            }
        }
    }

    public Remote getRemote(InputStreamReader reader, String source, String xpath, String manufacturer, String deviceClass) throws IOException {
        RemoteSet remoteSet = ConfigFile.parseConfig(reader, source, true, null);
        Remote lircRemote = remoteSet.iterator().next();
        if (lircRemote == null)
            return null;
        Remote.MetaData metaData = new Remote.MetaData(lircRemote.getName(), null, manufacturer, null, deviceClass, null);
        Remote remote = new Remote(metaData, source, lircRemote.getComment(), null, lircRemote.getCommandSets().values(), lircRemote.getApplicationParameters());
        return remote;
    }

    @Override
    String formatUrl(String url) {
        return String.format("%1$s?format=raw", url);
    }

    @Override
    public Remote getRemote(InputStreamReader reader, String source, String xpath, String manufacturer, String deviceClass, String remoteName) throws IOException {
        return getRemote(reader, source, xpath, manufacturer, deviceClass);
    }
}
