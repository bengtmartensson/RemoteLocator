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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;
import java.net.URI;
import java.net.URL;
import java.text.ParseException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import org.harctoolbox.girr.Command;
import org.harctoolbox.girr.CommandSet;
import org.harctoolbox.girr.GirrException;
import org.harctoolbox.girr.Remote;
import org.harctoolbox.ircore.IrCoreException;
import org.harctoolbox.irp.IrpException;
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
    //public static final String IRDB_URL     = "https://github.com/probonopd/irdb";
    public static final URI IRDB_BASE_URI   = URI.create(IRDB_BASE);
    private static final String IRDB_NAME = "irdb";
    private static final String SILLY_IRDB_HEADER = "functionname,protocol,device,subdevice,function";
    private static final String COMMA = ",";
    private static final Pattern WHITESPACE_QUOTE_STUFF = Pattern.compile("\\s+\".*");
    private static final String QUOTE = "\"";
    private static final char SPACECHAR = ' ';
    private static final char QUOTECHAR = '"';


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
            int lineno = 1;
            String remoteName = null;
            Map<String, Command> commands = new LinkedHashMap<>(32);
            while (true) {
                String line = bufferedReader.readLine();
                lineno++;
                if (line == null)
                    break;
                try {
                    List<String> list = splitCSV(line, COMMA, lineno);
                    if (list.size() != 5) {
                        // Silly lines just ignored
                        logger.log(Level.WARNING, "Wrong number of fields in line {0} in file {1}.", new Object[]{lineno, source});
                        continue;
                    }

                    String name = list.get(0);
                    String protocol = list.get(1);
                    if (protocol.equalsIgnoreCase("nec")) // TODO: remove
                        protocol = "NEC1";

                    long device = Long.parseLong(list.get(2));
                    long subdevice = Long.parseLong(list.get(3));
                    long function = Long.parseLong(list.get(4));

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
                } catch (ParseException ex) {
                    logger.log(Level.WARNING, "Unbalaned quotes in line {0}", lineno);
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

    /**
     * Splits a line of CSV into fields, taking quoting into account.
     * Embedded double quotation marks are not considered (yet?).
     * @param input Line to be split
     * @param separator String (typically a comma ",") to use as separator.
     * @param lineNumber Line number, only used for constructing error messages from un-balanced quotes.
     * @return
     * @throws ParseException in the presence of un-balanced quotes.
     */
    public static LinkedList<String> splitCSV(String input, String separator, int lineNumber) throws ParseException {
        // For performance reasons, use regexp only "occasionally"
        if (input.charAt(0) == SPACECHAR)
            if (WHITESPACE_QUOTE_STUFF.matcher(input).matches())
                return splitCSV(input.trim(), separator, lineNumber); // Strictly speaking, this is wrong, since trailing spaces may be significant...

        String first;
        int index;
        LinkedList<String> result; // need addFirst, so cannot use ArrayList
        if (input.startsWith(QUOTE)) {
            int closingQuoteIndex = input.indexOf(QUOTECHAR, 1);
            if (closingQuoteIndex == -1)
                throw new ParseException(input, lineNumber);
            first = input.substring(1, closingQuoteIndex);
            index = input.indexOf(separator, closingQuoteIndex);
            if (index == -1) {
                result = new LinkedList<>();
            } else {
                String rest = input.substring(index + separator.length());
                result = splitCSV(rest, separator, lineNumber);
            }
        } else {
            index = input.indexOf(separator);
            if (index == -1) {
                result = new LinkedList<>();
                first = input;
            } else {
                String rest = input.substring(index + separator.length());
                result = splitCSV(rest, separator, lineNumber);
                first = input.substring(0, index);
            }
        }

        result.addFirst(first);
        return result;
    }

    public static List<String> splitCSV(String input, String separator) throws ParseException {
        return splitCSV(input, separator, 0);
    }

    public static List<String> splitCSV(String input) throws ParseException {
        return splitCSV(input, COMMA);
    }

    public static String possiblyQuote(String str) {
        return str.contains(COMMA) ? (QUOTE + str + QUOTE) : str;
    }

    static void print(PrintStream out, Remote remote) throws IrpException, IrCoreException {
        out.println(SILLY_IRDB_HEADER);
        for (CommandSet cs : remote) {
            for (Command c : cs) {
                Map<String, Long> params = c.getParameters();
                out.printf("%1$s,%2$s,%3$d,%4$d,%5$d", possiblyQuote(c.getName()), possiblyQuote(c.getProtocolName()), getNumber(params, "D"), getNumber(params, "S"), getNumber(params, "F"));
                out.println();
            }
        }
    }

    static long getNumber(Map<String, Long>map, String name) {
        Long value = map.get(name);
        return value != null ? value : -1L;
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
