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
package org.jpos.util;

import java.util.Iterator;
import java.util.Vector;

/**
 * Peer class Logger forwards LogEvents generated by LogSources 
 * to LogListeners.
 * <br>
 * This little <a href="/doc/LoggerGuide.html">tutorial</a>
 * give you additional information on how to extend the jPOS's
 * Logger subsystem.
 *
 * @author apr@cs.com.uy
 * @version $Id: Logger.java 2993 2010-10-13 18:34:48Z apr $
 * @see LogEvent
 * @see LogSource
 * @see LogListener
 * @see Loggeable
 * @see SimpleLogListener
 * @see RotateLogListener
 */
public class Logger implements LogProducer
{
    String name;
    Vector listeners;
    static boolean versionShown = false;

    public Logger()
    {
        super();
        listeners = new Vector();
        name = "";
    }

    public void addListener(LogListener l)
    {
        synchronized (listeners)
        {
            listeners.add(l);
        }
    }

    public void removeListener(LogListener l)
    {
        synchronized (listeners)
        {
            listeners.remove(l);
        }
    }

    public void removeAllListeners()
    {
        synchronized (listeners)
        {
            Iterator i = listeners.iterator();
            while (i.hasNext())
            {
                LogListener l = ((LogListener) i.next());
                if (l instanceof Destroyable)
                {
                    ((Destroyable) l).destroy();
                }
            }
            listeners.clear();
        }
    }

    public static void log(LogEvent evt)
    {
        Logger l = null;
        LogSource source = evt.getSource();

        if (source != null)
        {
            l = source.getLogger();
        }
        if (l != null && l.hasListeners())
        {
            synchronized (l.listeners)
            {
                Iterator i = l.listeners.iterator();
                while (i.hasNext() && evt != null)
                {
                    evt = ((LogListener) i.next()).log(evt);
                }
            }
        }
    }

    /**
     * associates this Logger with a name using NameRegistrar
     * @param name name to register
     * @see NameRegistrar
     */
    public void setName(String name)
    {
        this.name = name;
        NameRegistrar.register("logger." + name, this);
    }

    /**
     * destroy logger
     */
    public void destroy()
    {
        NameRegistrar.unregister("logger." + name);
        removeAllListeners();
    }

    /**
     * @return logger instance with given name. Creates one if necessary
     * @see NameRegistrar
     */
    public synchronized static Logger getLogger(String name)
    {
        Logger l;
        try
        {
            l = (Logger) NameRegistrar.get("logger." + name);
        }
        catch (NameRegistrar.NotFoundException e)
        {
            l = new Logger();
            l.setName(name);
        }
        return l;
    }

    /**
     * @return this logger's name ("" if no name was set)
     */
    public String getName()
    {
        return this.name;
    }

    /**
     * Used by heavy used methods to avoid LogEvent creation 
     * @return true if Logger has associated LogListsners
     */
    public boolean hasListeners()
    {
        synchronized (listeners)
        {
            return listeners.size() > 0;
        }
    }
}
