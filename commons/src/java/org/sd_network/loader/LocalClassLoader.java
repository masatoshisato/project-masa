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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.sd_network.io.ClassJarFilenameFilter;
import org.sd_network.util.StringUtil;

/**
 * A class loader extends {@link java.lang.ClassLoader} for load class from
 * jar files located at specific parent directory.
 *
 * <p> <B>XXX</B>: This class do not process directory contains class files.
 * You have to fixed this to below.
 * <ul>
 *  <li> Constructor {@link #LocalClassLoader(String)} must be process
 *       directory contains class files. Now, it is only process jar file path.
 *  <li> If String parameter contains directory, must be search class files
 *       from the directory, also subdirectory of the directory recursivery too.
 * </ul>
 *
 * $Id$
 *
 * @author Masatoshi Sato
 */
public class LocalClassLoader
    extends ClassLoader
{
    //////////////////////////////////////////////////////////// 
    // Private fields.

    /** Logger. */
    private static final Logger _log = Logger.getLogger(
            LocalClassLoader.class.getName());

    /** List collection contains {@link java.util.jar.JarFile} instances. */
    private final File[] _jarFiles;

    //////////////////////////////////////////////////////////// 
    // Constructors and Initializations.

    /**
     * A constructor with parameter that is represented class path string for
     * search jar files.
     *
     * <p> <B>XXX</B>:This method must be process directory contains class
     * files. Now, it is not processed. You have to fixed this.
     *
     * @param classPath class path string for search jar files.
     *
     * @throws  IllegalArgumentException
     *          Throws if parameter specified <tt>classPath</tt> is null or
     *          zero length string.
     */
    public LocalClassLoader(String classPath)
        throws IllegalArgumentException
    {
        super();
        if (StringUtil.isEmpty(classPath, true))
            throw new IllegalArgumentException("classPath is empty.");

        List <File> jarFiles = new ArrayList <File> ();
        String[] classPaths =
            classPath.split(System.getProperty("path.separator"));
        for (int idx = 0; idx < classPaths.length; idx++) {
            File classPathFile = new File(classPaths[idx]);
            if (!classPathFile.exists()) {
                _log.log(Level.WARNING, 
                        "classPath[" + classPaths[idx] + "]" +
                        " is not found. Ignored this.");
                continue;
            }
            if (!classPathFile.canRead()) {
                _log.log(Level.WARNING,
                        "classPath[" + classPaths[idx] + "]" +
                        " can not readable. Ignored this.");
                continue;
            }
            if (!classPathFile.getName().endsWith(".jar")) {
                _log.log(Level.WARNING,
                        "classPath[" + classPaths[idx] + "]" +
                        " is not jar. Ignored this.");
                continue;
            }
            jarFiles.add(classPathFile);
        }
        _jarFiles = jarFiles.toArray(new File[0]);
    }

    /**
     * A constructor with parameter that is represented parent directory for
     * search jar files.
     *
     * @param parentDir parent directory of jar files to search.
     *
     * @throws  FileNotFoundException
     *          Throws if parent directory specified to <tt>parentDir</tt>
     *          is not found.
     *
     * @throws  IllegalArgumentException
     *          Throws if parameter specified <tt>parentDir</tt> is below.
     *          <ul>
     *              <li> It is not directory.
     *              <li> It is not readable.
     *          </ul>
     */
    public LocalClassLoader(File parentDir)
        throws FileNotFoundException, IllegalArgumentException
    {
        super();
        if (!parentDir.exists())
            throw new FileNotFoundException(
                    "parentDir[" + parentDir.getName() + "]");
        if (!parentDir.isDirectory())
            throw new IllegalArgumentException(
                    "parentDir[" + parentDir.getName() + "] is not directory.");
        if (!parentDir.canRead())
            throw new IllegalArgumentException(
                    "parentDir[" + parentDir.getName() + "] is not readable.");

        _jarFiles = parentDir.listFiles(
                new ClassJarFilenameFilter(ClassJarFilenameFilter.TARGET.JAR));
    }

    /**
     * This is a default consturctor, in this class, it is unsupported.
     *
     * <p> This class is required parent directory for search jar files.
     * You have to use {@link LocalClassLoader(String)} constructor with
     * String parameter instead of this constructor.
     *
     * @see LocalClassLoader(String)
     */
    private LocalClassLoader() {
        throw new UnsupportedOperationException();
    }

    //////////////////////////////////////////////////////////// 
    // Protected methods.

    /**
     * Find class from jar files in parent directory specified at constructor.
     *
     * @param className binary name of class.
     *
     * @return  Loaded class object.
     *
     * @throws  ClassNotFoundException
     *          Throws if specified <tt>className</tt> is not found, or 
     *          occurred I/O error at load class from jar file.
     */
    protected Class <?> findClass(String className)
        throws ClassNotFoundException
    {
        Class <?> cls = null;
        for (File targetFile : _jarFiles) {
            JarFile jarFile = null;
            try {
                jarFile = new JarFile(targetFile);
                cls = findClassFromJar(jarFile, className);
                if (cls != null)
                    return cls;
            } catch (IOException e) {
                throw new ClassNotFoundException(
                        "I/O error occurred. " + e.getMessage(), e);
            } finally {
                if (jarFile != null)
                    try {
                        jarFile.close();
                    } catch (IOException e) {
                    }
            }
        }
        throw new ClassNotFoundException(className);
    }

    //////////////////////////////////////////////////////////// 
    // Private methods.

    /**
     * Find class specified at <tt>className</tt> parameter from jar file
     * specified at <tt>jarFile</tt> parameter, and create create class object.
     *
     * @param jarFile   {@link java.util.jar.JarFile} object of jar file for
     *                  find class.
     * @param className Binary name of load class.
     *
     * @return  If found class in specified jar file, return created 
     *          {@link java.lang.Class} object.
     *          If not found class, return <tt>null</tt>.
     *
     * @throws  IOException
     *          Throws if I/O error occurred when belows.
     *          <ul>
     *              <li> open jar file.
     *              <li> load jar entry.
     *          </ul>
     */
    private Class <?> findClassFromJar(JarFile jarFile, String className)
        throws IOException
    {
        try {
            Class <?> cls = null;
            Enumeration <JarEntry> jarEntries = jarFile.entries();
            while (jarEntries.hasMoreElements()) {
                JarEntry jarEntry = jarEntries.nextElement();
                _log.log(Level.FINE, "JarEntry : " + jarEntry.getName());
                if (!isClassFileName(jarEntry.getName()))
                    continue;
                String canonicalClassName =
                    toCanonicalClassName(jarEntry.getName());
                _log.log(Level.FINE,
                        "JarEntry to Canonical class name : " +
                        canonicalClassName);
                if (className.equals(canonicalClassName)) {
                    byte[] classBytes = getClassBytesFromJar(jarFile, jarEntry);
                    cls = defineClass(
                            canonicalClassName, classBytes, 0, 
                            classBytes.length);
                    break;
                }
            }
            return cls;
        } catch (IOException e) {
            _log.log(Level.SEVERE, e.getMessage(), e);
            throw e;
        } finally {
            if (jarFile != null)
                try {
                    jarFile.close();
                } catch (IOException e) {
                }
        }
    }

    /**
     * Load binary data of class specified <tt>jarEntry</tt> from specified
     * <tt>jarFile</tt>.
     *
     * @param jarFile   jar file object for load class.
     * @param jarEntry  jar entry object represent to class.
     *
     * @return  Loaded binary data of class.
     *
     * @throws  IOException
     *          Throws if I/O error occurred when load binary data from
     *          jar file.
     */
    private byte[] getClassBytesFromJar(JarFile jarFile, JarEntry jarEntry)
        throws IOException
    {
        InputStream is = jarFile.getInputStream(jarEntry);
        byte[] isBuff = new byte[(int) jarEntry.getSize()];
        is.read(isBuff, 0, isBuff.length);
        is.close();
        return isBuff;
    }

    /**
     * Check specified <tt>jarEntryName</tt> is represented to class file name.
     *
     * @param jarEntryName  name of jar entry.
     *
     * @return  If <tt>jarEntryName</tt> is represented to class file name,
     *          return <tt>true</tt>, otherwise return false.
     */
    private boolean isClassFileName(String jarEntryName) {
        return jarEntryName.endsWith(".class");
    }

    /**
     * Convert <tt>jarEntryName</tt> to canonical class name.
     *
     * <p> Typically, <tt>jarEntryName</tt> is represent to related file path
     * when packed to jar file. This method convert it to canonical class name
     * by replace path separator '/' to package separator '.' and cut off 
     * file extention '.class'.
     *
     * @param jarEntryName  jar entry name convert to it.
     *
     * @return  canonical class name.
     */
    private String toCanonicalClassName(String jarEntryName) {
        return jarEntryName
            .replace('/', '.')
            .substring(0, jarEntryName.lastIndexOf(".class"));
    }

    //////////////////////////////////////////////////////////// 
    // Start point.

    public static void main(String[] args)
        throws Exception
    {
        if (args.length != 2) {
            printUsage();
            return;
        }
        String targetClassName = args[0];
        System.out.println("Target class : " + targetClassName);
        String srcDir = args[1];
        System.out.println("Directory for search jar files : " + srcDir);

        LocalClassLoader ld = new LocalClassLoader(srcDir);
        Class <?> cls = Class.forName(targetClassName, true, ld);
        System.out.println("loaded class name : " + cls.getCanonicalName());
    }

    private static void printUsage() {
        String usage =
            "usage : " + 
            LocalClassLoader.class.getName() + " [ClassPath] [Directory]\n" +
            "\n" +
            "  ClassPath   canonical class path.\n" +
            "              (e.g. javatools.LocalClassLoader)\n" +
            "  Directory   Directory for search jar files.\n" +
            "              (e.g. test\\JarUtil_getClassList)\n";
        System.out.println(usage);
    }
}
