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
package org.sd_network.io;

import java.io.File;
import java.util.logging.Logger;
import java.util.logging.Level;
import junit.framework.TestCase;

/**
 * Unit test for {@link ClassJarFilenameFilter} class.
 *
 * @author Masatoshi Sato
 */
public class ClassJarFilenameFilterTest
    extends TestCase
{
    //////////////////////////////////////////////////////////// 
    // Private class fields.

    /** Logger. */
    private static final Logger _log = Logger.getLogger(
            ClassJarFilenameFilter.class.getName());

    //////////////////////////////////////////////////////////// 
    // Constructors and Initialisations.

    public void setUp()
        throws Exception
    {
        super.setUp();
        _log.log(Level.FINE, "--- Run test case [" + getName() + "]");
    }

    //////////////////////////////////////////////////////////// 
    // Test cases for File#listFiles(FilenameFilter) method.

    /**
     * Test case: Expect that get list of Jar files underlying specified
     * directory when call {@link java.io.File#listFiles(File)} method.
     *
     * <p> List of instance of file included only jar files, other files
     * and directories is ignored this filter class.
     */
    public void testListJarFiles() {
        File dir = new File("test/repos");
        File[] jarFiles = dir.listFiles(
                new ClassJarFilenameFilter(ClassJarFilenameFilter.TARGET.JAR));

        // check count of engaged jar file.
        assertEquals(1, jarFiles.length);

        // check file name that is engaged jar file.
        File jarFile = jarFiles[0];
        assertEquals("javatools.jar", jarFile.getName());
    }

    /**
     * Test case: Expect that get list of class files and directories
     * underlying specified directory when call 
     * {@link java.io.File#listFiles(File)} method.
     *
     * <p> List of instance of file included class files and directory,
     * other file is ignored this filter class.
     */
    public void testListClassFilesAndDirectories() {
        File dir = new File("test/repos");
        ClassJarFilenameFilter filter =
            new ClassJarFilenameFilter(ClassJarFilenameFilter.TARGET.CLASS);

        /*
         * Engage flles or directories from "test/repos".
         */

        // Engage class files and directories from the directory.
        File[] classDirFiles1 = dir.listFiles(filter);

        // output logs for engaged file name.
        for (int idx = 0; idx < classDirFiles1.length; idx++) {
            _log.log(Level.INFO, "Engaged file [" + classDirFiles1[idx] + "].");
        }

        // check count of engaged class files and directories.
        assertEquals(1, classDirFiles1.length);

        // check whether engaged file object is directory.
        assertTrue(classDirFiles1[0].isDirectory());

        // check name of engaged directory.
        assertEquals("sub1", classDirFiles1[0].getName());

        /*
         * Engage files or directories from "test/repos/sub1".
         */

        // Engage class files and directories from the directory.
        File[] classDirFiles2 = classDirFiles1[0].listFiles(filter);

        // check count of engaged class files and directories.
        assertEquals(1, classDirFiles2.length);

        // check whether engaged file object is file.
        assertTrue(classDirFiles2[0].isFile());

        // check name that is engaged class files and directories.
        assertEquals("aaa.class", classDirFiles2[0].getName());
    }

    /**
     * Test case: Expect that throw IllegalArgumentExcepiton when
     * call constructor of {@link ClassJarFilenameFilter} with null parameter.
     */
    public void testConstructor_IllegalArgumentException() {
        ClassJarFilenameFilter.TARGET targetID = null;
        try {
            ClassJarFilenameFilter filter =
                new ClassJarFilenameFilter(targetID);
            fail("This test case is expected throws IllegalArgumentException" +
                    " when create instance with null parameter, but it is" +
                    " thrown the exception.");
        } catch (IllegalArgumentException e) {
            // This is expected result. do nothing.
        }
    }
}
