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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.sd_network.vfsshell.CommandHandler;

public class CommandHandlerFactory
{
    /** logger. */
    private static final Logger _log = Logger.getLogger(
            CommandHandlerFactory.class.getName());

    //////////////////////////////////////////////////////////// 
    // Private fields.

    private static final Map<String, Class<? extends CommandHandler>> _commandMap;
    static {
        Map<String, Class<? extends CommandHandler>> buf =
            new HashMap<String, Class<? extends CommandHandler>>();
        buf.put("login", Login.class);
        buf.put("logout", Logout.class);
        buf.put("adduser", AddUser.class);
        buf.put("mkdir", MkDir.class);
        buf.put("rmdir", RmDir.class);
        buf.put("cd", Cd.class);
        buf.put("ls", Ls.class);
        buf.put("pwd", Pwd.class);
        buf.put("touch", Touch.class);
        buf.put("upload", Upload.class);
        buf.put("download", Download.class);
        buf.put("loglevel", LogLevel.class);
        _commandMap = Collections.unmodifiableMap(buf);
    };

    //////////////////////////////////////////////////////////// 
    // Public methods.

    public static synchronized CommandHandler getCommandHandler(String command)
    {
        if (command == null)
            throw new NullPointerException("command");
        if (command.trim().length() == 0)
            throw new IllegalArgumentException("command is empty.");
        
        Class<? extends CommandHandler> cls = _commandMap.get(command);
        if (cls == null) {
            _log.log(Level.WARNING, "command[" + command + "] not found.");
            return null;
        }

        CommandHandler ch = null;
        try {
            ch = cls.newInstance();
        } catch (Exception e) {
            _log.log(Level.WARNING,
                    "command[" + command + "] instantiation failure.");
            return null;
        }
        return ch;
    }
}
