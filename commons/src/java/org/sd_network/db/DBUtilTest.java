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
package org.sd_network.db;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.sd_network.util.Config;

/**
 * Unit test for {@link org.sd_network.db.DBUtil} class.
 *
 * <p> $Id$
 *
 * @author Masatoshi Sato
 */
public class DBUtilTest
    extends TestCase
{
    /** Default logger. */
    private static final Logger _log = Logger.getLogger(
        DBUtilTest.class.getName());

    //////////////////////////////////////////////////////////// 
    // Constructors, Initializations and Finalizations.

    public DBUtilTest(String name) {
        super(name);
        Config config = Config.getInstance();
        ConnectionPool pool = ConnectionPool.getInstance();
    }

    public void setUp()
        throws Exception
    {
        super.setUp();
        DBUtil.execute(
                "CREATE TABLE IF NOT EXISTS unit_test_table " +
                "(" +
                " id    VARCHAR(255) NOT NULL PRIMARY KEY, " +
                " name  VARCHAR(255) NOT NULL DEFAULT '' " +
                ")");
    }

    public void tearDown()
        throws Exception
    {
        super.tearDown();
        DBUtil.update("DROP TABLE unit_test_table");
    }

    //////////////////////////////////////////////////////////// 
    // Test case.

    /**
     * Test case that insert data to a database normally.
     */
    public void testInsert_UseDefaultPool()
        throws Exception
    {
        Map<String, Object> columnMap = new HashMap<String, Object>();
        columnMap.put("id", "testID1");
        columnMap.put("name", "name1");
        assertEquals(1, DBUtil.insert("unit_test_table", columnMap));
    }

    /**
     * Test case that NullPointerException is thrown.
     */
    public void testInsert_NullPointer()
        throws Exception
    {
        // by specify null to "tableName" argument.
        Map<String, Object> columnMap = new HashMap<String, Object>();
        try {
            DBUtil.insert(null, columnMap);
            fail("It should be thrown NullPointerException.");
        } catch (NullPointerException e) {
            assertEquals("tableName", e.getMessage());
        }

        // by specify null to "columnMap" argument.
        try {
            DBUtil.insert("unit_test_table", null);
            fail("It should be thrown NullPointerException.");
        } catch (NullPointerException e) {
            assertEquals("columnMap", e.getMessage());
        }
    }

    /**
     * Test case that IllegalArgumentException is thrown.
     */
    public void testInsert_IllegalArgument()
        throws Exception
    {
        // by specify empty Map to "columnMap" argument.
        Map<String, Object> columnMap = new HashMap<String, Object>();
        try {
            DBUtil.insert("unit_test_table", columnMap);
            fail("It should be thrown IllegalArgumentException.");
        } catch (IllegalArgumentException e) {
            assertEquals("column map was empty.", e.getMessage());
        }
    }

    /**
     * Test case that update specified record normally.
     */
    public void testUpdate()
        throws Exception
    {
        assertEquals(0, DBUtil.update("DELETE FROM unit_test_table"));

        Map<String, Object> columnMap = new HashMap<String, Object>();
        columnMap.put("id", "testID1");
        columnMap.put("name", "name1");
        DBUtil.insert("unit_test_table", columnMap);

        assertEquals(1,
                DBUtil.update(
                    "DELETE FROM unit_test_table WHERE id=?",
                    new Object[] {"testID1"}));
    }

    /**
     * Test case that IllegalArgumentExcepiton is thrown.
     */
    public void testUpdate_IllegalArgument_sql()
        throws Exception
    {
        // by specify empty string to "sql" argument.
        try {
            DBUtil.update("", new Object[] {"testID1"});
            fail("It should be thrown IllegalArgumentException.");
        } catch (IllegalArgumentException e) {
            assertEquals("sql is empty.", e.getMessage());
        }
    }

    /**
     * Test case that NullPointerException is thrown.
     */
    public void testUpdate_NullPointer()
        throws Exception
    {
        // by specify null to "sql" argument.
        try {
            DBUtil.update(null, new Object[] {"testID1"});
            fail("It should be thrown NullPointerException.");
        } catch (NullPointerException e) {
            assertEquals("sql", e.getMessage());
        }

        // by specify null to the element of "args" argument.
        try {
            DBUtil.update(
                    "DELETE FROM unit_test_table WHERE id=?",
                    new Object[] {null});
            fail("It should be thrown NullPointerException.");
        } catch (NullPointerException e) {
            assertEquals("args[0]", e.getMessage());
        }
    }

    /**
     * Test case that execute sql statement normally.
     */
    public void testExecute()
        throws Exception
    {
        DBUtil.execute(
                "ALTER TABLE unit_test_table " +
                "ADD created TIMESTAMP DEFAULT NOW()");
    }

    /**
     * Test case that NullPointerException is thrown.
     */
    public void testExecute_NullPointer()
        throws Exception
    {
        // by specify null to "sql" argument.
        try {
            DBUtil.execute(null);
        } catch (NullPointerException e) {
            assertEquals("sql", e.getMessage());
        }
    }

    /**
     * Test case that IllegalArgumentException is thrown.
     */
    public void testExecute_IllegalArgument()
        throws Exception
    {
        // by specify empty string to "sql" argument.
        try {
            DBUtil.execute("");
        } catch (IllegalArgumentException e) {
            assertEquals("sql is empty.", e.getMessage());
        }

        // by specify DML statement to "sql" argument.
        try {
            DBUtil.execute("SELECT * FROM unit_test_table");
        } catch (IllegalArgumentException e) {
            assertEquals("sql was defined DML statement.", e.getMessage());
        }
    }
}
