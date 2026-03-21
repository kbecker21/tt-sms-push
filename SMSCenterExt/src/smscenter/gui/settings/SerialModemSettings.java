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
public class SerialModemSettings extends GatewaySettings {
    public SerialModemSettings() {
    }
    
    @Override
    public void readProperties(Properties props, String prefix) {
        setPort(getProperty(props, prefix, "port", "COM1"));
        setBaudrate(Integer.parseInt(getProperty(props, prefix, "baudrate", "115200")));
        setManufacturer(getProperty(props, prefix, "manufaturer", ""));
        setModel(getProperty(props, prefix, "model", ""));
        setProtocol(getProperty(props, prefix, "protocol", "PDU"));
        setPin(getProperty(props, prefix, "pin", ""));
        setInbound(getProperty(props, prefix, "inbound", "no").equals("yes"));
        setOutbound(getProperty(props, prefix, "outbound", "no").equals("yes"));
        setSmscNumber(getProperty(props, prefix, "smsc_number",""));
        setInitString(getProperty(props, prefix, "init_string", ""));
    }    
    
    @Override
    public void writeProperties(Properties props, String prefix) {
        setProperty(props, prefix, "port", getPort());
        setProperty(props, prefix, "baudrate", "" + getBaudrate());
        setProperty(props, prefix, "manucaturer", getManufacturer());
        setProperty(props, prefix, "model", getModel());
        setProperty(props, prefix, "protocol", getProtocol());
        setProperty(props, prefix, "pin", getPin());
        setProperty(props, prefix, "inbound", isInbound() ? "yes" : "no");
        setProperty(props, prefix, "outbound", isOutbound() ? "yes" : "no");
        setProperty(props, prefix, "smsc_number", getSmscNumber());
        setProperty(props, prefix, "init_string", getInitString());
    }
    
    
    @Override
    public SettingsPanel getPanel() {
        return new SerialModemSettingsPanel(this);
    }
    
    
    @Override
    public GatewayType getType() {
        return GatewayType.SerialModem;
    }
    
    
    /**
     * @return the port
     */
    public String getPort() {
        return port;
    }

    /**
     * @param port the port to set
     */
    public void setPort(String port) {
        this.port = port;
    }

    /**
     * @return the baudrate
     */
    public int getBaudrate() {
        return baudrate;
    }

    /**
     * @param baudrate the baudrate to set
     */
    public void setBaudrate(int baudrate) {
        this.baudrate = baudrate;
    }

    /**
     * @return the manufaturer
     */
    public String getManufacturer() {
        return manufacturer;
    }

    /**
     * @param manufaturer the manufaturer to set
     */
    public void setManufacturer(String manufaturer) {
        this.manufacturer = manufaturer;
    }

    /**
     * @return the model
     */
    public String getModel() {
        return model;
    }

    /**
     * @param model the model to set
     */
    public void setModel(String model) {
        this.model = model;
    }

    /**
     * @return the protocol
     */
    public String getProtocol() {
        return protocol;
    }

    /**
     * @param protocol the protocol to set
     */
    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    /**
     * @return the pin
     */
    public String getPin() {
        return pin;
    }

    /**
     * @param pin the pin to set
     */
    public void setPin(String pin) {
        this.pin = pin;
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
     * @return the smsc_number
     */
    public String getSmscNumber() {
        return smscNumber;
    }

    /**
     * @param smsc_number the smsc_number to set
     */
    public void setSmscNumber(String smscNumber) {
        this.smscNumber = smscNumber;
    }

    /**
     * @return the init_string
     */
    public String getInitString() {
        return initString;
    }

    /**
     * @param init_string the init_string to set
     */
    public void setInitString(String initString) {
        this.initString = initString;
    }
    
    
    private String port = "COM1";
    private int baudrate = 115200;
    private String manufacturer = "";
    private String model = "";
    private String protocol = "PDU";
    private String pin = "";
    private boolean inbound = true;
    private boolean outbound = true;
    private String smscNumber = "";
    private String initString = "ATZ\nATZ\n";
    
/*
    modem1.port=COM9
    modem1.baudrate=115200
    modem1.manufacturer=Huawei
    modem1.model=E160
    modem1.protocol=PDU
    modem1.pin=
    modem1.inbound=yes
    modem1.outbound=no
    modem1.smsc_number=
    modem1.init_string=ATZ\rATZ\rATZ\r
 */    

}
