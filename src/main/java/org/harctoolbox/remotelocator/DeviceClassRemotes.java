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
import java.util.logging.Level;
import java.util.logging.Logger;
import org.harctoolbox.girr.Named;
import org.harctoolbox.girr.Remote;
import static org.harctoolbox.remotelocator.RemoteDatabase.REMOTELOCATOR_NAMESPACE;
import static org.harctoolbox.remotelocator.RemoteDatabase.REMOTELOCATOR_PREFIX;
import static org.harctoolbox.remotelocator.RemoteLink.REMOTELINK_ELEMENT_NAME;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public final class DeviceClassRemotes implements Named, Iterable<RemoteLink> {
    private final static Logger logger = Logger.getLogger(DeviceClassRemotes.class.getName());

    static final String DEVICECLASS_ELEMENT_NAME = "deviceClass";
    static final String DEVICECLASS_ATTRIBUTE_NAME = "name";
    private static final int INITIAL_CAPACITY = 8;

    private ManufacturerDeviceClasses owner;
    private final String deviceClass;
    private final Map<String, RemoteLink> remoteLinks;

    DeviceClassRemotes(String key) {
        owner = null;
        deviceClass = key;
        remoteLinks = new LinkedHashMap<>(INITIAL_CAPACITY);
    }

    DeviceClassRemotes(Element devElement) throws MalformedURLException {
        this(devElement.getAttribute(DEVICECLASS_ATTRIBUTE_NAME));
        NodeList nodeList = devElement.getElementsByTagNameNS(REMOTELOCATOR_NAMESPACE, REMOTELINK_ELEMENT_NAME);
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element remoteLinkElement = (Element) nodeList.item(i);
            RemoteLink remoteLink = new RemoteLink(remoteLinkElement);
            add(remoteLink);
        }
    }

    public int numberRemotes() {
        return remoteLinks.size();
    }

    public void setOwner(ManufacturerDeviceClasses owner) {
        this.owner = owner;
    }

    public ManufacturerDeviceClasses getOwner() {
        return owner;
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
        Element element = document.createElementNS(REMOTELOCATOR_NAMESPACE, REMOTELOCATOR_PREFIX + ":" + DEVICECLASS_ELEMENT_NAME);
        element.setAttribute(DEVICECLASS_ATTRIBUTE_NAME, deviceClass);
        for (RemoteLink remoteLink : this)
            element.appendChild(remoteLink.toElement(document));

        return element;
    }

    void add(ScrapKind kind, Remote remote, URI baseUri, File baseDir, File path, String xpath) {
        RemoteLink remoteLink = new RemoteLink(kind, remote, baseUri, baseDir, path, xpath);
        add(remoteLink);
    }

    @SuppressWarnings("CallToPrintStackTrace")
    void add(RemoteLink remoteLink) {
        String key = RemoteDatabase.mkKey(remoteLink.getName());
        String actualKey = key;
        if (remoteLinks.containsKey(key)) {
            int number = 1;
            while (true) {
                actualKey = key + "$" + Integer.toString(number);
                if (! remoteLinks.containsKey(actualKey)) {
                    //key = actualKey;
                    break;
                } else
                    number++;
            }
            try {
            logger.log(Level.WARNING, "Remote {0} present several times, renamed to {1}", new Object[]{ key, actualKey});
            } catch (NullPointerException ex) {
                ex.printStackTrace();
            }
            //return;
        }
        remoteLinks.put(actualKey, remoteLink);
        remoteLink.setOwner(this);
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
        list.forEach(link -> {
            remoteLinks.put(RemoteDatabase.mkKey(link.getName()), link);
        });
    }

   public List<String> getRemotes() {
        return getRemotes(null);
    }

   public List<String> getRemotes(ScrapKind kind) {
        List<String> result = new ArrayList<>(remoteLinks.size());
        for (RemoteLink r : this) {
            if (kind == null || r.getKind() == kind)
                result.add(r.getName());
        }
        return result;
    }

    public boolean hasKind(ScrapKind kind) {
        if (kind == null)
            return true;
        for (RemoteLink remoteLink : this)
            if (remoteLink.getKind() == kind)
                return true;
        return false;
    }
}
