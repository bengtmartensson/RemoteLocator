
package org.harctoolbox.remotelocator;

import java.io.File;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.harctoolbox.girr.Named;
import org.harctoolbox.girr.Remote;
import static org.harctoolbox.girr.XmlStatic.COMMENT_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlStatic.MODEL_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlStatic.NAME_ATTRIBUTE_NAME;
import org.harctoolbox.ircore.ThisCannotHappenException;
import static org.harctoolbox.remotelocator.RemoteDatabase.FILE_SCHEME_NAME;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 */
public final class RemoteLink implements Named, Serializable {

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
    private final String remoteName;
    private final File path;
    private final String xpath;
    private final URL url;


    private final String comment;
    private final String displayName;
    private final String model;
    private final String origRemote;

    public RemoteLink(RemoteKind kind, URI baseUri, File baseDir, String remoteName, File path, String xpath, String comment, String displayName, String model, String origRemote) {
        try {
            this.kind = kind;
            this.remoteName = remoteName;
            if (baseDir != null) {
                Path baseDirPath = Paths.get(baseDir.getPath());
                Path localPath = Paths.get(path.getPath());
                this.path = baseDirPath.relativize(localPath).toFile();
            } else {
                this.path = path;
            }
            this.xpath = xpath;
            if (baseUri != null) {
                URI uri = new URI(FILE_SCHEME_NAME, this.path.toString() + kind.suffix(), null);
                String escapedPath = uri.toString().substring(5);
                uri = new URI(escapedPath);
                url = baseUri.resolve(uri).toURL();
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
        element.setAttribute(PATH_ELEMENT_NAME, path.toString());
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

}
