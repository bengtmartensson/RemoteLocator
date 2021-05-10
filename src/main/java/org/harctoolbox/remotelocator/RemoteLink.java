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
import java.util.logging.Logger;
import org.harctoolbox.girr.Named;
import org.harctoolbox.girr.Remote;
import static org.harctoolbox.girr.XmlStatic.COMMENT_ATTRIBUTE_NAME;
import static org.harctoolbox.girr.XmlStatic.NAME_ATTRIBUTE_NAME;
import org.harctoolbox.ircore.ThisCannotHappenException;
import static org.harctoolbox.remotelocator.RemoteDatabase.FILE_SCHEME_NAME;
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
    private static final String URL_ATTRIBUTE_NAME = "url";
//    private static final String DISPLAYNAME_ATTRIBUTE_NAME = "displayName";
//    private static final String ORIGREMOTE_ATTRIBUTE_NAME = "origRemote";

    private static void setAttributeIfNonNull(Element element, String attributeName, Object object) {
        if (object == null)
            return;
        String value = object.toString();
        if (value.isEmpty())
            return;
        element.setAttribute(attributeName, value);
    }

    private final ScrapKind kind;
    private String name;
    private final File file;
    private String xpath;
    private final URL url;


    private final String comment;
//    private final String displayName;
//    private final String model;
//    private final String origRemote;

    public RemoteLink(ScrapKind kind, URI baseUri, File baseDir, String name, File file, String xpath, String comment, String displayName, String model, String origRemote) {
        try {
            Scrapable scrap = ScrapKind.mkScrapable(kind);
            this.kind = kind;
            this.name = name;
            if (baseDir != null) {
                Path baseDirPath = Paths.get(baseDir.getPath());
                Path localPath = Paths.get(file.getPath());
                this.file = baseDirPath.relativize(localPath).toFile();
            } else {
                this.file = file;
            }
            this.xpath = xpath;
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
            this.comment = comment;
//            this.displayName = displayName;
//            this.model = model;
//            this.origRemote = origRemote;
        } catch (URISyntaxException | MalformedURLException ex) {
            throw new ThisCannotHappenException(ex);
        }
    }

    public RemoteLink(ScrapKind kind, Remote remote, URI baseUri, File baseDir, File file, String xpath) {
        this(kind, baseUri, baseDir, remote.getName(), file, xpath, remote.getComment(), remote.getDisplayName(), remote.getModel(), remote.getRemoteName());
    }

    public RemoteLink(ScrapKind kind, Remote remote, URI baseUri, File baseDir, File file) {
        this(kind, remote, baseUri, baseDir, file, (String) null);
    }

    RemoteLink(ScrapKind kind, URI uri, File baseDir, File dir, String remote) {
        this(kind, uri, baseDir, remote, new File(dir, remote), null, null, null, null, null);
    }

    RemoteLink(Element remoteLinkElement) throws MalformedURLException {
        kind = ScrapKind.valueOf(remoteLinkElement.getAttribute(KIND_ATTRIBUTE_NAME));
        name = remoteLinkElement.getAttribute(NAME_ATTRIBUTE_NAME);
        file = new File(remoteLinkElement.getAttribute(PATH_ELEMENT_NAME));
        url = new URL(remoteLinkElement.getAttribute(URL_ATTRIBUTE_NAME));
        xpath = remoteLinkElement.getAttribute(XPATH_ATTRIBUTE_NAME);
        comment = remoteLinkElement.getAttribute(COMMENT_ATTRIBUTE_NAME);
//        model = remoteLinkElement.getAttribute(MODEL_ATTRIBUTE_NAME);
//        displayName = remoteLinkElement.getAttribute(DISPLAYNAME_ATTRIBUTE_NAME);
//        origRemote = remoteLinkElement.getAttribute(ORIGREMOTE_ATTRIBUTE_NAME);
    }

    public Remote getRemote(String manufacturer, String deviceClass) throws IOException, NotGirrableException {
        return Scrapable.getRemoteStatic(this, manufacturer, deviceClass);
    }

    @Override
    public String getName() {
        return name;
    }

    public File getFile() {
        return file;
    }

    public String getXpath() {
        return xpath;
    }

    public Element toElement(Document document) {
        Element element = document.createElement(REMOTELINK_ELEMENT_NAME);
        element.setAttribute(NAME_ATTRIBUTE_NAME, name);
        setAttributeIfNonNull(element, PATH_ELEMENT_NAME, file);
        setAttributeIfNonNull(element, KIND_ATTRIBUTE_NAME, kind.name());
        setAttributeIfNonNull(element, XPATH_ATTRIBUTE_NAME, xpath);
        setAttributeIfNonNull(element, COMMENT_ATTRIBUTE_NAME, comment);
        setAttributeIfNonNull(element, URL_ATTRIBUTE_NAME, url);
//        setAttributeIfNonNull(element, MODEL_ATTRIBUTE_NAME, model);
//        setAttributeIfNonNull(element, DISPLAYNAME_ATTRIBUTE_NAME, displayName);
//        setAttributeIfNonNull(element, ORIGREMOTE_ATTRIBUTE_NAME, origRemote);
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
        return comment;
    }

//    /**
//     * @return the displayName
//     */
//    public String getDisplayName() {
//        return displayName;
//    }
//
//    /**
//     * @return the model
//     */
//    public String getModel() {
//        return model;
//    }
//
//    /**
//     * @return the origRemote
//     */
//    public String getOrigRemote() {
//        return origRemote;
//    }

    public URL getUrl() {
        return url;
    }
}
