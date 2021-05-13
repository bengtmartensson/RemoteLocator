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

    static Remote getRemoteStatic(RemoteLink remoteLink, String manufacturer, String deviceClass) throws IOException, Girrable.NotGirrableException {
        Scrapable scrap = ScrapKind.mkScrapable(remoteLink.getKind());
        if (! (scrap instanceof Girrable))
            throw new Girrable.NotGirrableException();

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
