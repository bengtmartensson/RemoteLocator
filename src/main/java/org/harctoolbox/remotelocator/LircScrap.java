package org.harctoolbox.remotelocator;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.harctoolbox.girr.Remote;
import org.harctoolbox.girr.RemoteSet;
import org.harctoolbox.jirc.ConfigFile;
import static org.harctoolbox.remotelocator.RemoteDatabase.UNKNOWN;
import org.xml.sax.SAXException;

public class LircScrap extends Girrable {
    private static final Logger logger = Logger.getLogger(GirrScrap.class.getName());

    public static final String LIRC_BASE    = "https://sourceforge.net/p/lirc-remotes/code/ci/master/tree/remotes/";
    public static final URI LIRC_BASE_URI   = parseURI(LIRC_BASE);

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
        add(devices, uri, baseDir, dir, manufacturerTypes.getName());
    }

    private void add(DeviceClassRemotes devices, URI uri, File baseDir, File dir, String manufacturer) {
        if (!isReadableDirectory(dir)) {
            // Can be junk file, non-fatal
            logger.log(Level.WARNING, "File {0} not a readable directory, ignored.", dir);
            return;
        }

        String[] array = dir.list();

        for (String remote : array) {
            File path = new File(dir, remote);
            String name = remote.endsWith(".lircd.conf") ? remote.substring(0, remote.length() - 11) : remote;
            Remote.MetaData metaData = new Remote.MetaData(name, null, manufacturer, null, UNKNOWN, null);
            Remote rem = new Remote(metaData, path.getPath(), null, null, null, null);
            RemoteLink remoteLink = new RemoteLink(RemoteKind.lirc, rem, uri, baseDir, path);//newRemoteLink(rem, uri, baseDir, dir); //uri, baseDir, dir, remote));
            devices.add(remoteLink);
        }
    }

    @Override
    public Remote getRemote(InputStreamReader reader, String source, String xpath, String manufacturer, String deviceClass) throws IOException {
        RemoteSet remoteSet = ConfigFile.parseConfig(reader, source, true, null, true);
        return remoteSet.iterator().hasNext() ? remoteSet.iterator().next() : null;
    }

    @Override
    String formatUrl(String url) {
        return String.format("%1$s?format=raw", url);
    }
}
