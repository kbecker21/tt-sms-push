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
import smscenter.database.Match;
import smscenter.database.Player;
import smscenter.gui.settings.Settings;

/**
 *
 * @author chtheis
 */
public class ResultsPanel extends BasePanel {

    /**
     * Creates new form ResultsPanel
     */
    public ResultsPanel(Database database) {
        super(database);
        initComponents();
    }
    
    
    @Override
    public void update() {
        Match[] results = database.getUpdateResults(MainFrame.updateDelay);
        List<Object[]> list = new java.util.ArrayList<>();
        Set<Integer> plSet = new java.util.HashSet<>();
        
        // Liste generieren und Test, ob die Gruppe combined bzw. fertig ist.
        int row = 0;
        for (Match mt : results) {
            if (!mt.scgEnabled)
                continue;
            
            list.add(new Object[] {++row, mt.scrID, mt.mtNr, mt.ts});
            
            if (mt.plA != null)
                plSet.add(mt.plA.plNr);
            if (mt.plB != null)
                plSet.add(mt.plB.plNr);
            if (mt.plX != null)
                plSet.add(mt.plX.plNr);
            if (mt.plY != null)
                plSet.add(mt.plY.plNr);
        }
        
        updateRows(list, resultsTable);
        
        if ( isSMSServerRunning() )
            sendMessages(results, plSet);
    }
    
    
    public void sendMessages(Match[] results, Set<Integer> plSet) {        
        Map<Integer, Boolean> grCombinedMap = new java.util.HashMap<>();
        
        // Liste generieren und Test, ob die Gruppe combined bzw. fertig ist.
        for (Match mt : results) {
            if (grCombinedMap.get(mt.gr.grID) == null)
                grCombinedMap.put(mt.gr.grID, mt.gr.grModus == 1 && database.isGroupCombined(mt.gr.grID));
        }
        
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("smscenter/resources/SMSCenter"); // NOI18N
        String phone;
        MessageFormat mf = new MessageFormat(bundle.getString("matchResult"), Locale.ENGLISH);
        
        final SimpleDateFormat sdf = new SimpleDateFormat("d MMM HH:mm:ss", Locale.ENGLISH);
        final String sendTime = sdf.format(new Date());
        
        for (Match mt : results) {
            if (mt.scgID == 0)
                continue;
            
            if (!mt.scgEnabled)
                continue;
            
            if (mt.plA == null || mt.plX == null)
                continue;
            
            if (mt.plA.plNr <= 0 || mt.plX.plNr <= 0)
                continue;
            
            if (2 * mt.mtResA < mt.mtBestOf && 2 * mt.mtResX < mt.mtBestOf) {
                if ( !(mt.mtWalkOverA || mt.mtInjuredA || mt.mtDisqualifiedA) && 
                     !(mt.mtWalkOverX || mt.mtInjuredX || mt.mtDisqualifiedX) )
                    continue;
            }
            
            // Keine Einzelergebnisse fuer combined group
            if (grCombinedMap.get(mt.gr.grID))
                continue;
            
            String nameA = formatPlayer(mt.plA);
            String nameX = formatPlayer(mt.plX);
            if (mt.plB != null && mt.plB.plNr > 0)
                nameA += "\n" + formatPlayer(mt.plB);
            if (mt.plY != null && mt.plY.plNr > 0)
                nameX += "\n" + formatPlayer(mt.plY);
            
            String wo = "";
            if (mt.mtWalkOverA || mt.mtWalkOverX)
                wo = "(w/o)";
            else if (mt.mtDisqualifiedA || mt.mtDisqualifiedX)
                wo = "(disqualified)";
            else if (mt.mtInjuredA || mt.mtInjuredX)
                wo = "(injured)";
            
            String resultMessage = mf.format(new Object[] {mt.gr.cp.cpName, mt.gr.grDesc, nameA, nameX, mt.mtResA, mt.mtResX, wo} );  
            
            final Properties props = new Properties();
            try {
                props.load(new FileInputStream(MainFrame.findPath("SMSServer.conf")));
            } catch (IOException ex) {

            }
            
            if (resultMessage != null)
                resultMessage = sendTime + "\n" + resultMessage;
        
            String sponsorLine = database.getSponsorLine();
            if (resultMessage != null && sponsorLine != null && !sponsorLine.isEmpty())
                resultMessage += "\n" + sponsorLine;
            
            // System.out.println(resultMessage);
            
            if ( (phone = mt.plA.phone) != null && plSet.contains(mt.plA.plNr) ) {
                database.sendMessage(prefix + phone, resultMessage, false);
            }
            
            if ( mt.plB != null && (phone = mt.plB.phone) != null && plSet.contains(mt.plB.plNr) ) {
                database.sendMessage(prefix + phone, resultMessage, false);
            }
            
            if ( (phone = mt.plX.phone) != null && plSet.contains(mt.plX.plNr) ) {
                database.sendMessage(prefix + phone, resultMessage, false);
            }
            
            if ( mt.plY != null && (phone = mt.plY.phone) != null && plSet.contains(mt.plY.plNr) ) {
                database.sendMessage(prefix + phone, resultMessage, false);
            }
        }
        
        for (Match mt : results) {
            database.removeUpdateResult(mt.scrID);
        }
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

        resultsTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Row", "ID", "Match No", "Timestamp"
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
        resultsTable.setFillsViewportHeight(true);
        resultsTable.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(resultsTable);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("smscenter/gui/resources/SMSCenter"); // NOI18N
        resultsTable.getColumnModel().getColumn(1).setHeaderValue(bundle.getString("ID")); // NOI18N
        resultsTable.getColumnModel().getColumn(2).setHeaderValue(bundle.getString("Match No")); // NOI18N
        resultsTable.getColumnModel().getColumn(3).setHeaderValue(bundle.getString("Timestamp")); // NOI18N

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 766, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 393, Short.MAX_VALUE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JScrollPane jScrollPane1;
    private final javax.swing.JTable resultsTable = new javax.swing.JTable();
    // End of variables declaration//GEN-END:variables
}
