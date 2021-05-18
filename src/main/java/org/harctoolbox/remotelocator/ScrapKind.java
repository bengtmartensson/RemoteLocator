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

public enum ScrapKind {
    girr,
    irdb,
    lirc,
    jp1;

   private static Scrapable mkScrapable(RemoteDatabase remoteDatabase, ScrapKind kind) {
        switch (kind) {
            case irdb:
                return new IrdbScrap(remoteDatabase);
            case girr:
                return new GirrScrap(remoteDatabase);
            case lirc:
                return new LircScrap(remoteDatabase);
            case jp1:
                return new Jp1Scrap(remoteDatabase);
            default:
                return null;
        }
    }

   static Scrapable mkScrapable(RemoteLink remoteLink) {
       return mkScrapable(remoteLink.getRemoteDatabase(), remoteLink.getKind());
   }

    static Scrapable mkScrapable(ScrapKind kind) {
        return mkScrapable(null, kind);
    }
}