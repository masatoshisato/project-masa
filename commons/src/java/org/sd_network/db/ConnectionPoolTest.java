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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import junit.framework.TestCase;

import org.sd_network.util.Config;

/**
 * {@link org.sd_network.db.ConnectionPool} �̃e�X�g�P�[�X���`���܂��B
 * <ul>
 *  <li> �C���X�^���X�`�F�b�N
 *      <ul>
 *       <li> �f�t�H���g�C���X�^���X�̎擾
 *       <li> ����ID�̃C���X�^���X�̎擾
 *       <li> �w���ID�ɕR�Â�ConnectionParameter�����݂��Ȃ��G���[
 *      </ul>
 *  <li> Connection�擾�`�F�b�N
 *      <ul>
 *       <li> ����i�V�K�����j
 *       <li> ����i�����C���X�^���X�j
 *       <li> Connection�����ł��Ȃ����߂̃^�C���A�E�g�G���[
 *       <li> �S�Ă�Connection���g�p����Ă��邽�߂̃^�C���A�E�g�G���[
 *      </ul>
 *  <li> Connection�ԋp�`�F�b�N
 *      <ul>
 *       <li> ����
 *       <li> ����i�N���[�Y���ꂽConnection�̂���Pool�ɂ͕ԋp���Ȃ��j
 *       <li> Null�G���[
 *       <li> ConnectionProxy�w��G���[
 *       <li> ���`�F�b�N�A�E�gPool�ɑ΂���release�G���[
 *      </ul>
 *  <li> clear�`�F�b�N
 *      <ul>
 *       <li> ����
 *       <li> �`�F�b�N�A�E�gConnection�����݂��邽�߂�Clear�ł��Ȃ��G���[
 *      </ul>
 * </ul>
 *
 * <p> �e�X�g�P�[�X�Ɏg�p����JDBC�ڑ����͈ȉ���ID���g�p���܂��B
 * <ul>
 *  <li> "default" : �ڑ��\
 *  <li> "test1"   : �ڑ��\
 *  <li> "test2    : �ڑ����̐ݒ�~�X�ɂ��ڑ��s��
 * </ul>
 *
 * <p> $Id$
 * 
 * @author Masatoshi Sato
 */
public class ConnectionPoolTest
    extends TestCase
{
    //////////////////////////////////////////////////////////// 
    // Class fields.

    /** Logger. */
    private static final Logger _log = Logger.getLogger(
        ConnectionPoolTest.class.getName());

    //////////////////////////////////////////////////////////// 
    // Instance fields.

    private String _name;

    ////////////////////////////////////////////////////////////
    // Constructor and Initialization.

    public ConnectionPoolTest(String name)
        throws Exception
    {
        super(name);
        _name = name;
    }

    public void setUp()
        throws Exception
    {
        _log.log(Level.FINE, "--- test [" + _name + "]");
    }

    public void tearDown()
        throws Exception
    {
        super.tearDown();
        ConnectionPool.getInstance().clear();
        ConnectionPool.getInstance("test1").clear();
        ConnectionPool.getInstance("test2").clear();
    }

    ////////////////////////////////////////////////////////////
    // �C���X�^���X�`�F�b�N

    /**
     * �v�[�����ʎq���w�肵�Ȃ��ŁA�f�t�H���g���ʎq��ConnectionPool�C���X�^���X
     * ���擾�ł��鎖���m�F���܂��B
     */
    public void testInstanceCheck_Default()
        throws Exception
    {
        ConnectionPool pool = ConnectionPool.getInstance();
        assertNotNull(pool);
        assertEquals("default", pool.getID());
        assertEquals(0, pool.getCheckedOutConnections());
        assertEquals(0, pool.getCurrentPoolSize());

        ConnectionParameter parameter = pool.getConnectionParameter();
        assertEquals("default", parameter.getID());
    }

    /**
     * ����̎��ʎq��ConnectionPool�̃C���X�^���X���擾�ł��鎖��
     * �m�F���܂��B
     */
    public void testInstanceCheck_SpecifiedID()
        throws Exception
    {
        ConnectionPool pool = ConnectionPool.getInstance("test1");
        assertNotNull(pool);
        assertEquals("test1", pool.getID());
        assertEquals(0, pool.getCheckedOutConnections());
        assertEquals(0, pool.getCurrentPoolSize());

        ConnectionParameter parameter = pool.getConnectionParameter();
        assertEquals("test1", parameter.getID());
    }

    /**
     * ����̎��ʎq�ɕR�Â�ConnectionParameter�����݂��Ȃ��ꍇ��
     * IllegalStateException���X���[����鎖���m�F���܂��B
     */
    public void testInstanceCheck_ConnectionParameterNotFoundError()
        throws Exception
    {
        try {
            ConnectionPool.getInstance("test99");
            fail("ConnectionParameter check error.");
        } catch (ConnectionPoolException e) {
            assertEquals(
                    "ConnectionParameter not found for test99",
                    e.getMessage());
        }
    }

    //////////////////////////////////////////////////////////// 
    // Connection�擾�`�F�b�N

    /**
     * �V�K��Connection����������A�擾�ł��鎖���m�F���܂��B
     */
    public void testEngageCheck_NewConnection()
        throws Exception
    {
        ConnectionPool pool = ConnectionPool.getInstance();
        Connection con = pool.engageConnection(1);
        assertNotNull(con);
        assertEquals(1, pool.getCheckedOutConnections());
        assertEquals(0, pool.getCurrentPoolSize());
        con.close();
    }

    /**
     * �v�[������Ă���Connection���擾�ł��鎖���m�F���܂��B
     */
    public void testEngageCheck_ExistsConnection()
        throws Exception
    {
        ConnectionPool pool = ConnectionPool.getInstance();
        assertEquals(0, pool.getCheckedOutConnections());
        assertEquals(0, pool.getCurrentPoolSize());

        Connection con1 = pool.engageConnection(1);
        assertNotNull(con1);
        assertEquals(1, pool.getCheckedOutConnections());
        assertEquals(0, pool.getCurrentPoolSize());

        con1.close();
        assertEquals(0, pool.getCheckedOutConnections());
        assertEquals(1, pool.getCurrentPoolSize());
        
        Connection con2 = pool.engageConnection(1);
        assertNotNull(con2);
        assertEquals(1, pool.getCheckedOutConnections());
        assertEquals(0, pool.getCurrentPoolSize());

        con2.close();
    }

    /**
     * ConnectionParameter��񂪊Ԉ���Ă��邽�߂ɐV����Connection������
     * �ł����AConnectTimeoutException���X���[����鎖���m�F���܂��B
     */
    public void testEngageCheck_TimeoutByConnectionCreationError()
        throws Exception
    {
        ConnectionPool pool = ConnectionPool.getInstance("test2");
        try {
            Connection con1 = pool.engageConnection(1);
            if (con1 != null)
                con1.close();
            fail("Creation of connection error.");
        } catch (ConnectTimeoutException e) {
            assertEquals(
                    "[test2]: Failed to engage connection.", e.getMessage());
        }
    }

    /**
     * �S�Ă�Connection���g�p����Ă��邽�߂�ConnectTimeoutException��
     * �X���[����鎖���m�F���܂��B
     */
    public void testEngageCheck_TimeoutByAllConnectionIsUsingError()
        throws Exception
    {
        ConnectionPool pool = ConnectionPool.getInstance("test1");
        Connection[] cons = new Connection[20];
        for (int idx = 0; idx < ConnectionPool.MAX_POOL_SIZE; idx++) {
            cons[idx] = pool.engageConnection(1);
        }
        assertEquals(20, pool.getCheckedOutConnections());
        assertEquals(0, pool.getCurrentPoolSize());

        try {
            Connection con = pool.engageConnection(1);
            if (con != null)
                con.close();
            fail("Connection pool size check error.");
        } catch (ConnectTimeoutException e) {
            assertEquals(
                    "[test1]: Failed to engage connection.", e.getMessage());
            assertEquals(20, pool.getCheckedOutConnections());
            assertEquals(0, pool.getCurrentPoolSize());
        }

        for (int idx = 0; idx < cons.length; idx++) {
            cons[idx].close();
        }
        assertEquals(0, pool.getCheckedOutConnections());
        assertEquals(20, pool.getCurrentPoolSize());
    }

    //////////////////////////////////////////////////////////// 
    // Connection�ԋp�`�F�b�N

    /**
     * �`�F�b�N�A�E�g���ꂽConnection������Ƀv�[���ɕԋp�ł��鎖��
     * �m�F���܂��B
     */
    public void testReleaseCheck_OK()
        throws Exception
    {
        ConnectionPool pool = ConnectionPool.getInstance("test1");
        Connection con = pool.engageConnection(1);
        assertEquals(1, pool.getCheckedOutConnections());
        assertEquals(0, pool.getCurrentPoolSize());

        con.close();
        assertEquals(0, pool.getCheckedOutConnections());
        assertEquals(1, pool.getCurrentPoolSize());
    }

    /**
     * ���ɐؒf����Ă���Connection���v�[���ɕԋp�ł��鎖���m�F���܂��B
     * �������A�C���X�^���X�̓v�[�����ꂸ�A�`�F�b�N�A�E�g�������J�E���g
     * ���܂��B
     */
    public void testReleaseCheck_ClosedConnection()
        throws Exception
    {
        ConnectionPool pool = ConnectionPool.getInstance("test1");
        Connection con1 = pool.engageConnection(1);
        assertEquals(1, pool.getCheckedOutConnections());
        assertEquals(0, pool.getCurrentPoolSize());

        ConnectionParameter parameter = pool.getConnectionParameter();
        Class.forName(parameter.getJDBCDriver());
        Connection con2 = DriverManager.getConnection(
                parameter.getURL(),
                parameter.getUserName(),
                parameter.getPassword());
        con2.close();
        pool.releaseConnection(con2);
        assertEquals(0, pool.getCheckedOutConnections());
        assertEquals(0, pool.getCurrentPoolSize()); // �ԋp�Ȃ�

        // ���̃e�X�g�ł͖������Connection�𐶐����ĕԋp�������s���Ă��邽�߁A
        // ConnectionPool�̃`�F�b�N�A�E�g���ɖ������������Ă��܂��B
        // ���̂��߁A����Ƀ`�F�b�N�A�E�g����Connection��close���悤�Ƃ����
        // ConnectionPoolException����������B
        // ���̃e�X�g�����̎��Ȃ̂Ŗ�������B
        try {
            con1.close();
        } catch (ConnectionPoolException e) {
            // OK.
        }
    }

    /**
     * Null�I�u�W�F�N�g���v�[���ɕԋp���悤�Ƃ�������NullPointerException��
     * �X���[����鎖���m�F���܂��B
     */
    public void testReleaseCheck_NullError()
        throws Exception
    {
        ConnectionPool pool = ConnectionPool.getInstance("test1");
        try {
            pool.releaseConnection(null);
            fail("Null check error.");
        } catch (NullPointerException e) {
            assertEquals("con", e.getMessage());
        }
    }

    /**
     * �v�[���֕ԋp���悤�Ƃ��Ă���̂�{@link java.sql.Connection} �ł͂Ȃ��A
     * {@link ConnectionProxy} �������ꍇ��IllegalArgumentException���X���[
     * ����鎖���m�F���܂��B
     */
    public void testReleaseCheck_SpecifiedConnectionProxyError()
        throws Exception
    {
        ConnectionPool pool = ConnectionPool.getInstance("test1");
        Connection con1 = pool.engageConnection(1);
        assertEquals(1, pool.getCheckedOutConnections());
        
        try {
            pool.releaseConnection(con1);
            fail("Connection instance check error.");
        } catch (IllegalArgumentException e) {
            assertEquals(1, pool.getCheckedOutConnections());
            assertEquals(
                    "ConnectionProxy have to call Connection#close() method " +
                    "instead of releaseConnection(Connection) method.",
                    e.getMessage());
            assertEquals(1, pool.getCheckedOutConnections());
        }
        if (con1 != null)
            con1.close();
    }

    /**
     * �`�F�b�N�A�E�g���ꂽConnection�����݂��Ȃ��v�[���ɑ΂��āA�ڑ��ς݂�
     * Connection��ԋp���悤�Ƃ�������ConnectionPoolException���������鎖��
     * �m�F���܂��B
     */
    public void testReleaseCheck_InitialPoolError()
        throws Exception
    {
        ConnectionPool pool = ConnectionPool.getInstance("test1");
        ConnectionParameter parameter = pool.getConnectionParameter();
        Class.forName(parameter.getJDBCDriver());
        Connection con1 = DriverManager.getConnection(
                parameter.getURL(),
                parameter.getUserName(),
                parameter.getPassword());
        try {
            pool.releaseConnection(con1);
            fail("Number of checked out connection check error.");
        } catch (ConnectionPoolException e) {
            assertEquals(
                    "[test1]: checked out connection is nothing.",
                    e.getMessage());
        }
        assertTrue(con1.isClosed());
    }

    //////////////////////////////////////////////////////////// 
    // Clear�`�F�b�N

    /**
     * {@link ConnectionPool#clear()} ���\�b�h������ɓ��삷�鎖���m�F���܂��B
     * clear()���\�b�h�����s�����ƁA�v�[���ɕێ�����Ă���Connection��
     * �S�Đؒf����A�S�Ẵv�[�����̓N���A����܂��B
     */
    public void testClearCheck_OK()
        throws Exception
    {
        ConnectionPool pool = ConnectionPool.getInstance("test1");

        // ������ԃ`�F�b�N
        assertEquals(0, pool.getCheckedOutConnections());
        assertEquals(0, pool.getCurrentPoolSize());

        // �v�[���̍ő�l�܂Ń`�F�b�N�A�E�g
        Connection[] cons = new Connection[ConnectionPool.MAX_POOL_SIZE];
        for (int idx = 0; idx < cons.length; idx++) {
            cons[idx] = pool.engageConnection(1);
        }
        assertEquals(
                ConnectionPool.MAX_POOL_SIZE, pool.getCheckedOutConnections());
        assertEquals(0, pool.getCurrentPoolSize());

        // �S�Ă�Connection�������[�X
        for (int idx = 0; idx < cons.length; idx++) {
            cons[idx].close();
        }
        assertEquals(0, pool.getCheckedOutConnections());
        assertEquals(
                ConnectionPool.MAX_POOL_SIZE, pool.getCurrentPoolSize());

        // clear.
        pool.clear();
        assertEquals(0, pool.getCheckedOutConnections());
        assertEquals(0, pool.getCurrentPoolSize());
    }

    /**
     * �`�F�b�N�A�E�g����Ă���Connection�����݂���ꍇ�ł�
     * {@link ConnectionPool#clear()} ���\�b�h�ŏ���������鎖���m�F���܂��B
     */
    public void testClearCheck_ExistCheckedOutConnections()
        throws Exception
    {
        ConnectionPool pool = ConnectionPool.getInstance("test1");

        // ������ԃ`�F�b�N
        assertEquals(0, pool.getCheckedOutConnections());
        assertEquals(0, pool.getCurrentPoolSize());

        // Connection�擾
        Connection[] cons = new Connection[ConnectionPool.MAX_POOL_SIZE];
        for (int idx = 0; idx < cons.length; idx++) {
            cons[idx] = pool.engageConnection(1);
        }
        assertEquals(cons.length, pool.getCheckedOutConnections());
        assertEquals(0, pool.getCurrentPoolSize());

        // ���������[�X
        for (int idx = 0; idx < (cons.length / 2); idx++) {
            cons[idx].close();
        }
        assertEquals((cons.length / 2), pool.getCheckedOutConnections());
        assertEquals(cons.length - (cons.length / 2), pool.getCurrentPoolSize());

        // clear.
        pool.clear();
        assertEquals(0, pool.getCheckedOutConnections());
        assertEquals(0, pool.getCurrentPoolSize());
    }
}
