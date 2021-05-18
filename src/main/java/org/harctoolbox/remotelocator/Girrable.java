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
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLConnection;
import org.harctoolbox.girr.Remote;
import static org.harctoolbox.ircore.IrCoreUtils.EXTENDED_LATIN1;

public abstract class Girrable extends Scrapable {

    Girrable(RemoteDatabase remoteDatabase) {
        super(remoteDatabase);
    }

    Girrable() {
        super();
    }

    public Remote getRemote(RemoteLink remoteLink) throws IOException {
        File file = remoteLink.getFile();
        return file.canRead() ? getRemoteFile(remoteLink) : getRemoteUrl(remoteLink);
    }

    private Remote getRemoteFile(RemoteLink remoteLink) throws IOException {
        try (InputStreamReader reader = new InputStreamReader(new FileInputStream(remoteLink.getFile()), EXTENDED_LATIN1)) {
            return getRemote(reader, remoteLink.getFile().getPath(), remoteLink.getXpath(), remoteLink.getManufacturer(), remoteLink.getDeviceClass());
        }
    }

    private Remote getRemoteUrl(RemoteLink remoteLink) throws IOException {
        URLConnection conn = remoteLink.getUrl().openConnection();
        try (InputStream stream = conn.getInputStream()) {
            InputStreamReader reader = new InputStreamReader(stream, EXTENDED_LATIN1);
            return getRemote(reader, remoteLink.getUrl().toString(), remoteLink.getXpath(), remoteLink.getManufacturer(), remoteLink.getDeviceClass());
        }
    }

    public Remote getRemote(String manufacturer, String deviceClass, String remoteName) throws NotFoundException, IOException, NotGirrableException {
        return remoteDatabase.getRemote(manufacturer, deviceClass, remoteName);
    }

    public abstract Remote getRemote(InputStreamReader reader, String source, String xpath, String manufacturer, String deviceClass) throws IOException;

    @SuppressWarnings("PackageVisibleInnerClass")
    static class NotGirrableException extends Exception {

        NotGirrableException() {
        }
    }
}
