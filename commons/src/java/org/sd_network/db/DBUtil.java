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
 * データベースアクセス用のユーティリティメソッドを定義します。
 * 同じ処理を行うメソッドでも、以下の３つのメソッドが定義されています。
 * <ul>
 *  <li> Connectionの指定
 *  <li> ConnectionPoolの識別子を指定
 *  <li> デフォルトのConnectionPoolを指定
 * </ul>
 * これらのメソッドは、トランザクションのコミットやロールバックについては
 * 殆ど考慮されていません。特定のConnectionを使用してＳＱＬを実行する
 * メソッドの場合に自動コミットモードがONの場合に自動的にコミットロールバック
 * が実行されるだけです。よって、１つのトランザクションで複数のＳＱＬを実行
 * する場合、かつこのクラスに定義されているメソッドを、その一部に利用する場合、
 * このクラスのメソッドを呼び出す前に自前でConnectionを取得し、自動コミット
 * モードをOFFにしてから、このクラスのConnectionを指定するタイプのメソッド
 * を利用してください。
 *
 * <p> データベースエラーが発生した場合、{@link java.sql.SQLException} は常に
 * {@link org.sd_network.db.DBException} にラップされ、ランタイム例外として
 * スローされます。
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
     * 引数で指定されたテーブル名・パラメータでSQLのINSERT文を実行します。
     * このメソッドはトランザクションのコミットを自動的に行います。<br>
     * デフォルトのConnectionPoolを使用します。
     *
     * @param tableName INSERT対象のテーブル名。
     * @param columnMap カラム名と値のMapコレクション。
     *
     * @return  INSERTレコード数。（通常は１）
     *
     * @throws  DBException
     *          データベースエラーが発生した場合にスローします。
     */
    public static int insert(String tableName, Map<String, Object> columnMap)
        throws DBException
    {
        return insert((String) null, tableName, columnMap);
    }

    /**
     * 引数で指定されたテーブル名・パラメータでSQLのINSERT文を実行します。
     * このメソッドはトランザクションのコミットを自動的に行います。<br>
     * <tt>poolName</tt> で指定されたConnectionPoolを使用します。
     * 
     * @param poolName  ConnectionPoolの識別子
     * @param tableName INSERT対象のテーブル名
     * @param columnMap カラム名と値のMapコレクション
     *
     * @return  INSERTレコード数（通常は１）
     *
     * @throws  DBException
     *          データベースエラーが発生した場合にスローします。
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
     * 指定されたテーブル・パラメータでSQLのINSERT文を実行します。
     * このメソッドではトランザクションのコミットおよびロールバックは
     * 引数に指定されたConnectionのAutoCommitモードに依存します。
     * もし、AutoCommitモードがfalseの場合、コミットおよびロールバックは
     * 行われませんので、メソッド実行後に行う必要があります。
     *
     * @param con       データベースコネクション
     * @param tableName INSERT対象のテーブル名
     * @param columnMap カラム名と値のMapコレクション
     *
     * @return  INSERTレコード数（通常は１）
     *
     * @throws  DBException
     *          データベースエラーが発生した場合にスローします。
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
     * デフォルトのConnectionPoolで管理されるConnectionを使用して更新系の
     * SQL文（INSERT, UPDATE, DELETE）を実行します。
     * SELECT文のようなResultSetを返すSQL文は実行できません。また、変数値は
     * 指定できません。
     *
     * <p> このメソッドではトランザクションは自動的にコミットもしくは
     * ロールバックされます。
     *
     * @param sql   データ更新系SQL文。変数値は指定できないので、"?"を含める
     *              事はできません。
     *
     * @return  データ更新されたレコード数。
     *
     * @throws  DBException
     *          データベースエラーが発生した場合にスローします。
     */
    public static int update(String sql)
        throws DBException
    {
        return update((String) null, sql);
    }

    /**
     * 引数に指定されたConnectionPool識別子で管理されるConnectionを
     * 使用して更新系のSQL文（UPDATE, INSERT, DELETE）を実行します。
     * SELECT文のようなResultSetを返すSQL文は実行できません。また、変数値は
     * 指定できません。
     *
     * <p> このメソッドではトランザクションは自動的にコミットもしくは
     * ロールバックされます。
     *
     * @param poolName  ConnectionPoolの識別子
     * @param sql       データ更新系のSQL文。変数値は指定できないので、
     *                  "?"を含める事はできません。
     *
     * @return  データ更新されたレコード数。
     *
     * @throws  DBException
     *          データベースエラーが発生した場合にスローします。
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
     * 指定の {@link java.sql.Connection} を使用して更新系のSQL文
     * （UPDATE, INSERT, DELETE）を実行します。SELECT文のようなResultSetを
     * 返すSQL文は実行できません。また、変数値は指定できません。
     *
     * <p> このメソッドではトランザクションのコミットおよびロールバックは
     * 引数に指定されたConnectionのAutoCommitモードに依存します。もし、
     * AutoCommitモードがfalseの場合、コミットおよびロールバックは行われません
     * ので、このメソッド実行後に行う必要があります。
     *
     * @param con   データベースコネクションオブジェクト
     * @param sql   データ更新系のSQL文。変数値は指定できないので、
     *              "?"は含める事ができません。
     *
     * @return  データ更新されたレコード数。
     *
     * @throws  DBException
     *          データベースエラーが発生した場合にスローします。
     */
    public static int update(Connection con, String sql)
        throws DBException
    {
        return update(con, sql, null);
    }

    /**
     * デフォルトのConnectionPoolで管理されるConnectionを使用して更新系の
     * SQL文（UPDATE, INSERT, DELETE）を実行します。
     * SELECT文のようなResultSetを返すSQL文は実行できません。
     *
     * <p> このメソッドではトランザクションは自動的にコミットもしくは
     * ロールバックされます。
     *
     * @param sql   データ更新系のSQL文。変数値を使用する場合は、変数を
     *              割り当てる部分に"?"を指定する必要があります。
     * @param args  SQLへの変数値配列。もし変数値が必要ない場合、<tt>null</tt>
     *              を指定する事ができます。
     *
     * @return  データ更新されたレコード数。
     *
     * @throws  DBException
     *          データベースエラーが発生した場合にスローします。
     */
    public static int update(String sql, Object[] args)
        throws DBException
    {
        return update((String) null, sql, args);
    }

    /**
     * 引数に指定されたConnectionPool識別子で管理されるConnectionを
     * 使用して更新系のSQL文（UPDATE, INSERT, DELETE）を実行します。
     * SELECT文のようなResultSetを返すSQL文は実行できません。
     *
     * <p> このメソッドではトランザクションは自動的にコミットもしくは
     * ロールバックされます。
     *
     * @param poolName  ConnectionPoolの識別子
     * @param sql       データ更新系のSQL文。変数値を使用する場合は、変数を
     *                  割り当てる部分に"?"を指定する必要があります。
     * @param args      SQLへの変数値配列。もし変数値が必要ない場合、
     *                  <tt>null</tt> を指定する事ができます。
     *
     * @return  データ更新されたレコード数。
     *
     * @throws  DBException
     *          データベースエラーが発生した場合にスローします。
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
     * 指定のConnectionを使って更新系のSQL文（UPDATE、INSERT、DELETE）
     * を実行します。SELECT文のようなResultSetを返すSQL文は実行できません。
     *
     * <p> このメソッドではトランザクションのコミットおよびロールバックは
     * 引数に指定されたConnectionのAutoCommitモードに依存します。もし、
     * AutoCommitモードがfalseの場合、コミットおよびロールバックは行われません
     * ので、このメソッド実行後に行う必要があります。
     *
     * @param con   データベースコネクションオブジェクト
     * @param sql   データ更新系のSQL文。変数値を使用する場合は、変数を
     *              割り当てる部分に"?"を指定する必要があります。
     * @param args  SQLへの変数値配列。もし変数値が必要ないSQLを実行する場合は
     *              <tt>null</tt> を指定する事ができます。
     *
     * @return  データ更新されたレコード数。
     *
     * @throws  DBException
     *          データベースエラーが発生した場合にスローします。
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
     * デフォルトのConnectionPoolを使用してResultSetを返さないSQLを実行します。
     * このメソッドはトランザクションのコミットを自動的に行います。<br>
     *
     * <p> このメソッドではResultSetを返しませんので、ResultSetが必要な
     * SQLの実行には使用できません。
     *
     * @param sql   SQLステートメント
     *
     * @throws  NullPointerException
     *          <tt>sql</tt> にnullが指定された場合にスローします。
     *
     * @throws  IllegalArgumentException
     *          <tt>sql</tt> が空文字の場合にスローします。
     *
     * @throws  DBException
     *          データベースエラーが発生した場合にスローします。
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
     * ResultSetを返さないSQLを実行します。
     * このメソッドはトランザクションのコミットを自動的に行います。<br>
     *
     * <p> このメソッドではResultSetを返しませんので、ResultSetが必要な
     * SQLの実行には使用できません。
     *
     * @param poolName  ConnectionPoolの識別子。Nullが指定された場合は
     *                  デフォルトが使用されます。
     * @param sql       SQLステートメント
     *
     * @throws  NullPointerException
     *          <tt>sql</tt> にnullが指定された場合にスローします。
     *
     * @throws  IllegalArgumentException
     *          <tt>sql</tt> が空文字の場合にスローします。
     *
     * @throws  DBException
     *          データベースエラーが発生した場合にスローします。
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
     * ResultSetを返さないSQLを実行します。
     * このメソッドではトランザクションのコミットおよびロールバックは
     * 引数に指定されたConnectionのAutoCommitモードに依存します。
     * もし、AutoCommitモードがfalseの場合、コミットおよびロールバックは
     * 行われませんので、このメソッド実行後に行う必要があります。
     *
     * <p> このメソッドではResultSetを返しませんので、ResultSetが必要な
     * SQLの実行には使用できません。
     *
     * @param con   データベースコネクションオブジェクト
     * @param sql   SQLステートメント
     *
     * @throws  NullPointerException
     *          <tt>sql</tt> もしくは <tt>con</tt> にnullが指定された場合に
     *          スローします。
     *
     * @throws  IllegalArgumentException
     *          <tt>sql</tt> が空文字の場合にスローします。
     *
     * @throws  DBException
     *          データベースエラーが発生した場合にスローします。
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
