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

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is JAR file utilities.
 *
 * <p> $Id$
 *
 * @author Masatoshi Sato
 */
public class JarUtil
{
    /** Logging category */
    private static final Logger _log = Logger.getLogger(
            JarUtil.class.getName());

    //////////////////////////////////////////////////////////// 
    // Constant values.

    /** A Comparator for alphabetical sort jar file. */
    public static final Comparator <String> _SORT_BY_ALPHA_IGNORE_CASE =
        new Comparator <String> () {
            public int compare(String o1, String o2) {
                return o1.compareToIgnoreCase(o2);
            }
        };

    //////////////////////////////////////////////////////////// 
    // Public methods.

    /**
     * Pack each files or directories specified by <tt>targetRoots</tt>
     * parameter to JAR file. 
     *
     * <p> It is expected that <tt>targetRoots</tt> is included related path
     * from current working directory. For example, If <tt>targetRoots</tt> is
     * included path "target.txt" and current working directory is "/foo",
     * this method suppose that absolute path of it is "/foo/target.txt". 
     * When this situation, this method pack /foo/target.txt file to Jar file 
     * as JarEntry named "target.txt" (related path from current working 
     * directory).
     *
     * <p> If <tt>targetRoots</tt> parameter is included directory, search
     * file or directory to bottom layer recursively.
     * Jar file is created as temporary file to temporary directory, and 
     * return object of it, thus if created jar file will store parmanentry,
     * move it from temporary directory to user directory used by 
     * java.io.File#renameTo(String) method.
     *
     * <p> If it is specified file or directory in <tt>targetRoots</tt> 
     * parameter is not exists, it is ignored. 
     *
     * @param targetRoots   Array of path name of file or directory that 
     *                      is packed to JAR file.
     *
     * @return  A File object of JAR file created as temporary file, you should
     *          rename to appropriate name.
     *
     * @throws  IllegalArgumentException
     *          Throws if <tt>targetRoots</tt> parameter is null or elements 
     *          not includes in it.
     *
     * @throws  FileNotFoundException
     *          Throws if file or directory that contains <tt>targetRoots</tt>
     *          parameter did not exists.
     *
     * @throws  IOException
     *          Throws if file I/O error occurred at create jar file (add 
     *          JarEntry or read target file/directory). 
     */
    public static File pack(String[] targetRoots)
        throws IllegalArgumentException, FileNotFoundException, IOException
    {
        // Check parameters.
        if (targetRoots == null)
            throw new IllegalArgumentException("targetRoots is null.");
        if (targetRoots.length == 0)
            throw new IllegalArgumentException("targetRoots is empty.");

        // Create jar file as temporary file to temporary directory.
        File temp = File.createTempFile("temp", ".jar");
        JarOutputStream jos = new JarOutputStream(new FileOutputStream(temp));

        // Add all files and directories to jar file as JarEntry object.
        for (int idx = 0; idx < targetRoots.length; idx++)
            addEntries(jos, null, new File(targetRoots[idx]));

        jos.close();
        return temp;
    }

    /**
     * Return unmodified List collection contains JAR file pathes that is 
     * defined by system property "java.class.path".
     *
     * <p> jar file pathes is sorted by alphabetical order. 
     * (not case-sensitive)
     *
     * @return  jar file names as list collection.
     */
    public static List <String> getJarFiles() {
        String path = System.getProperty("java.class.path");
        String separator = System.getProperty("path.separator");
        List <String> pathList = new ArrayList <String> ();
        StringTokenizer st = new StringTokenizer(path, separator);
        while (st.hasMoreTokens()) {
            pathList.add(st.nextToken());
        }
        Collections.sort(pathList, _SORT_BY_ALPHA_IGNORE_CASE);
        return Collections.unmodifiableList(pathList);
    }

    /**
     * Return list collection of class name (FQCN) of classes included in 
     * the JAR file specified by "jarFilePath" parameter.
     *
     * <P> In a JAR file, there is possibility included in various JarEntry
     * other than class file. This method ignore the file as below.
     * <ul>
     *  <li> Name of JarEntry without ".class" postfix string.
     *  <li> JarEntry as a directory.
     * </ul>
     *
     * @param	jarFilePath Path of JAR file.
     *
     * @return	List collection of Java class name (FQCN).
     *
     * @throws	FileNotFoundException
     *          Throws if specified JAR file was not found.
     *
     * @throws  IllegalArgumentException
     *          Throws if <tt>jarFilePath</tt> is empty or is not represent
     *          to regular file.
     *
     * @throws	IOException
     *          Throws if read error occured when access to JAR file.
     */
    public static List <String> getClassNameList(String jarFilePath)
        throws FileNotFoundException, IllegalArgumentException,
               IOException
    {
        // check parameters.
        if (StringUtil.isEmpty(jarFilePath, true))
            throw new IllegalArgumentException("jarFilePath is empty.");
        File jarFile = new File(jarFilePath);
        if (!jarFile.exists())
            throw new FileNotFoundException(
                    "jarFilePath [" + jarFilePath + "] is not found.");
        if (!jarFile.isFile())
            throw new IllegalArgumentException(
                    "jarFilePath [" + jarFilePath + "] is not regular file.");

        List <String> classList = new ArrayList <String> ();
        JarInputStream jis = null;
        try {
            jis = new JarInputStream(new FileInputStream(jarFile));

            JarEntry entry = jis.getNextJarEntry();
            while ((entry = jis.getNextJarEntry()) != null) {
                if (entry.isDirectory()) {
                    _log.log(Level.FINE,
                            "Entry [" + entry.getName() + "] is directory," +
                            " ignore it.");
                    continue;	// ignore directory.
                }

                String classFile = entry.getName();
                int classExtIdx = classFile.lastIndexOf(".class");
                if (classExtIdx < 0) {
                    _log.log(Level.FINE,
                            "Entry [" + entry.getName() + "] is not class," +
                            " ignore it.");
                    continue;	// ignore other than class.
                }

                classFile = classFile.substring(0, classExtIdx);

                StringTokenizer st = new StringTokenizer(classFile, "/");
                String className = "";
                while (st.hasMoreTokens()) {
                    className += (String) st.nextToken() + ".";
                }
                classList.add(className.substring(0, className.length() - 1));
            }
            return Collections.unmodifiableList(classList);
        } finally {
            if (jis != null) {
                try {
                    jis.close();
                } catch (IOException e) {
                    // ignore this because of the stream for read and 
                    // is expected no effect to after processes.
                    // Just output log for some problems was occurred.
                    _log.log(Level.INFO, 
                            "IOException occurred when call" +
                            " JarInputStream#close() method.", e);
                }
            }
        }

    }

    //////////////////////////////////////////////////////////// 
    // Private methods.

    /**
     * Add file or directory specified to <tt>target</tt> parameter to 
     * JAR file as JarEntry.
     *
     * <p> <tt>parentDir</tt> parameter is parent directory of <tt>target</tt>
     * file or directory. This is used to create related path of <tt>target</tt>
     * file or directory from parent directory for name of JarEntry.
     * If <tt>parentDir<tt> set <tt>null</tt>, parentDir is get from
     * <tt>target</tt> automatically.
     *
     * <p> This method programmed for re-entrant. If <tt>target</tt> parameter
     * set directory, this method call ownself with <tt>parentDir</tt> parameter
     * set to <tt>target</tt> as parent directory for process sub-directory
     * recursively.
     * 
     * @param jos       Output stream of JAR file.
     * @param parentDir Parent directory of <tt>target</tt>.
     * @param target    A File object of target file or directory.
     *
     * @throws  IllegalArgumentException
     *          Throws if parameter <tt>jos<tt> or <tt>targetFile</tt> 
     *          is set null.
     *
     * @throws  FileNotFoundException
     *          Throws if file or directory that is specified to 
     *          <tt>parentDir</tt> or <tt>targetFile</tt> does not exists.
     *
     * @throws  IOException
     *          Throw if file IO failed.
     */
    private static void addEntries(JarOutputStream jos, File parentDir, 
            File targetFile)
        throws IllegalArgumentException, FileNotFoundException, IOException
    {
        // Check parameters.
        if (jos == null)
            throw new IllegalArgumentException("jos is null.");
        if (targetFile == null)
            throw new IllegalArgumentException("targetFile is null.");
        if (!targetFile.exists())
            throw new FileNotFoundException(
                    "targetFile [" + targetFile.getAbsolutePath() + "]" +
                    " does not exists.");

        if (parentDir == null)
            parentDir = targetFile.getParentFile();
        if (!parentDir.exists())
            throw new FileNotFoundException(
                    "parentDir [" + parentDir.getAbsolutePath() + "]" +
                    " does not exists.");

        // Create jar entry name.
        String targetAbsolutePath =
            toAvailableJarPath(targetFile.getAbsolutePath());
        String parentAbsolutePath =
            toAvailableJarPath(parentDir.getAbsolutePath());
        String entryName = targetAbsolutePath.substring(
                parentAbsolutePath.length() + 1);

        if (targetFile.isFile()) {
            addFileEntry(jos, targetFile, entryName);
        } else {
            addDirectoryEntry(jos, entryName);
            File[] subTargetFiles = targetFile.listFiles();
            for (int idx = 0; idx < subTargetFiles.length; idx++) {
                addEntries(jos, parentDir, subTargetFiles[idx]);
            }
        }
    }

    /**
     * Add file entry to JAR file.
     *
     * @param jos           Output stream of JAR file.
     * @param targetFile    A file object of target file.
     * @param entryName     Name of JarEntry.
     *
     * @throws  IOException
     *          Thorws if create JarEntry or read data from target file failed.
     */
    private static void addFileEntry(JarOutputStream jos, File targetFile,
            String entryName)
        throws IOException
    {
        if (jos == null)
            throw new IllegalArgumentException("jos is null.");
        if (targetFile == null)
            throw new IllegalArgumentException("targetFile is null.");
        if (StringUtil.isEmpty(entryName, true))
            throw new IllegalArgumentException("entryName is empty.");

        _log.log(Level.FINE, "entryName as file = [" + entryName + "]");

        BufferedInputStream bis =
            new BufferedInputStream(new FileInputStream(targetFile));
        jos.putNextEntry(new JarEntry(entryName));

        byte buf[] = new byte[1024];
        int count;
        while ((count = bis.read(buf, 0, buf.length)) != -1)
            jos.write(buf, 0, count);

        jos.closeEntry();
        bis.close();
    }

    /**
     * Add directory entry to JAR file.
     *
     * @param jos           Output stream of JAR file.
     * @param entryName     Name of JarEntry.
     *
     * @throws  IOException
     *          Throws if create JarEntry failed.
     */
    private static void addDirectoryEntry(JarOutputStream jos, String entryName)
        throws IOException
    {
        if (jos == null)
            throw new IllegalArgumentException("jos is null.");
        if (StringUtil.isEmpty(entryName, true))
            throw new IllegalArgumentException("entryName is empty.");

        _log.log(Level.FINE, "entryName as directory = [" + entryName + "/]");

        jos.putNextEntry(new JarEntry(entryName + "/"));
        jos.closeEntry();
    }

    /**
     * Exchange <tt>path</tt> to available path at JAR file.
     *
     * <p> Java is enabled both path separator Windows "\" or Unix "/", and
     * is enabled mixed both path separator that is included to path string.
     * This method replace Windows path separator "\" to Unix of it because of
     * for program simplification. (It makes no difference, but I like Unix,
     * that all.)
     *
     * @param path  file path that is exchanged to available path.
     *
     * @return  exchanged path. If <tt>path</tt> parameter is included only
     *          current directory "." or "./", return empty string (because of
     *          no include current directory to Jar file).
     *
     * @throws  IllegalArgumentException
     *          Throws if <tt>path</tt> is null or empty string.
     */
    private static String toAvailableJarPath(String path)
        throws IllegalArgumentException
    {
        if (StringUtil.isEmpty(path, true))
            throw new IllegalArgumentException("path is empty.");

        // Get out current directory.
        if (path.equals("."))
            return "";

        // take out prefix path that is represented current directory.
        if (path.startsWith(".\\") || path.startsWith("./")) {
            path = path.substring(2);
            if (path.length() == 0)
                return path;
        }

        // Exchange Windows path to unix path.
        StringTokenizer st = new StringTokenizer(path, "\\", true);
        StringBuffer sb = new StringBuffer();
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (token.equals("\\"))
                sb.append("/");
            else
                sb.append(token);
        }
        return sb.toString();
    }
}
