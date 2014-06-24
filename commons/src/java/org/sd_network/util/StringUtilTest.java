/*
 * Copyright 2014 Masatoshi sato.
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

import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;

/**
 * Unit test for {@link StringUtil} class.
 *
 * <p> $Id$
 *
 * @author Masatoshi Sato
 */
public class StringUtilTest
    extends TestCase
{
    //////////////////////////////////////////////////////////// 
    // Private class fields.

    /** Logger. */
    private static final Logger _log = Logger.getLogger(
            StringUtilTest.class.getName());

    //////////////////////////////////////////////////////////// 
    // Constructors and Initialisation.

    public void setUp()
        throws Exception
    {
        super.setUp();
        _log.log(Level.FINE, "--- Run test case [" + getName() + "]");
    }

    //////////////////////////////////////////////////////////// 
    // Test cases.

    public void testIsEmpty() {
        String testStr = null;

        // test for null string and trimed.
        assertTrue(StringUtil.isEmpty(testStr, true));

        // test for null string and no trimed.
        assertTrue(StringUtil.isEmpty(testStr, false));

        testStr = "";

        // test for empty string and trimed.
        assertTrue(StringUtil.isEmpty(testStr, true));

        // test for empty string and no trimed.
        assertTrue(StringUtil.isEmpty(testStr, false));

        testStr = " ";

        // test for string as filled spaces and trimed.
        assertTrue(StringUtil.isEmpty(testStr, true));

        // test for string as filled spaces and no trimed.
        assertFalse(StringUtil.isEmpty(testStr, false));

        testStr = "a";

        // test for string as filled alphabetical string and trimed.
        assertFalse(StringUtil.isEmpty(testStr, true));
    }
}
