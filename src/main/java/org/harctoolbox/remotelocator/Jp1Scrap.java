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
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.validation.Schema;
import org.harctoolbox.girr.Remote;
import org.harctoolbox.xml.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Jp1Scrap extends Scrapable {

    private static final Logger logger = Logger.getLogger(Jp1Scrap.class.getName());

    private static final String TABLE_NAMESPACE_URI = "urn:oasis:names:tc:opendocument:xmlns:table:1.0";
    private static final String TEXT_NAMESPACE_URI = "urn:oasis:names:tc:opendocument:xmlns:text:1.0";
    private static final String JP1_NAME = "jp1";

    public static RemoteDatabase scrap(File jp1XmlFile) throws IOException, SAXException {
        Jp1Scrap jp1db = new Jp1Scrap();
        return jp1db.scrapSort(jp1XmlFile);
    }

    private static String getTextContent(Node node) {
        Element element = (Element) node;
        if (node == null)
            return "";
        NodeList nodeList = element.getElementsByTagNameNS(TEXT_NAMESPACE_URI, "p");
        if (nodeList.getLength() == 0)
            return "";
        return nodeList.item(0).getTextContent().trim();
    }

    public static void add(RemoteDatabase remoteDatabase, File file) throws SAXException, IOException {
        Jp1Scrap jp1 = new Jp1Scrap(remoteDatabase);
        jp1.add(file);
    }

    public Jp1Scrap(RemoteDatabase remoteDatabase) {
        super(remoteDatabase);
    }

    public Jp1Scrap() {
        super();
    }

    private void processRow(Element row) {
        if (!row.getAttributeNS(TABLE_NAMESPACE_URI, "style-name").equals("ro1"))
            return;
        NodeList cells = row.getElementsByTagNameNS(TABLE_NAMESPACE_URI, "table-cell");
        if (cells.getLength() < 7)
            return;
        Element viewCell = (Element) cells.item(1);
        String str = viewCell.getAttributeNS(TABLE_NAMESPACE_URI, "formula");
        if (str.isEmpty())
            return;
        String[] arr = str.split("\"");
        URI uri;
        try {
            uri = new URI(arr[1]);
        } catch (URISyntaxException ex) {
            logger.log(Level.WARNING, "Invalid URI: {0}, ignoring", arr[1]);
            return;
        }
        //String kind = Jp1Scrap.JP1_NAME; //getTextContent(cells.item(3)); // rmdu or txt
        String deviceClass = getTextContent(cells.item(4));
        String manufacturer = getTextContent(cells.item(5));
        String name = getTextContent(cells.item(6));
        String protocol = getTextContent(cells.item(9));
        String comment = null;//ew StringBuilder(32);
        String device = getTextContent(cells.item(10));
        String subdevice = getTextContent(cells.item(11));
        Remote remote = RemoteLink.mkRemote(name, comment, protocol, device, subdevice);
        RemoteLink remoteLink = new RemoteLink(ScrapKind.jp1, remote, uri, null, null);
        remoteDatabase.put(manufacturer, deviceClass, remoteLink);
    }

    private void add(Document document) {
        NodeList rows = document.getElementsByTagNameNS(TABLE_NAMESPACE_URI, "table-row");
        logger.log(Level.INFO, "Found {0} rows.", rows.getLength());
        for (int i = 0; i < rows.getLength(); i++) {
            processRow((Element) rows.item(i));
        }
    }

    @Override
    public void add(File file) throws SAXException, IOException {
        add(XmlUtils.openXmlFile(file, (Schema) null, true, false));
    }

    @Override
    public String getName() {
        return JP1_NAME;
    }
}
