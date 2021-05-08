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

public final class Irdb {
    private static final Logger logger = Logger.getLogger(Irdb.class.getName());

    /**
     * There is to my knowledge not an official character set for IRDB;
     * This is "be liberal in what you accept".
     */
    public static final String IRDB_CHARSET = "WINDOWS-1252"; // ?
    //public static final String IRDB_BASE    = "https://raw.githubusercontent.com/probonopd/irdb/master/codes/";
    public static final String IRDB_BASE    = "https://cdn.jsdelivr.net/gh/probonopd/irdb@master/codes/";
    public static final String IRDB_URL     = "https://github.com/probonopd/irdb";
    public static final URI IRDB_BASE_URI   = RemoteDatabase.parseURI(IRDB_BASE);

    public static String getName() {
        return "irdb";
    }

    public static Remote parse(RemoteLink remoteLink, String manufacturer, String deviceClass) throws IOException {
        if (remoteLink.getKind() != RemoteKind.irdb)
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

    public static Remote parse(File file, String manufacturer, String deviceClass) throws IOException {
        return parse(new InputStreamReader(new FileInputStream(file), IRDB_CHARSET), manufacturer, deviceClass, file.getPath());
    }

    public static Remote parse(Reader reader, String manufacturer, String deviceClass, String source) throws IOException {
        try (BufferedReader bufferedReader = new BufferedReader(reader)) {
            bufferedReader.readLine(); // junk first line
            int lineno = 2;
            StringBuilder remoteName = new StringBuilder(128);
            Map<String, Command> commands = new LinkedHashMap<>(32);
            while (true) {
                String line = bufferedReader.readLine();
                lineno++;
                if (line == null)
                    break;
                String[] array = line.split(",");
                if (array.length < 5) {
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

                    if (remoteName.length() == 0) {
                        remoteName.append("Protocol=").append(protocol);
                        remoteName.append(",device=").append(device);
                        if (subdevice != -1L)
                            remoteName.append(",subdevice=").append(subdevice);
                    }

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

            Remote.MetaData metadata = new Remote.MetaData(remoteName.toString().toLowerCase(Locale.US), null, manufacturer, null, deviceClass, null);
            return new Remote(metadata, null, null, commands, null);
        }
    }

    private Irdb() {
    }
}
