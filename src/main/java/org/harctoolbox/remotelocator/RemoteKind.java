
package org.harctoolbox.remotelocator;

public enum RemoteKind {
    girr,
    cvs,
    lirc;

    public boolean hasDeviceClasses() {
        return this != lirc;
    }

    public boolean recurse() {
        return this == girr;
    }
}
