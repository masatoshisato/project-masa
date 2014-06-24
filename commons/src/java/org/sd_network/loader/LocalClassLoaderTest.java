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
package org.sd_network.loader;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.logging.Level;
import java.util.logging.Logger;
import junit.framework.TestCase;

/**
 * Unit test for {@link LocalClassLoader} class.
 *
 * @author Masatoshi Sato
 */
public class LocalClassLoaderTest
    extends TestCase
{
    //////////////////////////////////////////////////////////// 
    // Private class fields.

    /** Logger. */
    private static final Logger _log = Logger.getLogger(
            LocalClassLoaderTest.class.getName());

    /**
     * Parent directory of jar files for load class.
     */
    private static final String _parentDir =
        System.getProperty("user.dir") + System.getProperty("file.separator") +
        "test" + System.getProperty("file.separator") +
        "repos";

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

    /**
     * Test case for {@link LocalClassLoader#LocalClassLoader(File)} method.
     * Expect specified class is loaded normally when call 
     * {@link java.lang.Class#forName(String, boolean, ClassLoader)} 
     * from jar file in specified directory with constructor.
     *
     * @throws  FileNotFoundException
     *          Throws if specified directory is not found when create
     *          instance of {@link LocalClassLoader} class.
     *
     * @throws  ClassNotFoundException
     *          Throws if expected class that it contains underlying jar file
     *          in specified directory is not found when load class.
     */
    public void testLoadWithParentDir()
        throws FileNotFoundException, ClassNotFoundException
    {
        // define load classes.
        String[] classNames = {
            "javatools.GetEnv",
            "javatools.GetSystemProperties"
        };

        LocalClassLoader ld = new LocalClassLoader(new File(_parentDir));
        for (String className : classNames) {
            Class <?> cls = Class.forName(className, true, ld);
            assertNotNull(cls);
            assertEquals(className, cls.getCanonicalName());
        }
    }

    /**
     * Test case for {@link LocalClassLoader#LocalClassLoader(String)} method.
     * Expect specified class is loaded normally when call 
     * {@link java.lang.Class#forName(String, boolean, ClassLoader)}
     * from jar file in specified classpath with constructor.
     *
     * @throws  ClassNotFoundException
     *          Throws if expected class is not found in specified jar file.
     */
    public void testLoadWithClassPath()
        throws ClassNotFoundException
    {
        // define load classes.
        String[] classNames = {
            "javatools.GetEnv",
            "javatools.GetSystemProperties"
        };

        String classPath = _parentDir + System.getProperty("file.separator")
            + "javatools.jar";
        LocalClassLoader ld = new LocalClassLoader(classPath);
        for (String className : classNames) {
            Class <?> cls = Class.forName(className, true, ld);
            assertNotNull(cls);
            assertEquals(className, cls.getCanonicalName());
        }
    }

    /**
     * Test case for {@link LocalClassLoader#findClass(String)} method.
     * Expect throws ClassNotFoundException when call 
     * {@link java.lang.Class#forName(String, boolean, ClassLoader)} method
     * with class name is not exists.
     *
     * @throws  FileNotFoundException
     *          Throws if directory that is specified parameter to
     *          {@link LocalClassLoader#LocalClassLoader(File)} is not found.
     */
    public void testLoad_ClassNotFoundException()
        throws FileNotFoundException
    {
        // define load classes.
        String className = "javatools.GetEnvs";

        LocalClassLoader ld = new LocalClassLoader(new File(_parentDir));
        try {
            Class <?> cls = Class.forName(className, true, ld);
            fail(
                    "This case expect throws ClassNotFoundException when" +
                    " call Class#forName with class name that class" +
                    " not exists in specified jar file, but did not thrown" +
                    " exception.");
        } catch (ClassNotFoundException e) {
            // This is OK. Do nothing.
        }
    }
}
