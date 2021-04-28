
package org.harctoolbox.remotelocator;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import org.harctoolbox.girr.Named;
import org.harctoolbox.girr.Remote;
import static org.harctoolbox.girr.XmlStatic.COMMENT_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlStatic.MODEL_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlStatic.NAME_ATTRIBUTE_NAME;
import org.harctoolbox.ircore.ThisCannotHappenException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 *
 */
public final class RemoteLink implements Named, Serializable {

    public static final String REMOTELINK_ELEMENT_NAME = "remoteLink";
    public static final String URL_ELEMENT_NAME = "url";
    public static final String XPATH_ATTRIBUTE_NAME = "xpath";
    public static final String KIND_ATTRIBUTE_NAME = "kind";

    private static void setAttributeIfNonNull(Element element, String attributeName, String value) {
        if (value == null || value.isEmpty())
            return;
        element.setAttribute(attributeName, value);
    }

    private static String mkUrl(File dir, String remote) {
//        try {
            return new File(dir, remote).toString();
//        } catch (IOException ex) {
//            throw new ThisCannotHappenException();
//        }
    }

    private final RemoteKind kind;
    private final String remoteName;
    private final String url;
    private final String xpath;


    private final String comment;
    private final String displayName;
    private final String model;
    private final String origRemote;

    public RemoteLink(RemoteKind kind, String remoteName, String url, String xpath, String comment, String displayName, String model, String origRemote) {
        this.kind = kind;
        this.remoteName = remoteName;
        this.url = url;
        this.xpath = xpath;
        this.comment = comment;
        this.displayName = displayName;
        this.model = model;
        this.origRemote = origRemote;
    }

    public RemoteLink(RemoteKind kind, String remoteName, String url) {
        this(kind, remoteName, url, null, null, null, null, null);
    }

    public RemoteLink(Remote remote, String url, String xpath) {
        this(RemoteKind.girr, remote.getName(), url, xpath, remote.getComment(), remote.getDisplayName(), remote.getModel(), remote.getRemoteName());
    }

    RemoteLink(RemoteKind kind, File dir, String remote) {
        this(kind, remote, mkUrl(dir, remote), null, null, null, null, null);
    }

    @Override
    public String getName() {
        return remoteName;
    }

    public String getUrl() {
        return url;
    }

    public String getXpath() {
        return xpath;
    }

    public Element toElement(Document document) {
        Element element = document.createElement(REMOTELINK_ELEMENT_NAME);
        element.setAttribute(NAME_ATTRIBUTE_NAME, remoteName);
        String shortUri = RemoteDatabase.relativize(url);
        element.setAttribute(URL_ELEMENT_NAME, shortUri);
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
