/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package smscenter.gui.settings;

import java.util.List;
import java.util.Properties;

/**
 *
 * @author chtheis
 */
public abstract class Settings {
    
    public static GeneralSettings readGeneralSettings(Properties props) {
        GeneralSettings general = new GeneralSettings();
        general.readProperties(props, "settings");
        
        return general;
    }    
    
    public static void writeGeneralSettings(GeneralSettings general, Properties props) {
        general.writeProperties(props, "settings");
    }
    
    public static DatabaseSettings readDatabase(Properties props) {
        DatabaseSettings database = new DatabaseSettings();
        database.readProperties(props, "db1");
        
        return database;
    }
    
    public static void writeDatabase(DatabaseSettings database, Properties props) {
        props.setProperty("interface.0", "db1" + ", Database");
        database.writeProperties(props, "db1");
    }
    
    
    public static List<GatewaySettings> readGateways(Properties props) {
        List<GatewaySettings> list = new java.util.ArrayList<>();
        
        for (int i = 0; i < Integer.MAX_VALUE; i++) {
            String propName = "gateway." + i;
            String propValue = props.getProperty(propName);
            if (propValue == null)                
                break;
            
            String[] tokens = propValue.split(",");
            if (tokens.length < 2)
                continue;
            
            String prefix = tokens[0].trim();
            String type = tokens[1].trim();
            GatewaySettings settings = null;
            
            switch (type) {                
                case "SerialModem" :
                    settings = new SerialModemSettings();
                    break;
                    
                case "BulkSmsHttp" :
                    settings = new BulkSmsHttpSettings();
                    break;

                case "PushGateway" :
                    settings = new PushGatewaySettings();
                    break;

                default :
                    break;
            }
            
            if (settings == null)
                continue;
            
            settings.readProperties(props, prefix);
            list.add(settings);
        }
        
        return list;
    }
    
    
    public static void writeGateways(List<GatewaySettings> gateways, Properties props) {
        int i = 0;
        for (GatewaySettings gateway : gateways) {
            if (gateway == null)
                continue;
            
            switch (gateway.getType()) {
                case SerialModem :
                    props.setProperty("gateway." + i, "modem" + i + ", SerialModem");
                    gateway.writeProperties(props, "modem" + i);
                    break;
                    
                case BulkSmsHttp :
                    props.setProperty("gateway." + i, "http" + i + ", BulkSmsHttp");
                    gateway.writeProperties(props, "http" + i);
                    break;

                case PushGateway :
                    props.setProperty("gateway." + i, "push" + i + ", PushGateway");
                    gateway.writeProperties(props, "push" + i);
                    break;

                default :
                    break;
            }
            
            ++i;
        }
    }
    
    public Settings() {
    }
    
    public String getProperty(Properties props, String prefix, String key, String defaultValue) {
        return props.getProperty(prefix + "." + key, defaultValue);
    }
    
    public Object setProperty(Properties props, String prefix, String key, String value) {
        return props.setProperty(prefix + "." + key, value);
    }
    
    
    public abstract void readProperties(Properties props, String prefix);
    public abstract void writeProperties(Properties props, String prefix);
}
