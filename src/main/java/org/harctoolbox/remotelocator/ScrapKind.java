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