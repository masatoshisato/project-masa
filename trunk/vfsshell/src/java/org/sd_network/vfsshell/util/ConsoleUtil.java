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
package org.sd_network.vfsshell.util;

import java.io.IOException;
import java.util.logging.Logger;

public class ConsoleUtil
{
    /** Logger. */
    private static final Logger _log = Logger.getLogger(
            ConsoleUtil.class.getName());

    //////////////////////////////////////////////////////////// 
    // Utility methods.

    /**
     * Read required data from console.
     * This method keep read from console until data inputed.
     *
     * @param dataName  data name.
     *
     * @return  input data.
     */
    public static String readRequiredData(String dataName) {
        String data = null;
        while (true) {
            System.out.print(dataName + " : ");
            data = readFromConsole();
            if (data != null && data.trim().length() > 0)
                break;
            System.out.println(
                    System.getProperty("line.separator") + 
                    "Please input " + dataName + ".");
        }
        return data;
    }

    /**
     * Read command from console.
     * When entered CR code, return entered all code as
     * {@link java.lang.String}.
     * If entered CR code after back slash, the CR code is ignored.
     * That means, allow multiple line command.
     *
     * @return  entered all code as String.
     *          If occured IOException, return empty string.
     */
    public static String readFromConsole() {
        String lineFeed = System.getProperty("line.separator");
        StringBuffer buf = new StringBuffer();
        boolean ignoreCR = false;
        while (true) {
            int c = 0;
            try {
                c = System.in.read();
                if (c == '\n' || c == '\r') {
                    // If line feed is "\r\n", read more one time.
                    if (lineFeed.length() == 2)
                        System.in.read();
                }
                // System.out.println(Integer.toHexString(c));
            } catch (IOException e) {
                return "";
            }

            if (c == '\\') {
                ignoreCR = true;
                // System.out.println("ignore flag on");
                continue;
            }
            if (c == '\n' || c == '\r') {
                if (ignoreCR) {
                    // System.out.println("ignore flag off");
                    ignoreCR = false;
                    continue;
                }
                break;
            }
            ignoreCR = false;
            // System.out.println("ignore flag off");
            buf.append((char) c);
        }
        return buf.toString();
    }
}
