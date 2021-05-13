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

import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import java.awt.Desktop;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.harctoolbox.girr.Command;
import org.harctoolbox.girr.CommandSet;
import org.harctoolbox.girr.GirrException;
import org.harctoolbox.girr.Remote;
import org.harctoolbox.ircore.IrCoreException;
import org.harctoolbox.ircore.IrCoreUtils;
import org.harctoolbox.irp.IrpException;
import static org.harctoolbox.irp.IrpUtils.EXIT_SUCCESS;
import static org.harctoolbox.irp.IrpUtils.EXIT_USAGE_ERROR;
import org.xml.sax.SAXException;

public class RemoteLocator {
    private static final Logger logger = Logger.getLogger(RemoteLocator.class.getName());

    private static JCommander argumentParser;
    private static final CommandLineArgs commandLineArgs = new CommandLineArgs();
    private static final String APP_NAME = "RemoteLocator";
    private static final String VERSION = "0.2.0";
    private static final String VERSION_STRING = APP_NAME + " version " + VERSION;
    private static RemoteDatabase remoteDatabase;
    private static final Object QUESTIONMARK = "?";
    private static PrintStream out;

    private static void usage(int exitcode) {
        StringBuilder str = new StringBuilder(256);
        argumentParser.usage();

        (exitcode == EXIT_SUCCESS ? System.out : System.err).println(str);
        doExit(exitcode); // placifying FindBugs...
    }

    private static void die(String message, int exitCode) {
        System.err.println(message);
        System.exit(exitCode);
    }

    private static void doExit(int exitcode) {
        System.exit(exitcode);
    }

    /**
     * @param args the command line arguments.
     */
    public static void main(String[] args) {
        argumentParser = new JCommander(commandLineArgs);
        argumentParser.setProgramName(APP_NAME);
        argumentParser.setAllowAbbreviatedOptions(true);
        argumentParser.setCaseSensitiveOptions(true);

        try {
            argumentParser.parse(args);
        } catch (ParameterException ex) {
            System.err.println(ex.getMessage());
            usage(EXIT_USAGE_ERROR);
        }

        if (commandLineArgs.helpRequested)
            usage(EXIT_SUCCESS);

        if (commandLineArgs.versionRequested) {
            System.out.println(VERSION_STRING);
            System.out.println("JVM: " + System.getProperty("java.vendor") + " " + System.getProperty("java.version")
                    + " " + System.getProperty("os.name") + "-" + System.getProperty("os.arch"));
            //System.out.println();
            //System.out.println(Version.licenseString);
            System.exit(EXIT_SUCCESS);
        }

        try {
            boolean readStuff = readScrapers();

            if (!readStuff)
                setupRemoteDatabaseFromConfig();

            if (remoteDatabase.isEmpty())
                die("No database content", EXIT_USAGE_ERROR);

            if (commandLineArgs.sort)
                remoteDatabase.sort();

            if (readStuff) {
                remoteDatabase.print(commandLineArgs.config);
                System.err.println("Configuration file " + commandLineArgs.config + " written.");
            }

            out = IrCoreUtils.getPrintStream(commandLineArgs.output);

            if (commandLineArgs.manufacturer != null)
                processManufacturer();
        } catch (NotFoundException | URISyntaxException | IOException | GirrException | IrpException | IrCoreException ex) {
            //logger.log(Level.SEVERE, null, ex);
            die(ex.getLocalizedMessage(), EXIT_USAGE_ERROR);
        } catch (SAXException | RemoteDatabase.FormatVersionMismatchException ex) {
            Logger.getLogger(RemoteLocator.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private static boolean isQuestion(String string) {
        return string.equals(QUESTIONMARK);
    }

    private static void setupRemoteDatabaseFromConfig() throws IOException, SAXException, RemoteDatabase.FormatVersionMismatchException {
        if (commandLineArgs.config != null)
            try {
                remoteDatabase = new RemoteDatabase(commandLineArgs.config);
                return;
            } catch (FileNotFoundException ex) {
            }

        remoteDatabase = new RemoteDatabase();
    }

    private static boolean readScrapers() throws IOException, SAXException {
        remoteDatabase = new RemoteDatabase();
        boolean doneStuff = false;

        if (commandLineArgs.girrDir != null) {
            new GirrScrap(remoteDatabase).add(new File(commandLineArgs.girrDir));
            doneStuff = true;
        }

        if (commandLineArgs.irdbDir != null) {
            new IrdbScrap(remoteDatabase).add(new File(commandLineArgs.irdbDir));
            doneStuff = true;
        }

        if (commandLineArgs.lircDir != null) {
            new LircScrap(remoteDatabase).add(new File(commandLineArgs.lircDir));
            doneStuff = true;
        }

        if (commandLineArgs.jp1File != null) {
            new Jp1Scrap(remoteDatabase).add(new File(commandLineArgs.jp1File));
            doneStuff = true;
        }
        return doneStuff;
    }

    private static void processManufacturer() throws NotFoundException, URISyntaxException, IOException, GirrException, IrpException, IrCoreException {
        if (isQuestion(commandLineArgs.manufacturer)) {
            List<String> manufacturers = remoteDatabase.getManufacturers();
            manufacturers.forEach(m -> {
                out.println(m);
            });
            return;
        }
        processDevices();
    }

    private static void processDevices() throws NotFoundException, URISyntaxException, IOException, GirrException, IrpException, IrCoreException {
        if (commandLineArgs.deviceClass == null || isQuestion(commandLineArgs.deviceClass)) {
            List<String> devices = remoteDatabase.getDeviceTypes(commandLineArgs.manufacturer);
            devices.forEach(dc -> {
                out.println(dc);
            });
            return;
        }
        processRemotes();
    }

    private static void processRemotes() throws NotFoundException, URISyntaxException, IOException, GirrException, IrpException, IrCoreException {
        if (commandLineArgs.remoteNames.isEmpty() || isQuestion(commandLineArgs.remoteNames.get(0))) {
            List<String> remotes = remoteDatabase.getRemotes(commandLineArgs.manufacturer, commandLineArgs.deviceClass);
            remotes.forEach(r -> {
                out.println(r);
            });
        } else
            processRemote();
    }

    private static void processRemote() throws NotFoundException, URISyntaxException, IOException, GirrException, IrpException, IrCoreException {
        if (!commandLineArgs.doUrl && !commandLineArgs.browse) {
            try {
                RemoteLink remoteLink = remoteDatabase.getRemoteLink(commandLineArgs.manufacturer, commandLineArgs.deviceClass, commandLineArgs.remoteNames.get(0));
                processRemoteLink(remoteLink);
                return;
            } catch (Girrable.NotGirrableException ex) {
                logger.log(Level.WARNING, "Not girr-able, just giving the URL");
            }
        }

        URL url = remoteDatabase.getUrl(commandLineArgs.manufacturer, commandLineArgs.deviceClass, commandLineArgs.remoteNames.get(0));
        if (commandLineArgs.browse)
            Desktop.getDesktop().browse(url.toURI());
        else
            out.println(url.toString());
    }

    private static void processRemoteLink(RemoteLink remoteLink) throws GirrException, IrpException, IrCoreException, IOException {
        boolean doneStuff = false;
        Remote remote;
        try {
            remote = remoteLink.getRemote(commandLineArgs.manufacturer, commandLineArgs.deviceClass);
        } catch (Girrable.NotGirrableException ex) {
             System.err.println("Remote found, but can only be browsed.");
             return;
        }
        if (commandLineArgs.girr) {
            remote.print(out, true, true, true);
            doneStuff = true;
        }
        if (commandLineArgs.prontoHex) {
            for (CommandSet cs : remote) {
                for (Command c : cs) {
                    out.println(c.getName());
                    out.println(c.getProntoHex());
                    out.println();
                }
            }
            doneStuff = true;
        }
        if (commandLineArgs.cvs) {
            IrdbScrap.print(out, remote);
            doneStuff = true;
        }
        if (!doneStuff) {
            System.err.println("Remote of type " + remoteLink.getKind() + " found; use --Girr, --pronto or --csv to get output requested.");
        }
    }

    private final static class CommandLineArgs {

       @Parameter(names = {"-c", "--config"}, description = "Name or URL of config file, to be read or written.")
       String config = null;

       @Parameter(names = {"--girrdir"}, description = "Pathname of directory (recursively) containing Girr files.")
       public String girrDir = null;

       @Parameter(names = {"-i", "--irdbdir"}, description = "Pathname of directory containing IRDB files in CSV format.")
       public String irdbDir = null;

       @Parameter(names = {"-l", "--lircdirs"}, description = "Pathname of directory containing Lirc files.")
       public String lircDir = null;

       @Parameter(names = {"-j", "--jp1file"}, description = "Filename of XML export of JP1 master file.")
       public String jp1File = null;

       @Parameter(names = {"-h", "--help", "-?"}, description = "Display help message.")
       private boolean helpRequested = false;

       @Parameter(names = {"-s", "--sort"}, description = "Sort the configuration file before writing.")
       public boolean sort = false;

       @Parameter(names = {"-v", "--version"}, description = "Display version information.")
       private boolean versionRequested;

       @Parameter(names = {"-o", "--output"}, description = "File name to write to, \"-\" for stdout.")
       private String output = "-";

       @Parameter(names = {"-m", "--manufacturer"}, description = "Manufacturer, \"?\" for list.")
       private String manufacturer = null;

       @Parameter(names = {"-d", "--deviceclass"}, description = "Device class, \"?\" for list.")
       private String deviceClass = null;

       @Parameter(names = {"-g", "--Girr"}, description = "Produce output in Girr format.")
       private boolean girr = false;

       @Parameter(names = {"-p", "--prontohex"}, description = "Produce output in Pronto Hex format.")
       private boolean prontoHex = false;

       @Parameter(names = {"--csv"}, description = "Produce output in IRDB CVS format.")
       private boolean cvs = false;

       @Parameter(names = {"-u", "--url"}, description = "Do not get the remote, just print its url.")
       private boolean doUrl = false;

        @Parameter(names = {"-b", "--browse"}, description = "Browse the remote, do not directly download it.")
        private boolean browse = false;

        @Parameter(description = "Arguments to the program")
        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        private List<String> remoteNames = new ArrayList<>(4);
    }
}
