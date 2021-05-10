package org.harctoolbox.remotelocator;

public enum ScrapKind {
    girr,
    irdb,
    lirc,
//    rmdu,
//    txt,
    jp1;

   static Scrapable mkScrapable(ScrapKind kind) {
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
}