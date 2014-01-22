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

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import org.sd_network.util.Config;

/**
 * １つの {@link ConnectionPool} インスタンスで使用するJDBCドライバの
 * パラメータを提供します。 <BR>
 * {@link org.sd_network.util.Config} に読み込まれたプロパティから
 * 以下のプロパティを検出し、グループ毎にインスタンスを生成し保持します。
 *
 * <ul>
 *  <li> org.sd_network.db.ConnectionParameter.グループ名.ID
 *  <li> org.sd_network.db.ConnectionParameter.グループ名.JDBCDriver
 *  <li> org.sd_network.db.ConnectionParameter.グループ名.URL
 *  <li> org.sd_network.db.ConnectionParameter.グループ名.UserName
 *  <li> org.sd_network.db.ConnectionParameter.グループ名.Password
 * </ul>
 *
 * 上記５つのプロパティを１つのグループとし、複数のグループを指定する事が
 * できます。グループ名は一意な文字列でなければなりません。
 *
 * <p> IDはConnectionPoolの識別子です。
 * {@link ConnectionPool#getInstance(String)} メソッドの引数にしている
 * <tt>poolName</tt> にIDを指定すると、JDBCDriver, URL, UserName, Password
 * でConnectionを生成するConnectionPoolのインスタンスを取得する事ができます。
 * このクラスは {@link ConnectionPool} から使用されます。
 * 
 * <p> $Id$
 *
 * @author Masatoshi Sato
 */ 
class ConnectionParameter
{
    //////////////////////////////////////////////////////////// 
    // Class fields.

    /** Logger. */
    private static final Logger _log = Logger.getLogger(
            ConnectionParameter.class.getName());

    /** パラメータ情報のプロパティprefix. */
    private static final String _PROP_PREFIX =
        "org.sd_network.db.ConnectionParameter";

    //////////////////////////////////////////////////////////// 
    // Instance fields.

    /** ID */
    private final String _ID;

    /** JDBCDriverクラスの完全クラスパス */
    private final String _JDBCDriver;

    /** JDBCDriverのURL */
    private final String _URL;

    /** データベースユーザのユーザ名 */
    private final String _userName;

    /** データベースユーザのパスワード */
    private final String _password;

    /** インスタンスバッファ */
    private static Map<String, ConnectionParameter> _instanceMap;

    /** インスタンス読み込み済みフラグ */
    private static boolean _loadInstances = false;

    //////////////////////////////////////////////////////////// 
    // Factories.

    /**
     * 指定IDのインスタンスを返します。
     * 存在しない場合は <tt>null</tt> を返します。
     *
     * @param ID    パラメータ識別子
     *
     * @return  <tt>ID</tt> に紐づくインスタンス。なければ <tt>null</tt> を
     *          返します。
     */
    static final ConnectionParameter getInstance(String ID) {
        loadInstances();
        return _instanceMap.get(ID);
    }

    /**
     * {@link org.sd_network.util.Config} からプロパティ情報を読み込み、
     * 各インスタンスを生成し、インスタンスバッファに保存します。
     *
     * <p> 各パラメータのフォーマットエラーを検出した場合、エラー情報を
     * ログに出力し、そのパラメータは処理から除外し、次のパラメータから
     * 処理を継続します。
     */
    private static final void loadInstances() {
        if (_loadInstances)
            return;

        _instanceMap = new HashMap<String, ConnectionParameter>();
        HashMap<String, Properties> groupMap =
            new HashMap<String, Properties>();
        int prefixLength = _PROP_PREFIX.length();

        // プロパティに設定されたConnectionParameter関連プロパティを
        // 検索して、グループ毎のPropertiesとしてgroupMapに保存する。
        Config config = Config.getInstance();
        Enumeration<?> names = config.propertyNames();
        while (names.hasMoreElements()) {
            String name = (String) names.nextElement();

            if (!name.startsWith(_PROP_PREFIX))
                continue;

            int groupNameEndIdx = name.indexOf('.', prefixLength + 1);
            if (groupNameEndIdx == -1) {
                _log.warning("Invalid format:" + name);
                continue;
            }
            if (name.indexOf('.', groupNameEndIdx + 1) != -1) {
                _log.warning("Invalid format:" + name);
                continue;
            }

            String groupName =
                name.substring(prefixLength + 1, groupNameEndIdx);
            _log.fine("Group name is " + groupName);

            String value = config.getProperty(name);
            Properties props = groupMap.get(groupName);
            if (props == null)
                props = new Properties();
            String paramName = name.substring(groupNameEndIdx + 1);
            props.setProperty(paramName, value);
            groupMap.put(groupName, props);
            _log.fine(
                    " GroupName : " + groupName +
                    " PropertyName : " + name +
                    " Parameter : " + paramName +
                    " Value : " + value);
        }

        // groupMapに保存されたPropertiesから各パラメータを取得し、
        // ConnectionParameterインスタンスを生成して_instanceMapに
        // 保存する。
        Set<String> groupNames = groupMap.keySet();
        for (String groupName : groupNames) {
            Properties props = groupMap.get(groupName);
            String ID = props.getProperty("ID");
            if (ID == null) {
                _log.warning("ID not found for " + groupName);
                continue;
            }
            String JDBCDriver = props.getProperty("JDBCDriver");
            if (JDBCDriver == null) {
                _log.warning("JDBCDriver not found for " + groupName);
                continue;
            }
            String URL = props.getProperty("URL");
            if (URL == null) {
                _log.warning("URL not found for " + groupName);
                continue;
            }
            String userName = props.getProperty("UserName");
            if (userName == null) {
                _log.warning("UserName not found for " + groupName);
                continue;
            }
            String password = props.getProperty("Password");
            if (password == null) {
                _log.warning("Password not found for " + groupName);
                continue;
            }

            _instanceMap.put(ID,
                    new ConnectionParameter(
                        ID, JDBCDriver, URL, userName, password));
        }

        _loadInstances = true;
    }

    //////////////////////////////////////////////////////////// 
    // Constructors.

    /**
     * 指定されたパラメータでインスタンスを生成します。
     *
     * @param ID            パラメータ識別子
     * @param JDBCDriver    JDBCDriverクラスの完全パス
     * @param URL           データベースへのアクセスURL
     * @param userName      データベースユーザのユーザID
     * @param password      データベースユーザのパスワード
     */
    private ConnectionParameter(String ID, String JDBCDriver, String URL,
            String userName, String password)
    {
        _ID = ID;
        _JDBCDriver = JDBCDriver;
        _URL = URL;
        _userName = userName;
        _password = password;
        _log.fine("new instance with " +
                "ID = " + _ID +
                ", JDBCDriver = " + _JDBCDriver +
                ", URL = " + _URL +
                ", UserName = " + _userName +
                ", Password = " + _password);
    }

    //////////////////////////////////////////////////////////// 
    // Package scope methods.

    /**
     * パラメータ識別子を返します。
     *
     * @return  パラメータ識別子。
     */
    String getID() {
        return _ID;
    }

    /**
     * JDBCDriverクラスの完全パスを返します。
     *
     * @return  JDBCDriverクラスの完全パス。
     */
    String getJDBCDriver() {
        return _JDBCDriver;
    }

    /**
     * データベースアクセスURLを返します。
     *
     * @return  データベースアクセスURL。
     */
    String getURL() {
        return _URL;
    }

    /**
     * データベースユーザのユーザ名を返します。
     *
     * @return  データベースユーザのユーザ名。
     */
    String getUserName() {
        return _userName;
    }

    /**
     * データベースユーザのパスワードを返します。
     *
     * @return  データベースユーザのパスワード。
     */
    String getPassword() {
        return _password;
    }

    //////////////////////////////////////////////////////////// 
    // Public methods.

    /**
     * ConnectionParameterクラスの文字列表現を返します。
     *
     * @return  ConnectionParameterクラスの文字列表現。
     */
    public String toString() {
        return 
                "ID = " + _ID +
                ", JDBCDriver = " + _JDBCDriver +
                ", URL = " + _URL +
                ", UserName = " + _userName +
                ", Password = xxx";
    }
}
