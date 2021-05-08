package org.harctoolbox.remotelocator;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
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
import static org.harctoolbox.remotelocator.RemoteDatabase.UNKNOWN;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public final class ManufacturerDeviceClasses implements Named, Iterable<DeviceClassRemotes>, Serializable {

    private final static Logger logger = Logger.getLogger(ManufacturerDeviceClasses.class.getName());

    private static final String MANUFACTURER_ELEMENT_NAME = "manufacturer";
    private static final String MANUFACTURER_ATTRIBUTE_NAME = MANUFACTURER_ELEMENT_NAME;
    private static final int INITIAL_CAPACITY = 8;

    private static String captitalize(String str) {
        return str.substring(0, 1).toUpperCase(Locale.US) + str.substring(1);
    }

    private final String manufacturer;
    private final Map<String, DeviceClassRemotes> deviceClasses;

    ManufacturerDeviceClasses(String key) {
        this.manufacturer = key;
        this.deviceClasses = new LinkedHashMap<>(INITIAL_CAPACITY);
    }

    @Override
    public String getName() {
        return manufacturer;
    }

    public Element toElement(Document document) {
        Element element = document.createElement(MANUFACTURER_ELEMENT_NAME);
        element.setAttribute(MANUFACTURER_ATTRIBUTE_NAME, captitalize(manufacturer));
        for (DeviceClassRemotes type : this)
            element.appendChild(type.toElement(document));
        return element;
    }

    @Override
    public Iterator<DeviceClassRemotes> iterator() {
        return deviceClasses.values().iterator();
    }

    void add(RemoteKind kind, URI uri, File baseDir, File dir) throws IOException {
        if (!(dir.isDirectory() && dir.canRead())) {
            // Non-fatal; there may lie junk files around
            logger.log(Level.WARNING, "{0} is not a readable directory", dir);
            return;
        }

        if (kind.hasDeviceClasses()) {
            String[] list = dir.list();
            if (list == null)
                // Something is wrong, so this is fatal
                throw new IOException(dir + " could not be read");

            for (String deviceClass : list) {
                DeviceClassRemotes devices = getOrCreate(deviceClass);
                devices.add(kind, uri, baseDir, new File(dir, deviceClass));
            }
        } else {
            DeviceClassRemotes devices = getOrCreate(UNKNOWN);
            devices.add(kind, uri, baseDir, dir);
        }
    }

    void add(Remote remote, URI baseUri, File baseDir, File path, String xpath) {
        DeviceClassRemotes deviceClass = getOrCreate(remote.getDeviceClass());
        deviceClass.add(remote, baseUri, baseDir, path, xpath);
    }

    private DeviceClassRemotes getOrCreate(String deviceClass) {
        String key = RemoteDatabase.mkKey(deviceClass);
        DeviceClassRemotes typeRemote = deviceClasses.get(key);
        if (typeRemote == null) {
            typeRemote = new DeviceClassRemotes(key);
            deviceClasses.put(key, typeRemote);
        }
        return typeRemote;
    }

    RemoteLink get(String deviceClass, String remoteName) throws NotFoundException {
        DeviceClassRemotes type = deviceClasses.get(deviceClass.toLowerCase(Locale.US));
        if (type == null)
            throw new NotFoundException("Device Class " + deviceClass + " not present in selected manufacturer.");
        return type.get(deviceClass, remoteName);
    }

    public void sort(Comparator<? super Named> comparator) {
        List<DeviceClassRemotes> list = new ArrayList<>(deviceClasses.values());
        Collections.sort(list, comparator);
        deviceClasses.clear();
        for (DeviceClassRemotes dev : list) {
            dev.sort(comparator);
            deviceClasses.put(RemoteDatabase.mkKey(dev.getName()), dev);
        }
    }

    void put(String deviceClass, RemoteLink remoteLink) {
        DeviceClassRemotes devCls = getOrCreate(deviceClass);
        devCls.add(remoteLink);
    }

    public static class CompareNameCaseInsensitive implements Comparator<ManufacturerDeviceClasses>, Serializable {
        @Override
        public int compare(ManufacturerDeviceClasses o1, ManufacturerDeviceClasses o2) {
            return o1.manufacturer.compareToIgnoreCase(o2.manufacturer);
        }
    }
}