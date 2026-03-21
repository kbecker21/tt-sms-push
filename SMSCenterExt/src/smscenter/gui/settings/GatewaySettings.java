/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package smscenter.gui.settings;

/**
 *
 * @author chtheis
 */
public abstract class GatewaySettings extends Settings {
    public enum GatewayType {
        None,
        SerialModem,
        BulkSmsHttp,
        PushGateway
    };
    
    public static GatewaySettings allocate(GatewayType type) {
        switch (type) {
            case None :
                return null;
            case SerialModem :
                return new SerialModemSettings();  
            case BulkSmsHttp :
                return new BulkSmsHttpSettings();
            case PushGateway :
                return new PushGatewaySettings();
        }
        
        return null;
    }
    
    
    
    public abstract SettingsPanel getPanel();
    
    public abstract GatewayType getType();
}
