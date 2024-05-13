/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package Mobile;

import java.awt.Component;
import java.awt.Font;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;

/**
 *
 * @author Pecherk
 */
public class TRendererMOB extends DefaultTreeCellRenderer
{
    Icon txnIcon = new ImageIcon(getClass().getResource("/images/txn16.png"));
    Icon rootIcon = new ImageIcon(getClass().getResource("/images/root16.gif"));
    Icon nodeIcon = new ImageIcon(getClass().getResource("/images/node16.gif"));
    Icon crossIcon = new ImageIcon(getClass().getResource("/images/leaf16.png"));

    @Override
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf, int row, boolean hasFocus)
    {
        super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
        DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
        setFont(new Font(getFont().getName(), (node.isLeaf() ? Font.PLAIN : Font.BOLD), getFont().getSize()));
        setIcon(node.isRoot() ? rootIcon : (node.getUserObject() instanceof EITxnMOB ? txnIcon : crossIcon));
        return this;
    }
}
