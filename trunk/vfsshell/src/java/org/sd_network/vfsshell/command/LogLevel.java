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

import org.sd_network.vfsshell.CommandHandlerBase;

public class LogLevel
    extends CommandHandlerBase
{
    /** Logger. */
    private static final Logger _log = Logger.getLogger(
            Upload.class.getName());

    private static final Options _options;
    static {
        Options buf = new Options();
        _options = buf;
    };

    public void execute(String[] args) {
        CommandLine cl = null;
        try {
            cl = new PosixParser().parse(_options, args);
        } catch (ParseException e) {
            printUsage(e.getMessage());
            return;
        }

        if (cl.getArgs() == null || cl.getArgs().length != 1) {
            printUsage("ERROR: require log_level number.");
            return;
        }

        System.setProperty("org.sd_network.logging.level", cl.getArgs()[0]);
        System.out.println("Set log level to " + cl.getArgs()[0]);
    }

    public void printUsage(String errorMessage) {
        System.out.println(errorMessage);
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("loglevel log_level number", _options);
    }
}
