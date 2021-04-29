
package org.harctoolbox.remotelocator;

import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.harctoolbox.girr.Named;
import org.harctoolbox.girr.Remote;
import static org.harctoolbox.girr.XmlStatic.COMMENT_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlStatic.MODEL_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlStatic.NAME_ATTRIBUTE_NAME;
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

    private static void setAttributeIfNonNull(Element element, String attributeName, String value) {
        if (value == null || value.isEmpty())
            return;
        element.setAttribute(attributeName, value);
    }

    private final RemoteKind kind;
    private final String remoteName;
    private final Path path; // not serializable
    private final String xpath;


    private final String comment;
    private final String displayName;
    private final String model;
    private final String origRemote;

    public RemoteLink(RemoteKind kind, String remoteName, Path path, String xpath, String comment, String displayName, String model, String origRemote) {
        this.kind = kind;
        this.remoteName = remoteName;
        this.path = RemoteDatabase.relativize(path);
        this.xpath = xpath;
        this.comment = comment;
        this.displayName = displayName;
        this.model = model;
        this.origRemote = origRemote;
    }

    public RemoteLink(RemoteKind kind, String remoteName, String path) {
        this(kind, remoteName, Paths.get(path), null, null, null, null, null);
    }

    public RemoteLink(Remote remote, Path path, String xpath) {
        this(RemoteKind.girr, remote.getName(), path, xpath, remote.getComment(), remote.getDisplayName(), remote.getModel(), remote.getRemoteName());
    }

    RemoteLink(RemoteKind kind, File dir, String remote) {
        this(kind, remote, new File(dir, remote).toPath(), null, null, null, null, null);
    }

    RemoteLink(Remote remote, String path, String xpath) {
        this(remote, Paths.get(path), xpath);
    }

    @Override
    public String getName() {
        return remoteName;
    }

    public Path getPath() {
        return path;
    }

    public String getXpath() {
        return xpath;
    }

    public Element toElement(Document document) {
        Element element = document.createElement(REMOTELINK_ELEMENT_NAME);
        element.setAttribute(NAME_ATTRIBUTE_NAME, remoteName);
        //String shortUri = RemoteDatabase.relativize(path).toString();
        element.setAttribute(PATH_ELEMENT_NAME, path.toString());
        setAttributeIfNonNull(element, KIND_ATTRIBUTE_NAME, kind.name());
        setAttributeIfNonNull(element, XPATH_ATTRIBUTE_NAME, xpath);
        setAttributeIfNonNull(element, COMMENT_ATTRIBUTE_NAME, comment);
        setAttributeIfNonNull(element, MODEL_ATTRIBUTE_NAME, model);

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
