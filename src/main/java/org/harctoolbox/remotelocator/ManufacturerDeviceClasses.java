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
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.harctoolbox.girr.Named;
import org.harctoolbox.girr.Remote;
import static org.harctoolbox.remotelocator.DeviceClassRemotes.DEVICECLASS_ELEMENT_NAME;
import static org.harctoolbox.remotelocator.RemoteDatabase.UNKNOWN;
import static org.harctoolbox.remotelocator.RemoteDatabase.mkKey;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public final class ManufacturerDeviceClasses implements Named, Iterable<DeviceClassRemotes>, Serializable {

    private final static Logger logger = Logger.getLogger(ManufacturerDeviceClasses.class.getName());

    static final String MANUFACTURER_ELEMENT_NAME = "manufacturer";
    static final String MANUFACTURER_ATTRIBUTE_NAME = MANUFACTURER_ELEMENT_NAME;
    private static final int INITIAL_CAPACITY = 8;

    private final String manufacturer;
    private final Map<String, DeviceClassRemotes> deviceClasses;

    ManufacturerDeviceClasses(String mani) {
        this.manufacturer = mani;
        this.deviceClasses = new LinkedHashMap<>(INITIAL_CAPACITY);
    }

    ManufacturerDeviceClasses(Element manifacturerElement) {
        this(manifacturerElement.getAttribute(MANUFACTURER_ATTRIBUTE_NAME));
        NodeList nodeList = manifacturerElement.getElementsByTagName(DEVICECLASS_ELEMENT_NAME);
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

    private void add(DeviceClassRemotes dev) {
        deviceClasses.put(mkKey(dev.getName()), dev);
    }

    @Override
    public String getName() {
        return manufacturer;
    }

    public Element toElement(Document document) {
        Element element = document.createElement(MANUFACTURER_ELEMENT_NAME);
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
        if (manufacturer.equals("bnk components"))
                System.out.println(">>> " + manufacturer);
        for (DeviceClassRemotes dev : list) {
            dev.sort(comparator);
            deviceClasses.put(RemoteDatabase.mkKey(dev.getName()), dev);
        }
    }

    void put(String deviceClass, RemoteLink remoteLink) {
        DeviceClassRemotes devCls = getOrCreate(deviceClass);
        devCls.add(remoteLink);
    }

    public List<String> getDeviceClasses() {
        List<String> result = new ArrayList<>(deviceClasses.size());
        for (DeviceClassRemotes d : this)
            result.add(d.getName());
        return result;
    }

    public DeviceClassRemotes getDeviceClass(String deviceClassName) {
        return deviceClasses.get(RemoteDatabase.mkKey(deviceClassName));
    }
}
