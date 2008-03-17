/*
 * Copyright 2007 Masatoshi sato.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.sd_network.vfsshell;

import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import org.sd_network.util.Config;
import org.sd_network.vfs.AuthenticationException;
import org.sd_network.vfs.VfsContext;
import org.sd_network.vfs.VfsService;
import org.sd_network.vfsshell.command.CommandHandlerFactory;
import org.sd_network.vfsshell.util.ConsoleUtil;

/**
 * A entry point for execute file explorer.
 * You can specify a properties file for variable setting for file explorer.
 * Default properties file is "fileexplorer.properties" on the same 
 * directory as jar file. <br>
 * The options you can specify is below.
 *
 * <ul>
 *  <li> -p : --propertiesfile  a properties file for variable setting.
 * </ul>
 *
 * <p> $Id$
 *
 * @author Masatoshi Sato
 */
public class Main
{
    /** Logger */
    private static final Logger _log = Logger.getLogger(
            Main.class.getName());
    
    //////////////////////////////////////////////////////////// 
    // Public fields.

    //////////////////////////////////////////////////////////// 
    // Private fields.

    // Instance of the command line parser.
    private static final CommandLineParser _parser = new PosixParser();

    // Define command line options.
    private static final Options _options;
    static {
        Options buf = new Options();
        Option option = new Option("p", "propertyfile", true, "property file.");
        option.setRequired(false);
        buf.addOption(option);
        _options = buf;
    };

    // Default properties file name
    private static final String SHELL_PROP_FILE = "vfsshell.properties";
    private static final String VFS_PROP_FILE = "vfs.properties";

    //////////////////////////////////////////////////////////// 
    // Program entry point.

    public static void main(String[] args) {

        // parse command line parameters.
        CommandLine commandLine = null;
        try {
            commandLine = _parser.parse(_options, args);
        } catch (ParseException e) {
            printUsage(e.getMessage());
        }

        // initialisation of VFS.
        VfsContext.init(VFS_PROP_FILE);

        // Set configuration
        Config.load(VFSSHELL_PROP_FILE);

        // Go ahead!!
        while (true) {
            System.out.print("vfs> ");
            String command = ConsoleUtil.readFromConsole();
            // System.out.println("Command=" + command);
            if (command.equalsIgnoreCase("exit"))
                break;
            if (command.equalsIgnoreCase("quit"))
                break;
            if (command.trim().length() == 0)
                continue;
            CommandHandler ch =
                CommandHandlerFactory.getCommandHandler(getCommand(command));
            if (ch == null)
                continue;
            ch.execute(getCommandArgs(command));
        }
        return;
    }


    //////////////////////////////////////////////////////////// 
    // Private methods.

    private static final void printUsage(String errorMessage) {
        System.out.println(errorMessage);
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("vfsshell", _options);
    }

    private static final String getCommand(String cmd) {
        if (cmd.indexOf(" ") != -1)
            return cmd.substring(0, cmd.indexOf(" "));
        return cmd;
    }

    private static final String[] getCommandArgs(String cmd) {
        if (cmd == null || cmd.trim().length() == 0)
            throw new IllegalArgumentException("cmd is empty.");

        StringTokenizer st = new StringTokenizer(cmd, " ");
        int count = st.countTokens();
        if (count <= 1)
            return new String[0];
        st.nextToken();  // remove command itself.

        String[] args = new String[st.countTokens()];
        int cnt = 0;
        while (st.hasMoreTokens()) {
            args[cnt] = st.nextToken();
            cnt++;
        }
        return args;
    }
}
