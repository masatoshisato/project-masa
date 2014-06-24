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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.AccessControlException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;

import org.sd_network.loader.LocalClassLoader;

/**
 * Unit test for JarUtil class.
 *
 * <p> $Id$
 *
 * @author Masatoshi Sato
 */
public class JarUtilTest
    extends TestCase
{
    //////////////////////////////////////////////////////////// 
    // Class fields.

    /** Logger. */
    private static final Logger _log = Logger.getLogger(
        JarUtilTest.class.getName());

    /** 
     * Parent directory for test jar files. 
     * This is a current working directory.
     */
    private static final String _PARENT_DIR =
        System.getProperty("user.dir") +
        System.getProperty("file.separator");

    /** target path for pack to jar file. */

    /** Jar file path. */
    private static final String _JAR_FILE_PATH = "test/JarUtil.jar";

    /** 
     * Test data for pack method for normal case.
     * These files and directories is exists actually.
     */
    private static final String[] _TARGET_ROOTS = new String[] {
        "test/datastore/test1",
        "test/datastore/test2",
    };

    /** 
     * Test data for pack method for FileNotFoundException case.
     * These files and directories does not exists.
     */
    private static final String[] _TARGET_ROOTS_NOT_EXISTS = new String[] {
        "test" + System.getProperty("file.separator") + "JarUtilDoesNotExists"
    };

    /** 
     * Set expected file/directory for pack to jar file.
     * There is in <tt>$COMMONS_HOME\test</tt> directory.
     */
    private static final List<String> _EXPECTED_JAR_ENTRY_LIST = Arrays.asList(
            "test1/",
            "test1/01.txt",
            "test1/02.txt",
            "test1/10.txt",
            "test1/1a.txt",
            "test1/1b.txt",
            "test1/2a.txt",
            "test1/2b.txt",
            "test1/a1.txt",
            "test1/a2.txt",
            "test1/z1.txt",
            "test1/z10.txt",
            "test1/z2.txt",
            "test1/sub1/",
            "test1/sub1/s1_01.txt",
            "test1/sub2/",
            "test2/"
            );

    //////////////////////////////////////////////////////////// 
    // Constructors and Initializations.

    public void setUp()
        throws Exception
    {
        super.setUp();
        _log.log(Level.FINE, "--- Setup for test case [" + getName() + "]");

        // XXX set system property "java.class.path" for test only.
    }

    public void tearDown()
        throws Exception
    {
        super.tearDown();

        // delete jar file that is created before.
        _log.log(Level.FINE, "--- tearDown for test case [" + getName() + "]");
        File jarFile = new File(_JAR_FILE_PATH);
        if (jarFile.exists()) {
            if (jarFile.delete())
                _log.log(Level.FINE,
                        "deleted file [" + jarFile.getAbsolutePath() + "]");
            else
                _log.log(Level.FINE,
                        "failed to delete file " +
                        "[" + jarFile.getAbsolutePath() + "]");
        } else {
            _log.log(Level.FINE,
                    "file [" + jarFile.getAbsolutePath() + "] not exists.");
        }
    }

    //////////////////////////////////////////////////////////// 
    // Test cases for Pack method.

    /**
     * Test case for {@link JarUtil#pack(String[])}. 
     * Expect pack files that is located to <tt>test/datastore</tt> directory 
     * of <tt>commons</tt> directory to Jar file, and can retrive these files
     * from packed Jar file.
     *
     * @throws  IOException
     *          Throws if file I/O error occurred when create jar file.
     */
    public void testPack()
        throws IOException
    {
        checkNotExistsFile(_JAR_FILE_PATH);

        // Pack files.
        File jarFile = packTo(_TARGET_ROOTS, _JAR_FILE_PATH);

        // test exists expected file/directory in jar file by expected list.
        JarFile packedJarFile = new JarFile(jarFile);
        for (String jarEntryName : _EXPECTED_JAR_ENTRY_LIST) {
            assertNotNull(packedJarFile.getJarEntry(jarEntryName));
        }
        try {
            packedJarFile.close();
        } catch (IOException e) {
            // ignore this exception.
        }
    }

    /**
     * Test case for {@link JarUtil#pack(String[])}.
     * Expect throws IllegalArgumentException when call pack method.
     *
     * <p> Patterns of throws the exception is below.
     * <ul>
     *  <li> p1: parameter <tt>targetRoots</tt> is set null.
     *  <li> p2: parameter <tt>targetRoots</tt> does not contain any element.
     * </ul>
     *
     * @throws  IOException
     *          Throws if specified jar file is not readable.
     */
    public void testPack_IllegalArgumentException()
        throws IOException
    {
        checkNotExistsFile(_JAR_FILE_PATH);

        // p1: test case for specified targetRoots parameter that set null.
        try {
            String[] targetRoots = null;
            JarUtil.pack(targetRoots);
            fail(
                    "This case expect throws IllegalArgumentException" +
                    " when call pack method with set null to targetRoots" +
                    " parameter, but did not thrown excpetion.");
        } catch (IllegalArgumentException e) {
            assertEquals("targetRoots is null.", e.getMessage());
        }

        // p2: test case for specified targetRoots parameter that does not
        // contain any element.
        try {
            String[] targetRoots = {};
            JarUtil.pack(targetRoots);
            fail(
                    "This case expect throws IllegalArgumentException" +
                    " when call pack method with does not contain any element" +
                    " set list with empty element " +
                    " to targetRoots parameter, but did not thrown exception.");
        } catch (IllegalArgumentException e) {
            assertEquals("targetRoots is empty.", e.getMessage());
        }
    }

    /**
     * Test case for {@link JarUtil#pack(String[])}.
     * Expect throws FileNotFoundException when specified 
     * files and directories that does not exists to <tt>targetRoots</tt>
     * parameter.
     *
     * @throws  IOException
     *          Throws if specified jar file is not readable.
     */
    public void testPack_FileNotFoundException()
        throws IOException
    {
        checkNotExistsFile(_JAR_FILE_PATH);

        // Pack files.
        try {
            File jarFile = packTo(_TARGET_ROOTS_NOT_EXISTS, _JAR_FILE_PATH);
            fail(
                    "This case expect throws FileNotFoundException when call" +
                    " pack methid with argetRoots parameter that contains" +
                    " file or directory does not exists, but did not thrown" +
                    " exception.");
        } catch (FileNotFoundException e) {
            assertEquals(
                    "targetFile" +
                    " [" + _PARENT_DIR + _TARGET_ROOTS_NOT_EXISTS[0] + "]" +
                    " does not exists.", e.getMessage());
        }
    }

    //////////////////////////////////////////////////////////// 
    // Test cases for getJarFiles method.

    /**
     * Test case for {@link JarUtil#getJarFiles()}.
     * Expect get jar file path list as List collection of String when call
     * {@link JarUtil#getJarFiles()} method.
     *
     * <p> jar file path list is sorted by alphabetical order 
     * (not case-sensitive) and is unmodifiable.
     */
    public void testGetJarFiles() {
        // backup system properties.
        String path = System.getProperty("java.class.path");
        String separator = System.getProperty("path.separator");

        try {
            // test List element whether included expected element and 
            // there is sorted by alphabetical order.
            System.setProperty("java.class.path",
                    "z.jar" + separator + 
                    "A.jar" + separator +
                    "a_.jar" + separator + 
                    "a.jar" + separator + 
                    "a0.jar" + separator +
                    "a1.jar" + separator +
                    "a10.jar" + separator +
                    "0.jar" + separator +
                    "10.jar" + separator);
            String[] expect = {
                "0.jar", "10.jar", "A.jar", "a.jar", "a0.jar", "a1.jar",
                "a10.jar", "a_.jar", "z.jar"
            };
            List<String> jarFileList = JarUtil.getJarFiles();
            for (int idx = 0; idx < jarFileList.size(); idx++) {
                assertEquals(expect[idx], jarFileList.get(idx));
            }
        } finally {
            // restore system properties.
            System.setProperty("java.class.path", path);
        }

        try {
            // test List is returned as unmodifiable.
            List<String> jarFileList = JarUtil.getJarFiles();
            jarFileList.add("Test");
            fail(
                    "This case expect return list as unmodifiable" +
                    " and throws UnsupportedOperationException when modify" +
                    " the list, but did not thrown the exception" +
                    " when add to it.");
        } catch (UnsupportedOperationException e) {
            // This is OK. Do nothing.
        }
    }
    ////////////////////////////////////////////////////////////
    // Test cases for getClassNameList method.

    /**
     * Test case for {@link JarUtil#getClassNameList(String)}.
     * This method expect get List collection of class name (FQCN) from
     * specified <tt>JarFilePath</tt> parameter without any exceptions.
     *
     * @throws  FileNotFoundException
     *          Throws if specified jar file is not exists.
     *
     * @throws IOException
     *          Throws if specified jar file is not readable.
     */
    public void testGetClassNameList()
        throws FileNotFoundException, IOException
    {
        // set test data.
        String classPath = "test/repos/javatools.jar";
        List <String> expectedClassNameList = Arrays.asList(
                "javatools.ClassLoaderFromDir",
                "javatools.ClassLoaderFromJar",
                "javatools.GetEnv",
                "javatools.GetSystemProperties"
                );

        // get class list from specified jar file.
        List <String> classNameList = JarUtil.getClassNameList(classPath);

        assertEquals(expectedClassNameList.size(), classNameList.size());
        for (String className : classNameList) {
            assertTrue(expectedClassNameList.contains(className));
        }
    }

    /**
     * Test case for {@link JarUtil#getClassNameList(String)}.
     * This method expect occorred FileNotFoundException when call
     * {@link JarUtil#getClassNameList(String)} with JAR file path that it not
     * exists.
     *
     * @throws  IOException
     *          Throws if specified jar file is not readable.
     */
    public void testGetClassNameList_FileNotFoundException()
        throws IOException
    {
        try {
            JarUtil.getClassNameList("test/repos/NotExists.jar");
            fail(
                "This case expect throws FileNotFoundException when call" +
                " getClassNameList method with not exists jar file path," +
                " but did not thrown the exception.");
        } catch (FileNotFoundException e) {
            // this is OK, do nothing.
        }
    }

    /**
     * Test case for {@link JarUtil#getClassNameList(String)}.
     * This method expect occurred IllegalArgumentException when call
     * {@link JarUtil#getClassNameList(String)} with parameter as empty string
     * or not regular file (i.e. Directory).
     *
     * @throws  FileNotFoundException
     *          Throws if speicifed jar file is not found.
     *
     * @throws  IOException
     *          Throws if specified jar file is not readable.
     */
    public void testGetClassNameList_IllegalArgumentException()
        throws FileNotFoundException, IOException
    {
        try {
            JarUtil.getClassNameList("");
            fail(
                "This case expect throws IllegalArgumentException when call" +
                " getClassNameList method with empty string," +
                " but did not thrown the exception.");
        } catch (IllegalArgumentException e) {
            // this is OK, do nothing.
        }

        try {
            String nullString = null;
            JarUtil.getClassNameList(nullString);
            fail(
                "This case expect throws IllegalArgumentException when call" +
                " getClassNameList method with null," +
                " but did not thrown the exception.");
        } catch (IllegalArgumentException e) {
            // this is OK. do nothing.
        }

        try {
            String nullString = null;
            JarUtil.getClassNameList("test/repos");
            fail(
                "This case expect throws IllegalArgumentException when call" +
                " getClassNameList method with directory," +
                " but did not thrown the exception.");
        } catch (IllegalArgumentException e) {
            // this is OK. do nothing.
        }
    }

    //////////////////////////////////////////////////////////// 
    // Private methods.

    /**
     * Pack file or directory specified by <tt>targetsRoots</tt> to
     * <tt>saveTo</tt>.
     *
     * @param targetRoots   target files and directories for pack to jar file.
     * @param saveTo        Store file path of packed jar file.
     *
     * @throws  IllegalArgumentException
     *          Throws if each arguments is below.
     *          <ul>
     *              <li> <tt>targetRoots</tt> is null or empty list.
     *              <li> <tt>saveTo</tt> is null or zero length.
     *          </ul>
     *
     * @throws  IOException
     *          Throws if occurred I/O error when access to files and 
     *          directories specified by <tt>targetRoots</tt> parameter.
     */
    private File packTo(String[] targetRoots, String saveTo)
        throws IllegalArgumentException, IOException
    {
        if (targetRoots == null)
            throw new IllegalArgumentException("targetRoot is null.");
        if (targetRoots.length == 0)
            throw new IllegalArgumentException("targetRoot is empty list.");
        if (StringUtil.isEmpty(saveTo, true))
            throw new IllegalArgumentException(
                    "saveTo is null or zero length.");

        File packedFile = JarUtil.pack(targetRoots);
        File destFile = new File(saveTo);
        packedFile.renameTo(destFile);
        packedFile = null;
        return destFile;
    }

    private void checkNotExistsFile(String filePath)
        throws IllegalStateException
    {
        File file = new File(filePath);
        if (file.exists()) {
            String msg =
                "file [" + file.getAbsolutePath() + "]" +
                " (specified filePath parameter as [" + filePath + "])" +
                " is already exists.";
            _log.log(Level.SEVERE, msg);
            throw new IllegalStateException(msg);
        }
    }
}
