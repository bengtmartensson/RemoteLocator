
package org.harctoolbox.remotelocator;

public enum RemoteKind {
    girr,
    irdb,
    lirc,
    rmdu,
    txt;

    public boolean hasDeviceClasses() {
        return this != lirc;
    }

    public boolean recurse() {
        return this == girr;
    }

    public String suffix() {
        return this == lirc ? "?format=raw" : "";
    }
}
