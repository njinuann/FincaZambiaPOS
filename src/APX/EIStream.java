/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package APX;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.util.regex.Pattern;
import javax.swing.SwingUtilities;

/**
 *
 * @author Pecherk
 */
public class EIStream extends ByteArrayOutputStream
{
    private Color color = Color.BLACK;
    private final static Pattern RTRIM = Pattern.compile("\\s+$");
    private final static Pattern LTRIM = Pattern.compile("^\\s+");

    public EIStream(Color color)
    {
        setColor(color);
    }

    @Override
    public void write(byte[] b)
    {
        write(new String(b));
    }

    @Override
    public void write(int b)
    {
        write(String.valueOf(b));
    }

    @Override
    public void write(byte[] b, int off, int len)
    {
        write(new String(b, off, len));
    }

    public void write(String message)
    {
        try
        {
            if (message.length() > 0)
            {
                SwingUtilities.invokeLater(()
                    -> 
                    {
                        if (!PHMain.isPosRunning || !PHMain.isMobRunning)
                        {
                            ((IDisplay) CPanel.displayConsole).append(message, getColor());
                        }
                        ((IDisplay) PHFrame.displayConsole).append(message, getColor());
                });
            }
        }
        catch (Exception ex)
        {
            if (PHMain.posBridgeLog != null)
            {
                PHMain.posBridgeLog.error(ex);
            }
        }
    }

    public String trimRight(String text)
    {
        return RTRIM.matcher(text).replaceAll("").replaceAll("\r\n", "");
    }

    public String trimLeft(String text)
    {
        return LTRIM.matcher(text).replaceAll("").replaceAll("\r\n", "");
    }

    /**
     * @return the color
     */
    public Color getColor()
    {
        return color;
    }

    /**
     * @param color the color to set
     */
    public final void setColor(Color color)
    {
        this.color = color;
    }
}
