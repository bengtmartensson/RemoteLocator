package org.harctoolbox.remotelocator;

public enum RemoteKind {
    girr,
    irdb,
    lirc,
    rmdu,
    txt,
    jp1;

    // TODO: remove
    String suffix() {
        return this == lirc ? "?format=raw" : "";
    }
}
