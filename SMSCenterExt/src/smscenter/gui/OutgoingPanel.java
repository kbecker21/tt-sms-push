/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package smscenter.gui;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.TableCellRenderer;
import smscenter.database.Database;

/**
 *
 * @author chtheis
 */
public class OutgoingPanel extends BasePanel {

    private static final String[][] STATI = {
        {" ", "U", "Q", "P", "S", "D", "F", "A", "?"},
        {" ", "Unsent", "Queued", "Pending", "Sent", "Delivered", "Failed", "Aborted", "Status Requested"}
    };
    
    private static final java.util.Map<String, String> STATUS_MAP = new java.util.HashMap<>();
    private static final java.util.Map<String, String> STATUS_INVERSE_MAP = new java.util.HashMap<>();
    
    static {
        for (int i = 0; i < STATI[0].length; i++) {
            STATUS_MAP.put(STATI[0][i], STATI[1][i]);
            STATUS_INVERSE_MAP.put(STATI[1][i], STATI[0][i]);
        }
    }
    
    /**
     * Creates new form Outgoing
     */
    public OutgoingPanel(Database database) {
        super(database);
        initComponents();
        
        outgoingTable.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                int column = outgoingTable.columnAtPoint(evt.getPoint());
                if (column == 0)
                    return;
                if (column == Math.abs(sort))
                    sort = -sort;
                else
                    sort = column;
                
                outgoingTable.repaint();
                
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        update();
                    }
                });
            }
        });
        
        final TableCellRenderer tcr = outgoingTable.getTableHeader().getDefaultRenderer();
        outgoingTable.getTableHeader().setDefaultRenderer(new TableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                Component c = tcr.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                
                if (c instanceof JLabel) {
                    if (column == Math.abs(sort)) {
                        if (sort < 0)
                            ((JLabel) c).setText(((JLabel) c).getText() + " \u25bc");
                        else 
                            ((JLabel) c).setText(((JLabel) c).getText() + " \u25b2");
                    }
                }
                return c;
            }            
        });
    }
    
    
    @Override
    public void update() {
        synchronized (outgoingTable) {
            List<Object[]> list;

            String filterStatus = STATUS_INVERSE_MAP.get(filterStatusComboBox.getSelectedItem().toString()).trim();
            String filterPlayer = filterPlayerTextField.getText().trim();
            String filterPhone  = filterPhoneTextField.getText().trim();
            list = database.getOutgoingMessages(start, count, filterStatus, filterPlayer, filterPhone, sort) ;      
            for (Object[] row : list) {
                row[2] = STATUS_MAP.get(row[2].toString());
            }

            updateRows(list, outgoingTable);
            
            jLabelTotal.setText("Of " + database.getOutgoingCount(filterStatus, filterPlayer, filterPhone));
        }
        
        updateStatus();
    }
    
    
    private void updateStatus() {
        synchronized (outgoingTable) {
            smscenter.smsserver.SMSServer smsServer = ((MainFrame) SwingUtilities.getWindowAncestor(OutgoingPanel.this)).smsServer;

            if (smsServer == null)
                return;

            java.util.Map<String, smscenter.smsserver.gateways.AGateway> gwMap = new java.util.HashMap<>();
            for (smscenter.smsserver.gateways.AGateway gw : smsServer.getGwList())
                gwMap.put(gw.getGatewayId(), gw);
                
            java.util.List<Object[]> msgList = new java.util.ArrayList<>();
                
            java.util.List<Object[]> list = database.getOutgoingMessages(0, 0, "?", "", "", 0);
            
            for (Object[] row : list) {
                int id = ((Integer) row[1]);
                String ref = row[6].toString();
                String gw = row[7].toString();

                msgList.add(new Object[] {id, ref, gw});
                
                database.setMessageStatus(id, " ");
            }
            
            for (Object[] o : msgList) {
                if (gwMap.get(o[2].toString()) == null)
                    continue;

                org.smslib.StatusReportMessage.DeliveryStatuses status;
                try {
                    status = gwMap.get(o[2].toString()).getGateway().queryMessage(o[1].toString());
                } catch (Exception ex) {
                    Logger.getLogger(OutgoingPanel.class.getName()).log(Level.SEVERE, null, ex);
                    continue;
                }

                switch (status) {
                    case INPROGRESS :
                    case KEEPTRYING :
                        database.setMessageStatus((Integer) o[0], "P");
                        break;

                    case DELIVERED :
                        database.setMessageStatus((Integer) o[0], "D");
                        break;

                    case ABORTED :
                        database.setMessageStatus((Integer) o[0], "A");
                        break;

                    case UNKNOWN :
                        database.setMessageStatus((Integer) o[0], "F");
                        break;
                }
            }
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        detailPanel = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        receiverTextField = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        dateTextField = new javax.swing.JTextField();
        jLabel5 = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        textTextArea = new javax.swing.JTextArea();
        statusTextField = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        refTextField = new javax.swing.JTextField();
        msgLengthLabel = new javax.swing.JLabel();
        jScrollPane2 = new javax.swing.JScrollPane();
        jLabel1 = new javax.swing.JLabel();
        deleteButton = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        startSpinner = new javax.swing.JSpinner();
        jLabel7 = new javax.swing.JLabel();
        countSpinner = new javax.swing.JSpinner();
        jLabel9 = new javax.swing.JLabel();
        filterStatusComboBox = new javax.swing.JComboBox();
        jLabel10 = new javax.swing.JLabel();
        filterPlayerTextField = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        filterPhoneTextField = new javax.swing.JTextField();
        queryButton = new javax.swing.JButton();
        resendButton = new javax.swing.JButton();
        jLabel12 = new javax.swing.JLabel();
        jLabelTotal = new javax.swing.JLabel();

        detailPanel.addHierarchyListener(new java.awt.event.HierarchyListener() {
            public void hierarchyChanged(java.awt.event.HierarchyEvent evt) {
                detailPanelHierarchyChanged(evt);
            }
        });

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("smscenter/gui/resources/SMSCenter"); // NOI18N
        jLabel2.setText(bundle.getString("Status:")); // NOI18N

        jLabel3.setText(bundle.getString("Receiver:")); // NOI18N

        receiverTextField.setEditable(false);
        receiverTextField.setText("jTextField1");

        jLabel4.setText(bundle.getString("Date:")); // NOI18N

        dateTextField.setEditable(false);
        dateTextField.setText("jTextField2");

        jLabel5.setText(bundle.getString("Text:")); // NOI18N

        textTextArea.setEditable(false);
        textTextArea.setColumns(40);
        textTextArea.setRows(8);
        jScrollPane1.setViewportView(textTextArea);

        statusTextField.setEditable(false);
        statusTextField.setText("jTextField1");

        jLabel8.setText(bundle.getString("Ref:")); // NOI18N

        refTextField.setText("jTextField1");

        msgLengthLabel.setText("0");

        javax.swing.GroupLayout detailPanelLayout = new javax.swing.GroupLayout(detailPanel);
        detailPanel.setLayout(detailPanelLayout);
        detailPanelLayout.setHorizontalGroup(
            detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(detailPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addComponent(jLabel4)
                    .addComponent(jLabel3)
                    .addComponent(jLabel8)
                    .addComponent(jLabel5))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(msgLengthLabel, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(detailPanelLayout.createSequentialGroup()
                        .addGroup(detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(receiverTextField)
                            .addComponent(statusTextField))
                        .addContainerGap())
                    .addGroup(detailPanelLayout.createSequentialGroup()
                        .addGroup(detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(dateTextField, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(refTextField, javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING))
                        .addGap(12, 12, 12))))
        );
        detailPanelLayout.setVerticalGroup(
            detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(detailPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(statusTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(receiverTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(dateTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel8)
                    .addComponent(refTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(detailPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 185, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(msgLengthLabel)
                .addContainerGap())
        );

        setPreferredSize(new java.awt.Dimension(786, 404));

        outgoingTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Row", "ID", "Status", "Receiver", "Text", "Date", "Ref", "Gateway"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.String.class, java.lang.Object.class, java.lang.String.class, java.lang.String.class
            };
            boolean[] canEdit = new boolean [] {
                false, false, false, false, false, false, false, false
            };

            public Class getColumnClass(int columnIndex) {
                return types [columnIndex];
            }

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        outgoingTable.setFillsViewportHeight(true);
        outgoingTable.getTableHeader().setReorderingAllowed(false);
        outgoingTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                outgoingTableMouseClicked(evt);
            }
        });
        jScrollPane2.setViewportView(outgoingTable);
        if (outgoingTable.getColumnModel().getColumnCount() > 0) {
            outgoingTable.getColumnModel().getColumn(1).setHeaderValue(bundle.getString("ID")); // NOI18N
            outgoingTable.getColumnModel().getColumn(2).setHeaderValue(bundle.getString("Status")); // NOI18N
            outgoingTable.getColumnModel().getColumn(3).setHeaderValue(bundle.getString("Receiver")); // NOI18N
            outgoingTable.getColumnModel().getColumn(4).setHeaderValue(bundle.getString("Text")); // NOI18N
            outgoingTable.getColumnModel().getColumn(5).setHeaderValue(bundle.getString("Date")); // NOI18N
        }

        jLabel1.setText(bundle.getString("Selection:")); // NOI18N

        deleteButton.setText(bundle.getString("Delete")); // NOI18N
        deleteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteButtonActionPerformed(evt);
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

        jLabel9.setText(bundle.getString("Filter status:")); // NOI18N

        filterStatusComboBox.setModel(new javax.swing.DefaultComboBoxModel<String>(STATI[1])
        );
        filterStatusComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterStatusComboBoxActionPerformed(evt);
            }
        });

        jLabel10.setText(bundle.getString("Player:")); // NOI18N

        filterPlayerTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterPlayerTextFieldActionPerformed(evt);
            }
        });

        jLabel11.setText("Phone:");

        filterPhoneTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterPhoneTextFieldActionPerformed(evt);
            }
        });

        queryButton.setText("Status");
        queryButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                queryButtonActionPerformed(evt);
            }
        });

        resendButton.setText("Resend");
        resendButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                resendButtonActionPerformed(evt);
            }
        });

        jLabel12.setText("Status:");

        jLabelTotal.setText("Of: 0");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane2)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel9)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addGap(18, 18, 18)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(jLabel12)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(filterStatusComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 79, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(18, 18, 18)
                                        .addComponent(jLabel10)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(filterPlayerTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 50, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGap(18, 18, 18)
                                        .addComponent(jLabel11)
                                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                        .addComponent(filterPhoneTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 147, javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(layout.createSequentialGroup()
                                        .addComponent(deleteButton)
                                        .addGap(18, 18, 18)
                                        .addComponent(queryButton)
                                        .addGap(18, 18, 18)
                                        .addComponent(resendButton)))))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 16, Short.MAX_VALUE)
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(startSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabel7)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(countSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addGap(18, 18, 18)
                        .addComponent(jLabelTotal)))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {countSpinner, startSpinner});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 425, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(deleteButton)
                    .addComponent(queryButton)
                    .addComponent(resendButton))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel6)
                        .addComponent(jLabel7)
                        .addComponent(countSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(startSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabelTotal))
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel10)
                        .addComponent(filterPlayerTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(filterPhoneTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel11))
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel12)
                        .addComponent(filterStatusComboBox)
                        .addComponent(jLabel9)))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void deleteButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteButtonActionPerformed
        new Thread(new Runnable() {

            @Override
            public void run() {
                synchronized (outgoingTable) {
                    int[] rows = outgoingTable.getSelectedRows();

                    for (int i = rows.length; i-- > 0; ) {
                        int id = ((Integer) outgoingTable.getModel().getValueAt(rows[i], 1));
                        database.removeOutgoingMessage(id);
                    }
                }
                
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        update();
                    }
                    
                });
            }            
        }).start();        
    }//GEN-LAST:event_deleteButtonActionPerformed

    private void outgoingTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_outgoingTableMouseClicked
        if (evt.getClickCount() != 2)
            return;
        
        int row = outgoingTable.rowAtPoint(evt.getPoint());
        statusTextField.setText(outgoingTable.getModel().getValueAt(row, 2).toString());
        receiverTextField.setText(outgoingTable.getModel().getValueAt(row, 3).toString());
        dateTextField.setText(outgoingTable.getModel().getValueAt(row, 5).toString());
        if (outgoingTable.getModel().getValueAt(row, 6) != null)
            refTextField.setText(outgoingTable.getModel().getValueAt(row, 6).toString());
        else
            refTextField.setText("");
        textTextArea.setText(outgoingTable.getModel().getValueAt(row, 4).toString());
        msgLengthLabel.setText("" + textTextArea.getText().length() + " chars");
        
        // javax.swing.JScrollPane scrollPane = new javax.swing.JScrollPane(detailPanel);
        
        javax.swing.JOptionPane.showMessageDialog(this, detailPanel);
    }//GEN-LAST:event_outgoingTableMouseClicked

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

    private void filterStatusComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filterStatusComboBoxActionPerformed
        update();
    }//GEN-LAST:event_filterStatusComboBoxActionPerformed

    private void filterPlayerTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filterPlayerTextFieldActionPerformed
        update();
    }//GEN-LAST:event_filterPlayerTextFieldActionPerformed

    private void filterPhoneTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filterPhoneTextFieldActionPerformed
        update();
    }//GEN-LAST:event_filterPhoneTextFieldActionPerformed

    private void queryButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_queryButtonActionPerformed
        new Thread(new Runnable() {

            @Override
            public void run() {
                synchronized (outgoingTable) {
                    int[] rows = outgoingTable.getSelectedRows();

                    for (int i = rows.length; i-- > 0; ) {
                        if (outgoingTable.getModel().getValueAt(rows[i], 6).toString().isEmpty()) 
                            continue;
                        
                        int id = ((Integer) outgoingTable.getModel().getValueAt(rows[i], 1));
                        database.setMessageStatus(id, "?");
                    }
                }

                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        update();
                    }
                    
                });
            }            
        }).start();        
    }//GEN-LAST:event_queryButtonActionPerformed

    private void resendButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_resendButtonActionPerformed
        new Thread(new Runnable() {

            @Override
            public void run() {
                synchronized (outgoingTable) {
                    int[] rows = outgoingTable.getSelectedRows();

                    for (int i = rows.length; i-- > 0; ) {
                        int id = ((Integer) outgoingTable.getModel().getValueAt(rows[i], 1));
                        database.setMessageStatus(id, "U");
                    }
                }
                
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        update();
                    }
                    
                });
            }            
        }).start();                
    }//GEN-LAST:event_resendButtonActionPerformed

    private void detailPanelHierarchyChanged(java.awt.event.HierarchyEvent evt) {//GEN-FIRST:event_detailPanelHierarchyChanged
        java.awt.Window window = SwingUtilities.getWindowAncestor(detailPanel);
            if (window instanceof java.awt.Dialog) {
                java.awt.Dialog dialog = (java.awt.Dialog) window;
                if (!dialog.isResizable()) {
                        dialog.setResizable(true);
                }
            }        
    }//GEN-LAST:event_detailPanelHierarchyChanged

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JSpinner countSpinner;
    private javax.swing.JTextField dateTextField;
    private javax.swing.JButton deleteButton;
    private javax.swing.JPanel detailPanel;
    private javax.swing.JTextField filterPhoneTextField;
    private javax.swing.JTextField filterPlayerTextField;
    private javax.swing.JComboBox filterStatusComboBox;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JLabel jLabelTotal;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JLabel msgLengthLabel;
    private final javax.swing.JTable outgoingTable = new javax.swing.JTable();
    private javax.swing.JButton queryButton;
    private javax.swing.JTextField receiverTextField;
    private javax.swing.JTextField refTextField;
    private javax.swing.JButton resendButton;
    private javax.swing.JSpinner startSpinner;
    private javax.swing.JTextField statusTextField;
    private javax.swing.JTextArea textTextArea;
    // End of variables declaration//GEN-END:variables

    private int start = 0;
    private int count = 50;
    private int sort = -1;
}
