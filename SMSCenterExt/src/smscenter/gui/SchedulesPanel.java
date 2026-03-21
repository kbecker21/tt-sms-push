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
public class SchedulesPanel extends BasePanel {
    /**
     * Creates new form ResultsPanel
     */
    public SchedulesPanel(Database database) {
        super(database);
        initComponents();
        myInitComponents();
    }
    
    private void myInitComponents() {
        schedulesTable.setDefaultRenderer(Boolean.class, new javax.swing.table.DefaultTableCellRenderer() {
            @Override
            public java.awt.Component getTableCellRendererComponent(
                    javax.swing.JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                String s = ((Boolean) value) ? "\u2713" : "\u2717";
                return super.getTableCellRendererComponent(table, s, isSelected, hasFocus, row, column);
            }
        });        
    }
    
    @Override
    public void update() {
        String filterPlayer = filterPlayerTextField.getText().trim();
        if (filterPlayer.isEmpty())
            filterPlayer = null;
        
        String filterStatus = "";
        if ("No".equals(rescheduledComboBox.getModel().getSelectedItem().toString()))
            filterStatus += "c";
        if ("Yes".equals(rescheduledComboBox.getModel().getSelectedItem().toString()))
            filterStatus += "C";

        if ("No".equals(reminderComboBox.getModel().getSelectedItem().toString()))
            filterStatus += "r";
        if ("Yes".equals(reminderComboBox.getModel().getSelectedItem().toString()))
            filterStatus += "R";
        
        if (filterStatus.isEmpty())
            filterStatus = null;
        
        String filterPhone = filterPhoneTextField.getText().trim();
        if (filterPhone.isEmpty())
            filterPhone = null;

        Match[] schedules = database.getUpdateSchedules(start, count, filterStatus, filterPhone, filterPlayer);
        
        if (schedules == null)
            return;
        
        List<Object[]> list = new java.util.ArrayList<>();
        Set<Integer> plSet = new java.util.HashSet<>();
                
        // Liste generieren und Test ob die Gruppen "combined" sind
        int row = start;
        for (Match mt : schedules) {
            if (!mt.scgEnabled)
                continue;
            
            String nameA = "", nameX = "";
            if (mt.plA != null && mt.plA.plNr > 0)
                nameA += formatPlayer(mt.plA);
            if (mt.plB != null && mt.plB.plNr > 0)
                nameA += " / " + formatPlayer(mt.plB);
            if (mt.plX != null && mt.plX.plNr > 0)
                nameX += formatPlayer(mt.plX);
            if (mt.plY != null && mt.plY.plNr > 0)
                nameX += formatPlayer(mt.plY);
            
            list.add(new Object[] {++row, mt.scsID, mt.mtNr, mt.mtDateTime, mt.mtTable, nameA, nameX, mt.ts, mt.scsRescheduled, mt.scsReminder});
        }
        
        updateRows(list, schedulesTable);
        
        if ( isSMSServerRunning() )
            sendMessages();
    }
    
    
    public void update(Set<Integer> plSet) {
        Match[] schedules = database.getSchedulesFor(plSet, MainFrame.updateDelay, 15 * 60);
        sendMessages(schedules, plSet);
    }
    
    synchronized public void sendMessages() {
        Match[] schedules = database.getUpdateSchedules(MainFrame.updateDelay);
        
        if (schedules == null)
            return;
        
        List<Object[]> list = new java.util.ArrayList<>();
        Set<Integer> plSet = new java.util.HashSet<>();
                
        // Liste der Spieler generieren
        for (Match mt : schedules) {
            if (mt.plA != null)
                plSet.add(mt.plA.plNr);
            if (mt.plB != null)
                plSet.add(mt.plB.plNr);
            if (mt.plX != null)
                plSet.add(mt.plX.plNr);
            if (mt.plY != null)
                plSet.add(mt.plY.plNr);
        }
        
        sendMessages(schedules, plSet);
    }
    
    
    synchronized public void sendMessages(Match[] schedules, Set<Integer> plSet) {        
        Map<Integer, Boolean> grCombinedList = new java.util.HashMap<>();
        Map<Integer, Boolean> grStartedList = new java.util.HashMap<>();
        
        Map<Integer, Match> mtList = new java.util.HashMap<>();
                
        // Liste der Gruppen generieren und Test ob die Gruppen "combined" sind
        for (Match mt : schedules) {
            if (grCombinedList.get(mt.gr.grID) == null)
                grCombinedList.put(mt.gr.grID, mt.gr.grModus == 1 && database.isGroupCombined(mt.gr.grID));
            
            if (grStartedList.get(mt.gr.grID) == null)
                grStartedList.put(mt.gr.grID, mt.gr.grModus == 1 && database.hasGropStarted(mt.gr.grID));
            
            if (mt.scsID != 0)
                mtList.put(mt.scsID, mt);
        }
        
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("smscenter/resources/SMSCenter"); // NOI18N
        String phone;               
        
        // Zunaechst alle Gruppen, die nicht combined sind
        MessageFormat mf = new MessageFormat(bundle.getString("matchSchedule"), Locale.ENGLISH);
        MessageFormat mc = new MessageFormat(bundle.getString("matchScheduleChange"), Locale.ENGLISH);
        MessageFormat mr = new MessageFormat(bundle.getString("matchScheduleReminder"), Locale.ENGLISH);
        MessageFormat mu = new MessageFormat(bundle.getString("matchScheduleUnknown"), Locale.ENGLISH);
        MessageFormat gf = new MessageFormat(bundle.getString("groupSchedule"), Locale.ENGLISH);
        MessageFormat gc = new MessageFormat(bundle.getString("groupScheduleChange"), Locale.ENGLISH);
        MessageFormat gr = new MessageFormat(bundle.getString("groupScheduleReminder"), Locale.ENGLISH);
        
        Set<Integer> stIdSet = new java.util.HashSet<>();
        Set<Integer> toDelete = new java.util.HashSet<>();
        Set<Integer> toUpdate = new java.util.HashSet<>();
        
        java.util.Date today = new java.util.Date(System.currentTimeMillis());

        final Properties props = new Properties();
        try {
            props.load(new FileInputStream(MainFrame.findPath("SMSServer.conf")));
        } catch (IOException ex) {

        }
        
        final int reminderTime = Settings.readGeneralSettings(props).getReminderTime() * 60 * 1000;
        final int reminderTimeNextDay = Settings.readGeneralSettings(props).getReminderTimeNextDay() * 60 * 1000;
        final int reminderCuttoff = Settings.readGeneralSettings(props).getReminderCutoff() * 60 * 1000;
        
        final String sponsorLine = database.getSponsorLine();
        
        final SimpleDateFormat sdf = new SimpleDateFormat("d MMM HH:mm:ss", Locale.ENGLISH);
        final String sendTime = sdf.format(new Date());
        
        for (Match mt : schedules) {
            if (mt.scsID != 0)
                toDelete.add(mt.scsID);
            
            if (mt.scgID == 0)
                continue;
            
            if (!mt.scgEnabled)
                continue;
                            
            // Kein Update fuer Spiele, die bereits begonnen haben
            if (mt.mtResA > 0 || mt.mtResX > 0)
                continue;
            
            // Nicht, wenn es keine Zeit gibt
            if (mt.mtDateTime == null)
                continue;
            
            // If the match is the previous day, don't send SMS again,
            // we might just edit the matches
            if (compareDays(mt.mtDateTime, today) < 0)
                continue;
            
            // Keine Freilose
            if (mt.stA > 0 && mt.tmA == 0 || mt.stX > 0 && mt.tmX == 0)
                continue;
            
            // Weiter, wenn kein Spieler bekannt ist
            if ( (mt.plA == null || mt.plA.plNr <= 0) && (mt.plX == null || mt.plX.plNr <= 0) )
                continue;
            
            // Nur ein Spieler bekannt oder noch kein Tisch: 
            // SMS senden, wenn Spiel innerhalb 30 Minuten beginnt 
            // oder am naechsten Tag ist. Ansonsten erstmal stehen lassen
            if ( mt.plA == null || mt.plA.plNr <= 0 || mt.plX == null || mt.plX.plNr <= 0 || 
                 (mt.mtDateTime.getHours() == 0 && mt.mtDateTime.getMinutes() == 0) || mt.mtTable == 0 ) {
                if ( mt.mtDateTime.getTime() > System.currentTimeMillis() + 2 * reminderCuttoff &&
                     compareDays(mt.mtDateTime, today) == 0 ) {
                    toDelete.remove(mt.scsID);
                    continue;
                }   
            }
            
            if (mt.scsID != 0) {
                toDelete.remove(mt.scsID);
                toUpdate.add(mt.scsID);
            }
            
            String nameA = formatPlayer(mt.plA);
            String nameX = formatPlayer(mt.plX);
            if (mt.plB != null && mt.plB.plNr > 0 && !nameA.isEmpty())
                nameA += "\n" + formatPlayer(mt.plB);
            if (mt.plY != null && mt.plY.plNr > 0 && !nameX.isEmpty())
                nameX += "\n" + formatPlayer(mt.plY);
            
            String messageA = null, messageX = null;
            if (grStartedList.get(mt.gr.grID)) {
                continue;
            } else if (mt.stX == 0) {
                messageA =  mu.format( new Object[] {mt.gr.cp.cpName, mt.gr.grDesc, nameA, null, mt.mtDateTime, mt.mtTable} );
            } else if (mt.stA == 0) {
                 messageX = mu.format( new Object[] {mt.gr.cp.cpName, mt.gr.grDesc, nameX, null, mt.mtDateTime, mt.mtTable} );
            } else if (grCombinedList.get(mt.gr.grID)) {
                if (mt.scsRescheduled) {
                    messageA = gc.format( new Object[] {mt.gr.cp.cpName, mt.gr.grDesc, nameA, null, mt.mtDateTime, mt.mtTable} );
                    messageX = gc.format( new Object[] {mt.gr.cp.cpName, mt.gr.grDesc, nameX, null, mt.mtDateTime, mt.mtTable} );                                       
                } else if (mt.scsReminder) {
                    messageA = gr.format( new Object[] {mt.gr.cp.cpName, mt.gr.grDesc, nameA, null, mt.mtDateTime, mt.mtTable} );
                    messageX = gr.format( new Object[] {mt.gr.cp.cpName, mt.gr.grDesc, nameX, null, mt.mtDateTime, mt.mtTable} );                                       
                } else {
                    messageA = gf.format( new Object[] {mt.gr.cp.cpName, mt.gr.grDesc, nameA, null, mt.mtDateTime, mt.mtTable} );
                    messageX = gf.format( new Object[] {mt.gr.cp.cpName, mt.gr.grDesc, nameX, null, mt.mtDateTime, mt.mtTable} );                   
                }
            } else if (mt.scsRescheduled) {
                messageA = messageX = mc.format( new Object[] {mt.gr.cp.cpName, mt.gr.grDesc, nameA, nameX, mt.mtDateTime, mt.mtTable} );
            } else if (mt.scsReminder) {
                messageA = messageX = mr.format( new Object[] {mt.gr.cp.cpName, mt.gr.grDesc, nameA, nameX, mt.mtDateTime, mt.mtTable} );                
            } else {
                messageA = messageX = mf.format( new Object[] {mt.gr.cp.cpName, mt.gr.grDesc, nameA, nameX, mt.mtDateTime, mt.mtTable} );
            }
            
            if (messageA != null)
                messageA = sendTime + "\n" + messageA;
            if (messageX != null)
                messageX = sendTime + "\n" + messageX;
            
            if (messageA != null && sponsorLine != null && !sponsorLine.isEmpty())
                messageA += "\n" + sponsorLine;
            if (messageX != null && sponsorLine != null && !sponsorLine.isEmpty())
                messageX += "\n" + sponsorLine;
            
            // System.out.println(message);
            
            if (messageA != null && mt.stA > 0 && !stIdSet.contains(mt.stA)) {
                if ( mt.plA != null && (phone = mt.plA.phone) != null && plSet.contains(mt.plA.plNr) ) {
                    database.sendMessage(prefix + phone, messageA, true);
                }                
            
                if ( mt.plB != null && (phone = mt.plB.phone) != null && plSet.contains(mt.plB.plNr) ) {
                    database.sendMessage(prefix + phone, messageA, true);
                }
                
                stIdSet.add(mt.stA);
            }
            
            if (messageX != null && mt.stX > 0 && !stIdSet.contains(mt.stX)) {
                if ( mt.plX != null && (phone = mt.plX.phone) != null && plSet.contains(mt.plX.plNr) ) {
                    database.sendMessage(prefix + phone, messageX, true);
                }

                if ( mt.plY != null && (phone = mt.plY.phone) != null && plSet.contains(mt.plY.plNr)  ) {
                    database.sendMessage(prefix + phone, messageX, true);
                }
                
                stIdSet.add(mt.stX);
            }
        }
        
        for (int scsID : toUpdate) {            
            java.util.Date mtDateTime = mtList.get(scsID).mtDateTime;
            
            // Wenn es zu lange dauert bis zum naechsten Spiel, 15 Minuten vorher eine Erinnerung senden
            if (compareDays(mtDateTime, today) > 0) {
                // Spiel am naechsten Tag
                database.updateScheduleTimestamp(scsID, mtDateTime.getTime() - reminderTimeNextDay);
            } else if (mtDateTime.getTime() >=  today.getTime() + reminderCuttoff) {
                // Spiel nicht in der uebernaechsten Runde
                database.updateScheduleTimestamp(scsID, mtDateTime.getTime() - reminderTime);
            } else {
                toDelete.add(scsID);
            }
        }
        
        for (int scsID : toDelete) {
            // Man kann es loeschen
            database.removeUpdateSchedule(scsID);
        }
    }
    
    private String formatPlayer(Player pl) {
        if (pl == null || pl.plNr <= 0 || pl.psLast == null)
            return "";
        
        return pl.psFirst.charAt(0) + ". " + pl.psLast;
    }
    
    
    // Test ob ein Datum gestern oder früher ist bzw. morgen oder spaeter
    private int compareDays(java.util.Date a, java.util.Date b) {
        if (a.getTime() < b.getTime()) {
            if (a.getYear() < b.getYear() || a.getMonth() < b.getMonth() || a.getDate() < b.getDate())
                return -1;
        }
        
        if (a.getTime() > b.getTime()) {
            if (a.getYear() > b.getYear() || a.getMonth() > b.getMonth() || a.getDate() > b.getDate())
                return +1;
        }
        
        return 0;
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
        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        deleteButton = new javax.swing.JButton();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        rescheduledComboBox = new javax.swing.JComboBox<>();
        jLabel4 = new javax.swing.JLabel();
        reminderComboBox = new javax.swing.JComboBox<>();
        jLabel9 = new javax.swing.JLabel();
        filterPlayerTextField = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        filterPhoneTextField = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        startSpinner = new javax.swing.JSpinner();
        jLabel7 = new javax.swing.JLabel();
        countSpinner = new javax.swing.JSpinner();
        jLabelTotal = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();

        schedulesTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Row", "ID", "Match No", "Date", "Table", "Player(s) A", "Player(s) X", "Timestamp", "Rescheduled", "Reminder"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Object.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.Object.class, java.lang.Boolean.class, java.lang.Boolean.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        schedulesTable.setFillsViewportHeight(true);
        schedulesTable.getTableHeader().setReorderingAllowed(false);
        jScrollPane1.setViewportView(schedulesTable);
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("smscenter/gui/resources/SMSCenter"); // NOI18N
        if (schedulesTable.getColumnModel().getColumnCount() > 0) {
            schedulesTable.getColumnModel().getColumn(1).setHeaderValue(bundle.getString("ID")); // NOI18N
            schedulesTable.getColumnModel().getColumn(2).setHeaderValue(bundle.getString("Match No")); // NOI18N
            schedulesTable.getColumnModel().getColumn(7).setHeaderValue(bundle.getString("Timestamp")); // NOI18N
        }

        jLabel1.setText("Selection:");

        deleteButton.setText("Delete");

        jLabel2.setText("Filter:");

        jLabel3.setText("Rescheduled:");

        rescheduledComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { " ", "No", "Yes" }));

        jLabel4.setText("Reminder:");

        reminderComboBox.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "", "Yes", "No" }));

        jLabel9.setText("Player:");

        filterPlayerTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterPlayerTextFieldActionPerformed(evt);
            }
        });

        jLabel10.setText("Phone:");

        filterPhoneTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterPhoneTextFieldActionPerformed(evt);
            }
        });

        jLabel6.setText(bundle.getString("Start:")); // NOI18N

        startSpinner.setModel(new javax.swing.SpinnerNumberModel(0, 0, null, 50));
        startSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                startSpinnerStateChanged(evt);
            }
        });

        jLabel7.setText(bundle.getString("Count:")); // NOI18N

        countSpinner.setModel(new javax.swing.SpinnerNumberModel(50, 10, null, 10));
        countSpinner.addChangeListener(new javax.swing.event.ChangeListener() {
            public void stateChanged(javax.swing.event.ChangeEvent evt) {
                countSpinnerStateChanged(evt);
            }
        });

        jLabelTotal.setText("Of: 0");

        jLabel5.setText("Rows:");

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2)
                    .addComponent(jLabel5))
                .addGap(18, 18, 18)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(deleteButton)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(rescheduledComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel6)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(startSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(18, 18, 18)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel7)
                                .addGap(18, 18, 18)
                                .addComponent(countSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(jLabelTotal))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addComponent(jLabel4)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(reminderComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel9)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(filterPlayerTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel10)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(filterPhoneTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 204, javax.swing.GroupLayout.PREFERRED_SIZE)))))
                .addContainerGap(225, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(deleteButton))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3)
                    .addComponent(rescheduledComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel4)
                    .addComponent(reminderComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel9)
                    .addComponent(filterPlayerTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel10)
                    .addComponent(filterPhoneTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(startSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel6)
                    .addComponent(jLabel7)
                    .addComponent(countSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabelTotal)
                    .addComponent(jLabel5))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 945, Short.MAX_VALUE)
                    .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 351, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void filterPlayerTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filterPlayerTextFieldActionPerformed
        update();
    }//GEN-LAST:event_filterPlayerTextFieldActionPerformed

    private void filterPhoneTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filterPhoneTextFieldActionPerformed
        update();
    }//GEN-LAST:event_filterPhoneTextFieldActionPerformed

    private void startSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_startSpinnerStateChanged
        start = Integer.parseInt(startSpinner.getValue().toString());
        update();
    }//GEN-LAST:event_startSpinnerStateChanged

    private void countSpinnerStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_countSpinnerStateChanged
        count = Integer.parseInt(countSpinner.getValue().toString());
        start = (start / count) * count;
        ((javax.swing.SpinnerNumberModel) startSpinner.getModel()).setStepSize(count);
        update();
    }//GEN-LAST:event_countSpinnerStateChanged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JSpinner countSpinner;
    private javax.swing.JButton deleteButton;
    private javax.swing.JTextField filterPhoneTextField;
    private javax.swing.JTextField filterPlayerTextField;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel jLabelTotal;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JComboBox<String> reminderComboBox;
    private javax.swing.JComboBox<String> rescheduledComboBox;
    private final javax.swing.JTable schedulesTable = new javax.swing.JTable();
    private javax.swing.JSpinner startSpinner;
    // End of variables declaration//GEN-END:variables

    private int start = 0;
    private int count = 50;
    private int sort = -1;
}
