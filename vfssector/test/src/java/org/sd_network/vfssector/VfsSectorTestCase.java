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
package org.sd_network.vfssector;

import java.util.logging.Logger;

import junit.framework.TestCase;

/**
 * vfssectorパッケージのテストケースのベースクラスです。
 * テストケースを定義する場合、このクラスを派生してください。
 *
 * <p> $Id$
 *
 * @author Masatoshi Sato
 */
public abstract class VfsSectorTestCase
    extends TestCase
{
    /** Logger. */
    private static final Logger _log = Logger.getLogger(
            VfsSectorTestCase.class.getName());

    //////////////////////////////////////////////////////////// 
    // Constructors and Initializations.
    
    //////////////////////////////////////////////////////////// 
    // Protected methods.

    protected void assertEquals(byte[] exp, byte[] act) {
        if (exp.length != act.length)
            fail("array size unmatched.");
        for (int idx = 0; idx < exp.length; idx++) {
            if (exp[idx] != act[idx])
                fail("byte unmatched at No." + idx);
        }
    }
}
