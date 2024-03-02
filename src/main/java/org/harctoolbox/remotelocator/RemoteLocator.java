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

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.ParameterException;
import java.awt.Desktop;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.harctoolbox.girr.Command;
import org.harctoolbox.girr.CommandSet;
import org.harctoolbox.girr.GirrException;
import org.harctoolbox.girr.Remote;
import org.harctoolbox.ircore.IrCoreException;
import org.harctoolbox.ircore.IrCoreUtils;
import org.harctoolbox.irp.IrpException;
import static org.harctoolbox.irp.IrpUtils.EXIT_SUCCESS;
import static org.harctoolbox.irp.IrpUtils.EXIT_USAGE_ERROR;
import static org.harctoolbox.remotelocator.RemoteDatabase.APP_NAME;
import static org.harctoolbox.remotelocator.RemoteDatabase.VERSION_STRING;
import org.xml.sax.SAXException;

@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class RemoteLocator {
    //private static final Logger logger = Logger.getLogger(RemoteLocator.class.getName());

    private static JCommander argumentParser;
    private static final CommandLineArgs commandLineArgs = new CommandLineArgs();
    private static RemoteDatabase remoteDatabase;
    private static final String QUESTIONMARK = "?";
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
        argumentParser.setCaseSensitiveOptions(false);

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
            System.exit(EXIT_SUCCESS);
        }

        if (commandLineArgs.config == null) {
            System.err.println("--config|-c is a required argument");
            usage(EXIT_USAGE_ERROR);
        }

        try {
            remoteDatabase = new RemoteDatabase(commandLineArgs.config);
            if (remoteDatabase.isEmpty())
                die("No database content", EXIT_USAGE_ERROR);

            out = IrCoreUtils.getPrintStream(commandLineArgs.output);
            processManufacturer();
        } catch (NotFoundException | URISyntaxException | IOException | GirrException | IrpException | IrCoreException | SAXException | RemoteDatabase.FormatVersionMismatchException ex) {
            die(ex.getLocalizedMessage(), EXIT_USAGE_ERROR);
        }
    }

    private static boolean isQuestion(String string) {
        return string.equals(QUESTIONMARK);
    }

    private static void processManufacturer() throws NotFoundException, URISyntaxException, IOException, GirrException, IrpException, IrCoreException {
        if (commandLineArgs.manufacturer == null || isQuestion(commandLineArgs.manufacturer)) {
            List<String> manufacturers = remoteDatabase.getManufacturers(commandLineArgs.kind);
            manufacturers.forEach(m -> {
                out.println(m);
            });
            return;
        }
        processDevices();
    }

    private static void processDevices() throws NotFoundException, URISyntaxException, IOException, GirrException, IrpException, IrCoreException {
        if (commandLineArgs.deviceClass == null || isQuestion(commandLineArgs.deviceClass)) {
            List<String> devices = remoteDatabase.getDeviceTypes(commandLineArgs.kind, commandLineArgs.manufacturer);
            devices.forEach(dc -> {
                out.println(dc);
            });
            return;
        }
        processRemotes();
    }

    private static void processRemotes() throws NotFoundException, URISyntaxException, IOException, GirrException, IrpException, IrCoreException {
        if (commandLineArgs.remoteNames.isEmpty() || isQuestion(commandLineArgs.remoteNames.get(0))) {
            List<String> remotes = remoteDatabase.getRemotes(commandLineArgs.kind, commandLineArgs.manufacturer, commandLineArgs.deviceClass);
            remotes.forEach(r -> {
                out.println(r);
            });
        } else
            processRemote();
    }

    private static void processRemote() throws NotFoundException, URISyntaxException, IOException, GirrException, IrpException, IrCoreException {
        if (!commandLineArgs.doUrl && !commandLineArgs.browse) {
            RemoteLink remoteLink = remoteDatabase.get(commandLineArgs.manufacturer, commandLineArgs.deviceClass, commandLineArgs.remoteNames.get(0));
            processRemoteLink(remoteLink);
            return;
        }

        URL url = remoteDatabase.getUrl(commandLineArgs.manufacturer, commandLineArgs.deviceClass, commandLineArgs.remoteNames.get(0));
        if (commandLineArgs.browse)
            Desktop.getDesktop().browse(url.toURI());
        else
            out.println(url.toString());
    }

    private static void processRemoteLink(RemoteLink remoteLink) throws GirrException, IrpException, IrCoreException, IOException, NotFoundException {
        if (commandLineArgs.kind != null && remoteLink.getKind() != commandLineArgs.kind) {
            throw new NotFoundException("Remote of kind " + remoteLink.getKind() + " found. but kind " + commandLineArgs.kind.toString() + " was requested.");
        }

        boolean doneStuff = false;
        Remote remote;
        try {
            remote = remoteLink.getRemote();
        } catch (Girrable.NotGirrableException ex) {
            System.err.println("Remote found, but can only be browsed (using --browse).");
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
            System.err.println("Remote of type " + remoteLink.getKind() + " found; use --girr, --pronto or --csv to request output.");
        }
    }

    private RemoteLocator() {
    }

    private final static class CommandLineArgs {

        @Parameter(names = {"-b", "--browse"}, description = "Browse the remote instead of downloading it.")
        private boolean browse = false;

        @Parameter(names = {"-c", "--config"}, description = "Name or URL of config file, to be read or written.")
        String config = null;

        @Parameter(names = {"--csv"}, description = "Produce output in IRDB CVS format.")
        private boolean cvs = false;

        @Parameter(names = {"-d", "--deviceclass"}, description = "Device class, \"?\" for list.")
        private String deviceClass = null;

        @Parameter(names = {"-g", "--girr"}, description = "Produce output in Girr format.")
        private boolean girr = false;

        @Parameter(names = {"-h", "--help", "-?"}, description = "Display help message.")
        private boolean helpRequested = false;

        @Parameter(names = {"-k", "--kind"}, description = "Only consider remotes of this kind.",
        converter = ScrapKindParser.class)
        private ScrapKind kind = null;

        @Parameter(names = {"-m", "--manufacturer"}, description = "Manufacturer, \"?\" for list.")
        private String manufacturer = null;

        @Parameter(names = {"-o", "--output"}, description = "File name to write to, \"-\" for stdout.")
        private String output = "-";

        @Parameter(names = {"-p", "--prontohex"}, description = "Produce output in Pronto Hex format.")
        private boolean prontoHex = false;

        @Parameter(names = {"-u", "--url"}, description = "Do not get the remote, just print its url.")
        private boolean doUrl = false;

        @Parameter(names = {"-v", "--version"}, description = "Display version information.")
        private boolean versionRequested;

        @Parameter(description = "Arguments to the program")
        @SuppressWarnings("MismatchedQueryAndUpdateOfCollection")
        private List<String> remoteNames = new ArrayList<>(4);
    }

    private static class ScrapKindParser implements IStringConverter<ScrapKind> {

        @Override
        public ScrapKind convert(String value) {
            try {
                return ScrapKind.valueOf(value.toLowerCase(Locale.US));
            } catch (IllegalArgumentException ex) {
                throw new ParameterException(ex);
            }
        }
    }
}
