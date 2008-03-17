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
package org.sd_network.vfs.db;

import java.util.UUID;
import org.sd_network.db.DBUtil;

/**
 * This is to setup database schemas for vfs.
 * Create database schema and insert variable initial values. and then
 * check these shcemas.
 *
 * <p> $Id$
 *
 * @author Masatoshi Sato
 */
public class Schema
{
    public static final void setup() {

        ////////////////////////////// 
        // create tables.

        // setup user table.
        DBUtil.execute(
                "vfs", 
                "CREATE TABLE IF NOT EXISTS user (" +
                " user_id VARCHAR(36) NOT NULL PRIMARY KEY, " +
                " login_name VARCHAR(255) NOT NULL, " +
                " password VARCHAR(255) NOT NULL, " +
                " is_admin BOOLEAN NOT NULL DEFAULT FALSE, " +
                " created TIMESTAMP NOT NULL DEFAULT current_timestamp() " +
                ");");

        // setup VfsFile table.
        DBUtil.execute(
                "vfs",
                "CREATE TABLE IF NOT EXISTS vfs_file (" +
                " file_id VARCHAR(36) NOT NULL PRIMARY KEY, " +
                " name VARCHAR(255) NOT NULL, " +
                " type_id INT NOT NULL, " +
                " parent_file_id VARCHAR(36) NULL, " +
                " size INT NOT NULL, " +
                " owner_id VARCHAR(36) NOT NULL, " +
                " created TIMESTAMP NOT NULL DEFAULT current_timestamp(), " +
                "FOREIGN KEY (parent_file_id) " +
                " REFERENCES vfs_file(file_id)," +
                "FOREIGN KEY (owner_id) " +
                " REFERENCES user(user_id) " +
                ");");

        // setup StorageInfo table.
        DBUtil.execute(
                "vfs",
                "CREATE TABLE IF NOT EXISTS storage_info (" +
                " user_id VARCHAR(36) NOT NULL PRIMARY KEY, " +
                " given_bytes BIGINT NOT NULL DEFAULT 0, " +
                " used_bytes BIGINT NOT NULL DEFAULT 0, " +
                " given_files BIGINT NOT NULL DEFAULT 0, " +
                " used_files BIGINT NOT NULL DEFAULT 0 " +
                ");");

        ////////////////////////////// 
        // insert master datas.

        // create User account as an Administrator.
        User user = UserDB.get("administrator", "password");
        if (user == null) {
            user = UserDB.create("administrator", "password", true);
        }

        // create top of vfs_file.
        VfsFile root = VfsFileDB.get("-1", user.getID());
        if (root == null) {
            DBUtil.update(
                    "vfs",
                    "INSERT INTO vfs_file (" +
                    " file_id, name, type_id, size, owner_id) " +
                    "VALUES (?,?,?,?,?)",
                    new Object[] {
                        "-1", "root", 1, new Integer(0), user.getID()
                    });
        }

        // create administrator's home directory.
        VfsFile home = VfsFileDB.get("-1", "Home", user.getID());
        if (home == null) {
            home = VfsFileDB.createDirectory("Home", "-1", user.getID());
        }
    }
}
