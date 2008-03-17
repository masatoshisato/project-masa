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

import java.sql.SQLException;

/**
 * データベース処理中にエラーが発生した場合にスローする実行時例外です。
 *
 * <p> $Id$
 *
 * @author Masatoshi Sato
 */
public class DBException
    extends RuntimeException
{
    //////////////////////////////////////////////////////////// 
    // Constructors.

    /**
     * デフォルトコンストラクタです。
     */
    public DBException() {
        super();
    }

    /**
     * 指定されたメッセージでインスタンスを生成します。
     *
     * @param message   例外メッセージ。
     */
    public DBException(String message) {
        super(message);
    }

    /**
     * この例外が発生した原因となった例外でインスタンスを生成します。
     *
     * @param exception 原因となった例外のインスタンス。
     */
    public DBException(Exception exception) {
        super(exception);
    }

    /**
     * 指定されたメッセージと原因となった例外でインスタンスを生成します。
     *
     * @param message   例外メッセージ。
     * @param exception 原因となった例外のインスタンス。
     */
    public DBException(String message, Exception exception) {
        super(message, exception);
    }
}
