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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * �f�[�^�x�[�X�A�N�Z�X�p�̃��[�e�B���e�B���\�b�h���`���܂��B
 * �����������s�����\�b�h�ł��A�ȉ��̂R�̃��\�b�h����`����Ă��܂��B
 * <ul>
 *  <li> Connection�̎w��
 *  <li> ConnectionPool�̎��ʎq���w��
 *  <li> �f�t�H���g��ConnectionPool���w��
 * </ul>
 * �����̃��\�b�h�́A�g�����U�N�V�����̃R�~�b�g�⃍�[���o�b�N�ɂ��Ă�
 * �w�Ǎl������Ă��܂���B�����Connection���g�p���Ăr�p�k�����s����
 * ���\�b�h�̏ꍇ�Ɏ����R�~�b�g���[�h��ON�̏ꍇ�Ɏ����I�ɃR�~�b�g���[���o�b�N
 * �����s����邾���ł��B����āA�P�̃g�����U�N�V�����ŕ����̂r�p�k�����s
 * ����ꍇ�A�����̃N���X�ɒ�`����Ă��郁�\�b�h���A���̈ꕔ�ɗ��p����ꍇ�A
 * ���̃N���X�̃��\�b�h���Ăяo���O�Ɏ��O��Connection���擾���A�����R�~�b�g
 * ���[�h��OFF�ɂ��Ă���A���̃N���X��Connection���w�肷��^�C�v�̃��\�b�h
 * �𗘗p���Ă��������B
 *
 * <p> �f�[�^�x�[�X�G���[�����������ꍇ�A{@link java.sql.SQLException} �͏��
 * {@link org.sd_network.db.DBException} �Ƀ��b�v����A�����^�C����O�Ƃ���
 * �X���[����܂��B
 *
 * <p> $Id$
 *
 * @author Masatoshi Sato
 */
public class DBUtil
{
    //////////////////////////////////////////////////////////// 
    // Class fields.

    /** Logger. */
    private static final Logger _log = Logger.getLogger(
            DBUtil.class.getName());

    //////////////////////////////////////////////////////////// 
    // Public methods.

    /**
     * �����Ŏw�肳�ꂽ�e�[�u�����E�p�����[�^��SQL��INSERT�������s���܂��B
     * ���̃��\�b�h�̓g�����U�N�V�����̃R�~�b�g�������I�ɍs���܂��B<br>
     * �f�t�H���g��ConnectionPool���g�p���܂��B
     *
     * @param tableName INSERT�Ώۂ̃e�[�u�����B
     * @param columnMap �J�������ƒl��Map�R���N�V�����B
     *
     * @return  INSERT���R�[�h���B�i�ʏ�͂P�j
     *
     * @throws  DBException
     *          �f�[�^�x�[�X�G���[�����������ꍇ�ɃX���[���܂��B
     */
    public static int insert(String tableName, Map<String, Object> columnMap)
        throws DBException
    {
        return insert((String) null, tableName, columnMap);
    }

    /**
     * �����Ŏw�肳�ꂽ�e�[�u�����E�p�����[�^��SQL��INSERT�������s���܂��B
     * ���̃��\�b�h�̓g�����U�N�V�����̃R�~�b�g�������I�ɍs���܂��B<br>
     * <tt>poolName</tt> �Ŏw�肳�ꂽConnectionPool���g�p���܂��B
     * 
     * @param poolName  ConnectionPool�̎��ʎq
     * @param tableName INSERT�Ώۂ̃e�[�u����
     * @param columnMap �J�������ƒl��Map�R���N�V����
     *
     * @return  INSERT���R�[�h���i�ʏ�͂P�j
     *
     * @throws  DBException
     *          �f�[�^�x�[�X�G���[�����������ꍇ�ɃX���[���܂��B
     */
    public static int insert(String poolName, String tableName,
            Map<String, Object> columnMap)
        throws DBException
    {
        ConnectionPool pool = ConnectionPool.getInstance(poolName);
        Connection con = pool.engageConnection(1);
        try {
            con.setAutoCommit(true);
            return insert(con, tableName, columnMap);
        } catch (SQLException e) {
            throw new DBException(e);
        } finally {
            try {
                if (con != null)
                    con.close();
            } catch (SQLException e) {
                _log.log(Level.WARNING, "Connection could not close.", e);
            }
        }
    }

    /**
     * �w�肳�ꂽ�e�[�u���E�p�����[�^��SQL��INSERT�������s���܂��B
     * ���̃��\�b�h�ł̓g�����U�N�V�����̃R�~�b�g����у��[���o�b�N��
     * �����Ɏw�肳�ꂽConnection��AutoCommit���[�h�Ɉˑ����܂��B
     * �����AAutoCommit���[�h��false�̏ꍇ�A�R�~�b�g����у��[���o�b�N��
     * �s���܂���̂ŁA���\�b�h���s��ɍs���K�v������܂��B
     *
     * @param con       �f�[�^�x�[�X�R�l�N�V����
     * @param tableName INSERT�Ώۂ̃e�[�u����
     * @param columnMap �J�������ƒl��Map�R���N�V����
     *
     * @return  INSERT���R�[�h���i�ʏ�͂P�j
     *
     * @throws  DBException
     *          �f�[�^�x�[�X�G���[�����������ꍇ�ɃX���[���܂��B
     */
    public static int insert(Connection con, String tableName,
            Map<String, Object> columnMap)
        throws DBException
    {
        if (con == null)
            throw new NullPointerException("con");
        if (tableName == null)
            throw new NullPointerException("tableName");
        if (columnMap == null)
            throw new NullPointerException("columnMap");
        if (columnMap.size() == 0)
            throw new IllegalArgumentException("column map was empty.");

        Object[] colNames = columnMap.keySet().toArray();
        Object[] values = new Object[colNames.length];
        StringBuffer sql = new StringBuffer("INSERT INTO " + tableName + " (");

        values[0] = columnMap.get(colNames[0]);
        sql.append(colNames[0]);
        for (int idx = 1; idx < colNames.length; idx++) {
            values[idx] = columnMap.get(colNames[idx]);
            sql.append("," + colNames[idx]);
        }
        sql.append(") VALUES (");
        sql.append("?");
        for (int idx = 1; idx < colNames.length; idx++)
            sql.append(",?");
        sql.append(")");

        return update(con, sql.toString(), values);
    }

    /**
     * �f�t�H���g��ConnectionPool�ŊǗ������Connection���g�p���čX�V�n��
     * SQL���iINSERT, UPDATE, DELETE�j�����s���܂��B
     * SELECT���̂悤��ResultSet��Ԃ�SQL���͎��s�ł��܂���B�܂��A�ϐ��l��
     * �w��ł��܂���B
     *
     * <p> ���̃��\�b�h�ł̓g�����U�N�V�����͎����I�ɃR�~�b�g��������
     * ���[���o�b�N����܂��B
     *
     * @param sql   �f�[�^�X�V�nSQL���B�ϐ��l�͎w��ł��Ȃ��̂ŁA"?"���܂߂�
     *              ���͂ł��܂���B
     *
     * @return  �f�[�^�X�V���ꂽ���R�[�h���B
     *
     * @throws  DBException
     *          �f�[�^�x�[�X�G���[�����������ꍇ�ɃX���[���܂��B
     */
    public static int update(String sql)
        throws DBException
    {
        return update((String) null, sql);
    }

    /**
     * �����Ɏw�肳�ꂽConnectionPool���ʎq�ŊǗ������Connection��
     * �g�p���čX�V�n��SQL���iUPDATE, INSERT, DELETE�j�����s���܂��B
     * SELECT���̂悤��ResultSet��Ԃ�SQL���͎��s�ł��܂���B�܂��A�ϐ��l��
     * �w��ł��܂���B
     *
     * <p> ���̃��\�b�h�ł̓g�����U�N�V�����͎����I�ɃR�~�b�g��������
     * ���[���o�b�N����܂��B
     *
     * @param poolName  ConnectionPool�̎��ʎq
     * @param sql       �f�[�^�X�V�n��SQL���B�ϐ��l�͎w��ł��Ȃ��̂ŁA
     *                  "?"���܂߂鎖�͂ł��܂���B
     *
     * @return  �f�[�^�X�V���ꂽ���R�[�h���B
     *
     * @throws  DBException
     *          �f�[�^�x�[�X�G���[�����������ꍇ�ɃX���[���܂��B
     */
    public static int update(String poolName, String sql)
        throws DBException
    {
        ConnectionPool pool = ConnectionPool.getInstance(poolName);
        Connection con = pool.engageConnection(1);
        try {
            con.setAutoCommit(true);
            return update(con, sql);
        } catch (SQLException e) {
            throw new DBException(e);
        } finally {
            try {
                if (con != null)
                    con.close();
            } catch (SQLException e) {
                _log.log(Level.WARNING, "Connection could not close.", e);
            }
        }
    }

    /**
     * �w��� {@link java.sql.Connection} ���g�p���čX�V�n��SQL��
     * �iUPDATE, INSERT, DELETE�j�����s���܂��BSELECT���̂悤��ResultSet��
     * �Ԃ�SQL���͎��s�ł��܂���B�܂��A�ϐ��l�͎w��ł��܂���B
     *
     * <p> ���̃��\�b�h�ł̓g�����U�N�V�����̃R�~�b�g����у��[���o�b�N��
     * �����Ɏw�肳�ꂽConnection��AutoCommit���[�h�Ɉˑ����܂��B�����A
     * AutoCommit���[�h��false�̏ꍇ�A�R�~�b�g����у��[���o�b�N�͍s���܂���
     * �̂ŁA���̃��\�b�h���s��ɍs���K�v������܂��B
     *
     * @param con   �f�[�^�x�[�X�R�l�N�V�����I�u�W�F�N�g
     * @param sql   �f�[�^�X�V�n��SQL���B�ϐ��l�͎w��ł��Ȃ��̂ŁA
     *              "?"�͊܂߂鎖���ł��܂���B
     *
     * @return  �f�[�^�X�V���ꂽ���R�[�h���B
     *
     * @throws  DBException
     *          �f�[�^�x�[�X�G���[�����������ꍇ�ɃX���[���܂��B
     */
    public static int update(Connection con, String sql)
        throws DBException
    {
        return update(con, sql, null);
    }

    /**
     * �f�t�H���g��ConnectionPool�ŊǗ������Connection���g�p���čX�V�n��
     * SQL���iUPDATE, INSERT, DELETE�j�����s���܂��B
     * SELECT���̂悤��ResultSet��Ԃ�SQL���͎��s�ł��܂���B
     *
     * <p> ���̃��\�b�h�ł̓g�����U�N�V�����͎����I�ɃR�~�b�g��������
     * ���[���o�b�N����܂��B
     *
     * @param sql   �f�[�^�X�V�n��SQL���B�ϐ��l���g�p����ꍇ�́A�ϐ���
     *              ���蓖�Ă镔����"?"���w�肷��K�v������܂��B
     * @param args  SQL�ւ̕ϐ��l�z��B�����ϐ��l���K�v�Ȃ��ꍇ�A<tt>null</tt>
     *              ���w�肷�鎖���ł��܂��B
     *
     * @return  �f�[�^�X�V���ꂽ���R�[�h���B
     *
     * @throws  DBException
     *          �f�[�^�x�[�X�G���[�����������ꍇ�ɃX���[���܂��B
     */
    public static int update(String sql, Object[] args)
        throws DBException
    {
        return update((String) null, sql, args);
    }

    /**
     * �����Ɏw�肳�ꂽConnectionPool���ʎq�ŊǗ������Connection��
     * �g�p���čX�V�n��SQL���iUPDATE, INSERT, DELETE�j�����s���܂��B
     * SELECT���̂悤��ResultSet��Ԃ�SQL���͎��s�ł��܂���B
     *
     * <p> ���̃��\�b�h�ł̓g�����U�N�V�����͎����I�ɃR�~�b�g��������
     * ���[���o�b�N����܂��B
     *
     * @param poolName  ConnectionPool�̎��ʎq
     * @param sql       �f�[�^�X�V�n��SQL���B�ϐ��l���g�p����ꍇ�́A�ϐ���
     *                  ���蓖�Ă镔����"?"���w�肷��K�v������܂��B
     * @param args      SQL�ւ̕ϐ��l�z��B�����ϐ��l���K�v�Ȃ��ꍇ�A
     *                  <tt>null</tt> ���w�肷�鎖���ł��܂��B
     *
     * @return  �f�[�^�X�V���ꂽ���R�[�h���B
     *
     * @throws  DBException
     *          �f�[�^�x�[�X�G���[�����������ꍇ�ɃX���[���܂��B
     */
    public static int update(String poolName, String sql, Object[] args)
        throws DBException
    {
        ConnectionPool pool = ConnectionPool.getInstance(poolName);
        Connection con = pool.engageConnection(1);
        try {
            con.setAutoCommit(true);
            return update(con, sql, args);
        } catch (SQLException e) {
            throw new DBException(e);
        } finally {
            try {
                if (con != null)
                    con.close();
            } catch (SQLException e) {
                _log.log(Level.WARNING, "Connection could not close.", e);
            }
        }
    }

    /**
     * �w���Connection���g���čX�V�n��SQL���iUPDATE�AINSERT�ADELETE�j
     * �����s���܂��BSELECT���̂悤��ResultSet��Ԃ�SQL���͎��s�ł��܂���B
     *
     * <p> ���̃��\�b�h�ł̓g�����U�N�V�����̃R�~�b�g����у��[���o�b�N��
     * �����Ɏw�肳�ꂽConnection��AutoCommit���[�h�Ɉˑ����܂��B�����A
     * AutoCommit���[�h��false�̏ꍇ�A�R�~�b�g����у��[���o�b�N�͍s���܂���
     * �̂ŁA���̃��\�b�h���s��ɍs���K�v������܂��B
     *
     * @param con   �f�[�^�x�[�X�R�l�N�V�����I�u�W�F�N�g
     * @param sql   �f�[�^�X�V�n��SQL���B�ϐ��l���g�p����ꍇ�́A�ϐ���
     *              ���蓖�Ă镔����"?"���w�肷��K�v������܂��B
     * @param args  SQL�ւ̕ϐ��l�z��B�����ϐ��l���K�v�Ȃ�SQL�����s����ꍇ��
     *              <tt>null</tt> ���w�肷�鎖���ł��܂��B
     *
     * @return  �f�[�^�X�V���ꂽ���R�[�h���B
     *
     * @throws  DBException
     *          �f�[�^�x�[�X�G���[�����������ꍇ�ɃX���[���܂��B
     */
    public static int update(Connection con, String sql, Object[] args)
        throws DBException
    {
        if (con == null)
            throw new NullPointerException("con");
        if (sql == null)
            throw new NullPointerException("sql");
        if (sql.length() == 0)
            throw new IllegalArgumentException("sql is empty.");
        if (args != null) {
            for (int idx = 0; idx < args.length; idx++) {
                if (args[idx] == null)
                    throw new NullPointerException("args[" + idx + "]");
            }
        }

        PreparedStatement stmt = null;
        try {
            stmt = con.prepareStatement(sql);
            if (args != null && args.length > 0) {
                for (int idx = 0; idx < args.length; idx++)
                    stmt.setObject(idx + 1, args[idx]);
            }
            return stmt.executeUpdate();
        } catch (SQLException e) {
            throw new DBException(e);
        } finally {
            try {
                if (stmt != null)
                    stmt.close();
            } catch (SQLException e) {
            }
        }
    }

    /**
     * �f�t�H���g��ConnectionPool���g�p����ResultSet��Ԃ��Ȃ�SQL�����s���܂��B
     * ���̃��\�b�h�̓g�����U�N�V�����̃R�~�b�g�������I�ɍs���܂��B<br>
     *
     * <p> ���̃��\�b�h�ł�ResultSet��Ԃ��܂���̂ŁAResultSet���K�v��
     * SQL�̎��s�ɂ͎g�p�ł��܂���B
     *
     * @param sql   SQL�X�e�[�g�����g
     *
     * @throws  NullPointerException
     *          <tt>sql</tt> ��null���w�肳�ꂽ�ꍇ�ɃX���[���܂��B
     *
     * @throws  IllegalArgumentException
     *          <tt>sql</tt> ���󕶎��̏ꍇ�ɃX���[���܂��B
     *
     * @throws  DBException
     *          �f�[�^�x�[�X�G���[�����������ꍇ�ɃX���[���܂��B
     */
    public static void execute(String sql)
        throws DBException
    {
        if (sql == null)
            throw new NullPointerException("sql");
        if (sql.trim().length() == 0)
            throw new IllegalArgumentException("sql is empty.");

        execute((String) null, sql);
    }

    /**
     * ResultSet��Ԃ��Ȃ�SQL�����s���܂��B
     * ���̃��\�b�h�̓g�����U�N�V�����̃R�~�b�g�������I�ɍs���܂��B<br>
     *
     * <p> ���̃��\�b�h�ł�ResultSet��Ԃ��܂���̂ŁAResultSet���K�v��
     * SQL�̎��s�ɂ͎g�p�ł��܂���B
     *
     * @param poolName  ConnectionPool�̎��ʎq�BNull���w�肳�ꂽ�ꍇ��
     *                  �f�t�H���g���g�p����܂��B
     * @param sql       SQL�X�e�[�g�����g
     *
     * @throws  NullPointerException
     *          <tt>sql</tt> ��null���w�肳�ꂽ�ꍇ�ɃX���[���܂��B
     *
     * @throws  IllegalArgumentException
     *          <tt>sql</tt> ���󕶎��̏ꍇ�ɃX���[���܂��B
     *
     * @throws  DBException
     *          �f�[�^�x�[�X�G���[�����������ꍇ�ɃX���[���܂��B
     */
    public static void execute(String poolName, String sql)
        throws DBException
    {
        if (sql == null)
            throw new NullPointerException("sql");
        if (sql.trim().length() == 0)
            throw new IllegalArgumentException("sql is empty.");

        ConnectionPool pool = ConnectionPool.getInstance(poolName);
        Connection con = pool.engageConnection(1);
        try {
            con.setAutoCommit(true);
            execute(con, sql);
        } catch (SQLException e) {
            throw new DBException(e);
        } finally {
            try {
                if (con != null)
                    con.close();
            } catch (SQLException e) {
                _log.log(Level.WARNING, "Connection could not close.", e);
            }
        }
    }

    /**
     * ResultSet��Ԃ��Ȃ�SQL�����s���܂��B
     * ���̃��\�b�h�ł̓g�����U�N�V�����̃R�~�b�g����у��[���o�b�N��
     * �����Ɏw�肳�ꂽConnection��AutoCommit���[�h�Ɉˑ����܂��B
     * �����AAutoCommit���[�h��false�̏ꍇ�A�R�~�b�g����у��[���o�b�N��
     * �s���܂���̂ŁA���̃��\�b�h���s��ɍs���K�v������܂��B
     *
     * <p> ���̃��\�b�h�ł�ResultSet��Ԃ��܂���̂ŁAResultSet���K�v��
     * SQL�̎��s�ɂ͎g�p�ł��܂���B
     *
     * @param con   �f�[�^�x�[�X�R�l�N�V�����I�u�W�F�N�g
     * @param sql   SQL�X�e�[�g�����g
     *
     * @throws  NullPointerException
     *          <tt>sql</tt> �������� <tt>con</tt> ��null���w�肳�ꂽ�ꍇ��
     *          �X���[���܂��B
     *
     * @throws  IllegalArgumentException
     *          <tt>sql</tt> ���󕶎��̏ꍇ�ɃX���[���܂��B
     *
     * @throws  DBException
     *          �f�[�^�x�[�X�G���[�����������ꍇ�ɃX���[���܂��B
     */
    public static void execute(Connection con, String sql)
        throws DBException
    {
        if (sql == null)
            throw new NullPointerException("sql");
        if (sql.length() == 0)
            throw new IllegalArgumentException("sql is empty.");
        if (con == null)
            throw new NullPointerException("con");

        Statement stmt = null;
        try {
            stmt = con.createStatement();
            if (stmt.execute(sql))
                _log.log(Level.WARNING,
                        "Statement returned the ResultSet in execute method. " +
                        "[" + sql + "].");
        } catch (SQLException e) {
            throw new DBException(e);
        } finally {
            try {
                if (stmt != null)
                    stmt.close();
            } catch (SQLException e) {
            }
        }
    }
}
