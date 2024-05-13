/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package Mobile;

import java.beans.Introspector;
import java.beans.MethodDescriptor;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Pecherk
 */
public class TXUtilityMOB implements Serializable
{
    public String convertToString(Object object)
    {
        return convertToString(object, null);
    }

    private String convertToString(Object object, String tag)
    {
        String text = "";
        boolean empty = true;
        tag = decapitalize(tag);
        Class<?> beanClass = object != null ? object.getClass() : String.class;
        try
        {
            if (object != null)
            {
                for (MethodDescriptor methodDescriptor : Introspector.getBeanInfo(beanClass).getMethodDescriptors())
                {
                    if ("toString".equalsIgnoreCase(methodDescriptor.getName()) && beanClass == methodDescriptor.getMethod().getDeclaringClass())
                    {
                        return tag != null ? (tag + "=<" + String.valueOf(object) + ">") : String.valueOf(object);
                    }
                }

                tag = tag == null ? beanClass.getSimpleName() : tag;

                if (object instanceof List)
                {
                    boolean append = false;
                    text += (empty ? "" : ", ");
                    for (Object item : ((List) object).toArray())
                    {
                        if (item != null)
                        {
                            text += (append ? ", " : "") + convertToString(item, null);
                            append = true;
                        }
                    }
                    empty = false;
                }
                else if (object instanceof Map)
                {
                    boolean append = false;
                    text += (empty ? "" : ", ");
                    for (Object key : ((Map) object).keySet())
                    {
                        text += (append ? ", " : "") + convertToString(((Map) object).get(key), String.valueOf(key));
                        append = true;
                    }
                    empty = false;
                }
                else if (beanClass.isArray())
                {
                    text += tag + "=<[\r\n";
                    for (Object item : (Object[]) object)
                    {
                        text += convertToString(item, null) + "\r\n";
                    }
                    text += "]>";
                }
                else
                {
                    for (PropertyDescriptor propertyDesc : Introspector.getBeanInfo(beanClass).getPropertyDescriptors())
                    {
                        Method readMethod = propertyDesc.getReadMethod();
                        if (readMethod != null)
                        {
                            Object value = propertyDesc.getReadMethod().invoke(object);
                            if (!(value instanceof Class))
                            {
                                text += (empty ? "" : ", ") + convertToString(value, propertyDesc.getName());
                                empty = false;
                            }
                        }
                    }
                }
            }
        }
        catch (Exception ex)
        {
            ex.printStackTrace();
        }
        return empty ? (tag != null ? (tag + "=<" + String.valueOf(object) + ">") : String.valueOf(object)) : (tag != null ? (tag + "=<[ " + text + " ]>") : text);
    }

    public String capitalize(String text)
    {
        if (text != null ? text.length() > 0 : false)
        {
            StringBuilder builder = new StringBuilder();
            for (String word : text.toLowerCase().split("\\s"))
            {
                builder.append(word.substring(0, 1).toUpperCase()).append(word.substring(1).toLowerCase()).append(" ");
            }
            return builder.toString().trim();
        }
        return text;
    }

    public String capitalize(String text, int minLen)
    {
        if (text != null)
        {
            StringBuilder builder = new StringBuilder();
            for (String word : text.split("\\s"))
            {
                builder.append(word.length() > minLen ? capitalize(word) : word.toUpperCase()).append(" ");
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
            for (String word : text.toLowerCase().split("\\s"))
            {
                builder.append(word.substring(0, 1).toLowerCase()).append(word.substring(1)).append(" ");
            }
            return builder.toString().trim();
        }
        return text;
    }
}
