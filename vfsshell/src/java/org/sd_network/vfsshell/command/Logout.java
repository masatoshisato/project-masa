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
import org.sd_network.vfs.VfsIOException;
import org.sd_network.vfs.VfsService;
import org.sd_network.vfsshell.CommandHandlerBase;
import org.sd_network.vfsshell.Session;
import org.sd_network.vfsshell.util.ConsoleUtil;

public class Logout
    extends CommandHandlerBase
{
    /** Logger. */
    private static final Logger _log = Logger.getLogger(
            Logout.class.getName());

    //////////////////////////////////////////////////////////// 
    // Implements to CommandHandler.
    
    public void execute(String[] args) {

        // check session.
        Session session = Session.getInstance();
        String sessionID = session.getSessionID();
        if (sessionID == null)
            System.out.println("You have already logged out.");

        // logout.
        VfsService vfsService = VfsContext.getService();
        try {
            vfsService.logout(sessionID);
        } catch (VfsIOException e) {
            System.out.println("ERROR: some file session close failed.");
        }
        session.clearSessionID();

        System.out.println("You were logged out.");
    }
}

