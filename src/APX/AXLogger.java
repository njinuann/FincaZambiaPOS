/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package APX;

import SMS.AXIgnore;
import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.Map;

/**
 *
 * @author Pecherk
 */
public class AXLogger
{

    private String indent = "\t";
    private final Long maxSize = 10485760L;
    private final BRFile brFile = new BRFile();
    public String realm = "acx", path = "acx" + File.separator + "logs";

    public AXLogger(String realm, String path)
    {
        this.realm = realm;
        this.path = path;
    }

    public void logDebug(Object event)
    {
        if (PHController.EnablePosDebug.equals("Y") || PHController.EnableMobDebug.equals("Y"))
        {
            logEvent(null, null, null, event);
        }
    }

    public void logDebug(String key, Object event)
    {
        if (PHController.EnablePosDebug.equals("Y") || PHController.EnableMobDebug.equals("Y"))
        {
            logEvent(key, null, null, event);
        }
    }

    public void logEvent(Object event)
    {
        logEvent(null, null, null, event);
    }

    public void logEvent(String input, Object event)
    {
        logEvent(null, input, null, event);
    }

    public void logEvent(String message, Object input, Object event)
    {
        logEvent(null, input, message, event);
    }

    public void logEvent(String key, Object input, String message, Object event)
    {
        try
        {
            StringBuilder logEvent = new StringBuilder("<event realm=\"" + (key != null ? key.toLowerCase() : realm) + "\" datetime=\"" + new Date() + "\">");
            if (event instanceof Throwable)
            {
                logEvent.append("\r\n").append(getIndent()).append("<error>");
                logEvent.append("\r\n").append(getIndent()).append(getIndent()).append("<class>").append(((Throwable) event).getClass().getSimpleName()).append("</class>");
                if (input != null)
                {
                    logEvent.append("\r\n").append(getIndent()).append(getIndent()).append("<input>").append(String.valueOf(input)).append("</input>");
                }
                logEvent.append("\r\n").append(getIndent()).append(getIndent()).append("<message>").append(message == null ? "" : message).append("[ ").append(cleanText(((Throwable) event).getMessage())).append(" ]").append("</message>");
                logEvent.append("\r\n").append(getIndent()).append(getIndent()).append("<stacktrace>");
                for (StackTraceElement s : ((Throwable) event).getStackTrace())
                {
                    logEvent.append("\r\n").append(getIndent()).append(getIndent()).append(getIndent()).append("at ").append(s.toString());
                }
                logEvent.append("\r\n").append(getIndent()).append(getIndent()).append("</stacktrace>");
                logEvent.append("\r\n").append(getIndent()).append("</error>");
                logEvent.append("\r\n").append("</event>\r\n");
            }
            else if (String.valueOf(event).trim().startsWith("<") && String.valueOf(event).trim().endsWith(">"))
            {
                logEvent.append("\r\n").append(indentAllLines(String.valueOf(event))).append("\r\n");
                logEvent.append("</event>\r\n");
            }
            else
            {
                logEvent.append("\r\n").append(getIndent()).append("<info>").append(String.valueOf(event)).append("</info>");
                logEvent.append("\r\n").append("</event>\r\n");
            }
            new Thread(()
                    ->
            {
                writeToLog(logEvent.toString(), event instanceof Throwable);
            }).start();
//            if (event instanceof AXCaller)
//            {
//                ((AXCaller) event).getCalls().clear();
//            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private String convertToString(Object object, String tag)
    {
        tag = decapitalize(tag);
        String text = "", items = "", prevVal = "";
        Class<?> beanClass = !isBlank(object) ? object.getClass() : String.class;
        try
        {
            if (isBlank(object))
            {
                return !isBlank(tag) ? (tag + "=<" + String.valueOf(object) + ">") : String.valueOf(object);
            }
            for (MethodDescriptor methodDescriptor : Introspector.getBeanInfo(beanClass).getMethodDescriptors())
            {
                if ("toString".equalsIgnoreCase(methodDescriptor.getName()) && beanClass == methodDescriptor.getMethod().getDeclaringClass() && !(methodDescriptor.getMethod().getAnnotation(AXIgnore.class) instanceof AXIgnore))
                {
                    return !isBlank(tag) ? (tag + "=<" + String.valueOf(object) + ">") : String.valueOf(object);
                }
            }
            if (object instanceof byte[])
            {
                return !isBlank(tag) ? (tag + "=<" + new String((byte[]) object) + ">") : new String((byte[]) object);
            }

            tag = isBlank(tag) ? beanClass.getSimpleName() : tag;

            if (object instanceof Collection)
            {
                for (Object item : ((Collection) object).toArray())
                {
                    items += (isBlank(items) ? "" : ", ") + (prevVal.contains("\r\n") ? "\r\n" : "") + (prevVal = convertToString(item, null)).trim();
                }
                return items.contains("\r\n") ? ("\r\n" + tag + "=<[" + "\r\n\t" + items + "\r\n" + "]>") : (tag + "=<[" + (!isBlank(items) ? " " + items + " " : "") + "]>");
            }
            else if (object instanceof Map)
            {
                for (Object key : ((Map) object).keySet())
                {
                    items += (isBlank(items) ? "" : ", ") + (prevVal.contains("\r\n") ? "\r\n" : "") + (prevVal = convertToString(((Map) object).get(key), String.valueOf(key))).trim();
                }
                return items.contains("\r\n") ? ("\r\n" + tag + "=<[" + "\r\n\t" + items + "\r\n" + "]>") : (tag + "=<[" + (!isBlank(items) ? " " + items + " " : "") + "]>");
            }
            else if (beanClass.isArray())
            {
                switch (beanClass.getSimpleName())
                {
                    case "int[]":
                        return (tag + "=<" + Arrays.toString((int[]) object) + ">");
                    case "long[]":
                        return (tag + "=<" + Arrays.toString((long[]) object) + ">");
                    case "boolean[]":
                        return (tag + "=<" + Arrays.toString((boolean[]) object) + ">");
                    case "byte[]":
                        return (tag + "=<" + Arrays.toString((byte[]) object) + ">");
                    case "char[]":
                        return (tag + "=<" + Arrays.toString((char[]) object) + ">");
                    case "double[]":
                        return (tag + "=<" + Arrays.toString((double[]) object) + ">");
                    case "float[]":
                        return (tag + "=<" + Arrays.toString((float[]) object) + ">");
                    case "short[]":
                        return (tag + "=<" + Arrays.toString((short[]) object) + ">");
                }
                if (object instanceof Object[])
                {
                    for (Object item : (Object[]) object)
                    {
                        items += (isBlank(items) ? "" : ", ") + (prevVal.contains("\r\n") ? "\r\n\t" : "") + (prevVal = convertToString(item, null)).trim();
                    }
                    return items.contains("\r\n") ? ("\r\n" + tag + "=<[" + "\r\n\t" + items + "\r\n" + "]>") : (tag + "=<[" + (!isBlank(items) ? " " + items + " " : "") + "]>");
                }
                else
                {
                    return (tag + "=<[" + String.valueOf(object) + "]>");
                }
            }
            else
            {
                Method readMethod;
                for (PropertyDescriptor propertyDesc : Introspector.getBeanInfo(beanClass).getPropertyDescriptors())
                {
                    if ((readMethod = propertyDesc.getReadMethod()) != null)
                    {
                        Object value = readMethod.invoke(object);
                        if (!(value instanceof Class))
                        {
                            text += (isBlank(text) ? "" : ", ") + convertToString(value, propertyDesc.getName());
                        }
                    }
                }
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return (!isBlank(tag) ? (tag + "=<[ " + (isBlank(text) ? String.valueOf(object) : text) + " ]>") : (isBlank(text) ? String.valueOf(object) : text));
    }

    public String indentAllLines(String text, String indent)
    {
        String line = "", buffer = "";
        try (BufferedReader bis = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(text.getBytes()))))
        {
            while (line != null)
            {
                buffer += indent + line + "\r\n";
                line = bis.readLine();
            }
            return indent + buffer.trim();
        }
        catch (Exception ex)
        {
            return indent + buffer.trim();
        }
    }

    public String capitalize(String text, boolean convertAllXters)
    {
        if (text != null ? text.length() > 0 : false)
        {
            StringBuilder builder = new StringBuilder();
            for (String word : text.replace("_", " ").split("\\s"))
            {
                builder.append(word.length() > 2 ? word.substring(0, 1).toUpperCase() + (convertAllXters ? word.substring(1).toLowerCase() : word.substring(1)) : word.toLowerCase()).append(" ");
            }
            return builder.toString().trim();
        }
        return text;
    }

    public String decapitalize(String text)
    {
        if (text != null ? text.length() > 0 : false)
        {
            StringBuilder builder = new StringBuilder();
            for (String word : text.split("\\s"))
            {
                builder.append(word.substring(0, 1).toLowerCase()).append(word.substring(1)).append(" ");
            }
            return builder.toString().trim();
        }
        return text;
    }

    public <T> T[] sortArray(T[] array, boolean ascending)
    {
        Arrays.sort(array, ascending ? ((Comparator<T>) (T o1, T o2) -> ((Comparable) o1).compareTo(o2)) : ((Comparator<T>) (T o1, T o2) -> ((Comparable) o2).compareTo(o1)));
        return array;
    }

    public boolean isBlank(Object object)
    {
        return object == null || "".equals(String.valueOf(object).trim()) || "null".equals(String.valueOf(object).trim()) || String.valueOf(object).trim().toLowerCase().contains("---select");
    }

    public String convertToString(Object object)
    {
        return convertToString(object, null).trim();
    }

    public String indentAllLines(String text)
    {
        String line = "", buffer = "";
        try (BufferedReader bis = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(text.getBytes()))))
        {
            while (line != null)
            {
                buffer += getIndent() + line + "\r\n";
                line = bis.readLine();
            }
        }
        catch (IOException ex)
        {
            return buffer;
        }

        return getIndent() + buffer.trim();
    }

    private String cleanText(String text)
    {
        String line, buffer = "";
        InputStream is = new ByteArrayInputStream(String.valueOf(text).getBytes());
        try (BufferedReader bis = new BufferedReader(new InputStreamReader(is, "UTF-8")))
        {
            while ((line = bis.readLine()) != null)
            {
                buffer += line;
            }
        }
        catch (IOException ex)
        {
            ex.printStackTrace();
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return buffer;
    }

    private void archiveOldLog(String lastDate, File logs, File logFile)
    {
        rotateExistingLogs(lastDate, logs);
        try
        {
            brFile.appendToFile(logFile, "</logger>");
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        File oldLog = new File(logs, "events-" + lastDate + "-0.log");
        logFile.renameTo(oldLog);
        try
        {
            brFile.compressFileToGzip(oldLog);
            brFile.deleteFile(oldLog);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        purgeOldLogs();
    }

    private void rotateExistingLogs(String lastDate, File logs)
    {
        int count = 99;
        while (count >= 0)
        {
            try
            {
                File prev = new File(logs, "events-" + lastDate + "-" + count + ".log.gz");
                if (prev.exists())
                {
                    if (count >= 99)
                    {
                        brFile.deleteFile(prev);
                    }
                    else
                    {
                        prev.renameTo(new File(logs, "events-" + lastDate + "-" + (count + 1) + ".log.gz"));
                    }
                }
            }
            catch (Exception ex)
            {
                ex.printStackTrace();
            }
            count--;
        }
    }

    private File getNewLog(File logsDir, File logFile)
    {
        logFile = new File(logsDir, "events.log");
        try
        {
            logFile.createNewFile();
            brFile.appendToFile(logFile, "<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
            brFile.appendToFile(logFile, "<logger class=\"" + AXLogger.class.getName() + "\" datetime=\"" + new Date() + "\">");
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return logFile;
    }

    private synchronized void writeToLog(String logEvent, boolean error)
    {
        try
        {
            (error ? System.err : System.out).print(logEvent);
            brFile.appendToFile(getLog(), logEvent);
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
    }

    private File getLog()
    {
        File logs = new File(path);
        if (!logs.exists())
        {
            logs.mkdirs();
        }
        File logFile = new File(path, "events.log");
        if (logFile.exists())
        {
            String lastDate = new SimpleDateFormat("yyyy-MM-dd").format(new Date(logFile.lastModified()));
            if (!lastDate.equals(new SimpleDateFormat("yyyy-MM-dd").format(new Date())) || logFile.length() >= maxSize)
            {
                archiveOldLog(lastDate, logs, logFile);
                return getNewLog(logs, logFile);
            }
            return logFile;
        }
        return getNewLog(logs, logFile);
    }

    private void purgeOldLogs()
    {

        File logs = new File(path);
        for (File log : logs.listFiles())
        {
            Calendar c1 = Calendar.getInstance();
            c1.setTime(new Date(log.lastModified()));
            if (Calendar.getInstance().get(Calendar.MONTH) - c1.get(Calendar.MONTH) >= 0 && Calendar.getInstance().get(Calendar.YEAR) - c1.get(Calendar.YEAR) >= 2)
            {
                try
                {
                    brFile.deleteFile(log);
                }
                catch (Exception ex)
                {
                    ex.printStackTrace();
                }
            }
        }
    }

    /**
     * @return the indent
     */
    public String getIndent()
    {
        return indent;
    }

    /**
     * @param indent the indent to set
     */
    public void setIndent(String indent)
    {
        this.indent = indent;
    }
}
