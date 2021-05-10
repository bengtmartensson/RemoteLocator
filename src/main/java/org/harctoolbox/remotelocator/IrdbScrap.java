package org.harctoolbox.remotelocator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.harctoolbox.girr.Command;
import org.harctoolbox.girr.GirrException;
import org.harctoolbox.girr.Remote;
import org.xml.sax.SAXException;

public final class IrdbScrap extends Girrable {

    private static final Logger logger = Logger.getLogger(IrdbScrap.class.getName());

    /**
     * There is to my knowledge not an official character set for IRDB;
     * This is "be liberal in what you accept".
     */
    public static final String IRDB_CHARSET = "WINDOWS-1252"; // ?
    //public static final String IRDB_BASE    = "https://raw.githubusercontent.com/probonopd/irdb/master/codes/";
    public static final String IRDB_BASE    = "https://cdn.jsdelivr.net/gh/probonopd/irdb@master/codes/";
//    public static final String IRDB_URL     = "https://github.com/probonopd/irdb";
    public static final URI IRDB_BASE_URI   = URI.create(IRDB_BASE);
    private static final String IRDB_NAME = "irdb";

    public static RemoteDatabase scrap(File baseDir) throws IOException, SAXException {
        IrdbScrap irdb = new IrdbScrap();
        return irdb.scrapSort(baseDir);
    }

    public static Remote parse(File file, String manufacturer, String deviceClass) throws IOException {
        return parse(new InputStreamReader(new FileInputStream(file), IRDB_CHARSET), manufacturer, deviceClass, file.getPath());
    }

    public static Remote parse(RemoteLink remoteLink, String manufacturer, String deviceClass) throws IOException {
        if (remoteLink.getKind() != ScrapKind.irdb)
            return null;

        try (InputStream inputStream = remoteLink.getUrl().openStream(); InputStreamReader inputStreamReader = new InputStreamReader(inputStream, IRDB_CHARSET)) {
            return parse(inputStreamReader, manufacturer, deviceClass, remoteLink.getUrl().toString());
        }
    }
    public static Remote parse(URL url, String manufacturer, String deviceClass) throws IOException {
        try (InputStream inputStream = url.openStream(); InputStreamReader inputStreamReader = new InputStreamReader(inputStream, IRDB_CHARSET)) {
            return parse(inputStreamReader, manufacturer, deviceClass, url.toString());
        }
    }

    public static Remote parse(Reader reader, String manufacturer, String deviceClass, String source) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(reader)) {
            bufferedReader.readLine(); // junk first line
            int lineno = 2;
            String remoteName = null;
            Map<String, Command> commands = new LinkedHashMap<>(32);
            while (true) {
                String line = bufferedReader.readLine();
                lineno++;
                if (line == null)
                    break;
                String[] array = line.split(",");
                if (array.length < 5) {
                    // Silly lines just ignored
                    logger.warning("Too short line in ");
                    continue;
                }
                String name = array[0];
                String protocol = array[1].toLowerCase(Locale.US);
                if (protocol.equals("nec")) // TODO: remove
                    protocol = "nec1";
                try {
                    long device = Long.parseLong(array[2]);
                    long subdevice = Long.parseLong(array[3]);
                    long function = Long.parseLong(array[4]);

                    if (remoteName == null)
                        remoteName = mkName(protocol, device, subdevice, function);

                    Map<String, Long> parameters = new HashMap<>(3);
                    parameters.put("D", device);
                    if (subdevice != -1L)
                        parameters.put("S", subdevice);
                    parameters.put("F", function);

                    Command command = new Command(name, null, protocol, parameters);
                    commands.put(name, command);
                } catch (NumberFormatException | GirrException | ArrayIndexOutOfBoundsException ex) {
                    logger.log(Level.WARNING, "Line {2} of {0}: {1}", new Object[]{source, ex.getLocalizedMessage(), lineno});
//                    ex.printStackTrace();
//                    System.exit(1);
                }
            }

            if (remoteName == null) {
                logger.log(Level.WARNING, "File {0} is effectively empty, ignored.", source);
                return null;
            }

            Remote.MetaData metadata = new Remote.MetaData(remoteName, null, manufacturer, null, deviceClass, null);
            return new Remote(metadata, source, null, null, commands, null, null, null);
        }
    }

    public static String mkName(String protocol, long device, long function) {
        return mkName(protocol, device, -1L, function);
    }

    public static String mkName(String protocol, long device, long subdevice, long function) {
        StringBuilder remoteName = new StringBuilder(64);
        remoteName.append("Protocol=").append(protocol);
        remoteName.append(",device=").append(device);
        if (subdevice != -1L)
            remoteName.append(",subdevice=").append(subdevice);
        return remoteName.toString();
    }

    IrdbScrap(RemoteDatabase remoteDatabase) {
        super(remoteDatabase);
    }

    IrdbScrap() {
        super();
    }

    @Override
    public String getName() {
        return IRDB_NAME;
    }

    @Override
    public void add(File dir) throws IOException {
        addManufacturerDirectory(IRDB_BASE_URI, dir, dir);
    }

    private void addManufacturerDirectory(URI uriBase, File baseDir, File file) throws IOException {
        assertReadableDirectory(file);

        String[] manufacturerArray = file.list();
        for (String manufacturer : manufacturerArray) {
            ManufacturerDeviceClasses manufacturerTypes = remoteDatabase.getOrCreate(manufacturer);
            addDevices(manufacturerTypes, uriBase, baseDir, new File(file, manufacturer), manufacturer);
        }
    }

    private void addDevices(ManufacturerDeviceClasses manufacturerTypes, URI uriBase, File baseDir, File dir, String manufacturer) throws IOException {
        if (!isReadableDirectory(dir)) {
            // Non-fatal; there may lie junk files around
            logger.log(Level.WARNING, "{0} is not a readable directory", dir);
            return;
        }

        String[] list = dir.list();
        if (list == null)
            // Something is wrong, so this is fatal
            throw new IOException(dir + " could not be read");

        for (String deviceClass : list) {
            DeviceClassRemotes devices = manufacturerTypes.getOrCreate(deviceClass);
            addRemotes(devices, uriBase, baseDir, new File(dir, deviceClass), manufacturer);
        }
    }

    private void addRemotes(DeviceClassRemotes devices, URI uriBase, File baseDir, File dir, String manufacturer) throws IOException {
        if (!isReadableDirectory(dir)) {
            // Can be junk file; non-fatal
            logger.log(Level.WARNING, "File {0} is not a readable directory, ignored.", dir);
            return;
        }

        String[] array = dir.list();
        for (String remoteName : array) {
            File file = new File(dir, remoteName);
            Remote remote = parse(file, manufacturer, devices.getName());
            if (remote != null) {
                RemoteLink remoteLink = new RemoteLink(ScrapKind.irdb, remote, uriBase, baseDir, file);
                devices.add(remoteLink);
            }
        }
    }

    @Override
    public Remote getRemote(InputStreamReader reader, String source, String xpath, String manufacturer, String deviceClass) throws IOException {
        return parse(reader, manufacturer, deviceClass, source);
    }
}
