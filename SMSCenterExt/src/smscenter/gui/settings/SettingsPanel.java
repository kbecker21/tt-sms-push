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
public abstract class SettingsPanel extends javax.swing.JPanel {
    public abstract void loadProperties(Properties props);
    public abstract void writeProperties(Properties props);
}
