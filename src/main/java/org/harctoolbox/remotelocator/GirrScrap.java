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
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.harctoolbox.girr.GirrException;
import org.harctoolbox.girr.Remote;
import org.harctoolbox.girr.RemoteSet;
import static org.harctoolbox.girr.XmlStatic.NAME_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlStatic.REMOTES_ELEMENT_NAME;
import static org.harctoolbox.girr.XmlStatic.REMOTE_ELEMENT_NAME;
import org.harctoolbox.xml.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class GirrScrap extends Girrable {
    private static final Logger logger = Logger.getLogger(GirrScrap.class.getName());

    public static final String GIRRLIB_BASE = "https://raw.githubusercontent.com/bengtmartensson/GirrLib/master/Girr/";
    public static final URI GIRRLIB_BASE_URI= URI.create(GIRRLIB_BASE);
    private static final String GIRR_NAME = "girr";
    private static final String[] junkExtensions = {
        ".xsl",
        ".jpg",
        ".html",
        ".pdf"
    };

    public static RemoteDatabase scrap(File dir) throws IOException, SAXException {
        GirrScrap girr = new GirrScrap();
        return girr.scrapSort(dir);
    }

    public GirrScrap() {
        super();
    }

    public GirrScrap(RemoteDatabase remoteDatabase) {
        super(remoteDatabase);
    }

    @Override
    public String getName() {
        return GIRR_NAME;
    }

    @Override
    public void add(File dir) throws IOException, SAXException {
        addRecursive(GIRRLIB_BASE_URI, dir, dir);
    }

    private void addRecursive(URI uriBase, File baseDir, File file) throws IOException {
        if (file.isDirectory()) {
            assertReadableDirectory(file);

            String[] files = file.list();
            for (String f : files)
                addRecursive(uriBase, baseDir, new File(file, f));
        } else if (ignoreByExtension(file.getName())) {
            logger.log(Level.FINE, "File {0} ignored due to its extension", file.toString());
        } else if (file.isFile()) {
            try {
                add(XmlUtils.openXmlFile(file), uriBase, baseDir, file);
            } catch (IOException | SAXException ex) {
                logger.log(Level.WARNING, "Could not read file {0}; {1}", new Object[]{file.toString(), ex.getLocalizedMessage()});
            } catch (GirrException ex) {
                Logger.getLogger(GirrScrap.class.getName()).log(Level.SEVERE, null, ex);
            }
        } else
            logger.log(Level.WARNING, "Unknown file {0}", file.toString());
    }

    private void add(Document document, URI baseUri, File baseDir, File file) throws GirrException {
        add(document.getDocumentElement(), baseUri, baseDir, file);
    }

    private void add(Element element, URI baseUri, File baseDir, File file) throws GirrException {
        switch (element.getTagName()) {
            case REMOTES_ELEMENT_NAME:
                RemoteSet remoteSet = new RemoteSet(element, file.toString());
                add(remoteSet, baseUri, baseDir, file);
                break;
            case REMOTE_ELEMENT_NAME:
                Remote remote = new Remote(element, file.toString());
                add(remote, baseUri, baseDir, file, "/" + REMOTE_ELEMENT_NAME);
                break;
            default:
                logger.log(Level.INFO, "File  {0} ignored, since its top level element is {1}", new Object[]{file, element.getTagName()});
        }
    }

    public void add(RemoteSet remoteSet, URI baseUri, File baseDir, File file) {
        for (Remote remote : remoteSet) {
            String xpath = REMOTES_ELEMENT_NAME + "/" + REMOTE_ELEMENT_NAME + "[@" + NAME_ATTRIBUTE_NAME + "=\'" + remote.getName() + "\']";
            add(remote, baseUri, baseDir, file, xpath);
        }
    }

    private void add(Remote remote, URI baseUri, File baseDir, File file, String xpath) {
        ManufacturerDeviceClasses manufacturerTypes = remoteDatabase.getOrCreate(remote.getManufacturer());
        manufacturerTypes.add(ScrapKind.girr, remote, baseUri, baseDir, file, xpath);
    }

    @Override
    public Remote getRemote(InputStreamReader reader, String source, String xpath, String manufacturer, String deviceClass) throws IOException {
        try {
            Document document = XmlUtils.openXmlReader(reader, null, false, true);
            XPath xpathy = XPathFactory.newInstance().newXPath();
            NodeList str = (NodeList) xpathy.compile(xpath).evaluate(document, XPathConstants.NODESET);
            Element el = (Element) str.item(0);
            return new Remote(el, "remote");
        } catch (XPathExpressionException | SAXException | GirrException ex) {
            logger.log(Level.WARNING, ex.getLocalizedMessage());
            return null;
        }
    }

    private boolean ignoreByExtension(String name) {
        for (String ext : junkExtensions)
            if (name.endsWith(ext))
                return true;
        return false;
    }
}
