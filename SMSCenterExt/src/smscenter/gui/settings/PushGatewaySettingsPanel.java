package smscenter.gui.settings;

import java.util.Properties;

/**
 * Settings panel for the Push Notification Gateway.
 */
public class PushGatewaySettingsPanel extends SettingsPanel {

    private PushGatewaySettings settings;

    public PushGatewaySettingsPanel(PushGatewaySettings settings) {
        this.settings = settings;
        initComponents();
    }

    @Override
    public void loadProperties(Properties props) {
        serviceUrlTextField.setText(settings.getServiceUrl());
        apiKeyTextField.setText(settings.getApiKey());
        outboundCheckbox.setSelected(settings.isOutbound());
        descriptionTextField.setText(settings.getDescription());
    }

    @Override
    public void writeProperties(Properties props) {
        settings.setServiceUrl(serviceUrlTextField.getText());
        settings.setApiKey(apiKeyTextField.getText());
        settings.setOutbound(outboundCheckbox.isSelected());
        settings.setDescription(descriptionTextField.getText());
    }

    private void initComponents() {
        jLabel1 = new javax.swing.JLabel();
        serviceUrlTextField = new javax.swing.JTextField();
        jLabel2 = new javax.swing.JLabel();
        apiKeyTextField = new javax.swing.JTextField();
        outboundCheckbox = new javax.swing.JCheckBox();
        jLabel3 = new javax.swing.JLabel();
        descriptionTextField = new javax.swing.JTextField();

        java.util.ResourceBundle bundle = java.util.ResourceBundle.getBundle("smscenter/gui/resources/SMSCenter");
        jLabel1.setText(bundle.getString("Service URL:"));
        jLabel2.setText(bundle.getString("API Key:"));
        jLabel3.setText(bundle.getString("Description:"));
        outboundCheckbox.setText(bundle.getString("Outbound"));

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel1)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(serviceUrlTextField, javax.swing.GroupLayout.DEFAULT_SIZE, 250, Short.MAX_VALUE)
                    .addComponent(apiKeyTextField)
                    .addComponent(descriptionTextField)
                    .addComponent(outboundCheckbox))
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(serviceUrlTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel2)
                    .addComponent(apiKeyTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel3)
                    .addComponent(descriptionTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 14, Short.MAX_VALUE)
                .addComponent(outboundCheckbox)
                .addGap(17, 17, 17))
        );
    }

    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JTextField serviceUrlTextField;
    private javax.swing.JTextField apiKeyTextField;
    private javax.swing.JCheckBox outboundCheckbox;
    private javax.swing.JTextField descriptionTextField;
}
