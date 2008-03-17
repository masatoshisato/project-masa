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

import java.io.File;
import java.io.IOException;
import java.io.FileOutputStream;

import java.util.logging.Logger;
import java.util.logging.Level;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import org.sd_network.vfs.FileSession;
import org.sd_network.vfs.SessionException;
import org.sd_network.vfs.VfsContext;
import org.sd_network.vfs.VfsIOException;
import org.sd_network.vfs.VfsService;
import org.sd_network.vfs.db.SystemInfo;
import org.sd_network.vfs.db.VfsFile;
import org.sd_network.vfsshell.CommandHandlerBase;
import org.sd_network.vfsshell.Session;

public class Download
    extends CommandHandlerBase
{
    /** Logger. */
    private static final Logger _log = Logger.getLogger(
            Download.class.getName());

    private static final Options _options;
    static {
        Options buf = new Options();
        _options = buf;
    };

    public void execute(String[] args) {
        // check session.
        Session session = Session.getInstance();
        String sessionID = session.getSessionID();
        if (sessionID == null) {
            System.out.println("ERROR: You must login.");
            return;
        }
        VfsService vfsService = VfsContext.getService();

        CommandLine cl = null;
        try {
            cl = new PosixParser().parse(_options, args);
        } catch (ParseException e) {
            printUsage(e.getMessage());
            return;
        }

        if (cl.getArgs() == null || cl.getArgs().length != 2) {
            printUsage("ERROR: require local_file_path and vfs_file_name");
            return;
        }

        // check file path.
        File file = new File(cl.getArgs()[1]);
        if (file.exists()) {
            System.out.println("ERROR: the local file already exists.");
            return;
        }

        SystemInfo systemInfo = vfsService.getSystemInfo();
        String parentFileID = session.getCurrentDirectory().getID();
        String fileSessionID = null;
        try {

            VfsFile vfsFile = vfsService.getVfsFile(
                    sessionID, parentFileID, cl.getArgs()[0]);
            if (vfsFile == null) {
                System.out.println("ERROR: the VfsFile not found.");
                return;
            }
            fileSessionID = vfsService.createFileSession(
                sessionID, vfsFile.getID(), FileSession.Mode.READ);

            byte[] buf = new byte[systemInfo.getBytesPerRead()];
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(file);
                int count = 0;
                while ((count = vfsService.readData(sessionID, fileSessionID, buf, buf.length)) != -1)
                {
                    fos.write(buf, 0, count);
                }
            } catch (IOException e) {
                e.printStackTrace(System.err);
            } finally {
                if (fos != null)
                    try {
                        fos.close();
                    } catch (IOException e) {
                    }
            }
        } catch (Exception e) {
            e.printStackTrace(System.err);
        } finally {
            if (fileSessionID != null)
                try {
                    vfsService.closeFileSession(sessionID, fileSessionID);
                } catch (Exception e) {
                    e.printStackTrace(System.err);
                }
        }
    }

    public void printUsage(String errorMessage) {
        System.out.println(errorMessage);
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("download [options] vfs_file local_file_path", _options);
    }
}
