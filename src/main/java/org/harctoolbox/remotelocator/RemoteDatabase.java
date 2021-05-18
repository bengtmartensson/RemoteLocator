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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_INSTANCE_NS_URI;
import static javax.xml.XMLConstants.XMLNS_ATTRIBUTE;
import javax.xml.validation.Schema;
import org.harctoolbox.girr.Named;
import org.harctoolbox.girr.Remote;
import static org.harctoolbox.girr.XmlStatic.CREATINGUSER_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlStatic.CREATIONDATE_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlStatic.TITLE_ATTRIBUTE_NAME;
import org.harctoolbox.ircore.IrCoreUtils;
import org.harctoolbox.ircore.ThisCannotHappenException;
import static org.harctoolbox.irp.IrpUtils.EXIT_SUCCESS;
import static org.harctoolbox.irp.IrpUtils.EXIT_USAGE_ERROR;
import static org.harctoolbox.remotelocator.ManufacturerDeviceClasses.MANUFACTURER_ELEMENT_NAME;
import org.harctoolbox.xml.XmlUtils;
import static org.harctoolbox.xml.XmlUtils.DEFAULT_CHARSETNAME;
import static org.harctoolbox.xml.XmlUtils.SCHEMA_LOCATION_ATTRIBUTE_NAME;
import static org.harctoolbox.xml.XmlUtils.W3C_SCHEMA_NAMESPACE_ATTRIBUTE_NAME;
import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 */
public final class RemoteDatabase implements Iterable<ManufacturerDeviceClasses>, Serializable {

    private static final Logger logger = Logger.getLogger(RemoteDatabase.class.getName());

    private static final String DEFAULT_TITLE = "Database of downloadable remotes";
    public static final String UNKNOWN = "unknown";
    public static final String FILE_SCHEME_NAME = "file";
    public static final String REMOTEDATABASE_ELEMENT_NAME = "remotedatabase";
    public static final String FORMATVERSION_ATTRIBUTE_NAME = "formatVersion";
    public static final String FORMATVERSION = "0.1";
    private static final int INITIAL_CAPACITY = 64;

    /**
     * Namespace URI
     */
    public static final String REMOTELOCATOR_NAMESPACE       = "http://www.harctoolbox.org/RemoteLocator";

    /**
     * Homepage URL.
     */
    public static final String REMOTELOCATOR_HOMEPAGE        = "https://github.com/bengtmartensson/RemoteLocator";

    /**
     * URL for schema file supporting name spaces.
     */
    public static final String REMOTELOCATOR_SCHEMA_LOCATION_URI = "http://www.harctoolbox.org/schemas/remotelocator-"  + FORMATVERSION + ".xsd";

    /**
     * Prefix for RemoteLocator.
     */
    public static final String REMOTELOCATOR_PREFIX = "rl";

    /**
     * Comment string pointing to RemoteLocator docu.
     */
    static final String REMOTELOCATOR_COMMENT = "This file is in the RemoteLocator format, see " + REMOTELOCATOR_HOMEPAGE;

            static final String DATE_FORMAT_STRING = "yyyy-MM-dd_HH:mm:ss";
            static final String APP_NAME = "RemoteLocator";
            static final String VERSION = "0.2.0";
            static final String VERSION_STRING = APP_NAME + " version " + VERSION;

    private static RemoteDatabase remoteDatabase;
    private static JCommander argumentParser;
    private static final CommandLineArgs commandLineArgs = new CommandLineArgs();
    private static PrintStream out;

    static String mkKey(String string) {
        return (string == null || string.isEmpty()) ? UNKNOWN : string.toLowerCase(Locale.US);
    }

    private static void usage(int exitcode) {
        StringBuilder str = new StringBuilder(256);
        argumentParser.usage();

        (exitcode == EXIT_SUCCESS ? System.out : System.err).println(str);
        doExit(exitcode); // placifying FindBugs...
    }

    private static void die(String message, int exitCode) {
        System.err.println(message);
        System.exit(exitCode);
    }

    private static void doExit(int exitcode) {
        System.exit(exitcode);
    }

    /**
     * @param args the command line arguments.
     */
    public static void main(String[] args) {
        argumentParser = new JCommander(commandLineArgs);
        argumentParser.setProgramName(APP_NAME);
        argumentParser.setAllowAbbreviatedOptions(true);
        argumentParser.setCaseSensitiveOptions(false);

        try {
            argumentParser.parse(args);
        } catch (ParameterException ex) {
            System.err.println(ex.getMessage());
            usage(EXIT_USAGE_ERROR);
        }

        if (commandLineArgs.helpRequested)
            usage(EXIT_SUCCESS);

        if (commandLineArgs.versionRequested) {
            System.out.println(VERSION_STRING);
            System.out.println("JVM: " + System.getProperty("java.vendor") + " " + System.getProperty("java.version")
                    + " " + System.getProperty("os.name") + "-" + System.getProperty("os.arch"));
            //System.out.println();
            //System.out.println(Version.licenseString);
            System.exit(EXIT_SUCCESS);
        }

        try {
            remoteDatabase = new RemoteDatabase();
            readScrapers();
            if (remoteDatabase.isEmpty())
                die("No database content", EXIT_USAGE_ERROR);

            if (commandLineArgs.sort)
                remoteDatabase.sort();

            out = IrCoreUtils.getPrintStream(commandLineArgs.output);

            remoteDatabase.print(out);
            System.err.println("Configuration file " + out.toString() + " written.");
        } catch (IOException ex) {
            die(ex.getLocalizedMessage(), EXIT_USAGE_ERROR);
        } catch (SAXException ex) {
            Logger.getLogger(RemoteDatabase.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static void readScrapers() throws IOException, SAXException {


        if (commandLineArgs.girrDir != null)
            new GirrScrap(remoteDatabase).add(new File(commandLineArgs.girrDir));

        if (commandLineArgs.irdbDir != null)
            new IrdbScrap(remoteDatabase).add(new File(commandLineArgs.irdbDir));

        if (commandLineArgs.lircDir != null)
            new LircScrap(remoteDatabase).add(new File(commandLineArgs.lircDir));

        if (commandLineArgs.jp1File != null)
            new Jp1Scrap(remoteDatabase).add(new File(commandLineArgs.jp1File));
    }

    private final Map<String, ManufacturerDeviceClasses> manufacturers;

    public RemoteDatabase() {
        manufacturers = new LinkedHashMap<>(INITIAL_CAPACITY);
    }

    public RemoteDatabase(String thing) throws IOException, SAXException, FormatVersionMismatchException {
        this(XmlUtils.openXmlUrlOrFile(thing, null, true, false));
    }

    public RemoteDatabase(File file) throws IOException, SAXException, FormatVersionMismatchException {
        this(XmlUtils.openXmlFile(file, (Schema) null, true, false));
    }

    public RemoteDatabase(URL url) throws IOException, SAXException, FormatVersionMismatchException {
        this(XmlUtils.openXmlUrl(url, null, true, false));
    }

    public RemoteDatabase(Reader reader) throws IOException, SAXException, FormatVersionMismatchException {
        this(XmlUtils.openXmlReader(reader, (Schema) null, true, false));
    }

    public RemoteDatabase(Document document) throws FormatVersionMismatchException {
        this(document.getDocumentElement());
    }

    public RemoteDatabase(Element root) throws FormatVersionMismatchException {
        this();
        String actualVersion = root.getAttribute(FORMATVERSION_ATTRIBUTE_NAME);
        if (!actualVersion.equals(FORMATVERSION))
            throw new FormatVersionMismatchException(actualVersion);

        NodeList nodeList = root.getElementsByTagNameNS(REMOTELOCATOR_NAMESPACE, MANUFACTURER_ELEMENT_NAME);
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element manufacturerElement = (Element) nodeList.item(i);
            ManufacturerDeviceClasses manufacturer = new ManufacturerDeviceClasses(manufacturerElement);
            add(manufacturer);
        }
    }

    public void sort(Comparator<? super Named> comparator) {
        List<ManufacturerDeviceClasses> list = new ArrayList<>(manufacturers.values());
        Collections.sort(list, comparator);
        manufacturers.clear();
        list.stream().map(manuf -> {
            manuf.sort(comparator);
            return manuf;
        }).forEachOrdered(manuf -> {
            add(manuf);
        });
    }

    public void sort() {
        sort(new Named.CompareNameCaseInsensitive());
    }

    public Document toDocument() {
        Document document = XmlUtils.newDocument(true);
//        if (stylesheetType != null && stylesheetUrl != null && !stylesheetUrl.isEmpty()) {
//            ProcessingInstruction pi = document.createProcessingInstruction("xml-stylesheet",
//                    "type=\"text/" + stylesheetType + "\" href=\"" + stylesheetUrl + "\"");
//            document.appendChild(pi);
//        }

        // At least in some Java versions (https://bugs.openjdk.java.net/browse/JDK-7150637)
        // there is no line feed before and after the comment.
        // This is technically correct, but looks awful to the human reader.
        // AFAIK, there is no clean way to fix this.
        // Possibly works with some Java versions?
        Comment comment = document.createComment(REMOTELOCATOR_COMMENT);
        document.appendChild(comment);
        Element element = toElement(document);
        document.appendChild(element);
        return document;
    }

    public Element toElement(Document document) {
        return toElement(document, null, null, null);
    }

    @SuppressWarnings("AssignmentToMethodParameter")
    public Element toElement(Document document, String title, String creatingUser, String createdDate) {
        Element element = document.createElementNS(REMOTELOCATOR_NAMESPACE, REMOTELOCATOR_PREFIX + ":" + REMOTEDATABASE_ELEMENT_NAME);
        element.setAttribute(FORMATVERSION_ATTRIBUTE_NAME, FORMATVERSION);
        if (title == null)
            title = DEFAULT_TITLE;
        if (!title.isEmpty())
            element.setAttribute(TITLE_ATTRIBUTE_NAME, title);
        String creator = (creatingUser != null) ? creatingUser : System.getProperty("user.name");
        element.setAttribute(CREATINGUSER_ATTRIBUTE_NAME, creator);
        String date = (createdDate != null) ? createdDate : (new SimpleDateFormat(DATE_FORMAT_STRING)).format(new Date());
        element.setAttribute(CREATIONDATE_ATTRIBUTE_NAME, date);
        element.setAttribute(W3C_SCHEMA_NAMESPACE_ATTRIBUTE_NAME, W3C_XML_SCHEMA_INSTANCE_NS_URI);
        element.setAttribute(XMLNS_ATTRIBUTE + ":" + REMOTELOCATOR_PREFIX, REMOTELOCATOR_NAMESPACE);
        element.setAttribute(SCHEMA_LOCATION_ATTRIBUTE_NAME, REMOTELOCATOR_NAMESPACE + " " + REMOTELOCATOR_SCHEMA_LOCATION_URI);

        for (ManufacturerDeviceClasses manufacturer : this)
            element.appendChild(manufacturer.toElement(document));

        return element;
    }

    /**
     * Convenience function that generates a DOM and dumps it onto the argument.
     * @param ostr
     */
    public void print(OutputStream ostr) {
        Document doc = toDocument();
        try {
            XmlUtils.printDOM(ostr, doc, DEFAULT_CHARSETNAME, null);
        } catch (UnsupportedEncodingException ex) {
            throw new ThisCannotHappenException(ex);
        }
    }

    /**
     * Convenience function that generates a DOM and dumps it onto the argument.
     * @param file
     * @throws java.io.FileNotFoundException
     */
    public void print(File file) throws IOException {
        try (OutputStream os = new FileOutputStream(file)) {
            print(os);
        }
    }

    /**
     * Convenience function that generates a DOM and dumps it onto the argument.
     * @param file
     * @throws java.io.FileNotFoundException
     */
    public void print(String file) throws IOException {
        print(new File(file));
    }

    @Override
    public Iterator<ManufacturerDeviceClasses> iterator() {
        return manufacturers.values().iterator();
    }

    private void add(ManufacturerDeviceClasses manifacturer) {
        manufacturers.put(mkKey(manifacturer.getName()), manifacturer);
    }

    void put(String manufacturer, String deviceClass, RemoteLink remoteLink) {
        ManufacturerDeviceClasses manufacturerTypes = getOrCreate(manufacturer);
        manufacturerTypes.put(deviceClass, remoteLink);
    }

    public RemoteLink get(String manufacturer, String deviceClass, String remoteName) throws NotFoundException {
        ManufacturerDeviceClasses manufact = getManufacturerDeviceClass(manufacturer);
        return manufact.get(deviceClass, remoteName);
    }

    public Remote getRemote(String manufacturer, String deviceClass, String remoteName) throws NotFoundException, IOException, Girrable.NotGirrableException {
        RemoteLink remoteLink = getRemoteLink(manufacturer, deviceClass, remoteName);
        return remoteLink.getRemote(manufacturer, deviceClass);
    }

    public RemoteLink getRemoteLink(String manufacturer, String deviceClass, String remoteName) throws NotFoundException, IOException, Girrable.NotGirrableException {
        return get(manufacturer, deviceClass, remoteName);
    }

    public List<String> getManufacturers() {
        return getManufacturers(null);
    }

    public List<String> getManufacturers(ScrapKind kind) {
        List<String> result = new ArrayList<>(manufacturers.size());
        for (ManufacturerDeviceClasses m : this)
            if (m.hasKind(kind))
                result.add(m.getName());
        return result;
    }

    public ManufacturerDeviceClasses getManufacturerDeviceClass(String manufacturer) throws NotFoundException {
        ManufacturerDeviceClasses m = manufacturers.get(mkKey(manufacturer));
        if (m == null)
            throw new NotFoundException("Manufacturer \""  + manufacturer + "\" not found in the data base.");
        return m;
    }

    public List<String> getDeviceTypes(String manufacturer) throws NotFoundException {
        return getDeviceTypes(null, manufacturer);
    }

    public List<String> getDeviceTypes(ScrapKind kind, String manufacturer) throws NotFoundException {
        ManufacturerDeviceClasses m = getManufacturerDeviceClass(manufacturer);
        List<String> list = m.getDeviceClasses(kind);
        return list;
    }

    public List<String> getRemotes(String manufacturer, String deviceType) throws NotFoundException {
        return getRemotes(null, manufacturer, deviceType);
    }

    public List<String> getRemotes(ScrapKind kind, String manufacturer, String deviceType) throws NotFoundException {
        ManufacturerDeviceClasses m = getManufacturerDeviceClass(manufacturer);
        DeviceClassRemotes d = m.getDeviceClass(deviceType);
        return d.getRemotes(kind);
    }

    ManufacturerDeviceClasses getOrCreate(String manufacturer) {
        String key = mkKey(manufacturer);

        ManufacturerDeviceClasses mt = manufacturers.get(key);
        if (mt == null) {
            mt = new ManufacturerDeviceClasses((manufacturer == null || manufacturer.isEmpty()) ? UNKNOWN : manufacturer);
            manufacturers.put(key, mt);
        }
        return mt;
    }

    public URL getUrl(String manufacturer, String deviceClass, String remoteName) throws NotFoundException {
        RemoteLink remoteLink = get(manufacturer, deviceClass, remoteName);
        return remoteLink.getUrl();
    }

    public boolean isEmpty() {
        return manufacturers.isEmpty();
    }

    private final static class CommandLineArgs {

        @Parameter(names = {"-g", "--girrdir"}, description = "Pathname of directory (recursively) containing Girr files.")
        public String girrDir = null;

        @Parameter(names = {"-h", "--help", "-?"}, description = "Display help message.")
        private boolean helpRequested = false;

        @Parameter(names = {"-i", "--irdbdir"}, description = "Pathname of directory containing IRDB files in CSV format.")
        public String irdbDir = null;

        @Parameter(names = {"-l", "--lircdirs"}, description = "Pathname of directory containing Lirc files.")
        public String lircDir = null;

        @Parameter(names = {"-j", "--jp1file"}, description = "Filename of XML export of JP1 master file.")
        public String jp1File = null;

        @Parameter(names = {"-o", "--output"}, description = "File name to write to, \"-\" for stdout.")
        private String output = "-";

        @Parameter(names = {"-s", "--sort"}, description = "Sort the configuration file before writing.")
        public boolean sort = false;

        @Parameter(names = {"-v", "--version"}, description = "Display version information.")
        private boolean versionRequested;
    }

    public static class FormatVersionMismatchException extends Exception {

        FormatVersionMismatchException(String actualVersion) {
            super("Format version mismatch, expeccted = " + FORMATVERSION + ", actual = " + actualVersion);
        }
    }
}
