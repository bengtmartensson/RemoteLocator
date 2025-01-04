/*
Copyright (C) 2025 Bengt Martensson.

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

import java.io.FileReader;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.text.ParseException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.harctoolbox.girr.Command;
import org.harctoolbox.girr.CommandSet;
import org.harctoolbox.girr.GirrException;
import org.harctoolbox.ircore.IrSequence;
import org.harctoolbox.ircore.IrSignal;
import org.harctoolbox.ircore.ModulatedIrSequence;
import org.harctoolbox.ircore.OddSequenceLengthException;
import org.harctoolbox.ircore.ThisCannotHappenException;
import org.harctoolbox.xml.XmlUtils;
import org.w3c.dom.Document;

/**
 * This class has only one public functions: parse(Reader), which delivers
 * a CommandSet from a Reader.
 */
public final class FlipperParser {

    private static final Logger logger = Logger.getLogger(FlipperParser.class.getName());

    // Using a LinkedHashMap preserves the order of the commands.
    private final Map<String, Command> commands = new LinkedHashMap<>(INITIAL_CAPACITY);
    private static final int INITIAL_CAPACITY = 64;
    private static final String HEADER1 = "Filetype: IR signals file";
    private static final String HEADER2 = "Version: 1";
    private static final Double DUMMY_ENDING_GAP = 50000.0;
    private static final char COMMENTCHAR = '#';

    /**
     * Parses an *.ir file and returns a CommandSet (if successfull).
     * This is the only public function of the class.
     *
     * @param reader
     * @return CommandSet
     * @throws IOException
     * @throws ParseException
     * @throws GirrException
     */
    public static CommandSet parse(Reader reader) throws IOException, ParseException, GirrException {
        FlipperParser parser = new FlipperParser(reader);
        return new CommandSet("commandSet", null, parser.commands, null, null);
    }

    // For testing and debugging only
    public static void main(String[] args) {
        try {
            FileReader reader = new FileReader(args[0]);
            CommandSet commandSet = parse(reader);
            Document doc = commandSet.toDocument("Parsed Flipper IR file " + args[0], false, true, false, false);
            XmlUtils.printDOM(doc);
        } catch (IOException | ParseException | GirrException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }

    private FlipperParser(Reader r) throws IOException, ParseException, GirrException {
        LineNumberReader reader = new LineNumberReader(r);

        checkHeader(reader);
        while (true) {
            Command command = parseCommand(reader);
            if (command == null) {
                break;
            }
            commands.put(command.getName(), command);
        }
    }

    private void checkHeader(LineNumberReader reader) throws IOException, ParseException {
        checkLine(reader, HEADER1);
        checkLine(reader, HEADER2);
        reader.readLine();
    }

    private void checkLine(LineNumberReader reader, String expected) throws IOException, ParseException {
        String line = nonCommentLineRead(reader);
        if (!line.equals(expected)) {
            throw new ParseException(line, reader.getLineNumber());
        }
    }

    private String nonCommentLineRead(LineNumberReader reader) throws IOException {
        String line;
        do {
            line = reader.readLine();
        } while (isComment(line));
        return line;
    }

    private Command parseCommand(LineNumberReader reader) throws IOException, ParseException, GirrException {
        Map<String, String> map = new HashMap<>(16);
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                if (map.isEmpty()) {
                    return null;
                } else {
                    break;
                }
            }
            if (isComment(line)) {
                if (map.isEmpty() || line.trim().length() > 1) {
                    continue;
                } else {
                    break;
                }
            }
            String[] chunks = line.split(":\\s+");
            map.put(chunks[0], chunks[1]);
        }

        Command command = null;
        switch (map.get("type")) {
            case "raw": {
                try {
                    IrSequence irSequence = new IrSequence(map.get("data"), DUMMY_ENDING_GAP);
                    ModulatedIrSequence modulatedIrSequence = new ModulatedIrSequence(irSequence, Double.valueOf(map.get("frequency")), Double.valueOf(map.get("duty_cycle")));
                    command = new Command(map.get("name"), null, new IrSignal(modulatedIrSequence));
                } catch (OddSequenceLengthException ex) {
                    throw new ThisCannotHappenException();
                }
            }
            break;

            case "parsed":
                command = processParsedCommand(map.get("name"), map.get("protocol"), parse(map.get("address")), parse(map.get("command")));
                break;

            default:
                throw new ParseException("Neither raw nor protocol", reader.getLineNumber());
        }
        return command;
    }

    private long parse(String str) {
        return Long.parseLong(str.replace(" ", ""), 16);
    }

    private Command processParsedCommand(String name, String protocol, long address, long cmd) throws GirrException {
        String protocolName;
        Map<String, Long> parameters = new HashMap<>(4);

        switch (protocol) {
            case "NEC42":
                protocolName = "Aiwa";
                parameters.put("D", address >> 24L);
                parameters.put("S", (address >> 16L) & 0xFFL); // not tested
                parameters.put("F", cmd >> 24L);
                break;

            case "NEC":
                protocolName = "NEC1";
                parameters.put("D", address >> 24L);
                parameters.put("F", cmd >> 24L);
                break;

            case "NECext": {
                protocolName = "NEC1";
                long D = address >> 24L;
                parameters.put("D", D);
                long S = (address >> 16L) & 0xFFL;
                if (S + D != 255L)
                    parameters.put("S", S);
                long F = cmd >> 24L;
                parameters.put("F", F);
                long E = (cmd >> 16L) & 0xFF;
                if (E != 0) {
                    protocolName = "NEC1-f16";
                    parameters.put("E", E);
                }
            }
            break;

            case "Samsung32":
                protocolName = "NECx1";
                parameters.put("D", address >> 24L);
                parameters.put("S", address >> 24L); // ???!!
                //parameters.put("S", (address >> 16L) & 0xFFL);
                parameters.put("F", cmd >> 24L);
                break;

            case "RC5":
                protocolName = "RC5";
                parameters.put("D", address >> 24L);
                parameters.put("F", cmd >> 24L);
                break;

            // Flipper's RC5X is not our RC5x; it is RC5 with bit 6 of F set to 1
            case "RC5X":
                protocolName = "RC5";
                parameters.put("D", address >> 24L);
                parameters.put("F", (cmd >> 24L) + 0x40);
                break;

            case "RC6":
                protocolName = "RC6";
                parameters.put("D", address >> 24L);
                parameters.put("F", cmd >> 24L);
                break;

            case "SIRC":
                protocolName = "Sony12";
                parameters.put("D", address >> 24L);
                parameters.put("F", cmd >> 24L);
                break;

            case "SIRC15":
                protocolName = "Sony15";
                parameters.put("D", address >> 24L);
                parameters.put("F", cmd >> 24L);
                break;

            case "SIRC20":
                protocolName = "Sony20";
                parameters.put("D", (address >> 24L) & 0x1FL); // bits 24-29
                parameters.put("S", (((address >> 16L) & 0xFFL) << 3L) | (address >> 29L));
                parameters.put("F", cmd >> 24L);
                break;

            case "Kaseikyo": {

                long a28_31 = address >> 28L;
                long a24_27 = (address >> 24L) & 0xFL;
                long M = (address >> 16L) & 0xFFL;
                long N = (address >> 8L) & 0xFFL;
                long a0_1 = address & 0x3L;
                long c28_31 = cmd >> 28L;
                long c24_27 = (cmd >> 24L) & 0xFL;
                long c16_17 = (cmd >> 16L) & 0x3L;

                if (M == 84L && N == 50L) {
                    protocolName = "Denon-K";
                    parameters.put("D", a28_31);
                    parameters.put("S", a24_27);
                    //long lowF = (c28_31 << 4L) | c24_27; //cmd >> 24L;
                    //long highF = (cmd >> 16L) & 0xFL;
                    parameters.put("F", (a0_1 << 10L) | (c16_17 << 8L) | (c28_31 << 4L) | c24_27);
                } else if (M == 2L && N == 32L) {
                    protocolName = "Panasonic";
                    parameters.put("D", (a28_31 << 4L)  );
                    parameters.put("S", (c24_27 << 4L) | a24_27);
                    parameters.put("F", (a0_1 << 6L) | ( c16_17 << 4L) | c28_31);
                } else if (M == 3L && N == 1L) {
                    protocolName = "JVC-48";
                    parameters.put("D", (a28_31 << 4L)  );
                    parameters.put("S", (c24_27 << 4L) | a24_27);
                    parameters.put("F", (a0_1 << 6L) | ( c16_17 << 4L) | c28_31);
                } else {
                    throw new UnsupportedOperationException("Not implemented yet");
                }
            }
            break;

            case "Pioneer": {
                protocolName = "Pioneer";
                parameters.put("D", address >> 24L);
                long S = (address >> 16L) & 0xFFL;
                if (S != 0L) // ???
                    parameters.put("S", S);
                parameters.put("F", cmd >> 24L);
            }
            break;

            case "RCA":
                protocolName = "RCA";
                parameters.put("D", address >> 24L);
                parameters.put("F", cmd >> 24L);
                break;

            default:
                throw new UnsupportedOperationException("Unimplemented protocol: " + protocol);
        }
        return new Command(name, null, null, null, protocolName, parameters, false);
    }

    private boolean isComment(String line) {
        return line.isEmpty() || line.trim().charAt(0) == COMMENTCHAR;
    }
}
