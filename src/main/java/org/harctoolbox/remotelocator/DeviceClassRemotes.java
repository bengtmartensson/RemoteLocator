package org.harctoolbox.remotelocator;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.harctoolbox.girr.Named;
import org.harctoolbox.girr.Remote;
import static org.harctoolbox.remotelocator.RemoteLink.REMOTELINK_ELEMENT_NAME;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public final class DeviceClassRemotes implements Named, Iterable<RemoteLink> {
//    private final static Logger logger = Logger.getLogger(DeviceClassRemotes.class.getName());

    static final String DEVICECLASS_ELEMENT_NAME = "deviceClass";
    static final String DEVICECLASS_ATTRIBUTE_NAME = "name";
    private static final int INITIAL_CAPACITY = 8;

    private final String deviceClass;
    private final Map<String, RemoteLink> remoteLinks;

    DeviceClassRemotes(String key) {
        deviceClass = key;
        remoteLinks = new LinkedHashMap<>(INITIAL_CAPACITY);
    }

    DeviceClassRemotes(Element devElement) throws MalformedURLException {
        this(devElement.getAttribute(DEVICECLASS_ATTRIBUTE_NAME));
        NodeList nodeList = devElement.getElementsByTagName(REMOTELINK_ELEMENT_NAME);
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element remoteLinkElement = (Element) nodeList.item(i);
            RemoteLink remoteLink = new RemoteLink(remoteLinkElement);
            add(remoteLink);
        }
    }

    @Override
    public String getName() {
        return deviceClass;
    }

    @Override
    public Iterator<RemoteLink> iterator() {
        return remoteLinks.values().iterator();
    }

    public Element toElement(Document document) {
        Element element = document.createElement(DEVICECLASS_ELEMENT_NAME);
        element.setAttribute(DEVICECLASS_ATTRIBUTE_NAME, deviceClass);
        for (RemoteLink remoteLink : this) {
            element.appendChild(remoteLink.toElement(document));

        }
        return element;
    }

    void add(ScrapKind kind, Remote remote, URI baseUri, File baseDir, File path, String xpath) {
        RemoteLink remoteLink = new RemoteLink(kind, remote, baseUri, baseDir, path, xpath);
        add(remoteLink);
    }

    void add(RemoteLink remoteLink) {
        String key = RemoteDatabase.mkKey(remoteLink.getName());
        if (remoteLinks.containsKey(key)) {
            //logger.log(Level.WARNING, "Remote {0} already present, skipping", key); // TODO
            //return;
            int number = 1;
            while (true) {
                String actualKey = key + "$" + Integer.toString(number);
                if (! remoteLinks.containsKey(actualKey)) {
                    key = actualKey;
                    break;
                } else
                    number++;
            }
        }
        remoteLinks.put(key, remoteLink);
    }

    RemoteLink get(String deviceClass, String remoteName) throws NotFoundException {
        RemoteLink rl = remoteLinks.get(remoteName.toLowerCase(Locale.US));
        if (rl == null)
            throw new NotFoundException("No such remote in selected manufacturer and device class");
        return rl;
    }

    public void sort(Comparator<? super Named> comparator) {
        List<RemoteLink> list = new ArrayList<>(remoteLinks.values());
        try {
            System.out.println(this.deviceClass);
            Collections.sort(list, comparator);
        } catch (NullPointerException ex) {
            ex.printStackTrace();
        }
        remoteLinks.clear();
        for (RemoteLink link : list) {
            remoteLinks.put(RemoteDatabase.mkKey(link.getName()), link);
        }
    }

    public List<String> getRemotes() {
        List<String> result = new ArrayList<>(remoteLinks.size());
        for (RemoteLink r : this)
            result.add(r.getName());
        return result;
    }
}
