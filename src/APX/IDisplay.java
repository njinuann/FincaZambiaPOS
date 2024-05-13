package APX;


import java.awt.Color;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Element;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import javax.swing.text.TabSet;
import javax.swing.text.TabStop;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates and open the template
 * in the editor.
 */
/**
 *
 * @author Pecherk
 */
public class IDisplay extends JTextPane implements DocumentListener
{
    SimpleAttributeSet attributeSet = new SimpleAttributeSet();
    StyledDocument doc = (StyledDocument) getDocument();
    Element root = doc.getDefaultRootElement();

    public IDisplay()
    {
        addListener();
        setTabSize();
    }

    private void addListener()
    {
        doc.addDocumentListener(this);
    }

    private void setTabSize()
    {
        StyleConstants.setTabSet(attributeSet, new TabSet(new TabStop[]
        {
            new TabStop(25, TabStop.ALIGN_LEFT, TabStop.LEAD_NONE),
            new TabStop(50, TabStop.ALIGN_LEFT, TabStop.LEAD_NONE),
            new TabStop(75, TabStop.ALIGN_LEFT, TabStop.LEAD_NONE),
            new TabStop(100, TabStop.ALIGN_LEFT, TabStop.LEAD_NONE),
            new TabStop(125, TabStop.ALIGN_LEFT, TabStop.LEAD_NONE),
            new TabStop(150, TabStop.ALIGN_LEFT, TabStop.LEAD_NONE),
            new TabStop(175, TabStop.ALIGN_LEFT, TabStop.LEAD_NONE),
            new TabStop(200, TabStop.ALIGN_LEFT, TabStop.LEAD_NONE),
            new TabStop(225, TabStop.ALIGN_LEFT, TabStop.LEAD_NONE),
            new TabStop(250, TabStop.ALIGN_LEFT, TabStop.LEAD_NONE),
            new TabStop(275, TabStop.ALIGN_LEFT, TabStop.LEAD_NONE),
            new TabStop(300, TabStop.ALIGN_LEFT, TabStop.LEAD_NONE),
        }));
        setParagraphAttributes(attributeSet, false);
    }

    public void append(final String str, final Color color)
    {
        try
        {
            StyleConstants.setForeground(attributeSet, color);
            doc.insertString(doc.getLength(), str, attributeSet);
        }
        catch (Exception ex)
        {
            ex = null;
        }
    }

    public void removeLines()
    {
        while (root.getElementCount() > PHController.DisplayLines)
        {
            try
            {
                doc.remove(0, root.getElement(0).getEndOffset());
            }
            catch (Exception ex)
            {
                ex = null;
                break;
            }
        }
    }

    @Override
    public void insertUpdate(DocumentEvent e)
    {
        SwingUtilities.invokeLater(this::removeLines);
    }

    @Override
    public void removeUpdate(DocumentEvent e)
    {
    }

    @Override
    public void changedUpdate(DocumentEvent e)
    {
    }
}
