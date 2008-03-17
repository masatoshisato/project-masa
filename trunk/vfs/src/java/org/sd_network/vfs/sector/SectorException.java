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
package org.sd_network.vfs.sector;

/**
 * A exception for sector process error.
 *
 * <p> $Id$
 *
 * @author Masatoshi Sato
 */
public class SectorException
    extends Exception
{
    //////////////////////////////////////////////////////////// 
    // Constructors.

    /**
     * Create a new instance with the given message.
     */
    public SectorException(String message) {
        super(message);
    }

    /**
     * Create a new instance from the given exception.
     */
    public SectorException(Exception exception) {
        super(exception);
    }

    /**
     * Create a new instance from the given exception with the given message.
     */
    public SectorException(String message, Exception exception) {
        super(message, exception);
    }
}
