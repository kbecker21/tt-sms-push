/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package smscenter.gui;

import java.io.FileInputStream;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import smscenter.database.Database;
import smscenter.database.GroupEntry;
import smscenter.database.Match;
import smscenter.database.Player;
import static smscenter.gui.BasePanel.prefix;
import smscenter.gui.settings.Settings;

/**
 *
 * @author chtheis
 */
public class PositionsPanel extends BasePanel {
    
    /**
     * Creates new form PositionsPanel
     */
    public PositionsPanel(Database database) {
        super(database);
        initComponents();
    }

    @Override
    public void update() {
        GroupEntry[] entries = database.getUpdatePositions(MainFrame.updateDelay);
        List<Object[]> list = new java.util.ArrayList<>();
        Set<Integer> plSet = new java.util.HashSet<>();
        
        int row = 0;
        for (GroupEntry entry : entries) {
            if (!entry.scgEnabled)
                continue;
            
            list.add(new Object[] {++row, entry.scpID, entry.stID, entry.ts});
            
            if (entry.pl != null)
                plSet.add(entry.pl.plNr);
            if (entry.bd != null)
                plSet.add(entry.bd.plNr);
        }
        
        updateRows(list, positionsTable);
        
        if ( isSMSServerRunning() )
            sendMessages(entries, plSet);
    }
    
    
    public void sendMessages(GroupEntry[] entries, Set<Integer> plSet) {
        Map<Integer, Boolean> grFinishedMap = new java.util.HashMap<>();
        
        // Liste generieren und Test, ob die Gruppe combined bzw. fertig ist.
        for (GroupEntry entry : entries) {
            if (grFinishedMap.get(entry.gr.grID) == null)
                grFinishedMap.put(entry.gr.grID, entry.gr.grModus == 1 && database.isGroupFinished(entry.gr.grID));
        }
        
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("smscenter/resources/SMSCenter"); // NOI18N
        String phone;
        MessageFormat gf = new MessageFormat(bundle.getString("groupPosition"), Locale.ENGLISH);
        
        List<Integer> toDelete = new java.util.ArrayList<>();
        
        final SimpleDateFormat sdf = new SimpleDateFormat("d MMM HH:mm:ss", Locale.ENGLISH);
        final String sendTime = sdf.format(new Date());
        
        for (GroupEntry entry : entries) {
            // Ungueltige Eintraege aussortieren
            if (entry.scpID == 0)
                continue;
            
            if (!entry.scgEnabled) {
                toDelete.add(entry.scpID);
                continue;
            }
            
            if (entry.pl == null || entry.pl.plNr <= 0) {
                toDelete.add(entry.scpID);
                continue;
            }
            
            // Nur Gruppenspiele
            if (entry.gr.grModus != 1) {
                toDelete.add(entry.scpID);
                continue;
            }
            
            // Keine SMS wenn Pos 0 ist
            if (entry.stPos == 0) {
                toDelete.add(entry.scpID);
                continue;
            }
            
            // Nicht loeschen, wenn die Gruppe noch nicht fertig ist
            // Wenn sie es mal ist habe ich alle Positionen
            if (!grFinishedMap.get(entry.gr.grID))
                continue;
            
            String name = formatPlayer(entry.pl);
            if (entry.bd != null && entry.bd.plNr > 0)
                name += "\n" + formatPlayer(entry.bd);

            String groupMessage = gf.format(new Object[] {entry.gr.cp.cpName, entry.gr.grDesc, name, entry.stPos});
            
            final Properties props = new Properties();
            try {
                props.load(new FileInputStream(MainFrame.findPath("SMSServer.conf")));
            } catch (IOException ex) {

            }
            
            if (groupMessage != null)
                groupMessage = sendTime + "\n" + groupMessage;
        
            String sponsorLine = database.getSponsorLine();
            
            if (groupMessage != null && sponsorLine != null &&
                   !sponsorLine.isEmpty())
                groupMessage += "\n" + sponsorLine;
            
            // System.out.println(groupMessage);

            if ( (phone = entry.pl.phone) != null ) {
                database.sendMessage(prefix + phone, groupMessage, false);
            }

            if ( entry.bd != null && (phone = entry.bd.phone) != null ) {
                database.sendMessage(prefix + phone, groupMessage, false);
            }
            
            toDelete.add(entry.scpID);
        }
           
        for (int id : toDelete)
            database.removeUpdatePosition(id);
    }
    
    private String formatPlayer(Player pl) {
        if (pl.psLast == null)
            return "";
        
        return pl.psFirst.charAt(0) + ". " + pl.psLast;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jScrollPane1 = new javax.swing.JScrollPane();
        positionsTable = new javax.swing.JTable();

        setPreferredSize(new java.awt.Dimension(786, 415));

        positionsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Row", "ID", "stID", "Timestamp"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Object.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        positionsTable.setFillsViewportHeight(true);
        jScrollPane1.setViewportView(positionsTable);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 762, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 402, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JTable positionsTable;
    // End of variables declaration//GEN-END:variables

}
