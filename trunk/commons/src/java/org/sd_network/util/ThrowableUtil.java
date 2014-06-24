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
package org.sd_network.util;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 例外処理用のユーティリティメソッドを定義します。
 *
 * <p> $Id$
 *
 * @author Masatoshi Sato
 */
public class ThrowableUtil
{
    //////////////////////////////////////////////////////////// 
    // Class fields.

    /** Logger. */
    private static final Logger _log = Logger.getLogger(
            ThrowableUtil.class.getName());

    //////////////////////////////////////////////////////////// 
    // Public methods.

    /**
     * 指定された例外のスタックトレースを {@link java.lang.String} に変換
     * して返します。
     *
     * @param t     例外オブジェクト
     *
     * @return  スタックトレース文字列。
     *
     * @throws  NullPointerException
     *          <tt>t</tt> に <tt>null</tt> が指定された場合にスローします。
     */
    public static String toStackTraceString(Throwable t) {
        if (t == null)
            throw new NullPointerException("t");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        t.printStackTrace(ps);
        String str = baos.toString();
        ps.close();
        return str;
    }
}
