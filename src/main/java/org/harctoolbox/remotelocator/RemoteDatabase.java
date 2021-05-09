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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
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
import java.util.logging.Logger;
import javax.xml.validation.Schema;
import org.harctoolbox.girr.Named;
import org.harctoolbox.girr.Remote;
import static org.harctoolbox.girr.XmlStatic.CREATINGUSER_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlStatic.CREATIONDATE_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlStatic.TITLE_ATTRIBUTE_NAME;
import org.harctoolbox.ircore.ThisCannotHappenException;
import static org.harctoolbox.remotelocator.ManufacturerDeviceClasses.MANUFACTURER_ELEMENT_NAME;
import org.harctoolbox.xml.XmlUtils;
import static org.harctoolbox.xml.XmlUtils.DEFAULT_CHARSETNAME;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 */
public final class RemoteDatabase implements Iterable<ManufacturerDeviceClasses>, Serializable {

    private static final Logger logger = Logger.getLogger(RemoteDatabase.class.getName());

    public static final String UNKNOWN = "unknown";
    public static final String FILE_SCHEME_NAME = "file";
    public static final String REMOTEDATABASE_ELEMENT_NAME = "remotedatabase";
    public static final String FORMATVERSION_ATTRIBUTE_NAME = "formatVersion";
    public static final String FORMAT_VERSION = "0.1";
    private static final int INITIAL_CAPACITY = 64;

    public static final String dateFormatString = "yyyy-MM-dd_HH:mm:ss";

    static String mkKey(String string) {
        return (string == null || string.isEmpty()) ? UNKNOWN : string.toLowerCase(Locale.US);
    }

//    public static void main(String[] args) {
//        try {
//            RemoteDatabase remoteDatabase = new RemoteDatabase();
//
////            Jp1Master.add(remoteDatabase, new File(JP1_XML));
////            Girr.add(remoteDatabase, new File("../GirrLib/Girr"));
////            remoteDatabase.add(RemoteKind.girr, GIRRLIB_BASE_URI, new File("../GirrLib/Girr"), new File("../GirrLib/Girr"));
////            remoteDatabase.add(RemoteKind.irdb, IRDB_BASE_URI, new File("../irdb/codes"), new File("../irdb/codes"));
////            remoteDatabase.add(RemoteKind.lirc, LIRC_BASE_URI, new File("../../lirc/lirc-remotes/remotes"), new File("../../lirc/lirc-remotes/remotes"));

    private final Map<String, ManufacturerDeviceClasses> manufacturers;

    public RemoteDatabase() {
        manufacturers = new LinkedHashMap<>(INITIAL_CAPACITY);
    }

    public RemoteDatabase(File file) throws IOException, SAXException {
        this(XmlUtils.openXmlFile(file, (Schema) null, false, true));
    }

    public RemoteDatabase(Reader reader) throws IOException, SAXException {
        this(XmlUtils.openXmlReader(reader, (Schema) null, false, true));
    }

    public RemoteDatabase(Document document) {
        this(document.getDocumentElement());
    }

    public RemoteDatabase(Element root) {
        this();
        NodeList nodeList = root.getElementsByTagName(MANUFACTURER_ELEMENT_NAME);
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
        for (ManufacturerDeviceClasses manuf : list) {
            manuf.sort(comparator);
            add(manuf);
        }
    }

    public void sort() {
        sort(new Named.CompareNameCaseInsensitive());
    }

    public Document toDocument() {
        Document document = XmlUtils.newDocument(false);
        Element element = toElement(document);
        document.appendChild(element);
        return document;
    }

    public Element toElement(Document document) {
        return toElement(document, null, null, null);
    }

    public Element toElement(Document document, String title, String creatingUser, String createdDate) {
        Element element = document.createElement(REMOTEDATABASE_ELEMENT_NAME);
        element.setAttribute(FORMATVERSION_ATTRIBUTE_NAME, FORMAT_VERSION);
        if (title != null && !title.isEmpty())
            element.setAttribute(TITLE_ATTRIBUTE_NAME, title);
        String creator = (creatingUser != null) ? creatingUser : System.getProperty("user.name");
        element.setAttribute(CREATINGUSER_ATTRIBUTE_NAME, creator);
        String date = (createdDate != null) ? createdDate : (new SimpleDateFormat(dateFormatString)).format(new Date());
        element.setAttribute(CREATIONDATE_ATTRIBUTE_NAME, date);
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

//    public void addRecursive(URI uriBase, File baseDir, File file) throws IOException {
//        if (file.isDirectory()) {
//            String[] files = file.list();
//            if (files == null)
//                throw new IOException("Cannot read directory " + file);
//
//            for (String f : files)
//                addRecursive(uriBase, baseDir, new File(file, f));
//        } else if (file.isFile() /*&& ! ignoreByExtension(path)*/) {
//            try {
//                add(XmlUtils.openXmlFile(file), uriBase, baseDir, file);
//            } catch (IOException | SAXException | GirrException ex) {
//                logger.log(Level.WARNING, "Could not read file {0}; {1}", new Object[]{file.toString(), ex.getLocalizedMessage()});
//            }
//        } else
//            logger.log(Level.WARNING, "Unknown file {0}", file.toString());
//    }

//    public void add(RemoteSet remoteSet, URI baseUri, File baseDir, File file) {
//        for (Remote remote : remoteSet) {
//            String xpath = REMOTES_ELEMENT_NAME + "/" + REMOTE_ELEMENT_NAME + "[@" + NAME_ATTRIBUTE_NAME + "=\'" + remote.getName() + "\']";
//            add(RemoteKind.girr, remote, baseUri, baseDir, file, xpath);
//        }
//    }

//    public void add(RemoteKind kind, Remote remote, URI baseUri, File baseDir, File file, String xpath) {
//        ManufacturerDeviceClasses manufacturerTypes = getOrCreate(remote.getManufacturer());
//        manufacturerTypes.add(kind, remote, baseUri, baseDir, file, xpath);
//    }

//    private void add(Element element, URI baseUri, File baseDir, File file) throws GirrException {
//        switch (element.getTagName()) {
//            case REMOTES_ELEMENT_NAME:
//                RemoteSet remoteSet = new RemoteSet(element, file.toString());
//                add(remoteSet, baseUri, baseDir, file);
//                break;
//            case REMOTE_ELEMENT_NAME:
//                Remote remote = new Remote(element, file.toString());
//                add(RemoteKind.girr, remote, baseUri, baseDir, file, "/" + REMOTE_ELEMENT_NAME);
//                break;
//            default:
//                logger.log(Level.INFO, "File  {0} ignored, since its top level element is {1}", new Object[]{file, element.getTagName()});
//        }
//    }



//    void add(RemoteKind kind, File file) throws IOException {
//        add(kind, null, null, file);
//    }

//    void add(RemoteKind kind, URI uriBase, File baseDir, File file) throws IOException {
//        if (kind.recurse()) {
//            addRecursive(uriBase, baseDir, file);
//        } else {
//            if (!(file.isDirectory() && file.canRead()))
//                throw new IOException(file + " is not a readable directory");
//
//            String[] manufacturerArray = file.list();
//            for (String manufacturer : manufacturerArray) {
//                ManufacturerDeviceClasses manufacturerTypes = getOrCreate(manufacturer);
//                manufacturerTypes.add(kind, uriBase, baseDir, new File(file, manufacturer));
//            }
//        }
//    }

    private void add(ManufacturerDeviceClasses manifacturer) {
        manufacturers.put(mkKey(manifacturer.getName()), manifacturer);
    }

    void put(String manufacturer, String deviceClass, RemoteLink remoteLink) {
        ManufacturerDeviceClasses manufacturerTypes = getOrCreate(manufacturer);
        manufacturerTypes.put(deviceClass, remoteLink);
    }

    public RemoteLink get(String manufacturer, String deviceClass, String remoteName) throws NotFoundException {
        ManufacturerDeviceClasses manufact = manufacturers.get(manufacturer.toLowerCase(Locale.US));
        if (manufact == null)
            throw new NotFoundException("Manufacturer " + manufacturer + " unknown.");
        return manufact.get(deviceClass, remoteName);
    }

    public Remote getRemote(String manufacturer, String deviceClass, String remoteName) throws NotFoundException, IOException {
        RemoteLink remoteLink = get(manufacturer, deviceClass, remoteName);
        return remoteLink.getRemote(manufacturer, deviceClass);
    }

    public List<String> getManufacturers() {
        List<String> result = new ArrayList<>(manufacturers.size());
        for (ManufacturerDeviceClasses m : this)
            result.add(m.getName());
        return result;
    }

    public List<String> getDeviceTypes(String manufacturer) {
        ManufacturerDeviceClasses m = manufacturers.get(mkKey(manufacturer));
        return m.getDeviceClasses();
    }

    public List<String> getRemotes(String manufacturer, String deviceType) {
        ManufacturerDeviceClasses m = manufacturers.get(mkKey(manufacturer));
        DeviceClassRemotes d = m.getDeviceClass(deviceType);
        return d.getRemotes();
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
}
