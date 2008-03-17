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

/**
 * �f�[�^�x�[�X�R�l�N�V�����������ł��Ȃ������ꍇ�ɃX���[������s����O�ł��B
 *
 * <p> $Id$
 *
 * @author Masatoshi Sato
 */
public class ConnectTimeoutException
    extends RuntimeException
{
    /////////////////////////////////////////////////////////////////////
    // Contructor methods.

    /**
     * �f�t�H���g�R���X�g���N�^�ł��B
     */
    public ConnectTimeoutException() {
        super();
    }

    /**
     * �w�肳�ꂽ���b�Z�[�W�ŃC���X�^���X�𐶐����܂��B
     *
     * @param message   ��O���b�Z�[�W�B
     */
    public ConnectTimeoutException(String message) {
        super(message);
    }

    /**
     * ���̗�O���������������ƂȂ�����O�ŃC���X�^���X�𐶐����܂��B
     *
     * @param exception �����ƂȂ�����O�̃C���X�^���X�B
     */
    public ConnectTimeoutException(Exception exception) {
        super(exception);
    }

    /**
     * �w�肳�ꂽ���b�Z�[�W�ƌ����ƂȂ�����O�ŃC���X�^���X�𐶐����܂��B
     *
     * @param message   ��O���b�Z�[�W�B
     * @param exception �����ƂȂ�����O�̃C���X�^���X�B
     */
    public ConnectTimeoutException(String message, Exception exception) {
        super(message, exception);
    }
}
