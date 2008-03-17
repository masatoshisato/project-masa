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

import java.util.LinkedList;
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

public class Pwd
    extends CommandHandlerBase
{
    /** Logger. */
    private static final Logger _log = Logger.getLogger(
            Pwd.class.getName());

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

        VfsFile curDir = session.getCurrentDirectory();

        try {
            VfsFile parent = curDir;
            LinkedList<VfsFile> pathList = new LinkedList<VfsFile>();
            while (!parent.getParentID().equals("-1")) {
                pathList.addFirst(parent);
                String parentID = parent.getParentID();
                parent = vfsService.getVfsFile(sessionID, parentID);
                if (parent == null)
                    throw new IllegalStateException(
                            "ERROR: parent directory not found.");
            }
            StringBuffer sb = new StringBuffer();
            for (int idx = 0; idx < pathList.size(); idx++) {
                sb.append("/").append(pathList.get(idx).getName());
            }
            if (sb.length() > 0)
                System.out.println(sb.toString());
            else
                System.out.println("/");
        } catch (SessionException e) {
            session.clearSessionID();
            throw new IllegalStateException("ERROR: Session time out.");
        }
    }

    public void printUsage(String errorMessage) {
        System.out.println(errorMessage);
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("pwd", null);
    }
}
