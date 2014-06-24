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
package org.sd_network.util;

/**
 * This class represent runtime exception that is thrown if error occurred
 * when configure setting.
 *
 * <p> $Id$
 *
 * @author Masatoshi Sato
 */
public class InvalidConfigException
    extends RuntimeException
{
    //////////////////////////////////////////////////////////// 
    // Constructors.

    /**
     * Create instance without parameter.
     */
    public InvalidConfigException() {
        super();
    }

    /**
     * Create instance with specified detail message.
     *
     * @param message   exception detail message.
     */
    public InvalidConfigException(String message) {
        super(message);
    }

    /**
     * Create instance with cause.
     *
     * @param cause     instance of cause.
     */
    public InvalidConfigException(Throwable cause) {
        super(cause);
    }

    /**
     * Create instance with detail message and cause.
     *
     * @param message   exception detail message.
     * @param cause     instance of cause.
     */
    public InvalidConfigException(String message, Throwable cause) {
        super(message, cause);
    }
}

