
package org.harctoolbox.remotelocator;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.validation.Schema;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.harctoolbox.girr.GirrException;
import org.harctoolbox.girr.Named;
import org.harctoolbox.girr.Remote;
import org.harctoolbox.girr.RemoteSet;
import static org.harctoolbox.girr.XmlStatic.COMMENT_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlStatic.MODEL_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlStatic.NAME_ATTRIBUTE_NAME;
import org.harctoolbox.ircore.IrCoreUtils;
import org.harctoolbox.ircore.ThisCannotHappenException;
import org.harctoolbox.jirc.ConfigFile;
import static org.harctoolbox.remotelocator.RemoteDatabase.FILE_SCHEME_NAME;
import org.harctoolbox.xml.XmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 *
 */
public final class RemoteLink implements Named, Serializable {
    private static final Logger logger = Logger.getLogger(RemoteDatabase.class.getName());

    public static final String REMOTELINK_ELEMENT_NAME = "remoteLink";
    public static final String PATH_ELEMENT_NAME = "path";
    public static final String XPATH_ATTRIBUTE_NAME = "xpath";
    public static final String KIND_ATTRIBUTE_NAME = "kind";
    private static final String URL_ATTRIBUTE_NAME = "url";

    private static void setAttributeIfNonNull(Element element, String attributeName, Object object) {
        if (object == null)
            return;
        String value = object.toString();
        if (value.isEmpty())
            return;
        element.setAttribute(attributeName, value);
    }

    private final RemoteKind kind;
    private String remoteName;
    private final File path;
    private String xpath;
    private final URL url;


    private final String comment;
    private final String displayName;
    private final String model;
    private final String origRemote;

    public RemoteLink(RemoteKind kind, URI baseUri, File baseDir, String remoteName, File path, String xpath, String comment, String displayName, String model, String origRemote) {
        try {
            this.kind = kind;
            switch (kind) {
                case irdb:
                    try {
                        Remote remote = Irdb.parse(path, null, null);
                        this.remoteName = remote.getName();
                    } catch (IOException ex) {
                        logger.log(Level.WARNING, ex.getLocalizedMessage());
                        this.remoteName = remoteName;
                    }
                    break;
                case lirc:
                    this.remoteName = remoteName.endsWith(".lircd.conf") ? remoteName.substring(0, remoteName.length() - 11) : remoteName;
                    break;

                default:
                    this.remoteName = remoteName;
            }
            if (baseDir != null) {
                Path baseDirPath = Paths.get(baseDir.getPath());
                Path localPath = Paths.get(path.getPath());
                this.path = baseDirPath.relativize(localPath).toFile();
            } else {
                this.path = path;
            }
            this.xpath = xpath;
            if (baseUri != null) {
                if (path != null) {
                    URI uri = new URI(FILE_SCHEME_NAME, this.path.toString() + kind.suffix(), null);
                    String escapedPath = uri.toString().substring(5);
                    uri = new URI(escapedPath);
                    url = baseUri.resolve(uri).toURL();
                } else
                    url = baseUri.toURL();
            } else {
                url = null;
            }
            this.comment = comment;
            this.displayName = displayName;
            this.model = model;
            this.origRemote = origRemote;
        } catch (URISyntaxException | MalformedURLException ex) {
            throw new ThisCannotHappenException(ex);
        }
    }

    public RemoteLink(Remote remote, URI baseUri, File baseDir, File file, String xpath) {
        this(RemoteKind.girr, baseUri, baseDir, remote.getName(), file, xpath, remote.getComment(), remote.getDisplayName(), remote.getModel(), remote.getRemoteName());
    }

    RemoteLink(RemoteKind kind, URI uri, File baseDir, File dir, String remote) {
        this(kind, uri, baseDir, remote, new File(dir, remote), null, null, null, null, null);
    }

    public Remote getRemote(String manufacturer, String deviceClass) throws IOException {
        switch (kind) {
            case irdb:
                return Irdb.parse(this, manufacturer, deviceClass);
            case girr:
                return getGirrRemote(manufacturer, deviceClass);
            case lirc:
                return getLircRemote(manufacturer, deviceClass);
            default:
                return null;
        }
    }

    @Override
    public String getName() {
        return remoteName;
    }

    public File getPath() {
        return path;
    }

    public String getXpath() {
        return xpath;
    }

    public Element toElement(Document document) {
        Element element = document.createElement(REMOTELINK_ELEMENT_NAME);
        element.setAttribute(NAME_ATTRIBUTE_NAME, remoteName);
        setAttributeIfNonNull(element, PATH_ELEMENT_NAME, path);
        setAttributeIfNonNull(element, KIND_ATTRIBUTE_NAME, kind.name());
        setAttributeIfNonNull(element, XPATH_ATTRIBUTE_NAME, xpath);
        setAttributeIfNonNull(element, COMMENT_ATTRIBUTE_NAME, comment);
        setAttributeIfNonNull(element, MODEL_ATTRIBUTE_NAME, model);
        setAttributeIfNonNull(element, URL_ATTRIBUTE_NAME, url);
        return element;
    }

    /**
     * @return the remoteName
     */
    public String getRemoteName() {
        return remoteName;
    }

    /**
     * @return the kind
     */
    public RemoteKind getKind() {
        return kind;
    }

    /**
     * @return the comment
     */
    public String getComment() {
        return comment;
    }

    /**
     * @return the displayName
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * @return the model
     */
    public String getModel() {
        return model;
    }

    /**
     * @return the origRemote
     */
    public String getOrigRemote() {
        return origRemote;
    }

    public URL getUrl() {
        return url;
    }

    private Remote getGirrRemote(String manufacturer, String deviceClass) throws IOException {
        try {
            Document doc = path.canRead() ? XmlUtils.openXmlFile(path, (Schema) null, false, false)
                    : XmlUtils.openXmlStream(url.openStream(), (Schema) null, false, false);
//            InputSource inputSource = new InputSource(inputStream);
            //XPathFactory factory = XPathFactory.newInstance();
            XPath xpathy = XPathFactory.newInstance().newXPath();
            NodeList str = (NodeList) xpathy.compile(xpath).evaluate(doc, XPathConstants.NODESET);
            Element el = (Element) str.item(0);
            return new Remote(el, "remote");
        } catch (XPathExpressionException | SAXException | GirrException ex) {
            logger.log(Level.WARNING, ex.getLocalizedMessage());
        }
        return null;
    }

    private Remote getLircRemote(String manufacturer, String deviceClass) throws IOException {
        RemoteSet remoteSet = path.canRead() ? ConfigFile.parseConfig(path, IrCoreUtils.EXTENDED_LATIN1_NAME, true, null, true)
                : ConfigFile.parseConfig(new InputStreamReader(url.openStream()), IrCoreUtils.EXTENDED_LATIN1_NAME, true, null, true);
        return remoteSet.iterator().hasNext() ? remoteSet.iterator().next() : null;
    }
}
