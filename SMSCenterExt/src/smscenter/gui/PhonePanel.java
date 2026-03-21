/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package smscenter.gui;

import java.awt.Component;
import java.awt.KeyboardFocusManager;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.TableCellRenderer;
import smscenter.database.Database;
import smscenter.database.Player;

/**
 *
 * @author chtheis
 */
public class PhonePanel extends BasePanel {

    /**
     * Creates new form Phone
     */
    public PhonePanel(Database database) {
        super(database);
        initComponents();
        
        final Properties props = new Properties();
        try {
            props.load(new FileInputStream(MainFrame.findPath("SMSServer.conf")));
        } catch (IOException ex) {

        }
        
        final String welcomeMsg = database.getWelcomeMsg();
        sendWelcomeButton.setEnabled(welcomeMsg != null && !welcomeMsg.isEmpty());
        
        phoneTable.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent evt) {
                int column = phoneTable.columnAtPoint(evt.getPoint());
                if (column == 0)
                    return;
                if (column == Math.abs(sort))
                    sort = -sort;
                else
                    sort = column;
                
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        update();
                    }
                });
            }
        });
        
        final TableCellRenderer tcr = phoneTable.getTableHeader().getDefaultRenderer();
        phoneTable.getTableHeader().setDefaultRenderer(new TableCellRenderer() {
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

        phoneNrTextArea.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (e.getLength() > 0)
                    statusComboBox.setSelectedItem("U");
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (e.getLength() > 0)
                    statusComboBox.setSelectedItem("U");
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                if (e.getLength() > 0)
                    statusComboBox.setSelectedItem("U");
            }
        
        });
    }
    
    
    private long ts = 0;
    
    @Override
    public void update() {
        final Properties props = new Properties();
        try {
            props.load(new FileInputStream(MainFrame.findPath("SMSServer.conf")));
        } catch (IOException ex) {

        }
        
        // Temp: Update info in ini file
        ts = database.updateConfiguration(props, ts);
        
        final String welcomeMsg = database.getWelcomeMsg();
        sendWelcomeButton.setEnabled(welcomeMsg != null && !welcomeMsg.isEmpty());
        
        synchronized (phoneTable) {
            List<Object[]> list;

            list = database.getNewIncomingMessages();

            for (Object[] s : list) {
                try {
                    int startNr = Integer.parseInt(s[2].toString().trim());
                    if (!database.addPhoneNumber(startNr, s[1].toString(), "U"))
                        continue;

                    database.markIncomingMessageRead(((Integer) s[0]));
                } catch (NumberFormatException e) {
                    database.markIncomingMessageRead(((Integer) s[0]));
                } catch (Exception e) {

                }
            }

            String filterStatus = filterComboBox.getSelectedItem().toString();
            String filterPhone = filterPhoneTextField.getText().trim();
            String filterPlayer = filterStartNrTextField.getText().trim();
            list = database.getPhoneNumbers(start, count, filterStatus, filterPhone, filterPlayer, sort);

            updateRows(list, phoneTable);
            
            jLabelTotal.setText("Of " + database.getPhoneCount(filterStatus, filterPhone, filterPlayer));
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

        phoneNumberPanel = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        startNrTextField = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        statusComboBox = new javax.swing.JComboBox();
        jLabel5 = new javax.swing.JLabel();
        playerTextField = new javax.swing.JTextField();
        jScrollPane2 = new javax.swing.JScrollPane();
        phoneNrTextArea = new javax.swing.JTextArea();
        phoneNrTextArea.setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, null);
        phoneNrTextArea.setFocusTraversalKeys(KeyboardFocusManager.BACKWARD_TRAVERSAL_KEYS, null);
        jScrollPane1 = new javax.swing.JScrollPane();
        jLabel1 = new javax.swing.JLabel();
        deleteButton = new javax.swing.JButton();
        addButton = new javax.swing.JButton();
        importButton = new javax.swing.JButton();
        jLabel6 = new javax.swing.JLabel();
        startSpinner = new javax.swing.JSpinner();
        jLabel7 = new javax.swing.JLabel();
        countSpinner = new javax.swing.JSpinner();
        markButton = new javax.swing.JButton();
        sendScheduleButton = new javax.swing.JButton();
        filterComboBox = new javax.swing.JComboBox();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        filterStartNrTextField = new javax.swing.JTextField();
        jLabel10 = new javax.swing.JLabel();
        filterPhoneTextField = new javax.swing.JTextField();
        sendWelcomeButton = new javax.swing.JButton();
        jLabel11 = new javax.swing.JLabel();
        jLabelTotal = new javax.swing.JLabel();

        phoneNumberPanel.addAncestorListener(new javax.swing.event.AncestorListener() {
            public void ancestorAdded(javax.swing.event.AncestorEvent evt) {
                phoneNumberPanelAncestorAdded(evt);
            }
            public void ancestorMoved(javax.swing.event.AncestorEvent evt) {
            }
            public void ancestorRemoved(javax.swing.event.AncestorEvent evt) {
            }
        });

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("smscenter/gui/resources/SMSCenter"); // NOI18N
        jLabel2.setText(bundle.getString("Start No.:")); // NOI18N

        startNrTextField.addFocusListener(new java.awt.event.FocusAdapter() {
            public void focusLost(java.awt.event.FocusEvent evt) {
                startNrTextFieldFocusLost(evt);
            }
        });

        jLabel3.setText(bundle.getString("Phone No.:")); // NOI18N

        jLabel4.setText(bundle.getString("Status:")); // NOI18N

        statusComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "U", "X" }));

        jLabel5.setText("Player:");

        playerTextField.setEditable(false);
        playerTextField.setDisabledTextColor(new java.awt.Color(50, 50, 50));
        playerTextField.setEnabled(false);
        playerTextField.setFocusable(false);

        phoneNrTextArea.setColumns(20);
        phoneNrTextArea.setRows(5);
        jScrollPane2.setViewportView(phoneNrTextArea);

        javax.swing.GroupLayout phoneNumberPanelLayout = new javax.swing.GroupLayout(phoneNumberPanel);
        phoneNumberPanel.setLayout(phoneNumberPanelLayout);
        phoneNumberPanelLayout.setHorizontalGroup(
            phoneNumberPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(phoneNumberPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(phoneNumberPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3)
                    .addComponent(jLabel4)
                    .addComponent(jLabel5))
                .addGap(18, 18, 18)
                .addGroup(phoneNumberPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(startNrTextField)
                    .addComponent(playerTextField, javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(statusComboBox, 0, 375, Short.MAX_VALUE)
                    .addComponent(jScrollPane2))
                .addContainerGap())
        );
        phoneNumberPanelLayout.setVerticalGroup(
            phoneNumberPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(phoneNumberPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(phoneNumberPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(startNrTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(phoneNumberPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel5)
                    .addComponent(playerTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(18, 18, 18)
                .addGroup(phoneNumberPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel3)
                    .addComponent(jScrollPane2))
                .addGap(18, 18, 18)
                .addGroup(phoneNumberPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel4)
                    .addComponent(statusComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        phoneTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "Row", "ID", "Start No", "Phone", "Status", "Success", "Failed", "Created"
            }
        ) {
            Class[] types = new Class [] {
                java.lang.Integer.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.String.class, java.lang.String.class, java.lang.Integer.class, java.lang.Integer.class, java.lang.Object.class
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
        phoneTable.setFillsViewportHeight(true);
        phoneTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                phoneTableMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(phoneTable);
        if (phoneTable.getColumnModel().getColumnCount() > 0) {
            phoneTable.getColumnModel().getColumn(1).setHeaderValue(bundle.getString("ID")); // NOI18N
            phoneTable.getColumnModel().getColumn(2).setHeaderValue(bundle.getString("Start No")); // NOI18N
            phoneTable.getColumnModel().getColumn(3).setHeaderValue(bundle.getString("Phone")); // NOI18N
        }

        jLabel1.setText(bundle.getString("Selection:")); // NOI18N

        deleteButton.setText(bundle.getString("Delete")); // NOI18N
        deleteButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deleteButtonActionPerformed(evt);
            }
        });

        addButton.setText(bundle.getString("Add")); // NOI18N
        addButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addButtonActionPerformed(evt);
            }
        });

        importButton.setText(bundle.getString("Import")); // NOI18N
        importButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                importButtonActionPerformed(evt);
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

        markButton.setText(bundle.getString("Mark")); // NOI18N
        markButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                markButtonActionPerformed(evt);
            }
        });

        sendScheduleButton.setText(bundle.getString("Schedules")); // NOI18N
        sendScheduleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendScheduleButtonActionPerformed(evt);
            }
        });

        filterComboBox.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "", "U", "X" }));
        filterComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterComboBoxActionPerformed(evt);
            }
        });

        jLabel8.setText("Status:");

        jLabel9.setText("Player:");

        filterStartNrTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterStartNrTextFieldActionPerformed(evt);
            }
        });

        jLabel10.setText("Phone:");

        filterPhoneTextField.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                filterPhoneTextFieldActionPerformed(evt);
            }
        });

        sendWelcomeButton.setText("Welcome");
        sendWelcomeButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sendWelcomeButtonActionPerformed(evt);
            }
        });

        jLabel11.setText("Filter:");

        jLabelTotal.setText("Of: 0");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 921, Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel1)
                            .addComponent(jLabel11))
                        .addGap(18, 18, 18)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(deleteButton)
                                .addGap(18, 18, 18)
                                .addComponent(markButton)
                                .addGap(18, 18, 18)
                                .addComponent(sendScheduleButton)
                                .addGap(18, 18, 18)
                                .addComponent(sendWelcomeButton)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(addButton)
                                .addGap(18, 18, 18)
                                .addComponent(importButton))
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel8)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(filterComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel9)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(filterStartNrTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 62, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel10)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(filterPhoneTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 243, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(jLabel6)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(startSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 64, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jLabel7)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(countSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 57, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(18, 18, 18)
                                .addComponent(jLabelTotal)))))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1)
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(addButton)
                        .addComponent(importButton))
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(jLabel1)
                        .addComponent(deleteButton)
                        .addComponent(markButton)
                        .addComponent(sendScheduleButton)
                        .addComponent(sendWelcomeButton)))
                .addGap(18, 18, 18)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(startSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel6)
                        .addComponent(jLabel7)
                        .addComponent(countSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabelTotal))
                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                        .addComponent(filterComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel8)
                        .addComponent(jLabel9)
                        .addComponent(filterStartNrTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel10)
                        .addComponent(filterPhoneTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addComponent(jLabel11)))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    private void deleteButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deleteButtonActionPerformed
        new Thread(new Runnable() {

            @Override
            public void run() {
                synchronized (phoneTable) {
                    int[] rows = phoneTable.getSelectedRows();

                    for (int i = rows.length; i-- > 0; ) {
                        int id = ((Integer) phoneTable.getModel().getValueAt(rows[i], 1));
                        database.removePhoneNumber(id);
                    }
                    
                    SwingUtilities.invokeLater(new Runnable() {

                        @Override
                        public void run() {
                            update();
                        }
                        
                    });
                }
            }            
        }).start();        
    }//GEN-LAST:event_deleteButtonActionPerformed

    private void addButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addButtonActionPerformed
        startNrTextField.setText("");
        playerTextField.setText("");
        phoneNrTextArea.setText("");        
        statusComboBox.setSelectedItem("U");
        
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("smscenter/gui/resources/SMSCenter"); 
        
        int ret = javax.swing.JOptionPane.showConfirmDialog(
                this, phoneNumberPanel, bundle.getString("Add Phone Number"), 
                javax.swing.JOptionPane.OK_CANCEL_OPTION
        );
        
        if (ret == javax.swing.JOptionPane.YES_OPTION) {
            try {
                String startNrString = startNrTextField.getText().trim();
                String phoneNrString = phoneNrTextArea.getText().trim().replaceAll("\n", ",").replaceAll("\\s+", "");
                
                boolean ok = startNrString.matches("^\\d+");
                for (String s : phoneNrString.split(",")) {
                    ok &= s.matches("^\\+\\d+");
                }
                
                if (ok)
                    database.addPhoneNumber(Integer.parseInt(startNrString), phoneNrString, statusComboBox.getSelectedItem().toString());
            } catch (Exception ex) {
                java.util.logging.Logger.getLogger(getClass().getName()).log(Level.SEVERE, null, ex);
            }
            
            SwingUtilities.invokeLater(new Runnable(){

                @Override
                public void run() {
                    update();
                }
            });
        }        
    }//GEN-LAST:event_addButtonActionPerformed

    private void phoneTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_phoneTableMouseClicked
        if (evt.getClickCount() != 2)
            return;
        
        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("smscenter/gui/resources/SMSCenter"); 
        
        int row = phoneTable.rowAtPoint(evt.getPoint());
        final int id = Integer.parseInt(phoneTable.getModel().getValueAt(row, 1).toString());
        startNrTextField.setText(phoneTable.getModel().getValueAt(row, 2).toString());
        phoneNrTextArea.setText(phoneTable.getModel().getValueAt(row, 3).toString().replaceAll(",", "\n"));
        statusComboBox.setSelectedItem(phoneTable.getModel().getValueAt(row, 4).toString());
        
        try {
            Player pl = database.getPlayer(Integer.parseInt(startNrTextField.getText()));
            
            String text = (pl == null ? "" : pl.psLast + ", " + pl.psFirst + " (" + pl.naName + ")");
            playerTextField.setText(text);                        
        } catch (Exception e) {
            playerTextField.setText("");
        }
        
        int ret = javax.swing.JOptionPane.showConfirmDialog(this, phoneNumberPanel, bundle.getString("Edit Phone Number"), javax.swing.JOptionPane.OK_CANCEL_OPTION);
        if (ret == javax.swing.JOptionPane.OK_OPTION) {
            String startNrString = startNrTextField.getText().trim();
            String phoneNrString = phoneNrTextArea.getText().trim().replaceAll("\n", ",").replaceAll("\\s+", "");
                
            boolean ok = startNrString.matches("^\\d+");
            for (String s : phoneNrString.split(",")) {
                ok &= s.matches("^\\+\\d+");
            }

            if (ok)
                database.addPhoneNumber(Integer.parseInt(startNrString), phoneNrString, statusComboBox.getSelectedItem().toString());

            update();
        }
    }//GEN-LAST:event_phoneTableMouseClicked

    private void importButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_importButtonActionPerformed

        javax.swing.JFileChooser jfc = new javax.swing.JFileChooser();
        jfc.setFileFilter(new javax.swing.filechooser.FileNameExtensionFilter("CSV Files", "csv"));
        if (jfc.showOpenDialog(this) != javax.swing.JFileChooser.APPROVE_OPTION)
            return;
        
        final File file = jfc.getSelectedFile();
        
        new Thread() {
            @Override
            public void run() {
                try {
                    java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(file));
                    String line;
                    
                    while ( (line = br.readLine()) != null ) {
                        String[] fields = line.split("[;,\t]", -1);
                        if (fields.length < 2)
                            continue;
                        
                        if (fields[0].isEmpty() || fields[1].isEmpty())
                            continue;
                        
                        int startNr = 0;
                        try {
                            startNr = Integer.parseInt(fields[0]);
                            if (!database.isStartNr(startNr))
                                startNr = 0;
                        } catch (NumberFormatException ex) {
                            
                        }
                        
                        if (startNr == 0)
                            startNr = database.getStartNrFromExternId(fields[0]);
                        
                        String phoneNr = fields[1];
                        if (!phoneNr.startsWith("0") && !phoneNr.startsWith("+"))
                            phoneNr = "+" + phoneNr;
                        
                        if (startNr != 0)
                            database.addPhoneNumber(startNr, phoneNr, "U");
                    }
                } catch (FileNotFoundException ex) {
                    Logger.getLogger(PhonePanel.class.getName()).log(Level.SEVERE, null, ex);
                } catch (IOException ex) {
                    Logger.getLogger(PhonePanel.class.getName()).log(Level.SEVERE, null, ex);
                }
                
                SwingUtilities.invokeLater(new Runnable() {

                    @Override
                    public void run() {
                        update();
                    }                    
                });
            }
        }.start();
    }//GEN-LAST:event_importButtonActionPerformed

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

    private void markButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_markButtonActionPerformed
        new Thread(new Runnable() {

            @Override
            public void run() {
                synchronized (phoneTable) {
                    int[] rows = phoneTable.getSelectedRows();

                    for (int i = rows.length; i-- > 0; ) {
                        int id = ((Integer) phoneTable.getModel().getValueAt(rows[i], 1));
                        database.markPhoneNumber(id);
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
    }//GEN-LAST:event_markButtonActionPerformed

    private void sendScheduleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sendScheduleButtonActionPerformed
        new Thread(new Runnable() {

            @Override
            public void run() {
                Set<Integer> plSet = new java.util.HashSet<>();
                    
                synchronized (phoneTable) {
                    int[] rows = phoneTable.getSelectedRows();
                    for (int row : rows) {
                        plSet.add( ((Integer) phoneTable.getModel().getValueAt(row, 2)));
                    }
                }
                
                java.awt.Container parent = getParent();
                while ( parent != null && !(parent instanceof MainFrame) )
                    parent = parent.getParent();
                
                if (parent != null)
                    ((MainFrame) parent).sendUpdateSchedules(plSet);
            }            
        }).start();        
    }//GEN-LAST:event_sendScheduleButtonActionPerformed

    private int editPhonePlNr;
    
    private void startNrTextFieldFocusLost(java.awt.event.FocusEvent evt) {//GEN-FIRST:event_startNrTextFieldFocusLost
        try {            
            int plNr = Integer.parseInt(startNrTextField.getText());
            if (plNr == editPhonePlNr)
                return;
            
            editPhonePlNr = plNr;
            
            Player pl = database.getPlayer(plNr);
            
            String text = (pl == null ? "" : pl.psLast + ", " + pl.psFirst + " (" + pl.naName + ")");
            playerTextField.setText(text);
            
            if (pl != null && pl.phone != null) {
               phoneNrTextArea.setText(pl.phone.replaceAll(",", "\n"));
                statusComboBox.setSelectedItem(database.getPhoneStatus(plNr));
            } else {
                phoneNrTextArea.setText("");
            }
            
        } catch (NumberFormatException ex) {
            playerTextField.setText("");
        }        
    }//GEN-LAST:event_startNrTextFieldFocusLost

    private void phoneNumberPanelAncestorAdded(javax.swing.event.AncestorEvent evt) {//GEN-FIRST:event_phoneNumberPanelAncestorAdded
        SwingUtilities.invokeLater(new Runnable() {

            @Override
            public void run() {
                startNrTextField.requestFocusInWindow();
                String str = startNrTextField.getText();
                try {
                    editPhonePlNr = str.isEmpty() ? 0 : Integer.parseInt(startNrTextField.getText());
                } catch (NumberFormatException ex) {
                    editPhonePlNr = 0;
                }
            }
           
        });
    }//GEN-LAST:event_phoneNumberPanelAncestorAdded

    private void filterComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filterComboBoxActionPerformed
        update();
    }//GEN-LAST:event_filterComboBoxActionPerformed

    private void filterStartNrTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filterStartNrTextFieldActionPerformed
        update();
    }//GEN-LAST:event_filterStartNrTextFieldActionPerformed

    private void filterPhoneTextFieldActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_filterPhoneTextFieldActionPerformed
        update();
    }//GEN-LAST:event_filterPhoneTextFieldActionPerformed

    private void sendWelcomeButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sendWelcomeButtonActionPerformed
        final Properties props = new Properties();
        try {
            props.load(new FileInputStream(MainFrame.findPath("SMSServer.conf")));
        } catch (IOException ex) {

        }
        
        final String welcomeMsg = database.getWelcomeMsg();
        if (welcomeMsg.isEmpty())
            return;
        
        new Thread(new Runnable() {

            @Override
            public void run() {
                synchronized (phoneTable) {
                    int[] rows = phoneTable.getSelectedRows();
                    for (int row : rows) {
                        String phone = phoneTable.getModel().getValueAt(row, 3).toString();
                        database.sendMessage(phone, welcomeMsg, true);
                    }
                }
                
            }            
        }).start();        
    }//GEN-LAST:event_sendWelcomeButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addButton;
    private javax.swing.JSpinner countSpinner;
    private javax.swing.JButton deleteButton;
    private javax.swing.JComboBox filterComboBox;
    private javax.swing.JTextField filterPhoneTextField;
    private javax.swing.JTextField filterStartNrTextField;
    private javax.swing.JButton importButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
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
    private javax.swing.JButton markButton;
    private javax.swing.JTextArea phoneNrTextArea;
    private javax.swing.JPanel phoneNumberPanel;
    private final javax.swing.JTable phoneTable = new javax.swing.JTable();
    private javax.swing.JTextField playerTextField;
    private javax.swing.JButton sendScheduleButton;
    private javax.swing.JButton sendWelcomeButton;
    private javax.swing.JTextField startNrTextField;
    private javax.swing.JSpinner startSpinner;
    private javax.swing.JComboBox statusComboBox;
    // End of variables declaration//GEN-END:variables

    private int start = 0;
    private int count = 50;
    private int sort = -1;
}
