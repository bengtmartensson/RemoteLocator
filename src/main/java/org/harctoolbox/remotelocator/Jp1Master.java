package org.harctoolbox.remotelocator;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.validation.Schema;
import org.harctoolbox.xml.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 * @author bengt
 */
public class Jp1Master {

    private static final Logger logger = Logger.getLogger(Jp1Master.class.getName());
    public  static final String JP1_XML = "/home/bengt/jp1/jp1-master/jp1-master-1.16.fods";
    private static final String TABLE_NAMESPACE_URI = "urn:oasis:names:tc:opendocument:xmlns:table:1.0";
    private static final String TEXT_NAMESPACE_URI = "urn:oasis:names:tc:opendocument:xmlns:text:1.0";


    public static void main(String[] args) {
        try {
            RemoteDatabase remoteDatabase = scrapJp1(new File(JP1_XML));
            remoteDatabase.sort();
            remoteDatabase.print("jp1.xml");
        } catch (IOException | SAXException ex) {
            Logger.getLogger(Jp1Master.class.getName()).log(Level.SEVERE, null, ex);
        }
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

    public static RemoteDatabase scrapJp1(File file) throws SAXException, IOException {
        Jp1Master jp1Master = new Jp1Master();
        jp1Master.scrap(file);
        return jp1Master.remoteDatabase;
    }

    private final RemoteDatabase remoteDatabase;

    private Jp1Master() {
        this.remoteDatabase = new RemoteDatabase();
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
        String kind = getTextContent(cells.item(3)); // rmdu or txt
        String deviceClass = getTextContent(cells.item(4));
        String manufacturer = getTextContent(cells.item(5));
        String name = getTextContent(cells.item(6));
        String protocolName = getTextContent(cells.item(9));
        StringBuilder comment = new StringBuilder(32);
        if (!protocolName.isEmpty())
            comment.append("Protocol=").append(protocolName);
        String device = getTextContent(cells.item(10));
        if (! device.isEmpty())
            comment.append("; device=").append(device);
        String subdevice = getTextContent(cells.item(11));
        if (! subdevice.isEmpty())
            comment.append("; subdevice=").append(subdevice);
        RemoteLink remoteLink = new RemoteLink(RemoteKind.valueOf(kind.toLowerCase(Locale.US)), uri, null, name, null, null, comment.toString(), null, null, null);
        remoteDatabase.put(manufacturer, deviceClass, remoteLink);
    }

    private void scrap(Document document) {
        NodeList rows = document.getElementsByTagNameNS(TABLE_NAMESPACE_URI, "table-row");
        logger.log(Level.INFO, "Found {0} rows.", rows.getLength());
        for (int i = 0; i < rows.getLength(); i++)
            processRow((Element) rows.item(i));
    }

    private void scrap(File file) throws SAXException, IOException {
        scrap(XmlUtils.openXmlFile(file, (Schema) null, true, false));
    }
}