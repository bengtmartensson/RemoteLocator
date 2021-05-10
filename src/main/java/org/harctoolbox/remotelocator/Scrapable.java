package org.harctoolbox.remotelocator;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Comparator;
import org.harctoolbox.girr.Named;
import org.harctoolbox.girr.Remote;
import org.xml.sax.SAXException;

public abstract class Scrapable {

    protected static boolean isReadableDirectory(File file) {
        return file.isDirectory() && file.canRead();
    }

    protected static void assertReadableDirectory(File file) throws IOException {
        if (!isReadableDirectory(file))
            throw new IOException(file + " is not a readable directory");
    }

    static Remote getRemoteStatic(RemoteLink remoteLink, String manufacturer, String deviceClass) throws IOException, NotGirrableException {
        Scrapable scrap = ScrapKind.mkScrapable(remoteLink.getKind());
        if (! (scrap instanceof Girrable))
            throw new NotGirrableException();

        return ((Girrable) scrap).getRemote(remoteLink, manufacturer, deviceClass);
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

    public URL getUrl(String manufacturer, String deviceClass, String remoteName) throws NotFoundException, IOException {
        return remoteDatabase.getUrl(manufacturer, deviceClass, remoteName);
    }

    public abstract String getName();

    public abstract void add(File file) throws IOException, SAXException;

    String formatUrl(String url) {
        return url;
    }
}
