package org.harctoolbox.remotelocator;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Comparator;
import org.harctoolbox.girr.Named;
import org.harctoolbox.girr.Remote;
import org.harctoolbox.ircore.IrCoreUtils;
import org.harctoolbox.ircore.ThisCannotHappenException;
import org.xml.sax.SAXException;

public abstract class Scrapable {

    protected static URI parseURI(String string) {
        try {
            return new URI(string);
        } catch (URISyntaxException ex) {
            throw new ThisCannotHappenException(ex);
        }
    }

    protected static boolean isReadableDirectory(File file) {
        return file.isDirectory() && file.canRead();
    }

    protected static void assertReadableDirectory(File file) throws IOException {
        if (!isReadableDirectory(file))
            throw new IOException(file + " is not a readable directory");
    }

    static Scrapable mkScrapable(RemoteKind kind) {
        switch (kind) {
            case irdb:
                return new IrdbScrap(null);
            case girr:
                return new GirrScrap(null);
            case lirc:
                return new LircScrap(null);
            case jp1:
                return new Jp1Scrap(null);
            default:
                return null;
        }
    }
    //abstract RemoteLink newRemoteLink(Remote remote, URI uri, File baseDir, File file) throws IOException;

    public static Remote getRemoteStatic(RemoteLink remoteLink, String manufacturer, String deviceClass) throws IOException {
        Scrapable scrapable = mkScrapable(remoteLink.getKind());
        return scrapable.getRemote(remoteLink, manufacturer, deviceClass);
    }

    /**
     * This is typically shared between different instances of subclasses.
     *
     */
    protected final RemoteDatabase remoteDatabase;

    protected Scrapable(RemoteDatabase remoteDatabase) {
        this.remoteDatabase = remoteDatabase;
    }

    protected Scrapable() {
        this(new RemoteDatabase());
    }

    public Remote getRemote(RemoteLink remoteLink, String manufacturer, String deviceClass) throws IOException {
        File file = remoteLink.getFile();
        return file.canRead() ? getRemoteFile(remoteLink, manufacturer, deviceClass)
                : getRemoteUrl(remoteLink, manufacturer, deviceClass);
    }

    private Remote getRemoteFile(RemoteLink remoteLink, String manufacturer, String deviceClass) throws IOException {
        InputStreamReader reader = new InputStreamReader(new FileInputStream(remoteLink.getFile()), IrCoreUtils.EXTENDED_LATIN1);
        return getRemote(reader, remoteLink.getFile().getPath(), remoteLink.getXpath(), manufacturer, deviceClass);
    }

    private Remote getRemoteUrl(RemoteLink remoteLink, String manufacturer, String deviceClass) throws IOException {
        URLConnection conn = remoteLink.getUrl().openConnection();
        try (InputStream stream = conn.getInputStream()) {
            InputStreamReader reader = new InputStreamReader(stream, IrCoreUtils.EXTENDED_LATIN1);
            return getRemote(reader, remoteLink.getUrl().toString(), remoteLink.getXpath(), manufacturer, deviceClass);
        }
    }

    protected RemoteDatabase scrapSort(File file) throws IOException, SAXException {
        add(file);
        sort();
        return remoteDatabase;
    }

    public void sort() {
        sort(new Named.CompareNameCaseInsensitive());
    }

    public void sort(Comparator<? super Named> comparator) {
        remoteDatabase.sort(comparator);
    }

    public Remote getRemote(String manufacturer, String deviceClass, String remoteName) throws NotFoundException, IOException {
        return remoteDatabase.getRemote(manufacturer, deviceClass, remoteName);
    }

    public URL getUrl(String manufacturer, String deviceClass, String remoteName) throws NotFoundException, IOException {
        return remoteDatabase.getUrl(manufacturer, deviceClass, remoteName);
    }

    // TODO: nuke
//    RemoteDatabase getRemoteDatabase() {
//        return remoteDatabase;
//    }

    public abstract String getName();

//    public abstract RemoteKind getKind(); // TODO: nuke

    public abstract void add(File file) throws IOException, SAXException;

    public abstract Remote getRemote(InputStreamReader reader, String source, String xpath, String manufacturer, String deviceClass) throws IOException;
}
