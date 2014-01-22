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
 * �P�� {@link ConnectionPool} �C���X�^���X�Ŏg�p����JDBC�h���C�o��
 * �p�����[�^��񋟂��܂��B <BR>
 * {@link org.sd_network.util.Config} �ɓǂݍ��܂ꂽ�v���p�e�B����
 * �ȉ��̃v���p�e�B�����o���A�O���[�v���ɃC���X�^���X�𐶐����ێ����܂��B
 *
 * <ul>
 *  <li> org.sd_network.db.ConnectionParameter.�O���[�v��.ID
 *  <li> org.sd_network.db.ConnectionParameter.�O���[�v��.JDBCDriver
 *  <li> org.sd_network.db.ConnectionParameter.�O���[�v��.URL
 *  <li> org.sd_network.db.ConnectionParameter.�O���[�v��.UserName
 *  <li> org.sd_network.db.ConnectionParameter.�O���[�v��.Password
 * </ul>
 *
 * ��L�T�̃v���p�e�B���P�̃O���[�v�Ƃ��A�����̃O���[�v���w�肷�鎖��
 * �ł��܂��B�O���[�v���͈�ӂȕ�����łȂ���΂Ȃ�܂���B
 *
 * <p> ID��ConnectionPool�̎��ʎq�ł��B
 * {@link ConnectionPool#getInstance(String)} ���\�b�h�̈����ɂ��Ă���
 * <tt>poolName</tt> ��ID���w�肷��ƁAJDBCDriver, URL, UserName, Password
 * ��Connection�𐶐�����ConnectionPool�̃C���X�^���X���擾���鎖���ł��܂��B
 * ���̃N���X�� {@link ConnectionPool} ����g�p����܂��B
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

    /** �p�����[�^���̃v���p�e�Bprefix. */
    private static final String _PROP_PREFIX =
        "org.sd_network.db.ConnectionParameter";

    //////////////////////////////////////////////////////////// 
    // Instance fields.

    /** ID */
    private final String _ID;

    /** JDBCDriver�N���X�̊��S�N���X�p�X */
    private final String _JDBCDriver;

    /** JDBCDriver��URL */
    private final String _URL;

    /** �f�[�^�x�[�X���[�U�̃��[�U�� */
    private final String _userName;

    /** �f�[�^�x�[�X���[�U�̃p�X���[�h */
    private final String _password;

    /** �C���X�^���X�o�b�t�@ */
    private static Map<String, ConnectionParameter> _instanceMap;

    /** �C���X�^���X�ǂݍ��ݍς݃t���O */
    private static boolean _loadInstances = false;

    //////////////////////////////////////////////////////////// 
    // Factories.

    /**
     * �w��ID�̃C���X�^���X��Ԃ��܂��B
     * ���݂��Ȃ��ꍇ�� <tt>null</tt> ��Ԃ��܂��B
     *
     * @param ID    �p�����[�^���ʎq
     *
     * @return  <tt>ID</tt> �ɕR�Â��C���X�^���X�B�Ȃ���� <tt>null</tt> ��
     *          �Ԃ��܂��B
     */
    static final ConnectionParameter getInstance(String ID) {
        loadInstances();
        return _instanceMap.get(ID);
    }

    /**
     * {@link org.sd_network.util.Config} ����v���p�e�B����ǂݍ��݁A
     * �e�C���X�^���X�𐶐����A�C���X�^���X�o�b�t�@�ɕۑ����܂��B
     *
     * <p> �e�p�����[�^�̃t�H�[�}�b�g�G���[�����o�����ꍇ�A�G���[����
     * ���O�ɏo�͂��A���̃p�����[�^�͏������珜�O���A���̃p�����[�^����
     * �������p�����܂��B
     */
    private static final void loadInstances() {
        if (_loadInstances)
            return;

        _instanceMap = new HashMap<String, ConnectionParameter>();
        HashMap<String, Properties> groupMap =
            new HashMap<String, Properties>();
        int prefixLength = _PROP_PREFIX.length();

        // �v���p�e�B�ɐݒ肳�ꂽConnectionParameter�֘A�v���p�e�B��
        // �������āA�O���[�v����Properties�Ƃ���groupMap�ɕۑ�����B
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

        // groupMap�ɕۑ����ꂽProperties����e�p�����[�^���擾���A
        // ConnectionParameter�C���X�^���X�𐶐�����_instanceMap��
        // �ۑ�����B
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
     * �w�肳�ꂽ�p�����[�^�ŃC���X�^���X�𐶐����܂��B
     *
     * @param ID            �p�����[�^���ʎq
     * @param JDBCDriver    JDBCDriver�N���X�̊��S�p�X
     * @param URL           �f�[�^�x�[�X�ւ̃A�N�Z�XURL
     * @param userName      �f�[�^�x�[�X���[�U�̃��[�UID
     * @param password      �f�[�^�x�[�X���[�U�̃p�X���[�h
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
     * �p�����[�^���ʎq��Ԃ��܂��B
     *
     * @return  �p�����[�^���ʎq�B
     */
    String getID() {
        return _ID;
    }

    /**
     * JDBCDriver�N���X�̊��S�p�X��Ԃ��܂��B
     *
     * @return  JDBCDriver�N���X�̊��S�p�X�B
     */
    String getJDBCDriver() {
        return _JDBCDriver;
    }

    /**
     * �f�[�^�x�[�X�A�N�Z�XURL��Ԃ��܂��B
     *
     * @return  �f�[�^�x�[�X�A�N�Z�XURL�B
     */
    String getURL() {
        return _URL;
    }

    /**
     * �f�[�^�x�[�X���[�U�̃��[�U����Ԃ��܂��B
     *
     * @return  �f�[�^�x�[�X���[�U�̃��[�U���B
     */
    String getUserName() {
        return _userName;
    }

    /**
     * �f�[�^�x�[�X���[�U�̃p�X���[�h��Ԃ��܂��B
     *
     * @return  �f�[�^�x�[�X���[�U�̃p�X���[�h�B
     */
    String getPassword() {
        return _password;
    }

    //////////////////////////////////////////////////////////// 
    // Public methods.

    /**
     * ConnectionParameter�N���X�̕�����\����Ԃ��܂��B
     *
     * @return  ConnectionParameter�N���X�̕�����\���B
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
