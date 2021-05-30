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
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.harctoolbox.girr.Named;
import org.harctoolbox.girr.Remote;
import static org.harctoolbox.remotelocator.DeviceClassRemotes.DEVICECLASS_ELEMENT_NAME;
import static org.harctoolbox.remotelocator.RemoteDatabase.REMOTELOCATOR_NAMESPACE;
import static org.harctoolbox.remotelocator.RemoteDatabase.REMOTELOCATOR_PREFIX;
import static org.harctoolbox.remotelocator.RemoteDatabase.UNKNOWN;
import static org.harctoolbox.remotelocator.RemoteDatabase.mkKey;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public final class ManufacturerDeviceClasses implements Named, Iterable<DeviceClassRemotes>, Serializable {

    private final static Logger logger = Logger.getLogger(ManufacturerDeviceClasses.class.getName());

    static final String MANUFACTURER_ELEMENT_NAME = "manufacturer";
    static final String MANUFACTURER_ATTRIBUTE_NAME = "name";
    private static final int INITIAL_CAPACITY = 8;

    private final String manufacturer;
    private final Map<String, DeviceClassRemotes> deviceClasses;
    private RemoteDatabase remoteDatabase;

    ManufacturerDeviceClasses(String mani) {
        this.manufacturer = mani;
        this.deviceClasses = new LinkedHashMap<>(INITIAL_CAPACITY);
    }

    ManufacturerDeviceClasses(Element manifacturerElement) {
        this(manifacturerElement.getAttribute(MANUFACTURER_ATTRIBUTE_NAME));
        NodeList nodeList = manifacturerElement.getElementsByTagNameNS(REMOTELOCATOR_NAMESPACE, DEVICECLASS_ELEMENT_NAME);
        for (int i = 0; i < nodeList.getLength(); i++) {
            Element devElement = (Element) nodeList.item(i);
            try {
                DeviceClassRemotes dev = new DeviceClassRemotes(devElement);
                add(dev);
            } catch (MalformedURLException ex) {
                logger.log(Level.WARNING, "Erroneous URL {0}", ex.getLocalizedMessage());
            }
        }
    }

    public int numberRemotes() {
        int sum = 0;
        for (DeviceClassRemotes deviceClassRemotes : this)
            sum += deviceClassRemotes.numberRemotes();
        return sum;
    }

    private void add(DeviceClassRemotes dev) {
        deviceClasses.put(mkKey(dev.getName()), dev);
        dev.setOwner(this);
    }

    @Override
    public String getName() {
        return manufacturer;
    }

    public Element toElement(Document document) {
        Element element = document.createElementNS(REMOTELOCATOR_NAMESPACE, REMOTELOCATOR_PREFIX + ":" + MANUFACTURER_ELEMENT_NAME);
        element.setAttribute(MANUFACTURER_ATTRIBUTE_NAME, manufacturer);
        for (DeviceClassRemotes type : this)
            element.appendChild(type.toElement(document));
        return element;
    }

    @Override
    public Iterator<DeviceClassRemotes> iterator() {
        return deviceClasses.values().iterator();
    }

    void add(ScrapKind kind, Remote remote, URI baseUri, File baseDir, File path, String xpath) {
        DeviceClassRemotes deviceClass = getOrCreate(remote.getDeviceClass());
        deviceClass.add(kind, remote, baseUri, baseDir, path, xpath);
    }

    DeviceClassRemotes getOrCreate(String deviceClass) {
        String key = mkKey(deviceClass);
        DeviceClassRemotes typeRemote = deviceClasses.get(key);
        if (typeRemote == null) {
            typeRemote = new DeviceClassRemotes((deviceClass == null || deviceClass.isEmpty()) ? UNKNOWN : deviceClass);
            deviceClasses.put(key, typeRemote);
            typeRemote.setOwner(this);
        }
        return typeRemote;
    }

    RemoteLink get(String deviceClass, String remoteName) throws NotFoundException {
        DeviceClassRemotes type = deviceClasses.get(mkKey(deviceClass));
        if (type == null)
            throw new NotFoundException("Device Class " + deviceClass + " not present in selected manufacturer.");
        return type.get(deviceClass, remoteName);
    }

    public void sort(Comparator<? super Named> comparator) {
        List<DeviceClassRemotes> list = new ArrayList<>(deviceClasses.values());
        Collections.sort(list, comparator);
        deviceClasses.clear();
        list.stream().map(dev -> {
            dev.sort(comparator);
            return dev;
        }).forEachOrdered(dev -> {
            deviceClasses.put(RemoteDatabase.mkKey(dev.getName()), dev);
        });
    }

    void put(String deviceClass, RemoteLink remoteLink) {
        DeviceClassRemotes devCls = getOrCreate(deviceClass);
        devCls.add(remoteLink);
    }

    public List<String> getDeviceClasses() {
        return getDeviceClasses(null);
    }

    public List<String> getDeviceClasses(ScrapKind kind) {
        List<String> result = new ArrayList<>(deviceClasses.size());
        for (DeviceClassRemotes d : this)
            if (d.hasKind(kind))
                result.add(d.getName());
        return result;
    }

    public DeviceClassRemotes getDeviceClass(String deviceClassName) throws NotFoundException {
        DeviceClassRemotes d = deviceClasses.get(RemoteDatabase.mkKey(deviceClassName));
        if (d == null)
            throw new NotFoundException("Manufacturer \""  + manufacturer + "\" has no device class \"" + deviceClassName + "\" in the data base.");
        return d;
    }

    public boolean hasKind(ScrapKind kind) {
        return deviceClasses.values().stream().anyMatch(deviceClassRemotes -> (deviceClassRemotes.hasKind(kind)));
    }

    RemoteDatabase getRemoteDatabase() {
        return remoteDatabase;
    }

    void setRemoteDatabase(RemoteDatabase remoteDatabase) {
        this.remoteDatabase = remoteDatabase;
    }
}
