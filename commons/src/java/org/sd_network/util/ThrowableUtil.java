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
 * ��O�����p�̃��[�e�B���e�B���\�b�h���`���܂��B
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
     * �w�肳�ꂽ��O�̃X�^�b�N�g���[�X�� {@link java.lang.String} �ɕϊ�
     * ���ĕԂ��܂��B
     *
     * @param t     ��O�I�u�W�F�N�g
     *
     * @return  �X�^�b�N�g���[�X������B
     *
     * @throws  NullPointerException
     *          <tt>t</tt> �� <tt>null</tt> ���w�肳�ꂽ�ꍇ�ɃX���[���܂��B
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
