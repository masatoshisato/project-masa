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
package org.sd_network.io;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This is {@link java.io.FilenameFilter} implementation for class file and
 * jar file filter.
 *
 * <p> For filtering class file or jar file or both, you have to specified
 * target file type when call constructor. Default constructor is set file
 * type to {@link ClassJarFilenameFilter.TARGET#JAR_CLASS}.
 *
 * <br> To filter these files, this class check file name postfix string. 
 * If file name postfix is ".clsss", determine as class file.
 * If file name postfix is ".jar", determine as jar file.
 *
 * <p> Filtering file type is
 * <ul>
 *  <li> {@link ClassJarFilenameFilter.TARGET#CLASS} : for only class file.
 *  <li> {@link ClassJarFilenameFilter.TARGET#JAR} : for only jar file.
 *  <li> {@link ClassJarFilenameFilter.TARGET#JAR_CLASS} : for both file.
 * </ul>
 *
 * <br> If you specified {@link ClassJarFilenameFilter.TARGET#CLASS} or
 * {@link ClassJarFilenameFilter.TARGET#JAR_CLASS}, this class is accepted
 * a directory. Because these file types contains class file, and typically
 * each class files exists underlying directory hierarchically by package name.
 *
 * @author Masatoshi Sato
 */
public class ClassJarFilenameFilter
    implements FilenameFilter
{
    //////////////////////////////////////////////////////////// 
    // Public Constant values.

    /** Represent to filter target file type. */
    public enum TARGET {
        /**
         * This is represent class file.
         */
        CLASS,

        /**
         * This is represent jar file and class file.
         */
        JAR_CLASS,

        /**
         * This is represent jar file.
         */
        JAR
    };

    //////////////////////////////////////////////////////////// 
    // Private fields.

    /** Logger */
    private static final Logger _log = Logger.getLogger(
            ClassJarFilenameFilter.class.getName());

    /** Search target */
    private final TARGET _target;

    //////////////////////////////////////////////////////////// 
    // Constructors and Initializations.

    /**
     * This is default constructor.
     * It is filtering to both class file and jar file.
     * ({@link ClassJarFilenameFilter.TARGET#JAR_CLASS})
     */
    public ClassJarFilenameFilter() {
        _target = TARGET.JAR_CLASS;
    }

    /**
     * This is constructor with <tt>target</tt> parameter specified
     * {@link ClassJarFilenameFilter.TARGET} value.
     *
     * @param target    filtering target.
     *
     * @throws  IllegalArgumentException
     *          Throws if <tt>target</tt> parameter is specified <tt>null</tt>.
     */
    public ClassJarFilenameFilter(TARGET target)
        throws IllegalArgumentException
    {
        if (target == null)
            throw new IllegalArgumentException("target is null.");

        _target = target;
    }

    //////////////////////////////////////////////////////////// 
    // Implements to FilenameFilter.

    public boolean accept(File dir, String name) {
        File targetFile = null;
        try {
            targetFile = new File(
                dir.getCanonicalPath() +
                System.getProperty("file.separator") +
                name);
        } catch (IOException e) {
            _log.log(Level.WARNING, e.getMessage(), e);
            return false;
        }

        // check targetFile is directory.
        // If target cointains class file, directory must be accepted because
        // of each class files included directory, and search class file
        // under the directory recursively.
        if (isContainsClass() && targetFile.isDirectory()) {
            _log.log(Level.FINE,
                    "file [" + targetFile.getName() + "] is directory," +
                    " accepted.");
            return true;
        }

        // check name of targetFile endsWith ".jar" or ".class".
        if ((isContainsClass() && targetFile.getName().endsWith(".class"))
                || isContainsJar() && targetFile.getName().endsWith(".jar"))
        {
            _log.log(Level.FINE, 
                    "file [" + targetFile.getName() + "] is accepted.");
            return true;
        }
        _log.log(Level.FINE, 
                "file [" + targetFile.getName() + "] is ignored.");
        return false;
    }

    //////////////////////////////////////////////////////////// 
    // Private methods.

    /**
     * Check whether filtering target contains class file.
     *
     * @return  If filtering target is 
     *          {@link ClassJarFilenameFilter.TARGET.CLASS} or
     *          {@link ClassJarFilenameFilter.TARGET.JAR_CLASS}, return true.
     *          Otherwise return false.
     */
    private boolean isContainsClass() {
        return (_target == TARGET.CLASS || _target == TARGET.JAR_CLASS);
    }

    /**
     * Check whether filtering target contains jar file.
     *
     * @return  If filtering target is
     *          {@link ClassJarFilenameFilter.TARGET.JAR} or
     *          {@link ClassJarFilenameFilter.TARGET.JAR_CLASS}, return true.
     *          Otherwise return false.
     */
    private boolean isContainsJar() {
        return (_target == TARGET.JAR || _target == TARGET.JAR_CLASS);
    }
}
