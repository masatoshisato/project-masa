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
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import org.sd_network.vfs.AuthenticationException;
import org.sd_network.vfs.VfsContext;
import org.sd_network.vfs.VfsService;
import org.sd_network.vfs.SessionException;
import org.sd_network.vfs.db.VfsFile;
import org.sd_network.vfsshell.CommandHandlerBase;
import org.sd_network.vfsshell.Session;
import org.sd_network.vfsshell.util.ConsoleUtil;

public class Login
    extends CommandHandlerBase
{
    /** Logger. */
    private static final Logger _log = Logger.getLogger(
            Login.class.getName());

    //////////////////////////////////////////////////////////// 
    // Implements to CommandHandler.
    
    public void execute(String[] args) {

        // check session.
        Session session = Session.getInstance();
        if (session.getSessionID() != null) {
            System.out.println("You must logout from previous session.");
            return;
        }

        // parse command line parameters.
        CommandLine commandLine = null;
        try {
            commandLine = new PosixParser().parse(new Options(), args);
        } catch (ParseException e) {
            printUsage(e.getMessage());
            return;
        }

        // retrive login name.
        // If login_name contains in command line, retrive from command line,
        // otherwise, get it from console.
        String loginName = null;
        String[] buf = commandLine.getArgs();
        if (buf.length > 0)
            loginName = buf[0];
        else
            loginName = ConsoleUtil.readRequiredData("login name");

        // get password from console.
        String password = ConsoleUtil.readRequiredData("password");

        try {
            VfsService vfsService = VfsContext.getService();
            session.setSessionID(vfsService.login(loginName, password));
            String sessionID = session.getSessionID();
            VfsFile home = vfsService.getVfsFile(sessionID, "-1", "Home");
            if (home == null)
                throw new IllegalStateException(
                        "User home directory not found.");
            session.setCurrentDirectory(home);
            System.out.println("SessionID = " + session.getSessionID());
            System.out.println("Logged in.");
        } catch (AuthenticationException e) {
            System.out.println("login failed. (" + e.getMessage() + ")");
        } catch (SessionException e) {
            throw new IllegalStateException(
                    "User was logged in, but session was invalid.");
        }
    }

    public void printUsage(String errorMessage) {
        System.out.println(errorMessage);
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("login [login_name]", null);
    }
}
