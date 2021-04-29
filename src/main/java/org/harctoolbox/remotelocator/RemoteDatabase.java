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
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.harctoolbox.girr.GirrException;
import org.harctoolbox.girr.Named;
import org.harctoolbox.girr.Remote;
import org.harctoolbox.girr.RemoteSet;
import static org.harctoolbox.girr.XmlStatic.NAME_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlStatic.REMOTES_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlStatic.REMOTE_ELEMENT_NAME;
import org.harctoolbox.ircore.ThisCannotHappenException;
import org.harctoolbox.xml.XmlExport;
import org.harctoolbox.xml.XmlUtils;
import static org.harctoolbox.xml.XmlUtils.DEFAULT_CHARSETNAME;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

/**
 *
 */
public final class RemoteDatabase implements XmlExport, Iterable<ManufacturerDeviceClasses>, Serializable {

    private static final Logger logger = Logger.getLogger(RemoteDatabase.class.getName());
    public static final String UNKNOWN = "unknown";
    public static final String FILE_SCHEME_NAME = "file";
    public static final String REMOTEDATABASE_ELEMENT_NAME = "remotedatabase";
    public static final String FORMATVERSION_ATTRIBUTE_NAME = "formatVersion";
    public static final String FORMAT_VERSION = "0.1";
    private static Path baseDir = Paths.get(System.getProperty("user.dir"));
    //private static URI base = workingDirectoryAsURI();
    private static final int INITIAL_CAPACITY = 64;
    private static final String BASEDIR_ATTRIBUTE_NAME = "baseDir";

//    private static URI workingDirectoryAsURI() {
//        try {
//            return new URI(FILE_SCHEME_NAME, new File(System.getProperty("user.dir")).getCanonicalPath(), null);
//        } catch (IOException | URISyntaxException ex) {
//            return null;
//        }
//    }

    static String mkKey(String manufacturer) {
        return (manufacturer == null || manufacturer.isEmpty()) ? UNKNOWN : manufacturer.toLowerCase(Locale.US);
    }

//    static URL relativize(URL url) {
//        try {
//            URI u = url.toURI();
//            //URI u = new URI(FILE_SCHEME_NAME, url, null);
//            return base.relativize(u).toURL();
//        } catch (URISyntaxException | MalformedURLException ex) {
//            return url;
//        }
//    }

    /**
     * @return the baseDir
     */
    public static Path getBaseDir() {
        return baseDir;
    }

//    /**
//     * @return the base
//     */
//    public static URI getBase() {
//        return base;
//    }

//    /**
//     * @param aBase the base to set
//     */
//    public static void setBase(URI aBase) {
//        base = aBase;
////        String scheme = base.getScheme();
////        baseDir = scheme != null && scheme.equals(FILE_SCHEME_NAME) ? new File(aBase.getSchemeSpecificPart()) : null;
//    }

//    static void setBase(String string) throws URISyntaxException, IOException {
//        setBase(new URI(string));
////        URI uri = new URI(string);
////        if (uri.getScheme() == null)
////            setBase(new File(string));
////        else
////            setBase(uri);
//    }

    public static void setBaseDir(Path newBaseDir) {
        baseDir = newBaseDir;
    }

    public static void setBaseDir(String newBaseDir) {
        baseDir = Paths.get(newBaseDir);
    }
    //    URI relativize(URI uri) {
//        return base.relativize(uri);
//    }

    static Path relativize(Path path) {
        return baseDir.relativize(path);
    }

    private final Map<String, ManufacturerDeviceClasses> manufacturers;

    public RemoteDatabase() {
        manufacturers = new LinkedHashMap<>(INITIAL_CAPACITY);
    }

    public RemoteDatabase(RemoteKind kind, File... files) {
        this();
        add(kind, files);
    }

    public RemoteDatabase(RemoteKind kind, String... files) {
        this();
        add(kind, files);
    }

    public RemoteDatabase(File... files) {
        this(RemoteKind.girr, files);
    }

    public RemoteDatabase(String... files) {
        this(RemoteKind.girr, files);
    }


    public void sort(Comparator<? super Named> comparator) {
        List<ManufacturerDeviceClasses> list = new ArrayList<>(manufacturers.values());
        Collections.sort(list, comparator);
        manufacturers.clear();
        for (ManufacturerDeviceClasses manuf : list) {
            manuf.sort(comparator);
            manufacturers.put(manuf.getName(), manuf);
        }
    }

    public void sort() {
        sort(new Named.CompareNameCaseInsensitive());
    }

    @Override
    public Document toDocument() {
        Document document = XmlUtils.newDocument(false);
        Element element = toElement(document);
        document.appendChild(element);
        return document;
    }

    @Override
    public Element toElement(Document document) {
        Element element = document.createElement(REMOTEDATABASE_ELEMENT_NAME);
        element.setAttribute(FORMATVERSION_ATTRIBUTE_NAME, FORMAT_VERSION);
        if (baseDir != null)
            element.setAttribute(BASEDIR_ATTRIBUTE_NAME, baseDir.toString());
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
        print (new File(file));
    }

    @Override
    public Iterator<ManufacturerDeviceClasses> iterator() {
        return manufacturers.values().iterator();
    }

    public void addRecursive(File file) {
        if (file.isDirectory()) {
            String[] files = file.list();
            if (files == null) {
                logger.log(Level.WARNING, "Error when reading directory {0}", file);
                return;
            }

            for (String f : files)
                addRecursive(new File(file, f));
//            } catch (IOException ex) {
//                logger.log(Level.WARNING, "Could not read directory {0}", file.toString());
//            }
        } else if (file.isFile() /*&& ! ignoreByExtension(path)*/) {
            try {
                add(XmlUtils.openXmlFile(file), file.toString());
            } catch (IOException | SAXException | GirrException ex) {
                logger.log(Level.WARNING, "Could not read file {0}; {1}", new Object[]{file.toString(), ex.getLocalizedMessage()});
            }
        } else
            logger.log(Level.WARNING, "Funny file {0}", file.toString());
    }

    public void add(RemoteSet remoteSet, String path) {
        for (Remote remote : remoteSet) {
            String xpath = "/" + REMOTES_ELEMENT_NAME + "/" + REMOTE_ELEMENT_NAME + "[@" + NAME_ATTRIBUTE_NAME + "=\'" + remote.getName() + "\']";
            add(remote, path, xpath);
        }
    }

    public void add(Remote remote, String path, String xpath) {
        ManufacturerDeviceClasses manufacturerTypes = getOrCreate(remote.getManufacturer());
        manufacturerTypes.add(remote, path, xpath);
    }

    private void add(Element element, String path) throws GirrException {
        switch (element.getTagName()) {
            case REMOTES_ELEMENT_NAME:
                RemoteSet remoteSet = new RemoteSet(element, path);
                add(remoteSet, path);
                break;
            case REMOTE_ELEMENT_NAME:
                Remote remote = new Remote(element, path);
                add(remote, path, "/" + REMOTE_ELEMENT_NAME);
                break;
            default:
                logger.log(Level.INFO, "File  {0} ignored, since its top level element is {1}", new Object[]{path, element.getTagName()});
        }
    }

    private void add(Document document, String string) throws GirrException {
        add(document.getDocumentElement(), string);
    }

    void add(RemoteKind kind, File baseDir) {
        if (kind.recurse()) {
            addRecursive(baseDir);
        } else {
            if (!(baseDir.isDirectory() && baseDir.canRead())) {
                logger.log(Level.WARNING, "{0} is not a readable directory", baseDir);
                return;
            }
            String[] manufacturerArray = baseDir.list();
            for (String manufacturer : manufacturerArray) {
                ManufacturerDeviceClasses manufacturerTypes = getOrCreate(manufacturer);
                manufacturerTypes.add(kind, new File(baseDir, manufacturer));
            }
        }
    }

    public void add(RemoteKind kind, File... files) {
        for (File file : files)
//            if (file.isAbsolute())
                add(kind, file);
//            else
//                add(kind, new File(baseDir, file.toString()));
    }

    public void add(RemoteKind kind, String... files) {
        for (String file : files)
            add(kind, new File(file));
    }

    public void add(File... files) {
        add(RemoteKind.girr, files);
    }

    public void add(String... files) {
        add(RemoteKind.girr, files);
    }

    public RemoteLink get(String manufacturer, String deviceClass, String remoteName) throws NotFoundException {
        ManufacturerDeviceClasses manufact = manufacturers.get(manufacturer.toLowerCase(Locale.US));
        if (manufact == null)
            throw new NotFoundException("Manufacturer " + manufacturer + " unknown.");
        return manufact.get(deviceClass, remoteName);
    }

    private ManufacturerDeviceClasses getOrCreate(String manufacturer) {
        String key = mkKey(manufacturer);

        ManufacturerDeviceClasses mt = manufacturers.get(key);
        if (mt == null) {
            mt = new ManufacturerDeviceClasses(key);
            manufacturers.put(key, mt);
        }
        return mt;
    }
}