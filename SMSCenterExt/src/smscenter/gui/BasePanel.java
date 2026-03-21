/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package smscenter.gui;

import smscenter.database.Database;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author chtheis
 */
abstract class BasePanel extends javax.swing.JPanel {
    
    protected static final String prefix = "";
    
    protected final Timer timer = new Timer();
    
    private class UpdateTimerClass extends TimerTask {
        @Override
        public void run() {
            try {
                update();
            } catch (Throwable t) {
                Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, t);                                
            }   
            
            timer.schedule(new UpdateTimerClass(), MainFrame.updateInterval * 1000);
        }        
    }
    
    protected BasePanel(Database database) {
        this.database = database;
        
        timer.schedule(new UpdateTimerClass(), MainFrame.updateInterval * 1000);
    }
    
    abstract public void update();

    protected <T> void updateRows(List<T[]> list, javax.swing.JTable table) {
        javax.swing.table.DefaultTableModel dtm = (javax.swing.table.DefaultTableModel) table.getModel();
        int oldRowCount = dtm.getDataVector().size();
        int[] oldSelRows = table.getSelectedRows();
        Object[] oldSelValues = new Object[oldSelRows.length];

        for (int idx = 0; idx < oldSelRows.length; idx++)
            oldSelValues[idx] = dtm.getValueAt(oldSelRows[idx], 1);
        
        dtm.getDataVector().clear();
        for (T[] o: list)
            dtm.getDataVector().add(convertToVector(o));
        int newRowCount = dtm.getDataVector().size();

        if (oldRowCount > newRowCount) {
            dtm.fireTableRowsDeleted(newRowCount, oldRowCount - 1);
            dtm.fireTableRowsUpdated(0, newRowCount);
        } 
        
        if (newRowCount > oldRowCount) {
            dtm.fireTableRowsInserted(oldRowCount, newRowCount - 1);  
            dtm.fireTableRowsUpdated(0, oldRowCount);
        }
        
        if (newRowCount == oldRowCount) {
            dtm.fireTableRowsUpdated(0, newRowCount);
        }
        
        Map<Object, Integer> selectionValues = new java.util.HashMap<>();
        Set<Integer> selectedRows = new java.util.HashSet<>();
        
        for (int row = dtm.getRowCount(); row > 0; ) {
            --row;
            selectionValues.put(dtm.getValueAt(row, 1), row);
        }
        
        for (Object o : oldSelValues) {
            if (selectionValues.containsKey(o))
                selectedRows.add(selectionValues.get(o));
        }
        
        table.clearSelection();
        for (int row : selectedRows)
            table.addRowSelectionInterval(row, row);
    }
    
    
    @SuppressWarnings("UseOfObsoleteCollectionType")
    protected <T> java.util.Vector<T> convertToVector(T[] a) {
        java.util.Vector<T> v = new java.util.Vector<>();
        v.addAll(Arrays.asList(a));

        return v;
    }
              
    protected boolean isSMSServerRunning() {
        return ((MainFrame) getTopLevelAncestor()).isSMSServerRunning();         
    }
    
    protected Database database;

}
