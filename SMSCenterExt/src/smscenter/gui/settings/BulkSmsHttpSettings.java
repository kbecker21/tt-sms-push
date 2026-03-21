/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package smscenter.gui.settings;

import java.util.Properties;
        
/**
 *
 * @author chtheis
 */
public class BulkSmsHttpSettings extends GatewaySettings {

    @Override
    public SettingsPanel getPanel() {
        return new BulkSmsHttpSettingsPanel(this);
    }

    @Override
    public GatewayType getType() {
        return GatewayType.BulkSmsHttp;
    }

    @Override
    public void readProperties(Properties props, String prefix) {
        setUsername(getProperty(props, prefix, "username", ""));
        setPassword(getProperty(props, prefix, "password", ""));
        setRegion(getProperty(props, prefix, "region", "Germany"));
        setInbound(getProperty(props, prefix, "inbound", "no").equals("yes"));
        setOutbound(getProperty(props, prefix, "outbound", "no").equals("yes"));
        setDescription(getProperty(props, prefix, "description", "Default BulkSms Gateway"));
    }

    @Override
    public void writeProperties(Properties props, String prefix) {
        setProperty(props, prefix, "username", getUsername());
        setProperty(props, prefix, "password", getPassword());
        setProperty(props, prefix, "region", getRegion());
        setProperty(props, prefix, "inbound", isInbound() ? "yes" : "no");
        setProperty(props, prefix, "outbound", isOutbound() ? "yes" : "no");
        setProperty(props, prefix, "description", getDescription());
    }
 
    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @param username the username to set
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * @return the password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * @return the region
     */
    public String getRegion() {
        return region;
    }

    /**
     * @param region the region to set
     */
    public void setRegion(String region) {
        this.region = region;
    }

    /**
     * @return the inbound
     */
    public boolean isInbound() {
        return inbound;
    }

    /**
     * @param inbound the inbound to set
     */
    public void setInbound(boolean inbound) {
        this.inbound = inbound;
    }

    /**
     * @return the outbound
     */
    public boolean isOutbound() {
        return outbound;
    }

    /**
     * @param outbound the outbound to set
     */
    public void setOutbound(boolean outbound) {
        this.outbound = outbound;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }
    
    private String description;
    private String username;
    private String password;
    private String region;
    private boolean inbound;
    private boolean outbound;
}
