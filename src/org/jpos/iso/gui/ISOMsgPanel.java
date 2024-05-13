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
package org.jpos.iso.gui;

//import Mobile.BRMainMOB;
//import Mobile.BRFrameMOB;
//import PHilae.BRFrame;
//import PHilae.BRMain;
import APX.PHFrame;
import APX.PHMain;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import org.jpos.iso.ISOException;
import org.jpos.iso.ISOMsg;
import org.jpos.iso.ISOPackager;
import org.jpos.iso.ISOUtil;

import javax.swing.event.ListSelectionEvent;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.util.Vector;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import org.jpos.iso.packager.GenericPackager;
import org.jpos.iso.packager.PostPackager;

/**
 * Called from ISOChannelPanel when you click on it's ISOMeter. It enable field and header visualization by
 * means of visual components such as JTable
 *
 * @see ISOChannelPanel
 * @see ISORequestListenerPanel
 */
public class ISOMsgPanel extends JPanel
{
    private static final long serialVersionUID = 7779880613544725704L;
    /**
     * @serial
     */
    ISOMsg m;
    /**
     * @serial
     */
    Vector validFields;

    public ISOMsgPanel(ISOMsg m, boolean withDump)
    {
        super();
        this.m = m;
        setLayout(new BorderLayout());
        setBorder(BorderFactory.createRaisedBevelBorder());
        setValidFields();

        if (withDump)
        {
            add(createISOMsgDumpPanel(), BorderLayout.SOUTH);
        }
    }

    public ISOMsgPanel(ISOMsg m)
    {
        this(m, false);
    }

    private void setValidFields()
    {
        validFields = new Vector();
        for (int i = 0; i <= m.getMaxField(); i++)
        {
            if (m.hasField(i))
            {
                validFields.addElement(i);
            }
        }
    }
  public JTable createMobISOMsgTable()
    {
        TableModel dataModel = new AbstractTableModel()
        {
            private static final long serialVersionUID = 8917029825751856951L;

            @Override
            public int getColumnCount()
            {
                return 3;
            }

            @Override
            public int getRowCount()
            {
                return validFields.size();
            }

            @Override
            public Class<?> getColumnClass(int col)
            {
                if (col == 0)
                {
                    return Integer.class;
                }

                return String.class;
            }

            @Override
            public String getColumnName(int columnIndex)
            {
                switch (columnIndex)
                {
                    case 0:
                        return "Field";
                    case 1:
                        return "Content";
                    case 2:
                        return "Description";
                    default:
                        return "";
                }
            }

            @Override
            public Object getValueAt(int row, int col)
            {
                switch (col)
                {
                    case 0:
                        return ((Integer) validFields.elementAt(row));
                    case 1:
                        try
                        {
                            int index = ((Integer) validFields.elementAt(row));

                            Object obj = m.getValue(index);
                            if (index == 0)
                            {
                                obj = m.getMTI();
                            }
                            if (obj instanceof String)
                            {
                                if (index == 2)
                                {                                   
                                        return PHMain.phFrame.protectCard(obj.toString());
                                   
                                }
                                return obj.toString();
                            }
                            else if (obj instanceof byte[])
                            {
                                return ISOUtil.hexString((byte[]) obj);
                            }
                            else if (obj instanceof ISOMsg)
                            {
                                return "<ISOMsg>";
                            }
                        }
                        catch (ISOException e)
                        {
                            e.printStackTrace();
                        }
                        break;
                    case 2:
                        int i = ((Integer) validFields.elementAt(row));
                        ISOPackager p = m.getPackager();

                        if (p == null || p instanceof GenericPackager)
                        {
                            if (m.isInner())
                            {
                                return (new PostPackager().getFld127Packager()).getFieldDescription(m, i);
                            }
                            return new PostPackager().getFieldDescription(m, i);
                        }

                        return p.getFieldDescription(m, i);
                }
                return "<???>";
            }
        };
        JTable table = new JTable(dataModel);
        Font tableFont = table.getTableHeader().getFont();
        table.getTableHeader().setFont(new Font(tableFont.getName(), Font.BOLD, tableFont.getSize()));

        table.getColumnModel().getColumn(0).setMinWidth(75);
        table.getColumnModel().getColumn(0).setMaxWidth(75);

        table.getColumnModel().getColumn(1).setMinWidth(325);
        table.getColumnModel().getColumn(2).setMinWidth(325);

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setBorder(javax.swing.BorderFactory.createLineBorder(javax.swing.UIManager.getDefaults().getColor("Table.gridColor")));

        table.setPreferredScrollableViewportSize(new Dimension(545, table.getRowCount() * table.getRowHeight()));
        ListSelectionModel rowSM = table.getSelectionModel();

        rowSM.addListSelectionListener((ListSelectionEvent e) ->
        {
            if (e.getValueIsAdjusting())
            {
                return;
            }

            ListSelectionModel lsm = (ListSelectionModel) e.getSource();
            if (!lsm.isSelectionEmpty())
            {
                int selectedRow = lsm.getMinSelectionIndex();
                try
                {
                    int index = ((Integer) validFields.elementAt(selectedRow));

                    Object obj = m.getValue(index);
                    if (obj instanceof ISOMsg)
                    {
                        ISOMsg sm = (ISOMsg) obj;
                        JDialog dialog = new JDialog(PHMain.phFrame);

                        dialog.setTitle("Field " + index);
                        ISOMsgPanel p = new ISOMsgPanel(sm, false);

                        dialog.setContentPane(new JScrollPane(p.createISOMsgTable()));
                        dialog.pack();

                        dialog.setLocationRelativeTo(PHMain.phFrame);
                        dialog.setVisible(true);
                    }
                }
                catch (ISOException ex)
                {
                    ex.printStackTrace();
                }
            }
        });

        return PHFrame.prepareTable(table);
    }
    public JTable createISOMsgTable()
    {
        TableModel dataModel = new AbstractTableModel()
        {
            private static final long serialVersionUID = 8917029825751856951L;

            @Override
            public int getColumnCount()
            {
                return 3;
            }

            @Override
            public int getRowCount()
            {
                return validFields.size();
            }

            @Override
            public Class<?> getColumnClass(int col)
            {
                if (col == 0)
                {
                    return Integer.class;
                }

                return String.class;
            }

            @Override
            public String getColumnName(int columnIndex)
            {
                switch (columnIndex)
                {
                    case 0:
                        return "Field";
                    case 1:
                        return "Content";
                    case 2:
                        return "Description";
                    default:
                        return "";
                }
            }

            @Override
            public Object getValueAt(int row, int col)
            {
                switch (col)
                {
                    case 0:
                        return ((Integer) validFields.elementAt(row));
                    case 1:
                        try
                        {
                            int index = ((Integer) validFields.elementAt(row));

                            Object obj = m.getValue(index);
                            if (index == 0)
                            {
                                obj = m.getMTI();
                            }
                            if (obj instanceof String)
                            {
                                if (index == 2)
                                {
                                    return PHMain.phFrame.protectCard(obj.toString());
                                }
                                return obj.toString();
                            }
                            else if (obj instanceof byte[])
                            {
                                return ISOUtil.hexString((byte[]) obj);
                            }
                            else if (obj instanceof ISOMsg)
                            {
                                return "<ISOMsg>";
                            }
                        }
                        catch (ISOException e)
                        {
                            e.printStackTrace();
                        }
                        break;
                    case 2:
                        int i = ((Integer) validFields.elementAt(row));
                        ISOPackager p = m.getPackager();

                        if (p == null || p instanceof GenericPackager)
                        {
                            if (m.isInner())
                            {
                                return (new PostPackager().getFld127Packager()).getFieldDescription(m, i);
                            }
                            return new PostPackager().getFieldDescription(m, i);
                        }

                        return p.getFieldDescription(m, i);
                }
                return "<???>";
            }
        };
        JTable table = new JTable(dataModel);
        Font tableFont = table.getTableHeader().getFont();
        table.getTableHeader().setFont(new Font(tableFont.getName(), Font.BOLD, tableFont.getSize()));

        table.getColumnModel().getColumn(0).setMinWidth(75);
        table.getColumnModel().getColumn(0).setMaxWidth(75);

        table.getColumnModel().getColumn(1).setMinWidth(325);
        table.getColumnModel().getColumn(2).setMinWidth(325);

        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setBorder(javax.swing.BorderFactory.createLineBorder(javax.swing.UIManager.getDefaults().getColor("Table.gridColor")));

        table.setPreferredScrollableViewportSize(new Dimension(545, table.getRowCount() * table.getRowHeight()));
        ListSelectionModel rowSM = table.getSelectionModel();

        rowSM.addListSelectionListener((ListSelectionEvent e) ->
        {
            if (e.getValueIsAdjusting())
            {
                return;
            }
            
            ListSelectionModel lsm = (ListSelectionModel) e.getSource();
            if (!lsm.isSelectionEmpty())
            {
                int selectedRow = lsm.getMinSelectionIndex();
                try
                {
                    int index = ((Integer) validFields.elementAt(selectedRow));
                    
                    Object obj = m.getValue(index);
                    if (obj instanceof ISOMsg)
                    {
                        ISOMsg sm = (ISOMsg) obj;
                        JDialog dialog = new JDialog(PHMain.phFrame);
                        
                        dialog.setTitle("Field " + index);
                        ISOMsgPanel p = new ISOMsgPanel(sm, false);
                        
                        dialog.setContentPane(new JScrollPane(p.createISOMsgTable()));
                        dialog.pack();
                        
                        dialog.setLocationRelativeTo(PHMain.phFrame);
                        dialog.setVisible(true);
                    }
                }
                catch (ISOException ex)
                {
                    ex.printStackTrace();
                }
            }
        });

        return PHFrame.prepareTable(table);
    }

    final JComponent createISOMsgDumpPanel()
    {
        JPanel p = new JPanel();
        JTextArea t = new JTextArea(3, 20);

        p.setLayout(new BorderLayout());
        p.setBackground(Color.white);
        p.setBorder(BorderFactory.createLoweredBevelBorder());
        p.add(new JLabel("Dump", SwingConstants.LEFT), BorderLayout.NORTH);
        t.setFont(new Font("Helvetica", Font.PLAIN, 8));
        t.setLineWrap(true);
        try
        {
            StringBuilder buf = new StringBuilder();
            if (m.getHeader() != null)
            {
                buf.append("--[Header]--\n").append(ISOUtil.hexString(m.getHeader())).append("\n--[Msg]--\n");
            }
            byte[] b = m.pack();
            buf.append(ISOUtil.hexString(b));
            t.setText(buf.toString());
        }
        catch (ISOException e)
        {
            t.setText(e.toString());
            t.setForeground(Color.red);
        }
        p.add(t, BorderLayout.CENTER);
        return p;
    }
}
