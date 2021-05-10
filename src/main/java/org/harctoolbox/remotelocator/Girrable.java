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

    public Remote getRemote(RemoteLink remoteLink, String manufacturer, String deviceClass) throws IOException {
        File file = remoteLink.getFile();
        return file.canRead() ? getRemoteFile(remoteLink, manufacturer, deviceClass)
                : getRemoteUrl(remoteLink, manufacturer, deviceClass);
    }

    private Remote getRemoteFile(RemoteLink remoteLink, String manufacturer, String deviceClass) throws IOException {
        InputStreamReader reader = new InputStreamReader(new FileInputStream(remoteLink.getFile()), EXTENDED_LATIN1);
        return getRemote(reader, remoteLink.getFile().getPath(), remoteLink.getXpath(), manufacturer, deviceClass);
    }

    private Remote getRemoteUrl(RemoteLink remoteLink, String manufacturer, String deviceClass) throws IOException {
        URLConnection conn = remoteLink.getUrl().openConnection();
        try (InputStream stream = conn.getInputStream()) {
            InputStreamReader reader = new InputStreamReader(stream, EXTENDED_LATIN1);
            return getRemote(reader, remoteLink.getUrl().toString(), remoteLink.getXpath(), manufacturer, deviceClass);
        }
    }

    public Remote getRemote(String manufacturer, String deviceClass, String remoteName) throws NotFoundException, IOException, NotGirrableException {
        return remoteDatabase.getRemote(manufacturer, deviceClass, remoteName);
    }

    public abstract Remote getRemote(InputStreamReader reader, String source, String xpath, String manufacturer, String deviceClass) throws IOException;
}
