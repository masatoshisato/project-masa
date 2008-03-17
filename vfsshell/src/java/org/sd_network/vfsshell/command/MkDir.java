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
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import org.sd_network.vfs.SessionException;
import org.sd_network.vfs.VfsContext;
import org.sd_network.vfs.VfsIOException;
import org.sd_network.vfs.VfsService;
import org.sd_network.vfsshell.CommandHandlerBase;
import org.sd_network.vfsshell.Session;

public class MkDir
    extends CommandHandlerBase
{
    /** Logger. */
    private static final Logger _log = Logger.getLogger(
            MkDir.class.getName());

    //////////////////////////////////////////////////////////// 
    // Private fields.

    // Define command line options.
    private static final Options _options;
    static {
        Options buf = new Options();
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

        CommandLine commandLine = null;
        try {
            commandLine = new PosixParser().parse(_options, args);
        } catch (ParseException e) {
            printUsage(e.getMessage());
            return;
        }

        String dirName = commandLine.getArgs()[0];
        String parentFileID = session.getCurrentDirectory().getID();

        try {
            vfsService.createDirectory(sessionID, parentFileID, dirName);
        } catch (VfsIOException e) {
            System.out.println("ERROR: " + e.getMessage());
        } catch (SessionException e) {
            session.clearSessionID();
            System.out.println("ERROR: Session time out.");
        }
    }

    public void printUsage(String errorMessage) {
        System.out.println(errorMessage);
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("mkdir [options] new_dir_path", _options);
    }
}
