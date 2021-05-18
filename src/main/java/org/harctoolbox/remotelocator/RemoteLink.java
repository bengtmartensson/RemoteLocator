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
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;
import org.harctoolbox.girr.Command;
import org.harctoolbox.girr.CommandSet;
import org.harctoolbox.girr.Named;
import org.harctoolbox.girr.Remote;
import static org.harctoolbox.girr.XmlStatic.COMMENT_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlStatic.NAME_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlStatic.PROTOCOL_ATTRIBUTE_NAME;
import org.harctoolbox.ircore.IrCoreException;
import org.harctoolbox.ircore.ThisCannotHappenException;
import org.harctoolbox.irp.IrpException;
import static org.harctoolbox.remotelocator.RemoteDatabase.FILE_SCHEME_NAME;
import static org.harctoolbox.remotelocator.RemoteDatabase.REMOTELOCATOR_NAMESPACE;
import static org.harctoolbox.remotelocator.RemoteDatabase.REMOTELOCATOR_PREFIX;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 */
public final class RemoteLink implements Named, Serializable {
    private static final Logger logger = Logger.getLogger(RemoteLink.class.getName());

    public static final String REMOTELINK_ELEMENT_NAME = "remoteLink";
    public static final String PATH_ELEMENT_NAME = "path";
    public static final String XPATH_ATTRIBUTE_NAME = "xpath";
    public static final String KIND_ATTRIBUTE_NAME = "kind";
    public static final String URL_ATTRIBUTE_NAME = "url";
    public static final String DEVICE_ATTRIBUTE_NAME = "device";
    public static final String SUBDEVICE_ATTRIBUTE_NAME = "subdevice";
    public static final String DUMMY_COMMAND_NAME = "dummy-command";

    private static void setAttributeIfNonNull(Element element, String attributeName, Object object) {
        if (object == null)
            return;
        String value = object.toString();
        if (value.isEmpty())
            return;
        element.setAttribute(attributeName, value);
    }

    static Remote mkRemote(String name, String comment, String protocolName, String device, String subdevice) {
        if (! Command.isKnownProtocol(protocolName))
            return mkRemote(name, comment, null, null, (Long) null);

        Long dev = null;
        Long subdev = null;
        try {
            dev = Long.parseLong(device);
            subdev = Long.parseLong(subdevice);
        } catch (NumberFormatException ex) {
            logger.fine(ex.getLocalizedMessage());
        }
        return mkRemote(name, comment, protocolName, dev, subdev);
    }

    private static Remote mkRemote(String name, String comment, String protocolName, Long device, Long subdevice) {
        Remote.MetaData metaData = new Remote.MetaData(name);
        CommandSet commandSet;
//        Map<String, Long> parameters = new HashMap<>(2);
        if (Command.isKnownProtocol(protocolName)) {
//            if (device != null)
//                parameters.put(D_PARAMETER_NAME, device);
//            if (subdevice != null)
//                parameters.put(S_PARAMETER_NAME, subdevice);
//            try {
                Command command = new Command(protocolName, device, subdevice);
                commandSet = new CommandSet(command);
//            } catch (GirrException ex) {
//                logger.log(Level.WARNING, ex.getLocalizedMessage());
//                commandSet = new CommandSet();
//            }
        } else
            commandSet = new CommandSet();

        return new Remote(metaData, comment, null, commandSet, null);
    }

    private DeviceClassRemotes owner;
    private final ScrapKind kind;
//    private String name;
    private final File file;
    private String xpath;
    private final URL url;
//    private final String comment;
    private final Remote remote;

    public RemoteLink(ScrapKind kind, Remote remote, URI baseUri, File baseDir, File file, String xpath) {
//        this(kind, baseUri, baseDir, remote.getName(), file, xpath, remote.getComment(), remote.getProtocolName(), remote.getD(), remote.getS());
//    }
//
//    public RemoteLink(ScrapKind kind, URI baseUri, File baseDir, String name, File file, String xpath, String comment, String protocol, Long device, Long subdevice) {
//        try {
        Scrapable scrap = ScrapKind.mkScrapable(kind);
        this.remote = remote;
        this.kind = kind;
//            this.name = name;
        if (baseDir != null) {
            Path baseDirPath = Paths.get(baseDir.getPath());
            Path localPath = Paths.get(file.getPath());
            this.file = baseDirPath.relativize(localPath).toFile();
        } else {
            this.file = file;
        }
        this.xpath = xpath;
        try {
            if (baseUri != null) {
                if (file != null) {
                    URI uri = new URI(FILE_SCHEME_NAME, scrap.formatUrl(this.file.toString()), null);
                    String escapedPath = uri.toString().substring(5);
                    uri = new URI(escapedPath);
                    url = baseUri.resolve(uri).toURL();
                } else
                    url = baseUri.toURL();
            } else {
                url = null;
            }
//            this.comment = comment;
//            this.displayName = displayName;
//            this.model = model;
//            this.origRemote = origRemote;
        } catch (URISyntaxException | MalformedURLException ex) {
            throw new ThisCannotHappenException(ex);
        }
    }

    RemoteLink(ScrapKind kind, Remote remote, URI baseUri, File baseDir, File file) {
        this(kind, remote, baseUri, baseDir, file, (String) null);
    }
//    public RemoteLink(ScrapKind kind, Remote remote, URI baseUri, File baseDir, File file) {
//        this(kind, remote, baseUri, baseDir, file, (String) null);
//    }
//
//    RemoteLink(ScrapKind kind, URI uri, File baseDir, File dir, String remoteName) {
//        this(kind, uri, baseDir, remote, new File(dir, remote), null);
//    }

    RemoteLink(Element remoteLinkElement) throws MalformedURLException {
        kind = ScrapKind.valueOf(remoteLinkElement.getAttribute(KIND_ATTRIBUTE_NAME));
        file = new File(remoteLinkElement.getAttribute(PATH_ELEMENT_NAME));
        url = new URL(remoteLinkElement.getAttribute(URL_ATTRIBUTE_NAME));
        xpath = remoteLinkElement.getAttribute(XPATH_ATTRIBUTE_NAME);

        String name = remoteLinkElement.getAttribute(NAME_ATTRIBUTE_NAME);
        String comment = remoteLinkElement.getAttribute(COMMENT_ATTRIBUTE_NAME);
        String protocol = remoteLinkElement.getAttribute(PROTOCOL_ATTRIBUTE_NAME);
        String device = remoteLinkElement.getAttribute(DEVICE_ATTRIBUTE_NAME);
        String subdevice = remoteLinkElement.getAttribute(SUBDEVICE_ATTRIBUTE_NAME);
        remote = mkRemote(name, comment, protocol, device, subdevice);
    }

    void setOwner(DeviceClassRemotes owner) {
        this.owner = owner;
    }

    String getDeviceClass() {
        return owner.getName();
    }

    String getManufacturer() {
        return owner.getOwner().getName();
    }

    private Command getFirstCommand() {
        Iterator<CommandSet> iterator = remote.iterator();
        if (!iterator.hasNext())
            return null;
        CommandSet firstCommandSet = iterator.next();
        Iterator<Command> it = firstCommandSet.iterator();
        return it.hasNext() ? it.next() : null;
    }

    private String getProtocolName() {
        try {
            Command firstCommand = getFirstCommand();
            return firstCommand != null ? firstCommand.getProtocolName() : null;
        } catch (IrpException | IrCoreException ex) {
            return null;
        }
    }

    private Map<String, Long> getParameters() {
        try {
            Command firstCommand = getFirstCommand();
            return firstCommand != null ? firstCommand.getParameters() : null;
        } catch (IrpException | IrCoreException ex) {
            return null;
        }
    }

    private Long getD() {
        Map<String, Long> parameters = getParameters();
        return parameters != null ? parameters.get(Command.D_PARAMETER_NAME) : null;
    }

    private Long getS() {
        Map<String, Long> parameters = getParameters();
        return parameters != null ? parameters.get(Command.S_PARAMETER_NAME) : null;
    }

    public Remote getRemote() throws IOException, Girrable.NotGirrableException, NotFoundException {
        Scrapable scrap = ScrapKind.mkScrapable(this);
        if (!(scrap instanceof Girrable))
            throw new Girrable.NotGirrableException();

        return ((Girrable) scrap).getRemote(this);
    }

    @Override
    public String getName() {
        return remote.getName();
    }

    public File getFile() {
        return file;
    }

    public String getXpath() {
        return xpath;
    }

    public Element toElement(Document document) {
        Element element = document.createElementNS(REMOTELOCATOR_NAMESPACE, REMOTELOCATOR_PREFIX + ":" + REMOTELINK_ELEMENT_NAME);
        element.setAttribute(NAME_ATTRIBUTE_NAME, getName());
        setAttributeIfNonNull(element, PATH_ELEMENT_NAME, file);
        setAttributeIfNonNull(element, KIND_ATTRIBUTE_NAME, kind.name());
        setAttributeIfNonNull(element, XPATH_ATTRIBUTE_NAME, xpath);
        setAttributeIfNonNull(element, COMMENT_ATTRIBUTE_NAME, getComment());
        setAttributeIfNonNull(element, URL_ATTRIBUTE_NAME, url);
        setAttributeIfNonNull(element, PROTOCOL_ATTRIBUTE_NAME, getProtocolName());
        setAttributeIfNonNull(element, DEVICE_ATTRIBUTE_NAME, getD());
        setAttributeIfNonNull(element, SUBDEVICE_ATTRIBUTE_NAME, getS());
        return element;
    }

    /**
     * @return the kind
     */
    public ScrapKind getKind() {
        return kind;
    }

    /**
     * @return the comment
     */
    public String getComment() {
        return remote.getComment();
    }

    public URL getUrl() {
        return url;
    }

    RemoteDatabase getRemoteDatabase() {
        return owner.getOwner().getRemoteDatabase();
    }

}
