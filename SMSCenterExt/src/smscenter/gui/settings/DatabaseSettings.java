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
public class DatabaseSettings extends Settings {
    public DatabaseSettings() {
        super();
    }
    
    @Override
    public void readProperties(Properties props, String prefix) {
        setDatabase(getProperty(props, prefix, "database", ""));
        setServer(getProperty(props, prefix, "server", "(local)"));
        setUser(getProperty(props, prefix, "user", ""));
        setPassword(getProperty(props, prefix, "password", ""));
        setWindowsAuth(getProperty(props, prefix, "windowsAuth", "yes").equals("yes"));
        setBatch_size(Integer.parseInt(getProperty(props, prefix, "batch_size", "50")));
        setRetries(Integer.parseInt(getProperty(props, prefix, "retries", "2")));
        setUpdateOutboundOnStatusreport(getProperty(props, prefix, "update_outbound_on_statusreport", "no").equals("yes"));
    }    
    
    @Override
    public void writeProperties(Properties props, String prefix) {
        setProperty(props, prefix, "database", getDatabase());
        setProperty(props, prefix, "server", getServer());
        setProperty(props, prefix, "user", getUser());
        setProperty(props, prefix, "password", getPassword());
        setProperty(props, prefix, "windowsAuth", isWindowsAuth() ? "yes" : "no");
        setProperty(props, prefix, "type", "mssql");
        setProperty(props, prefix, "batch_size", "" + getBatch_size());
        setProperty(props, prefix, "retries", "" + getRetries());
        setProperty(props, prefix, "update_outbound_on_statusreport", isUpdateOutboundOnStatusreport() ? "yes" : "no");
    }
    
    
    /**
     * @return the batch_size
     */
    public int getBatch_size() {
        return batch_size;
    }

    /**
     * @param batch_size the batch_size to set
     */
    public void setBatch_size(int batch_size) {
        this.batch_size = batch_size;
    }

    /**
     * @return the retries
     */
    public int getRetries() {
        return retries;
    }

    /**
     * @param retries the retries to set
     */
    public void setRetries(int retries) {
        this.retries = retries;
    }

    /**
     * @return the update_outbound_on_statusreport
     */
    public boolean isUpdateOutboundOnStatusreport() {
        return updateOutboundOnStatusreport;
    }

    /**
     * @param update_outbound_on_statusreport the update_outbound_on_statusreport to set
     */
    public void setUpdateOutboundOnStatusreport(boolean updateOutboundOnStatusreport) {
        this.updateOutboundOnStatusreport = updateOutboundOnStatusreport;
    }
    
    
    private String  database;
    private String  server;
    private boolean windowsAuth;
    private String  user;
    private String  password;
    private int     batch_size;
    private int     retries;
    private boolean updateOutboundOnStatusreport;
    
/*
    interface.0=db1, Database
    db1.url=jdbc:odbc:DRIVER=SQL Server;SERVER=(local);Trusted_Connection=Yes;DATABASE=EYC2009;AnsiNPW=No
    db1.driver=java.lang.String
    db1.username=
    db1.password=
    db1.type=mssql
    #db1.tables.sms_in=
    #db1.tables.sms_out=
    #db1.tables.calls=
    db1.batch_size=50
    db1.retries=2
    db1.update_outbound_on_statusreport=no
 */    

    /**
     * @return the database
     */
    public String getDatabase() {
        return database;
    }

    /**
     * @param database the database to set
     */
    public void setDatabase(String database) {
        this.database = database;
    }

    /**
     * @return the server
     */
    public String getServer() {
        return server;
    }

    /**
     * @param server the server to set
     */
    public void setServer(String server) {
        this.server = server;
    }

    /**
     * @return the windowsAuth
     */
    public boolean isWindowsAuth() {
        return windowsAuth;
    }

    /**
     * @param windowsAuth the windowsAuth to set
     */
    public void setWindowsAuth(boolean windowsAuth) {
        this.windowsAuth = windowsAuth;
    }

    /**
     * @return the user
     */
    public String getUser() {
        return user;
    }

    /**
     * @param user the user to set
     */
    public void setUser(String user) {
        this.user = user;
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

}
