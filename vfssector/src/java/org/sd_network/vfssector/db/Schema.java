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
package org.sd_network.vfssector.db;

import org.sd_network.db.DBException;
import org.sd_network.db.DBUtil;

/**
 * This is the setup database schema class for vfssector.
 * Create database schema and insert variable initial values.
 *
 * <p> $Id$
 *
 * @author Masatoshi Sato
 */
public class Schema
{
    /**
     * Setup database scheme for vfssector.
     */
    public static final void setup()
        throws DBException
    {

        ////////////////////////////// 
        // create tables.

        // setup content of sector table.
        DBUtil.execute(
                "vfssector",
                "CREATE TABLE IF NOT EXISTS sector (" +
                " sector_id VARCHAR(36) NOT NULL PRIMARY KEY, " +
                " file_id VARCHAR(36) NOT NULL, " +
                " seq_num INT NOT NULL, " +
                " size INT NOT NULL, " +
                " content BLOB NOT NULL " +
                ");");
        DBUtil.execute(
                "vfssector",
                "CREATE INDEX IF NOT EXISTS sector_ix1 " +
                " ON sector (file_id) " +
                ";");
        DBUtil.execute(
                "vfssector",
                "CREATE UNIQUE INDEX IF NOT EXISTS sector_ux1 " +
                " ON sector (file_id, seq_num) " +
                ";");
    }
}
