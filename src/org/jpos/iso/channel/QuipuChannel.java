/*
 * jPOS Project [http://jpos.org]
 * Copyright (C) 2000-2010 Alejandro P. Revilla
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jpos.iso.channel;

import java.io.IOException;
import java.net.ServerSocket;
import org.jpos.iso.BaseChannel;
import org.jpos.iso.ISOChannel;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOPackager;

/**
 * ISOChannel implementation - Quipu Channel Send packet len (2 bytes
 * network byte order MSB/LSB) followed by raw data.
 *
 * @author salaman@teknos.com
 * @version Id: PostChannel.java,v 1.0 1999/05/14 19:00:00 may Exp
 * @see ISOMsg
 * @see ISOException
 * @see ISOChannel
 */
public class QuipuChannel extends BaseChannel
{
    /**
     * Public constructor (used by Class.forName("...").newInstance())
     */
    public QuipuChannel()
    {
        super();
    }

    /**
     * Construct client ISOChannel
     *
     * @param host server TCP Address
     * @param port server port number
     * @param p an ISOPackager
     * @see ISOPackager
     */
    public QuipuChannel(String host, int port, ISOPackager p)
    {
        super(host, port, p);
    }

    /**
     * Construct server ISOChannel
     *
     * @param p an ISOPackager
     * @exception IOException
     * @see ISOPackager
     */
    public QuipuChannel(ISOPackager p) throws IOException
    {
        super(p);
    }

    /**
     * constructs a server ISOChannel associated with a Server Socket
     *
     * @param p an ISOPackager
     * @param serverSocket where to accept a connection
     * @exception IOException
     * @see ISOPackager
     */
    public QuipuChannel(ISOPackager p, ServerSocket serverSocket) throws IOException
    {
        super(p, serverSocket);
    }

    protected void sendMessageLength(int len) throws IOException
    {
        serverOut.write(len >> 8);
        serverOut.write(len);
    }

    protected int getMessageLength() throws IOException, ISOException
    {
        byte[] b = new byte[2];
        serverIn.readFully(b, 0, 2);
        return (int) (((((int) b[0]) & 0xFF) << 8) | (((int) b[1]) & 0xFF));
    }
}
