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
package org.sd_network.test;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.LogManager;

import junit.framework.AssertionFailedError;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestFailure;
import junit.framework.TestListener;
import junit.framework.TestSuite;
import junit.framework.TestResult;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import org.sd_network.loader.LocalClassLoader;
import org.sd_network.util.Config;
import org.sd_network.util.JarUtil;

/**
 * This class is customized test runner.
 * This class extended TestSuite of junit framework. This has Main method
 * for run by cui. 
 *
 * <p> <b> Usage </b><br>
 * org.sd_network.test.TestRunner CLASS_PATH_PREFIX &lt;OPTIONS&gt;
 *
 * <p><b> Parameters. </b><br>
 * <ul>
 *  <li><b> CLASS_PATH_PREFIX </b><br>
 *      You have to specifed class path prefix of test classes. 
 *      For example, if you specified "org.sd_network", run all test cases 
 *      of TestCase classes under the "org.sd_network" name space. 
 *  <li><b> -l LOGGER_PROPERTY_FILE_PATH </b><br>
 *      You have to specified property file path for Logger 
 *      (for java.util.logging packages)
 *  <li><b> -h </b><br>
 *      Display usage.
 *  <li><b> -p TEST_PROPERTY_FILE_PATH </b><br>
 *      You have to specified property file path which is used to target
 *      test classes.
 * </ul>
 *
 * <p>
 * When execute Main method of this class, set system property "UnitTest"
 * to "TRUE". If you expect used class or method for unit test only, check
 * system property "UnitTest" is "TRUE" in the class or method, and throws
 * {@link java.lang.UnsupportedOperationException} if not set the system
 * property.
 *
 * <p> $Id$
 *
 * @author Masatoshi sato
 */
public class TestRunner
{
    /** Default Logger. */
    private static final Logger _log = Logger.getLogger(
            TestRunner.class.getName());

    ////////////////////////////////////////////////////////////
    // Private fields.

    private static final PrintStream _out = System.out;
    private static final CommandLineParser _parser = new PosixParser();
    private static final Options _options;
    static {
        Options buf = new Options();
        buf.addOption(new Option("p", "property", true, 
                    "Property file which is used to target test classes."));
        buf.addOption(new Option("l", "log", true, 
                    "Log configuration file path for Logger" +
                    "(java.util.logger)."));
        buf.addOption(new Option("h", "help", false, 
                    "Display usage."));
        _options = buf;
    };

    private List specPackageList;

    ////////////////////////////////////////////////////////////
    // Execute entry point.

    /**
     * This is a execute entry point.
     *
     * @param args  Array of command line parameters.
     */
    public static void main(String[] args) {
        // set system property "UnitTest"
        System.setProperty("UnitTest", "TRUE");

        // parse command line parameters.
        CommandLine commandLine = null;
        try {
            commandLine = _parser.parse(_options, args);
        } catch (ParseException e) {
            printUsage(e.getMessage());
            return;
        }

        // Display Usage.
        if (commandLine.hasOption("h")) {
            printUsage();
            return;
        }

        // Set log configuration.
        try {
            LogManager.getLogManager().readConfiguration(
                    new FileInputStream(commandLine.getOptionValue("l")));
        } catch (Exception e) {
            printUsage(e.getMessage());
        }

        // Set configuration.
        try {
            Config.load(commandLine.getOptionValue("p"));
        } catch (Exception e) {
            printUsage(e.getMessage());
            return;
        }

        // Set test packages, classes, and methods.
        String[] packageNames = commandLine.getArgs();
        TestRunner tester = new TestRunner();
        tester.run(packageNames);
    }

    private static final void printUsage(String errorMessage) {
        _out.println(errorMessage);
        printUsage();
    }

    private static final void printUsage() {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("TestRunner", _options);
    }

    ////////////////////////////////////////////////////////////
    // Testing methods.

    private void run(String[] packageNames) {
        List <Class <? extends TestCase>> testClassList =
            getTestClasses(packageNames);
        _out.println("TestCase is " + testClassList.size());

        List <TestFailure> failed = new ArrayList <TestFailure> ();
        List <TestFailure> errored = new ArrayList <TestFailure> ();
        Iterator <Class <? extends TestCase>> testCases =
            testClassList.iterator();

        while (testCases.hasNext()) {

            Class <? extends TestCase> testCase = testCases.next();
            _out.print(testCase.getName() + " > ");
            TestSuite suite = new TestSuite(testCase);
            TestResult result = new TestResult();
            result.addListener(_listener);
            suite.run(result);
            if (result.wasSuccessful()) {
                _out.println(" OK.");
            } else {
                _out.println(" NG.");
                for (Enumeration <TestFailure> i = result.failures();
                        i.hasMoreElements(); )
                {
                    failed.add(i.nextElement());
                }
                for (Enumeration <TestFailure> i = result.errors();
                        i.hasMoreElements(); )
                {
                    errored.add((TestFailure) i.nextElement());
                }
            }
        }
        for (Iterator <TestFailure> errores = errored.iterator();
                errores.hasNext(); )
        {
            printFailedInfo(errores.next());
        }
        for (Iterator <TestFailure> failes = failed.iterator();
                failes.hasNext(); )
        {
            printFailedInfo(failes.next());
        }

        _out.println("Total : error=" + errored.size() +
            ": failure=" + failed.size() + ".");
    }

    private void printFailedInfo(TestFailure failure) {
        _out.println("*** " + failure.failedTest());
        _out.println(getFilterdString(failure.thrownException()));
        _out.println();
    }

    /**
     * Return List collection that is included class objects that extends from
     * TestCase class.
     */
    private List <Class <? extends TestCase>> getTestClasses(
            String[] packageNames)
    {
        List <String> jarFileNameList = JarUtil.getJarFiles();
        if (jarFileNameList.size() == 0)
            throw new IllegalStateException(
                "Not found any jar files in classpath.");

        Class testCase;
        try {
            testCase = Class.forName("junit.framework.TestCase");
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException(
                    "Not found junit.framework.TestCase class in classpath. " +
                    ": " + e.getMessage());
        }

        List <String> classNameList = new ArrayList <String> ();
        for (String jarFileName : jarFileNameList) {
            try {
                classNameList.addAll(JarUtil.getClassNameList(jarFileName));
            } catch (FileNotFoundException e) {
                throw new IllegalStateException(
                        "Not found jar file. : " + e.getMessage());
            } catch (IOException e) {
                throw new IllegalStateException(
                        "I/O error occurred. : " + e.getMessage());
            }
        }

        List <Class <? extends TestCase>> testClassList =
            new ArrayList <Class <? extends TestCase>> ();
        for (String className : classNameList) {
            Class <?> cls = null;
            try {
                cls = Class.forName(className);
            } catch (ClassNotFoundException e) {
                throw new IllegalStateException(
                        "Not found class " +
                        " [" + className + "] in classpath. : " +
                        e.getMessage());
            }

            // ignore a class unextends from TestCase.
            Class <? extends TestCase> testCaseClass = null;
            try {
                testCaseClass = cls.asSubclass(TestCase.class);
            } catch (ClassCastException e) {
                _log.log(Level.FINE, 
                        "Class [" + className + "] does not extends from" +
                        " TestCase. Ignore this class.");
                continue;
            }

            // check class path that is target or not.
            for (String targetPrefix : packageNames) {
                if (cls.getName().startsWith(targetPrefix)) {
                    testClassList.add(testCaseClass);
                }
            }
        }
        return testClassList;
    }


    private String getFilterdString(Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        String buf = sw.getBuffer().toString();
        BufferedReader br = new BufferedReader(new StringReader(buf));

        String line;
        StringBuffer sb = new StringBuffer();
        try {
            while ((line = br.readLine()) != null) {
                if (!isPatternMatch(line))
                    sb.append(line + "\n");
            }
        } catch (IOException e) {
            return buf;
        }
        return sb.toString();
    }

    private boolean isPatternMatch(String buf) {
        for (int idx = 0; idx < _futilePatterns.length; idx++) {
            if (buf.indexOf(_futilePatterns[idx]) >= 0)
                return true;
        }
        return false;
    }
    private static final String[] _futilePatterns= new String[] {
        "junit.framework.TestCase",
        "junit.framework.TestResult",
        "junit.framework.TestSuite",
        "junit.framework.Assert.", // don't filter AssertionFailure
        "junit.swingui.TestRunner",
        "junit.awtui.TestRunner",
        "junit.textui.TestRunner",
        "java.lang.reflect.Method.invoke("
    };

    //////////////////////////////////////////////////////////////
    // Internal classes.

    private static final PrintingTestListener _listener =
        new PrintingTestListener();

    private static class PrintingTestListener
        implements TestListener
    {
        private boolean failed;

        public void startTest(Test test) {
        }

        public void endTest(Test test) {
            if (failed)
                failed = false;
            else
                System.out.print(".");
        }

        public void addFailure(Test test, AssertionFailedError t) {
            System.out.print("F");
            failed = true;
        }

        public void addError(Test test, Throwable t) {
            System.out.print("E");
            failed = true;
        }
    }
}
