
package org.harctoolbox.remotelocator;

import java.io.File;
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
import org.harctoolbox.girr.Named;
import org.harctoolbox.girr.Remote;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 * @author bengt
 */
public final class DeviceClassRemotes implements Named, Iterable<RemoteLink> {
    private final static Logger logger = Logger.getLogger(DeviceClassRemotes.class.getName());

    private static final String DEVICECLASS_ELEMENT_NAME = "deviceClass";
    private static final String DEVICECLASS_ATTRIBUTE_NAME = DEVICECLASS_ELEMENT_NAME;
    private static final int INITIAL_CAPACITY = 8;

    private final String deviceClass;
    private final Map<String, RemoteLink> remoteLinks;

    DeviceClassRemotes(String key) {
        deviceClass = key;
        remoteLinks = new LinkedHashMap<>(INITIAL_CAPACITY);
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

    void add(Remote remote, String path, String xpath) {
        RemoteLink remoteLink = new RemoteLink(remote, path, xpath);
        add(remoteLink);
    }

    void add(RemoteLink remoteLink) {
        String key = RemoteDatabase.mkKey(remoteLink.getName());
        if (remoteLinks.containsKey(key)) {
            logger.log(Level.WARNING, "Remote {0} already present, skipping", key); // TODO
            return;
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
        Collections.sort(list, comparator);
        remoteLinks.clear();
        for (RemoteLink link : list) {
            remoteLinks.put(link.getName(), link);
        }
    }

    void add(RemoteKind kind, File dir) {
        if (! (dir.isDirectory() && dir.canRead())) {
            logger.log(Level.WARNING, "File {0} not a readable directory, ignored.", dir);
            return;
        }
        String[] array = dir.list();

        for (String remote : array)
            add(new RemoteLink(kind, dir, remote));
    }
}