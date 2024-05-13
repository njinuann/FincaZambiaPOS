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
package org.jpos.q2.qbean;

import org.jpos.iso.ISOUtil;
import org.jpos.q2.Q2;
import org.jpos.q2.QBeanSupport;
import org.jpos.util.Loggeable;
import org.jpos.util.Logger;
import org.jpos.util.NameRegistrar;

import java.io.PrintStream;

/**
 * Periodically dumps Thread and memory usage
 * 
 * @author apr@cs.com.uy
 * @version $Id: SystemMonitor.java 2986 2010-09-28 20:18:24Z apr $
 * @jmx:mbean description="System Monitor"
 *            extends="org.jpos.q2.QBeanSupportMBean"
 * @see Logger
 */
public class SystemMonitor extends QBeanSupport implements Runnable, SystemMonitorMBean, Loggeable
{
    private long sleepTime = 60 * 60 * 1000;
    private long delay = 0;
    private boolean detailRequired = false;
    private Thread me = null;

    public void startService()
    {
        try
        {
            me = new Thread(this, "SystemMonitor");
            me.start();
        }
        catch (Exception e)
        {
            log.warn("<message>Error starting TXNListener</message>", e);
        }
    }

    public void stopService()
    {
        if (me != null)
        {
            me.interrupt();
        }
        log.info("TXNListener stopped");
    }

    /**
     * @jmx:managed-attribute description="Milliseconds between dump"
     */
    public synchronized void setSleepTime(long sleepTime)
    {
        this.sleepTime = sleepTime;
        setModified(true);
        if (me != null)
        {
            me.interrupt();
        }
    }

    /**
     * @jmx:managed-attribute description="Milliseconds between dump"
     */
    public synchronized long getSleepTime()
    {
        return sleepTime;
    }

    /**
     * @jmx:managed-attribute description="Detail required?"
     */
    public synchronized void setDetailRequired(boolean detail)
    {
        this.detailRequired = detail;
        setModified(true);
        if (me != null)
        {
            me.interrupt();
        }
    }

    /**
     * @jmx:managed-attribute description="Detail required?"
     */
    public synchronized boolean getDetailRequired()
    {
        return detailRequired;
    }

    void dumpThreads(ThreadGroup g, PrintStream p, String indent)
    {
        Thread[] list = new Thread[g.activeCount() + 5];
        int nthreads = g.enumerate(list);
        for (int i = 0; i < nthreads; i++)
        {
            p.println(indent + "<thread>" + list[i] + "</thread>");
        }
    }

    public void showThreadGroup(ThreadGroup g, PrintStream p, String indent)
    {
        if (g.getParent() != null)
        {
            showThreadGroup(g.getParent(), p, indent);
        }
        else
        {
            dumpThreads(g, p, indent);
        }
    }

    public void run()
    {
        while (running())
        {
            log.info(this);
            try
            {
                long expected = System.currentTimeMillis() + sleepTime;
                Thread.sleep(sleepTime);
                delay = (System.currentTimeMillis() - expected);
            }
            catch (InterruptedException e)
            {
                //log.error(e.getMessage());
            }
        }
    }

    public void dump(PrintStream p, String indent)
    {
        String newIndent = indent + "\t";
        Runtime r = Runtime.getRuntime();
        p.printf("%s<revision>%s</revision>\n", indent, Q2.getRevision());
        p.printf("%s<uptime>%s</uptime>\n", indent, ISOUtil.millisToString(getServer().getUptime()));
        p.println(indent + "<memory>");
        p.println(newIndent + "<free>" + (r.freeMemory() / 1024) + " kb</free>");
        p.println(newIndent + "<total>" + (r.totalMemory() / 1024) + " kb</total>");
        p.println(newIndent + "<used>" + ((r.totalMemory() - r.freeMemory()) / 1024) + " kb</used>");
        p.println(indent + "</memory>");
        if (System.getSecurityManager() != null)
        {
            p.println(indent + "sec.manager=" + System.getSecurityManager());
        }
        p.println(indent + "<threads>");
        p.println(newIndent + "<delay>" + delay + " ms</delay>");
        p.println(newIndent + "<count>" + Thread.activeCount() + "</count>");
        showThreadGroup(Thread.currentThread().getThreadGroup(), p, newIndent);
        p.println(indent + "</threads>");
        NameRegistrar.getInstance().dump(p, indent, detailRequired);
    }
}
