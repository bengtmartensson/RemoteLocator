# RemoteLocator
This program helps the user to locate and download infrared remotes.
It operates on data bases of IR remotes. It either generates am XML index of download information,
or, using a previously generated such file, allows for easy download and conversion of therein registered remotes.
It can handle collections of remotes in the following formats:

* [IRDB format](https://github.com/probonopd/irdb): This collection was until recently available at the server `irdb.ik`, which is now defunct.
The IRDB format is a very simple CSV format ("the IT version of the stone axe"), containing command name, protocol and the device and subdevice parameters,
but no meta information. Instead, information on the manufacturer and the device class are gleaned from the file path.
* The [Lirc](http://lirc.org) collection of remotes, which are collected in the Sourceforge project [lirc-remotes](https://sourceforge.net/projects/lirc-remotes/).
This format also contains no meta information.
The manufacturer information is gleaned from the file path; however there are no information on device class,
so all the Lirc remotes have device class "unknown" in the generated list.
* [Girr](http://harctoolbox.org/Girr.html) is a very versatile format for IR remotes.
It is the native format for [IrScrutinizer](http://harctoolbox.org/IrScrutinizer.html), and also supported by main program
[RMIR](https://sourceforge.net/projects/controlremote/) of the [JP1 project](http://hifi-remote.com/forums/index.php).
Meta information, including manufacturer and device class, is contained in the Girr file.
There is a small collection ([GirrLib](https://github.com/bengtmartensson/GirrLib)),
which is more of a proof-of-concept than a sizeable collection of remotes.
* The JP1 project has a large collection of "device upgrades". These are summarized in
[an Excel file](http://www.hifi-remote.com/forums/dload.php?action=file&file_id=269991)
(or a later version thereof).
This list also contains meta information such as manufacturer and device class.
This can be read into the present program, and used to browse the therein contained remotes (or rather, "device upgrades").
These can, with some manual work, be translated to Girr files.
* [Flipper-IRDB](https://github.com/Lucaslhm/Flipper-IRDB). This is a large and fairly active collection of IR signals in the Flipper IR format (`*.ir`).
It is intended for the [Flipper Zero "Multi-tool Device for Geeks"](https://flipperzero.one), but since the format is a
[simple text based format](https://developer.flipper.net/flipperzero/doxygen/infrared_file_format.html), we can read that format too.
(In IrScrutinizer supported for import and export since early January 2025.)

Basically, there are three use case for the program:

1. Generaton of the index file, `remotelocator.xml`, and
2. Using said index file (possibly as an URL rather than a local file) for extracting information of its content,
3. Using the index file and information from 2. to downloading or browse a contained remote.

## Generation of the index file
Typically, only an "administrator" invokes this use case.
This is achieved by calling the main-routine of the RemoteDatabase class.
From (a subset of) the four sources, a local file is generated.
The `--out` option is (effectively) mandatory, and must point to a local (preferably non-existing) write-able file.
Using the options `--girrdir`, `--lircdir`, `--irdbdir` are used to point to a locally clone of the respective GitHub/Sourceforge repositories
(to the extent desired).
Use the option `--jp1file` to point to an OpenOffice format XML export of the JP1 master list.
(One such export is contained in the present project as  `src/test/jp1/jp1-master-1.17.fods`.)
If Lirc is not involved, this takes a few seconds. If Lirc _is_ included, this is slightly constlier (half a minute CPU time, 1 GB memory)
since all the commands in the (almost 3000) Lirc files have to be rendered and decoded.

### Global file
The current version of the complete file is available for download as
[`http://harctoolbox.org/downloads/remotelocator.xml`](http://harctoolbox.org/downloads/remotelocator-1.0.xml).

### HTML version of the remotelocator file
There exists an XSLT transformation `src/main/xslt/remotelocator2html.xsl` that turnes the XML file into a HTML file
containing clickable `a`-elements for download/browsing.
This is available for download and browsing as
[`http://harctoolbox.org/downloads/remotelocator-1.0.html`](http://harctoolbox.org/downloads/remotelocator-1.0.html).

## Gathering information from the file
For this use case, the argument of `--config` must be a valid and readable file or URL. By using the options
`--manufacturer`, `--deviceclass` (possibly with an argument of `?`) information on contained manufacturers, their device classes,
and the contained remotes can be queried.

## Downloading/browsing/converting remotes
For this use case, the argument of `--config` must be a correct, readable file or URL. By using the options
`--manufacturer`, `--deviceclass` and the name-less last argument, denoting the remote name, the corresponing remote can be fetched.
With the option `--browse` the remote is browse in the way the user's desktop is configured.
The option `--url` just prints the URL where the remote can be downloaded. Finally, the options `--Girr`, `--pronto` and `--csv`
prints the (possibly converted) remote in Girr, Pronto Hex or IRDB CSV-format respectivelly.

## Integration in IrScrutinizer
This program is integrated in IrScrutinizer version 2.3.1 and later, giving it a GUI.
I can be accessed as the pane `Import -> RemoteLocator`.
By selecting `Select me to load` a global index file file is downloaded, once per session.

## Appendix. All program options:
```
./remotelocator --help
Usage: RemoteLocator [options] Arguments to the program
  Options:
    -b, --browse
      Browse the remote instead of downloading it.
      Default: false
    -c, --config
      Name or URL of config file, to be read or written.
    --csv
      Produce output in IRDB CVS format.
      Default: false
    -d, --deviceclass
      Device class, "?" for list.
    -g, --girr
      Produce output in Girr format.
      Default: false
    -h, --help, -?
      Display help message.
      Default: false
    -k, --kind
      Only consider remotes of this kind.
      Possible Values: [girr, flipper, irdb, lirc, jp1]
    -m, --manufacturer
      Manufacturer, "?" for list.
    -o, --output
      File name to write to, "-" for stdout.
      Default: -
    -p, --prontohex
      Produce output in Pronto Hex format.
      Default: false
    -u, --url
      Do not get the remote, just print its url.
      Default: false
    -v, --version
      Display version information.
      Default: false
```
