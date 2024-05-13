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

import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.*;

/**
 * @author apr@cs.com.uy
 * @version $Id: LogEvent.java 2877 2010-02-20 18:48:47Z apr $
 */
public class LogEvent
{
    private LogSource source;
    private String tag;
    private List<Object> payLoad;
    private long createdAt;
    private long dumpedAt;

    public LogEvent(String tag)
    {
        super();
        this.tag = tag;
        createdAt = System.currentTimeMillis();
        this.payLoad = new ArrayList<>();
    }

    public LogEvent()
    {
        this("info");
    }

    public LogEvent(String tag, Object msg)
    {
        this(tag);
        addMessage(msg);
    }

    public LogEvent(LogSource source, String tag)
    {
        this(tag);
        this.source = source;
    }

    public LogEvent(LogSource source, String tag, Object msg)
    {
        this(tag);
        this.source = source;
        addMessage(msg);
    }

    public String getTag()
    {
        return tag;
    }

    public final void clear()
    {
        payLoad.clear();
    }
    
    public final void addMessage(Object msg)
    {
        payLoad.add(msg);
    }

    public void addMessage(String tagname, String message)
    {
        payLoad.add("<" + tagname + ">" + message + "</" + tagname + ">");
    }

    public LogSource getSource()
    {
        return source;
    }

    public void setSource(LogSource source)
    {
        this.source = source;
    }

    protected String dumpHeader(PrintStream p, String indent)
    {
        if (dumpedAt == 0L)
        {
            dumpedAt = System.currentTimeMillis();
        }
        Date date = new Date(dumpedAt);
        StringBuilder sb = new StringBuilder(indent);
        sb.append("<log realm=\"");
        sb.append(getRealm());
        sb.append("\" at=\"");
        sb.append(date.toString());
        sb.append('.');
        sb.append(Long.toString(dumpedAt % 1000));
        sb.append('"');
        if (dumpedAt != createdAt)
        {
            sb.append(" lifespan=\"");
            sb.append(Long.toString(dumpedAt - createdAt));
            sb.append(" ms\"");
        }
        sb.append('>');
        p.println(sb.toString());
        return indent + "\t";
    }

    protected void dumpTrailer(PrintStream p, String indent)
    {
        p.println(indent + "</log>");
    }

    public void dump(PrintStream p, String outer)
    {
        if (!payLoad.isEmpty())
        {
            String newIndent = "";
            boolean insertLineBreak = true;
            String indent = dumpHeader(p, outer);

            if (tag != null)
            {
                p.print(indent + "<" + tag + ">");
                newIndent = indent + "\t";
            }
            else
            {
                newIndent = "";
            }
            for (Object o : payLoad)
            {
                if (o instanceof Loggeable)
                {
                    p.println();
                    ((Loggeable) o).dump(p, newIndent);
                }
                else if (o instanceof Throwable)
                {
                    p.println();
                    Exception e = (Exception) o;
                    p.println(newIndent + "<class>" + e.getClass().getSimpleName() + "</class>");
                    p.println(newIndent + "<message>" + e.getMessage() + "</message>");
                    p.println(newIndent + "<stacktrace>");
                    for (StackTraceElement s : ((Throwable) o).getStackTrace())
                    {
                        p.println(newIndent + "\t" + "at " + s.toString());
                    }
                    p.print(newIndent + "</stacktrace>");
                }
                else if (o instanceof Object[])
                {
                    p.println();
                    Object[] oa = (Object[]) o;
                    p.print(newIndent + "[");
                    for (int j = 0; j < oa.length; j++)
                    {
                        if (j > 0)
                        {
                            p.print(",");
                        }
                        p.print(oa[j].toString());
                    }
                    p.println("]");
                }
                else if (o instanceof Element)
                {
                    p.println();
                    p.println(newIndent + "<![CDATA[");
                    XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
                    out.getFormat().setLineSeparator("\n");
                    try
                    {
                        out.output((Element) o, p);
                    }
                    catch (IOException ex)
                    {
                        ex.printStackTrace(p);
                    }
                    p.println();
                    p.println(newIndent + "]]>");
                }
                else if (o != null)
                {
                    insertLineBreak = payLoad.size() > 1;
                    if (o.toString().startsWith("<"))
                    {
                        p.println();
                        p.println(newIndent + o.toString());
                    }
                    else
                    {
                        p.print(o.toString());
                        indent = "";
                    }
                }
            }
            if (tag != null)
            {
                if (insertLineBreak)
                {
                    p.println();
                }
                p.println(indent + "</" + tag + ">");
            }

            dumpTrailer(p, outer);
        }
    }

    public String getRealm()
    {
        return source != null ? source.getRealm() : "";
    }

    public List getPayLoad()
    {
        return payLoad;
    }

    public String toString()
    {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream p = new PrintStream(baos);
        dump(p, "");
        return baos.toString();
    }
}
