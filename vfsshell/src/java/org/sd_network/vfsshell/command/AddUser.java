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
package org.sd_network.vfsshell.command;

import java.util.logging.Logger;
import java.util.logging.Level;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import org.sd_network.vfs.PermissionException;
import org.sd_network.vfs.SessionException;
import org.sd_network.vfs.VfsContext;
import org.sd_network.vfs.VfsService;
import org.sd_network.vfsshell.CommandHandlerBase;
import org.sd_network.vfsshell.Session;
import org.sd_network.vfsshell.util.ConsoleUtil;

public class AddUser
    extends CommandHandlerBase
{
    /** Logger. */
    private static final Logger _log = Logger.getLogger(
            AddUser.class.getName());

    //////////////////////////////////////////////////////////// 
    // Private fields.

    // Define command line options.
    private static final Options _options;
    static {
        Options buf = new Options();
        // -a option.
        Option option =
            new Option("a", "isadmin", false,
                    "whether new user is administrator.");
        option.setRequired(false);
        buf.addOption(option);
        _options = buf;
    };

    //////////////////////////////////////////////////////////// 
    // Implements to CommandHandler.
    
    public void execute(String[] args) {

        // check session.
        Session session = Session.getInstance();
        String sessionID = session.getSessionID();
        if (sessionID == null) {
            System.out.println("ERROR: You must login.");
            return;
        }
        VfsService vfsService = VfsContext.getService();

        // parse command line parameters.
        CommandLine commandLine = null;
        try {
            commandLine = new PosixParser().parse(_options, args);
        } catch (ParseException e) {
            printUsage(e.getMessage());
            return;
        }

        // retrives command line parameters.
        boolean isAdmin = commandLine.hasOption("a");
        String[] buf = commandLine.getArgs();
        if (buf.length == 0) {
            printUsage("ERROR: Login name not found.");
            return;
        }

        String loginName = buf[0];
        String password = null;
        while (true) {
            String password1 = ConsoleUtil.readRequiredData("input password");
            if (password1 == null || password1.trim().length() == 0) {
                System.out.println("BAD PASSWORD. ");
                continue;
            }
            String password2 = ConsoleUtil.readRequiredData("input confirm");
            if (!password1.equals(password2)) {
                System.out.println("UNMATCH PASSWORD.");
                continue;
            }
            password = password1;
            break;
        }

        try {
            vfsService.addUser(sessionID, loginName, password, isAdmin);
            System.out.println("User [" + loginName + "] added.");
        } catch (PermissionException e) {
            System.out.println("ERROR: add user failed. (" + e.getMessage() + ")");
        } catch (SessionException e) {
            session.clearSessionID();
            System.out.println("ERROR: Session time out.");
        }
    }

    public void printUsage(String errorMessage) {
        System.out.println(errorMessage);
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("adduser [options] login_name", _options);
    }
}
