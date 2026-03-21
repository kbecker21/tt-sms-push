/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package smscenter.database;

import java.io.IOException;
import java.io.Reader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.Normalizer;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import smscenter.gui.settings.Settings;

/**
 *
 * @author chtheis
 */
public class Database {
    
    private Connection connection = null;
    private String connectString = null;
    private boolean validConnection = false;
    
    public static boolean testConnection(String connectString) {
        if (connectString == null)
            return false;
        
        if (connectString.isEmpty())
            return false;
        
        try (Connection conn = DriverManager.getConnection(connectString)) {
            return conn != null;
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        } 
        
        return false;
    }
    
        
    public Database() {
        
    }    
    
    
    public boolean isValidConnection() {
        return validConnection;
    }
    
    
    synchronized public boolean setConfiguration(Properties props) {
        smscenter.gui.settings.DatabaseSettings settings = Settings.readDatabase(props);
        validConnection = true;
        
        StringBuilder sb = new StringBuilder();
        sb.append("jdbc:sqlserver://");
        if (settings.getServer().equalsIgnoreCase("(local)"))
            sb.append("localhost");
        else
            sb.append(settings.getServer());
        sb.append(";");
        
        String[] database = settings.getDatabase().split("\\\\");
        sb.append("databaseName=").append(database[0]).append(";");
        if (database.length > 1)
            sb.append("instanceName=").append(database[1]).append(";");
        
        if (settings.isWindowsAuth())
            sb.append("integratedSecurity=true;trustServerCertificate=true;encrypt=true;");
        else
            sb.append("user=").append(settings.getUser()).append(";").append("password=").append(settings.getPassword()).append(";").
               append("integratedSecurity=false;trustServerCertificate=true;encrypt=true;");

        connectString = sb.toString();
        
        try {
            if (connection != null)
                connection.close();
        } catch (SQLException ex) {
            return false;
        }
        
        connection = null;
        
        validConnection = initializeDatabase(props);
        
        if (validConnection)
            updateConfiguration(props, 0);
        
        return validConnection;
    }
    
    
    // Temp. write back to ini file if info is newer than ts
    public long updateConfiguration(Properties props, long ts) {
        smscenter.gui.settings.GeneralSettings settings = Settings.readGeneralSettings(props);

        long newTS = getSettingsTimestamp();
        if (newTS <= ts)
            return newTS;
        
        String welcomeMsg = getWelcomeMsg();
        String sponsorLine = getSponsorLine();
        
        if (welcomeMsg != null)
            settings.setWelcomeMsg(welcomeMsg);
        if (sponsorLine != null)
            settings.setSponsorLine(sponsorLine);
        
        Settings.writeGeneralSettings(settings, props);
        
        return newTS;
    }
    
    synchronized public List<Object[]> getIncomingMessages(int start, int count) {
        List<Object[]> list = new java.util.ArrayList<>();
        
        Connection conn;
        String sql = "SELECT * FROM (SELECT ROW_NUMBER() OVER (ORDER BY id DESC) AS row, id, status, originator, text, receive_date FROM smsserver_in) AS incoming ";
        if (count > 0)
            sql += "WHERE row BETWEEN " + (start + 1) + " AND " + (start + count);
        else if (start > 0)
            sql += "WHERE row > " + start;
        
        sql += " ORDER BY row ASC";
        
        try {
            if ( (conn = getConnection()) == null )
                return list;
            
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    Object[] s = new Object[6];
                    s[0] = rs.getInt(1);
                    s[1] = rs.getInt(2);
                    s[2] = rs.getString(3);
                    s[3] = rs.getString(4);
                    s[4] = rs.getString(5);
                    s[5] = rs.getTimestamp(6);
                    
                    list.add(s);
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        }
        
        return list;
    }
    
    
    synchronized public List<Object[]> getNewIncomingMessages() {
        List<Object[]> list = new java.util.ArrayList<>();
        
        Connection conn;
        String sql = "SELECT id, originator, text FROM smsserver_in WHERE status = 'U' ORDER BY id DESC ";
        
        try {
            if ( (conn = getConnection()) == null )
                return list;
            
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    Object[] s = new Object[3];
                    s[0] = rs.getInt(1);
                    s[1] = rs.getString(2);
                    s[2] = rs.getString(3);
                    
                    list.add(s);                    
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        }
        
        return list;
    }
    
    synchronized public boolean markIncomingMessageRead(int id) {
        Connection conn;
        String sql = "UPDATE smsserver_in SET status = 'R' WHERE id = ?";
        
        try {
            if ( (conn = getConnection()) == null )
                return false;
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                
                stmt.executeUpdate();
                conn.commit();
            }
            
            return true;
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        }
        
        return false;
    }
    
    
    synchronized public boolean removeIncomingMessage(int id) {
        Connection conn;
        String sql = "DELETE FROM smsserver_in WHERE id = ?";
        
        try {
            if ( (conn = getConnection()) == null )
                return false;
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                
                stmt.executeUpdate();
                conn.commit();
            }
            
            return true;
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        }                        
        
        return false;
    }
    
    
    synchronized public List<Object[]> getOutgoingMessages(int start, int count, String status, String player, String phone, int order) {
        List<Object[]> list = new java.util.ArrayList<>();
        
        String sort = "smsserver_out.id";
        
        String[] rows = {
            "id",
            "status",
            "recipient",
            "text",
            "create_date",
            "ref_no",
            "gateway_id"
        };
        
        if (order != 0 && Math.abs(order) <= rows.length && rows[Math.abs(order) - 1] != null) {
            sort = rows[Math.abs(order) - 1] + " " + (order > 0 ? " ASC" : " DESC");
        }
       
        Connection conn;
        String sql = "SELECT * FROM (SELECT ROW_NUMBER() OVER (ORDER BY " + sort + ") AS row, id, status, recipient, text, create_date, ref_no, gateway_id FROM smsserver_out ";
        sql += " WHERE 1 = 1 ";
        if (status != null && !status.isEmpty())
            sql += " AND status = ? "; // '" + status + "' ";  
        if (player != null && !player.isEmpty())
            sql += " AND recipient IN (SELECT SUBSTRING(phone, 1, 99) FROM smscenter_phones WHERE plNr = ? ) "; //  + player + ")";
        if (phone != null && !phone.isEmpty()) {
            sql += "AND recipient LIKE ?";
        }
        
        sql += " ) AS outgoing ";
        sql += "WHERE 1 = 1 ";
        if (count > 0)
            sql += " AND row BETWEEN ? AND ? "; //  + (start + 1) + " AND " + (start + count);
        else if (start > 0)
            sql += " AND row > ? "; // " + start;
        
        sql += " ORDER BY row ASC";
        
        try {
            if ( (conn = getConnection()) == null )
                return list;
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                int idx = 1;
                
                if (status != null && !status.isEmpty())
                    stmt.setString(idx++, status);                
                if (player != null && !player.isEmpty())
                    stmt.setInt(idx++, Integer.parseInt(player));   
                if (phone != null && !phone.isEmpty())
                    stmt.setString(idx++, phone.replaceAll("\\*", "%").replaceAll("\\?", "_") + "%");                
                if (count > 0) {
                    stmt.setInt(idx++, start);
                    stmt.setInt(idx++, start + count);
                } else if (start > 0)
                    stmt.setInt(idx++, start);
                
                try (ResultSet rs = stmt.executeQuery()) {
                
                    while (rs.next()) {
                        Object[] s = new Object[8];
                        s[0] = rs.getInt(1);
                        s[1] = rs.getInt(2);
                        s[2] = rs.getString(3);
                        s[3] = rs.getString(4);
                        s[4] = rs.getString(5);
                        s[5] = rs.getTimestamp(6);
                        s[6] = rs.getString(7);
                        s[7] = rs.getString(8);

                        list.add(s);
                    }
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        }
        
        return list;
    }
    
    
    synchronized public int getOutgoingCount(String status, String player, String phone) {
        Connection conn;
        String sql = "SELECT COUNT(*) FROM smsserver_out ";
        
        sql += "WHERE 1 = 1 ";
        if (status != null && !status.isEmpty())
            sql += " AND status = ? "; // '" + status + "' ";  
        if (player != null && !player.isEmpty())
            sql += " AND recipient IN (SELECT SUBSTRING(phone, 1, 99) FROM smscenter_phones WHERE plNr = ? ) "; //  + player + ")";
        if (phone != null && !phone.isEmpty()) {
            sql += "AND recipient LIKE ?";
        }
        
        try {
            if ( (conn = getConnection()) == null )
                return 0;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                int idx = 1;
                if (status != null && !status.isEmpty())
                    stmt.setString(idx++, status);                
                if (player != null && !player.isEmpty())
                    stmt.setInt(idx++, Integer.parseInt(player));   
                if (phone != null && !phone.isEmpty())
                    stmt.setString(idx++, phone.replaceAll("\\*", "%").replaceAll("\\?", "_") + "%");                
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next())
                        return 0;
                    return rs.getInt(1);
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        }

        return 0;
    }
    
    synchronized public boolean setMessageStatus(int id, String status) {
        Connection conn;
        String sql = "UPDATE smsserver_out SET status = ? WHERE id = ?";
        
        try {
            if ( (conn = getConnection()) == null )
                return false;
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, status);
                stmt.setInt(2, id);
                stmt.executeUpdate();
                conn.commit();
            }
            
            return true;
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        }
        
        return false;
    }

    
    synchronized public boolean removeOutgoingMessage(int id) {
        Connection conn;
        String sql = "DELETE FROM smsserver_out WHERE id = ?";
        
        try {
            if ( (conn = getConnection()) == null )
                return false;
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                
                stmt.executeUpdate();
                conn.commit();
            }
            
            return true;
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        }                        
        
        return false;
    }
    
    
    synchronized public boolean sendMessage(String number, String text, boolean wantStatusReport) {
        if (number == null || text == null)
            return true;
        
        if (number.isEmpty())
            return true;
        
        // Multiple numbers
        if (number.contains(",")) {
            boolean ok = false;
            for (String n : number.split(","))
                ok |= sendMessage(n, text, wantStatusReport);
            
            return ok;
        }
        
        if (number.charAt(0) == '+' || number.charAt(0) == '0') 
            return sendMessageToNumber(number, text, wantStatusReport);
        
        List<String> list = new java.util.ArrayList<>();
        String sql = null;
        
        if (number.charAt(0) == '*') {
             sql = "SELECT DISTINCT phone FROM smscenter_phones";
        } else if (Character.isDigit(number.charAt(0))) {
            sql = "SELECT phone FROM smscenter_phones WHERE plNr = " + number;
        } else {
            String[] s = number.split(" ");
            sql = "SELECT DISTINCT phone FROM smscenter_phones " +
                  " INNER JOIN PlList ON smscenter_phones.plNr = PlList.plNr " +
                  " INNER JOIN LtList ON PlList.plID = LtList.plID " +
                  " INNER JOIN CpList ON LtList.cpID = CpList.cpID ";
            
            if (s.length > 1)
                sql += 
                  " INNER JOIN NtList ON LtList.ltId = NtList.ltID " +
                  " INNER JOIN StList ON StList.tmID = NtList.tmID " +
                  " INNER JOIN GrList ON StList.grID = GrList.grID ";
            
            sql += " WHERE CpList.cpName = '" + s[0] + "'";
            if (s.length > 1)
                sql += " AND GrList.grName = '" + s[1] + "'";
        }
        
        Connection conn;
        
        try {
            if ( (conn = getConnection()) == null )
                return false;
            
            try (Statement stmt = conn.createStatement()) {
                try (ResultSet res = stmt.executeQuery(sql)) {
                    while (res.next())
                        list.add(res.getString(1));
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        
        for (String s : list)
            sendMessageToNumber(s, text, wantStatusReport);
        
        return true;
    }
            
    
    private boolean sendMessageToNumber(String number, String text, boolean wantStatusReport) {
        Connection conn;
        String sql = "INSERT INTO smsserver_out (recipient, text, status_report) VALUES(?, ?, ?)";
        
        String msg = normalizeString(text);
        
        try {
            if ( (conn = getConnection()) == null )
                return false;
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, number);
                stmt.setString(2, msg);
                stmt.setInt(3, wantStatusReport ? 1 : 0);
                
                stmt.executeUpdate();
                conn.commit();
            }
            
            return true;
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        }
        
        return false;
    }
    
    synchronized public int getPhoneCount(String status, String phone, String player) {
        Connection conn;
        String sql = "SELECT COUNT(*) FROM smscenter_phones ";
        
        sql += "WHERE 1 = 1";
        
        if (status != null && !status.isEmpty())
            sql += " AND smscenter_phones.status = ? "; // '" + status + "'";
        if (phone != null && !phone.isEmpty()) {
            sql += " AND smscenter_phones.phone LIKE ?";
        }
        if (player != null && !player.isEmpty())
            sql += " AND smscenter_phones.plNr = ?"; // " + player + " ";

        try {
            if ( (conn = getConnection()) == null )
                return 0;

            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                int idx = 1;
                
                if (status != null && !status.isEmpty())
                    stmt.setString(idx++, status);                
                if (phone != null && !phone.isEmpty())
                    stmt.setString(idx++, phone.replaceAll("\\*", "%").replaceAll("\\?", "_") + "%");                
                if (player != null && !player.isEmpty())
                    stmt.setInt(idx++, Integer.parseInt(player));

                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next())
                        return 0;
                    return rs.getInt(1);
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        }

        return 0;
    }
    
    synchronized public List<Object[]> getPhoneNumbers(int start, int count, String filter, int order) {
        return getPhoneNumbers(start, count, filter, null, null, order);
    }
    
    synchronized public List<Object[]> getPhoneNumbers(int start, int count, String status, String phone, String player, int order) {
        List<Object[]> list = new java.util.ArrayList<>();
        
        String sort = "smscenter_phones.id";
        
        String[] rows = {
            "smscenter_phones.id",
            "plNr",
            "phone",
            "smscenter_phones.status",
            null,
            null,
            "smscenter_phones.created"
        };
        
        if (order != 0 && Math.abs(order) <= rows.length && rows[Math.abs(order) - 1] != null) {
            sort = rows[Math.abs(order) - 1] + " " + (order > 0 ? " ASC" : " DESC");
        }
       
        Connection conn;
        String sql = 
                "SELECT * " +
                "  FROM ( " +
                "         SELECT ROW_NUMBER() OVER (ORDER BY " + sort + ") AS row, " +
                "         smscenter_phones.id, plNr, phone, smscenter_phones.status, " +
                "         (SELECT COUNT(*) FROM smsserver_out WHERE phone = recipient AND smsserver_out.status = 'S') AS count_success, " +
                "         (SELECT COUNT(*) FROM smsserver_out WHERE phone = recipient AND smsserver_out.status = 'F') AS count_failed, " +
                "         smscenter_phones.created " +
                "  FROM smscenter_phones ";
        sql += " WHERE 1 = 1 ";
        if (status != null && !status.isEmpty())
            sql += "AND smscenter_phones.status = ? "; // '" + status + "' ";
        if (phone != null && !phone.isEmpty()) {
            sql += "AND smscenter_phones.phone LIKE ? ";
        }
        if (player != null && !player.isEmpty())
            sql += "AND smscenter_phones.plNr = ? "; // " + player + " ";
        sql += ") AS phone ";

        sql += "WHERE 1 = 1 " ;
        if (count > 0)
            sql += " AND row BETWEEN ? AND ? "; //  + (start + 1) + " AND " + (start + count);
        else if (start > 0)
            sql += " AND row > ? "; //  + start;
        
        sql += " ORDER BY row ASC";
        
        try {
            if ( (conn = getConnection()) == null )
                return list;
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                int idx = 1;
                
                if (status != null && !status.isEmpty())
                    stmt.setString(idx++, status);                
                if (phone != null && !phone.isEmpty())
                    stmt.setString(idx++, phone.replaceAll("\\*", "%").replaceAll("\\?", "_") + "%");                
                if (player != null && !player.isEmpty())
                    stmt.setInt(idx++, Integer.parseInt(player));
                if (count > 0) {
                    stmt.setInt(idx++, start);
                    stmt.setInt(idx++, start + count);
                } else if (start > 0)
                    stmt.setInt(idx++, start);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        Object[] s = new Object[8];
                        s[0] = rs.getInt(1);
                        s[1] = rs.getInt(2);
                        s[2] = rs.getInt(3);
                        s[3] = rs.getString(4);
                        s[4] = rs.getString(5);
                        s[5] = rs.getInt(6);
                        s[6] = rs.getInt(7);
                        s[7] = rs.getTimestamp(8);

                        list.add(s);
                    }
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
            
        }

        return list;
    }
    
    
    synchronized public String getPhoneNumber(int startNr) {
        if (startNr <= 0)
            return null;
        
        Connection conn;
        String sql = "SELECT phone FROM smscenter_phones WHERE plNr = ? ";
        
        try {
            if ( (conn = getConnection()) == null )
                return null;
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, (startNr % 10000));
                try (ResultSet rs = stmt.executeQuery()) {                
                    return rs.next() ? rs.getString(1) : null;
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        }
        
        return null;
    }

    
    synchronized public boolean addPhoneNumber(int startNr, String phone, String status) {
        if (phone == null || status == null)
            return false;
        
        Connection conn;
        String sqlFind = "SELECT phone, status FROM smscenter_phones WHERE plNr = ?";
        String sqlInsert =
                "INSERT INTO smscenter_phones (plNr, phone, status) VALUES(?, ?, ?) ";
        String sqlUpdate =
                "UPDATE smscenter_phones SET phone = ?, status = ? WHERE plNr = ?";
        
        try {
            if ( (conn = getConnection()) == null )
                return false;
            
            String oldPhone = null;
            String oldStatus = null;
            
            try (PreparedStatement stmt = conn.prepareStatement(sqlFind)) {
                stmt.setInt(1, startNr);
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next())
                        oldPhone = getString(rs, 1);
                }
            } catch (SQLException ex) {
                
            }
            
            if (oldPhone == null) {
                try (PreparedStatement stmt = conn.prepareStatement(sqlInsert)) {
                    stmt.setInt(1, startNr);
                    stmt.setString(2, phone);
                    stmt.setString(3, status);

                    stmt.executeUpdate();
                    conn.commit();
                }

                return true;
            } else if (phone.equals(oldPhone) && status.equals(oldStatus))
                return true;
            else {
                try (PreparedStatement stmt = conn.prepareStatement(sqlUpdate)) {
                    stmt.setString(1, phone);
                    stmt.setString(2, status);
                    stmt.setInt(3, startNr);

                    stmt.executeUpdate();
                    conn.commit();
                }

                return true;                
            }
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        }
        
        return false;
    }

    
    synchronized public boolean updatePhoneNumber(int id, int startNr, String phone, String status) {
        Connection conn;
        String sql = "UPDATE smscenter_phones SET plNr = ?, phone = ?, status = ? WHERE id = ?";
        
        try {
            if ( (conn = getConnection()) == null )
                return false;
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, startNr);
                stmt.setString(2, phone);
                stmt.setString(3, status);
                stmt.setInt(4, id);
                
                stmt.executeUpdate();
                conn.commit();
            }
            
            return true;
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        }
        
        return false;
    }
    
    synchronized public boolean markPhoneNumber(int id) {
        Connection conn;
        String sql = "UPDATE smscenter_phones SET status = 'X' WHERE id = ?";
        
        try {
            if ( (conn = getConnection()) == null )
                return false;
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                stmt.executeUpdate();
                conn.commit();
            }
            
            return true;
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        }
        
        return false;
    }
    
    synchronized public boolean removePhoneNumber(int id) {
        Connection conn;
        String sql = "DELETE FROM smscenter_phones WHERE id = ?";
        
        try {
            conn = getConnection();
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                
                stmt.executeUpdate();
                conn.commit();
            }
            
            return true;
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        }                        
        
        return false;
    }
    
    
    synchronized public String getPhoneStatus(int startNr) {
        if (startNr <= 0)
            return null;
        
        Connection conn;
        String sql = "SELECT status FROM smscenter_phones WHERE plNr = ? ";
        
        try {
            if ( (conn = getConnection()) == null )
                return null;
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, (startNr % 10000));
                try (ResultSet rs = stmt.executeQuery()) {                
                    return rs.next() ? rs.getString(1) : null;
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        }
        
        return null;
    }

    
    final static java.nio.charset.Charset charsetUTF = java.nio.charset.Charset.forName("UTF-16LE");
    final static java.nio.charset.Charset charsetISO = java.nio.charset.Charset.forName("ISO-8859-1");

    private String getString(ResultSet rs, int idx) throws SQLException {
        return rs.getString(idx);
        
/*
        // Unter jTDS kann man nicht mehr zwischen varchar und nvarchar unterscheiden
        byte[] bytes = rs.getBytes(idx);
        if (bytes == null)
            return null;
        else if (bytes.length == 0)
            return "";
        else if (rs.getMetaData().getColumnType(idx) == java.sql.Types.NVARCHAR)
            return new String(bytes, charsetUTF);
        else
            return new String(bytes, charsetISO);
*/
    }
    
    
    synchronized public Match[] getUpdateSchedules(int start, int count, String status, String phone, String player) {
        List<Match> list = new java.util.ArrayList<>();
        
        String filterPhone = "";
        if (phone != null)
            filterPhone = phone + "%";
            
        String statusFilter = "";
        if (status != null) {
            if (status. indexOf('R') != -1)
                statusFilter += " AND scs.reminder = 1 ";
            if (status. indexOf('r') != -1)
                statusFilter += " AND scs.reminder = 0 ";
            if (status. indexOf('C') != -1)
                statusFilter += " AND scs.rescheduled = 1 ";
            if (status. indexOf('c') != -1)
                statusFilter += " AND scs.rescheduled = 0 ";
        }

        Connection conn;
        String sql = 
                "SELECT * FROM (SELECT *, ROW_NUMBER() OVER (ORDER BY scsID ASC) AS row FROM ( " +
                "       SELECT " +
                "       gr.cpID, gr.cpName, cpDesc, cpType, " +
                "       gr.grID, grName, grDesc, grStage, grModus, " +
                "       mt.mtNr, mtBestOf, mtTimeStamp, " +
                "       mtTable, mtDateTime, mtResA, mtResX, " +
                "       mtWalkOverA, mtWalkOverX, " +
                "       mtInjuredA, mtInjuredX, " +
                "       mtDisqualifiedA, mtDisqualifiedX, " +
                "       stA, stX, tmAtmID, tmXtmID, " +
                "       (plAplNr % 10000) AS plAplNr, plApsLast, plApsFirst, plAnaName, phoneA.phone AS plAphone, " +
                "       NULL AS plBplNr, NULL AS plBpsLast, NULL AS plBpsFirst, NULL AS plBnaName, NULL AS plBphone, " +
                "       (plXplNr % 10000) AS plXplNr, plXpsLast, plXpsFirst, plXnaName, phoneX.phone AS plXphone, " +
                "       NULL AS plYplNr, NULL AS plYpsLast, NULL AS plYpsFirst, NULL AS plYnaName, NULL AS plYphone, " +
                "       NULL AS scrID, scs.id AS scsID, scs.ts, scg.id AS scgID, scg.state_enabled AS scgEnabeld, scs.rescheduled, scs.reminder " +
                "       FROM MtSingleList mt " +
                "       INNER JOIN GrList gr ON mt.grID = gr.grID " +
                "       INNER JOIN CpList cp ON gr.cpID = cp.cpID " +
                "       INNER JOIN smscenter_schedules scs ON mt.mtNr = scs.mtNr " +
                "       INNER JOIN smscenter_groups scg ON mt.grID = scg.grID " +
                "       LEFT OUTER JOIN smscenter_phones phoneA ON (mt.plAplNr % 10000) = phoneA.plNr " +
                "       LEFT OUTER JOIN smscenter_phones phoneX ON (mt.plXplNr % 10000) = phoneX.plNr " +
                " WHERE cp.cpType = 1 " +
                (player == null ? "" : " AND (plAplNr = ? OR plXplNr = ?) ") +
                (filterPhone.isEmpty() ? "" : "AND (phoneA.phone LIKE ? OR phoneX.phone LIKE ?) ") +
                statusFilter +
                "UNION " +
                "       SELECT " +
                "       gr.cpID, gr.cpName, cpDesc, cpType, " +
                "       gr.grID, grName, grDesc, grStage, grModus, " +
                "       mt.mtNr, mtBestOf, mtTimeStamp, " +
                "       mtTable, mtDateTime, mtResA, mtResX, " +
                "       mtWalkOverA, mtWalkOverX, " +
                "       mtInjuredA, mtInjuredX, " +
                "       mtDisqualifiedA, mtDisqualifiedX, " +
                "       stA, stX, tmAtmID, tmXtmID, " +
                "       (plAplNr % 10000) AS plAplNr, plApsLast, plApsFirst, plAnaName, phoneA.phone AS plAphone, " +
                "       (plBplNr % 10000) AS plBplNr, plBpsLast, plBpsFirst, plBnaName, phoneB.phone AS plBphone, " +
                "       (plXplNr % 10000) AS plXplNr, plXpsLast, plXpsFirst, plXnaName, phoneX.phone AS plXphone, " +
                "       (plYplNr % 10000) AS plYplNr, plYpsLast, plYpsFirst, plYnaName, phoneY.phone AS plYphone, " +
                "       NULL AS scrID, scs.id AS scsID, scs.ts, scg.id AS scgID, scg.state_enabled AS scgEnabled, scs.rescheduled, scs.reminder " +
                "  FROM MtDoubleList mt " +
                "       INNER JOIN GrList gr ON mt.grID = gr.grID " +
                "       INNER JOIN CpList cp ON gr.cpID = cp.cpID " +
                "       INNER JOIN smscenter_schedules scs ON mt.mtNr = scs.mtNr " +
                "       INNER JOIN smscenter_groups scg ON mt.grID = scg.grID " +
                "       LEFT OUTER JOIN smscenter_phones phoneA ON (mt.plAplNr % 10000) = phoneA.plNr " +
                "       LEFT OUTER JOIN smscenter_phones phoneB ON (mt.plBplNr % 10000) = phoneB.plNr " +
                "       LEFT OUTER JOIN smscenter_phones phoneX ON (mt.plXplNr % 10000) = phoneX.plNr " +
                "       LEFT OUTER JOIN smscenter_phones phoneY ON (mt.plYplNr % 10000) = phoneY.plNr " +
                " WHERE (cp.cpType = 2 OR cp.cpType = 3) " +
                (player == null ? "" : "AND (plAplNr = ? OR plBplNr = ? OR plXplNr = ? OR plYplNr = ?) ") +
                (filterPhone.isEmpty() ? "" : "AND (phoneA.phone LIKE ? OR phoneB.phone LIKE ? OR phoneX.phone LIKE ? OR phoneY.phone LIKE ?) ") +
                statusFilter +
                " ) schedules ) schedulerows  " +
                " WHERE row BETWEEN " + (start + 1) + " AND " + (start + count) +
                " ORDER BY row ASC " +
                ""
            ;
        
        try {
            if ( (conn = getConnection()) == null )
                return new Match[0];
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                int idx = 0;
                if (player != null) {
                    int plNr = Integer.parseInt(player);
                    stmt.setInt(++idx, plNr);
                    stmt.setInt(++idx, plNr);
                }
                
                if (!filterPhone.isEmpty()) {
                    stmt.setString(++idx, filterPhone);
                    stmt.setString(++idx, filterPhone);
                }
                
                if (player != null) {
                    int plNr = Integer.parseInt(player);
                    stmt.setInt(++idx, plNr);
                    stmt.setInt(++idx, plNr);
                    stmt.setInt(++idx, plNr);
                    stmt.setInt(++idx, plNr);
                }

                if (!filterPhone.isEmpty()) {
                    stmt.setString(++idx, filterPhone);
                    stmt.setString(++idx, filterPhone);
                    stmt.setString(++idx, filterPhone);
                    stmt.setString(++idx, filterPhone);
                }
                
                try (ResultSet rs = stmt.executeQuery()) {                            
                    while (rs.next()) {
                        Match mt = new Match();
                        readMatch(mt, rs);
                        list.add(mt);
                    }
                }
            } catch (SQLException ex) {
                Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
            }
                
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        }
        
        return list.toArray(Match[]::new);
    }
    
    
    synchronized public Match[] getUpdateSchedules(int delay) {
        List<Match> list = new java.util.ArrayList<>();
        Connection conn;
        String sql = 
                "SELECT gr.cpID, gr.cpName, cpDesc, cpType, " +
                "       gr.grID, grName, grDesc, grStage, grModus, " +
                "       mt.mtNr, mtBestOf, mtTimeStamp, " +
                "       mtTable, mtDateTime, mtResA, mtResX, " +
                "       mtWalkOverA, mtWalkOverX, " +
                "       mtInjuredA, mtInjuredX, " +
                "       mtDisqualifiedA, mtDisqualifiedX, " +
                "       stA, stX, tmAtmID, tmXtmID, " +
                "       (plAplNr % 10000), plApsLast, plApsFirst, plAnaName, phoneA.phone AS plAphone, " +
                "       NULL AS plBplNr, NULL AS plBpsLast, NULL AS plBpsFirst, NULL AS plBnaName, NULL AS plBphone, " +
                "       (plXplNr % 10000), plXpsLast, plXpsFirst, plXnaName, phoneX.phone AS plXphone, " +
                "       NULL AS plYplNr, NULL AS plYpsLast, NULL AS plYpsFirst, NULL AS plYnaName, NULL AS plYphone, " +
                "       NULL, scs.id, scs.ts, scg.id, scg.state_enabled, scs.rescheduled, scs.reminder " +
                "  FROM MtSingleList mt " +
                "       INNER JOIN GrList gr ON mt.grID = gr.grID " +
                "       INNER JOIN CpList cp ON gr.cpID = cp.cpID " +
                "       INNER JOIN smscenter_schedules scs ON mt.mtNr = scs.mtNr " +
                "       INNER JOIN smscenter_groups scg ON mt.grID = scg.grID " +
                "       LEFT OUTER JOIN smscenter_phones phoneA ON (mt.plAplNr % 10000) = phoneA.plNr " +
                "       LEFT OUTER JOIN smscenter_phones phoneX ON (mt.plXplNr % 10000) = phoneX.plNr " +
                " WHERE cp.cpType = 1 AND scs.ts < DATEADD(second, " + (-delay) + ", CURRENT_TIMESTAMP) " +
                "UNION " +
                "SELECT gr.cpID, gr.cpName, cpDesc, cpType, " +
                "       gr.grID, grName, grDesc, grStage, grModus, " +
                "       mt.mtNr, mtBestOf, mtTimeStamp, " +
                "       mtTable, mtDateTime, mtResA, mtResX, " +
                "       mtWalkOverA, mtWalkOverX, " +
                "       mtInjuredA, mtInjuredX, " +
                "       mtDisqualifiedA, mtDisqualifiedX, " +
                "       stA, stX, tmAtmID, tmXtmID, " +
                "       (plAplNr % 10000), plApsLast, plApsFirst, plAnaName, phoneA.phone AS plAphone, " +
                "       (plBplNr % 10000), plBpsLast, plBpsFirst, plBnaName, phoneB.phone AS plBphone, " +
                "       (plXplNr % 10000), plXpsLast, plXpsFirst, plXnaName, phoneX.phone AS plXphone, " +
                "       (plYplNr % 10000), plYpsLast, plYpsFirst, plYnaName, phoneY.phone AS plYphone, " +
                "       NULL, scs.id, scs.ts, scg.id, scg.state_enabled, scs.rescheduled, scs.reminder " +
                "  FROM MtDoubleList mt " +
                "       INNER JOIN GrList gr ON mt.grID = gr.grID " +
                "       INNER JOIN CpList cp ON gr.cpID = cp.cpID " +
                "       INNER JOIN smscenter_schedules scs ON mt.mtNr = scs.mtNr " +
                "       INNER JOIN smscenter_groups scg ON mt.grID = scg.grID " +
                "       LEFT OUTER JOIN smscenter_phones phoneA ON (mt.plAplNr % 10000) = phoneA.plNr " +
                "       LEFT OUTER JOIN smscenter_phones phoneB ON (mt.plBplNr % 10000) = phoneB.plNr " +
                "       LEFT OUTER JOIN smscenter_phones phoneX ON (mt.plXplNr % 10000) = phoneX.plNr " +
                "       LEFT OUTER JOIN smscenter_phones phoneY ON (mt.plYplNr % 10000) = phoneY.plNr " +
                " WHERE (cp.cpType = 2 OR cp.cpType = 3) AND scs.ts < DATEADD(second, " + (-delay) + ", CURRENT_TIMESTAMP) " +
                "ORDER BY mtDateTime, mtTable ";
        try {
            if ( (conn = getConnection()) == null )
                return new Match[0];
            
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {                            
                while (rs.next()) {
                    Match mt = new Match();
                    readMatch(mt, rs);
                    list.add(mt);
                }
            } catch (SQLException ex) {
                Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
            }
                
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        }
        
        return list.toArray(Match[]::new);
    }
    
    synchronized public Match[] getSchedulesFor(Set<Integer> plSet, int tsBefore, int tsAfter) {
        if (plSet.isEmpty())
            return new Match[0];
        
        StringBuilder sb = new StringBuilder();
        for (int i : plSet)
            sb.append(", ").append(i);
        
        
        List<Match> list = new java.util.ArrayList<>();
        Connection conn;
        String sql = 
                "SELECT gr.cpID, gr.cpName, cpDesc, cpType, " +
                "       gr.grID, grName, grDesc, grStage, grModus, " +
                "       mt.mtNr, mtBestOf, mtTimeStamp, " +
                "       mtTable, mtDateTime, mtResA, mtResX, " +
                "       mtWalkOverA, mtWalkOverX, " +
                "       mtInjuredA, mtInjuredX, " +
                "       mtDisqualifiedA, mtDisqualifiedX, " +
                "       stA, stX, tmAtmID, tmXtmID, " +
                "       (plAplNr % 10000), plApsLast, plApsFirst, plAnaName, phoneA.phone AS plAphone, " +
                "       NULL AS plBplNr, NULL AS plBpsLast, NULL AS plBpsFirst, NULL AS plBnaName, NULL AS plBphone, " +
                "       (plXplNr % 10000), plXpsLast, plXpsFirst, plXnaName, phoneX.phone AS plXphone, " +
                "       NULL AS plYplNr, NULL AS plYpsLast, NULL AS plYpsFirst, NULL AS plYnaName, NULL AS plYphone, " +
                "       NULL, scs.id, scs.ts, scg.id, scg.state_enabled, scs.rescheduled, scs.reminder  " +
                "  FROM MtSingleList mt " +
                "       INNER JOIN GrList gr ON mt.grID = gr.grID " +
                "       INNER JOIN CpList cp ON gr.cpID = cp.cpID " +
                "       INNER JOIN smscenter_groups scg ON mt.grID = scg.grID " +
                "       LEFT OUTER JOIN smscenter_schedules scs ON mt.mtNr = scs.mtNr " +
                "       LEFT OUTER JOIN smscenter_phones phoneA ON (mt.plAplNr % 10000) = phoneA.plNr " +
                "       LEFT OUTER JOIN smscenter_phones phoneX ON (mt.plXplNr % 10000) = phoneX.plNr " +
                " WHERE cp.cpType = 1 AND (" +
                            "scs.ts IS NULL OR " +
                            "scs.ts < DATEADD(second, -" + tsBefore + ", CURRENT_TIMESTAMP) OR " + 
                            "scs.ts > DATEADD(second, " + tsAfter + ", CURRENT_TIMESTAMP) " +
                         ") AND " +
                "       ((plAplNr % 10000) IN (" + sb.substring(2) + ") OR (plXplNr % 10000) IN (" + sb.substring(2) + ")) " +
                "UNION " +
                "SELECT gr.cpID, gr.cpName, cpDesc, cpType, " +
                "       gr.grID, grName, grDesc, grStage, grModus, " +
                "       mt.mtNr, mtBestOf, mtTimeStamp, " +
                "       mtTable, mtDateTime, mtResA, mtResX, " +
                "       mtWalkOverA, mtWalkOverX, " +
                "       mtInjuredA, mtInjuredX, " +
                "       mtDisqualifiedA, mtDisqualifiedX, " +
                "       stA, stX, tmAtmID, tmXtmID, " +
                "       (plAplNr % 10000), plApsLast, plApsFirst, plAnaName, phoneA.phone AS plAphone, " +
                "       (plBplNr % 10000), plBpsLast, plBpsFirst, plBnaName, phoneB.phone AS plBphone, " +
                "       (plXplNr % 10000), plXpsLast, plXpsFirst, plXnaName, phoneX.phone AS plXphone, " +
                "       (plYplNr % 10000), plYpsLast, plYpsFirst, plYnaName, phoneY.phone AS plYphone, " +
                "       NULL, scs.id, scs.ts, scg.id, scg.state_enabled, scs.rescheduled, scs.reminder  " +
                "  FROM MtDoubleList mt " +
                "       INNER JOIN GrList gr ON mt.grID = gr.grID " +
                "       INNER JOIN CpList cp ON gr.cpID = cp.cpID " +
                "       INNER JOIN smscenter_groups scg ON mt.grID = scg.grID " +
                "       LEFT OUTER JOIN smscenter_schedules scs ON mt.mtNr = scs.mtNr " +
                "       LEFT OUTER JOIN smscenter_phones phoneA ON (mt.plAplNr % 10000) = phoneA.plNr " +
                "       LEFT OUTER JOIN smscenter_phones phoneB ON (mt.plBplNr % 10000) = phoneB.plNr " +
                "       LEFT OUTER JOIN smscenter_phones phoneX ON (mt.plXplNr % 10000) = phoneX.plNr " +
                "       LEFT OUTER JOIN smscenter_phones phoneY ON (mt.plYplNr % 10000) = phoneY.plNr " +
                " WHERE (cp.cpType = 2 OR cp.cpType = 3) AND ( " +
                            "scs.ts IS NULL OR " +
                            "scs.ts < DATEADD(second, -" + tsBefore + ", CURRENT_TIMESTAMP) OR " + 
                            "scs.ts > DATEADD(second, " + tsAfter + ", CURRENT_TIMESTAMP) " +
                         ") AND " +
                "       ((plAplNr % 10000) IN (" + sb.substring(2) + ") OR (plBplNr % 10000) IN (" + sb.substring(2) + ") OR " +
                "        (plXplNr % 10000) IN (" + sb.substring(2) + ") OR (plYplNr % 10000) IN (" + sb.substring(2) + ")) " +
                "ORDER BY mtDateTime, mtTable ";
        try {
            if ( (conn = getConnection()) == null )
                return new Match[0];
            
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {                            
                while (rs.next()) {
                    Match mt = new Match();
                    readMatch(mt, rs);
                    list.add(mt);
                }
            } catch (SQLException ex) {
                Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
            }
                
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        }
        
        return list.toArray(Match[]::new);
    }
    
    synchronized public boolean addUpdateScheduleGroup(int grID) {
        Connection conn;
        String sqlDelete =
                "DELETE FROM smscenter_schedules WHERE mtNr IN (SELECT mtNr FROM MtRec WHERE grID = ?)";
        String sqlInsert = 
                "INSERT INTO smscenter_schedules (mtNr) " +
                "SELECT mtNr FROM MtRec WHERE grID = ? AND mtNr NOT IN (SELECT mtNr FROM smscenter_schedules) ";
        
        try {
            if ( (conn = getConnection()) == null )
                return false;
            
            try (PreparedStatement stmt = conn.prepareStatement(sqlDelete)) {
                stmt.setInt(1, grID);
                
                stmt.executeUpdate();
            }
            
            try (PreparedStatement stmt = conn.prepareStatement(sqlInsert)) {
                stmt.setInt(1, grID);
                
                stmt.executeUpdate();
                conn.commit();
            }
            
            return true;
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        }                        
        
        return false;
    }
    
    
    synchronized public boolean updateScheduleTimestamp(int id, long ts) {
        Connection conn;
        String sql = 
                "UPDATE smscenter_schedules SET ts = ?, rescheduled = 0, reminder = 1 WHERE id = ?";
        
        try {
            if ( (conn = getConnection()) == null )
                return false;
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setTimestamp(1, new java.sql.Timestamp(ts));
                stmt.setInt(2, id);
                
                stmt.executeUpdate();
                conn.commit();
            }
            
            return true;
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        }                        
        
        return false;
    }
    
    
    synchronized public boolean removeUpdateSchedule(int id) {
        Connection conn;
        String sql = "DELETE FROM smscenter_schedules WHERE id = ?";
        
        try {
            if ( (conn = getConnection()) == null )
                return false;
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                
                stmt.executeUpdate();
                conn.commit();
            }
            
            return true;
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        }                        
        
        return false;
    }
    
    
    synchronized public Match[] getUpdateResults(int delay) {
        List<Match> list = new java.util.ArrayList<>();
        Connection conn;
        String sql = 
                "SELECT gr.cpID, gr.cpName, cpDesc, cpType, " +
                "       gr.grID, grName, grDesc, grStage, grModus, " +
                "       mt.mtNr, mtBestOf, mtTimeStamp, " +
                "       mtTable, mtDateTime, mtResA, mtResX, " +
                "       mtWalkOverA, mtWalkOverX, " +
                "       mtInjuredA, mtInjuredX, " +
                "       mtDisqualifiedA, mtDisqualifiedX, " +
                "       stA, stX, tmAtmID, tmXtmID, " +
                "       plAplNr, plApsLast, plApsFirst, plAnaName, phoneA.phone AS plAphone, " +
                "       NULL AS plBplNr, NULL AS plBpsLast, NULL AS plBpsFirst, NULL AS plBnaName, NULL AS plBphone, " +
                "       plXplNr, plXpsLast, plXpsFirst, plXnaName, phoneX.phone AS plXphone, " +
                "       NULL AS plYplNr, NULL AS plYpsLast, NULL AS plYpsFirst, NULL AS plYnaName, NULL AS plYphone, " +
                "       scr.id, NULL, scr.ts, scg.id, scg.state_enabled, NULL, NULL " +
                "  FROM MtSingleList mt " +
                "       INNER JOIN GrList gr ON mt.grID = gr.grID " +
                "       INNER JOIN CpList cp ON gr.cpID = cp.cpID " +
                "       INNER JOIN smscenter_results scr ON mt.mtNr = scr.mtNr " +
                "       INNER JOIN smscenter_groups scg ON mt.grID = scg.grID " +
                "       LEFT OUTER JOIN smscenter_phones phoneA ON mt.plAplNr = phoneA.plNr " +
                "       LEFT OUTER JOIN smscenter_phones phoneX ON mt.plXplNr = phoneX.plNr " +
                " WHERE cp.cpType = 1 AND scr.ts < DATEADD(second, " + (-delay) + ", CURRENT_TIMESTAMP) " +
                "UNION " +
                "SELECT gr.cpID, cp.cpName, cpDesc, cpType, " +                
                "       gr.grID, grName, grDesc, grStage, grModus, " +
                "       mt.mtNr, mtBestOf, mtTimeStamp, " +
                "       mtTable, mtDateTime, mtResA, mtResX, " +
                "       mtWalkOverA, mtWalkOverX, " +
                "       mtInjuredA, mtInjuredX, " +
                "       mtDisqualifiedA, mtDisqualifiedX, " +
                "       stA, stX, tmAtmID, tmXtmID, " +
                "       plAplNr, plApsLast, plApsFirst, plAnaName, phoneA.phone AS plAphone, " +
                "       plBplNr, plBpsLast, plBpsFirst, plBnaName, phoneB.phone AS plBphone, " +
                "       plXplNr, plXpsLast, plXpsFirst, plXnaName, phoneX.phone AS plXphone, " +
                "       plYplNr, plYpsLast, plYpsFirst, plYnaName, phoneY.phone AS plYphone, " +
                "       scr.id, NULL, scr.ts, scg.id, scg.state_enabled, NULL, NULL " +
                "  FROM MtDoubleList mt " +
                "       INNER JOIN GrList gr ON mt.grID = gr.grID " +
                "       INNER JOIN CpList cp ON gr.cpID = cp.cpID " +
                "       INNER JOIN smscenter_results scr ON mt.mtNr = scr.mtNr " +
                "       INNER JOIN smscenter_groups scg ON mt.grID = scg.grID " +
                "       LEFT OUTER JOIN smscenter_phones phoneA ON mt.plAplNr = phoneA.plNr " +
                "       LEFT OUTER JOIN smscenter_phones phoneB ON mt.plBplNr = phoneB.plNr " +
                "       LEFT OUTER JOIN smscenter_phones phoneX ON mt.plXplNr = phoneX.plNr " +
                "       LEFT OUTER JOIN smscenter_phones phoneY ON mt.plYplNr = phoneY.plNr " +
                " WHERE (cp.cpType = 2 OR cp.cpType = 3) AND scr.ts < DATEADD(second, " + (-delay) + ", CURRENT_TIMESTAMP) " +
                "ORDER BY mtDateTime, mtTable ";
        try {
            if ( (conn = getConnection()) == null )
                return new Match[0];
            
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {                            
                while (rs.next()) {
                    Match mt = new Match();
                    readMatch(mt, rs);
                    list.add(mt);
                }
            } catch (SQLException ex) {
                Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
            }
                
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        }
        
        return list.toArray(Match[]::new);
    }
    
    
    synchronized public boolean addUpdateResultGroup(int grID) {
        Connection conn;
        String sqlDelete =
                "DELETE FROM smscenter_results WHERE mtNr IN (SELECT mtNr FROM MtRec WHERE grID = ?)";
        String sqlInsert = 
                "INSERT INTO smscenter_results (mtNr) " +
                "SELECT mtNr FROM MtRec WHERE grID = ? AND mtNr NOT IN (SELECT mtNr FROM smscenter_results) ";
        
        try {
            if ( (conn = getConnection()) == null )
                return false;
            
            try (PreparedStatement stmt = conn.prepareStatement(sqlDelete)) {
                stmt.setInt(1, grID);
                
                stmt.executeUpdate();
            }
            
            try (PreparedStatement stmt = conn.prepareStatement(sqlInsert)) {
                stmt.setInt(1, grID);
                
                stmt.executeUpdate();
                conn.commit();
            }
            
            return true;
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        }                        
        
        return false;
    }
    
    
    synchronized public boolean removeUpdateResult(int id) {
        Connection conn;
        String sql = "DELETE FROM smscenter_results WHERE id = ?";
        
        try {
            if ( (conn = getConnection()) == null )
                return false;
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                
                stmt.executeUpdate();
                conn.commit();
            }
            
            return true;
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        }                        
        
        return false;
    }
    
    
    synchronized public boolean addUpdatePositionGroup(int grID) {
        Connection conn;
        String sqlDelete = 
                "DELETE FROM smscenter_positions WHERE stID IN (SELECT stID FROM StRec WHERE grID = ?)";
        String sqlInsert = 
                "INSERT INTO smscenter_positions (stID) " +
                "SELECT stID FROM StRec WHERE grID = ? AND stID NOT IN (SELECT stID FROM smscenter_positions) ";
        
        try {
            if ( (conn = getConnection()) == null )
                return false;
            
            try (PreparedStatement stmt = conn.prepareStatement(sqlDelete)) {
                stmt.setInt(1, grID);
                stmt.executeUpdate();
            }
            
            try (PreparedStatement stmt = conn.prepareStatement(sqlInsert)) {
                stmt.setInt(1, grID);
                
                stmt.executeUpdate();
                conn.commit();
            }
            
            return true;
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        }                        
        
        return false;
    }
    
    
    synchronized public Competition[] getCompetitions() {
        List<Competition> list = new java.util.ArrayList<>();
        
        Connection conn;
        String sql = "SELECT cpID, cpName, cpDesc, cpType FROM CpList ORDER BY cpDesc";
        
        try {
            if ( (conn = getConnection()) == null )
                return new Competition[0];
            
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    int idx = 0;
                    Competition cp = new Competition();
                    cp.cpID = rs.getInt(++idx);
                    cp.cpName = getString(rs, ++idx);
                    cp.cpDesc = getString(rs, ++idx);
                    cp.cpType = rs.getInt(++idx);
                    
                    list.add(cp);
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return list.toArray(Competition[]::new);
    }
    
    synchronized public Group[] getGroups(Competition cp) {
        List<Group> list = new java.util.ArrayList<>();
        
        if (cp == null)
            return new Group[0];
        
        Connection conn;
        String sql = 
                "SELECT " +
                "       gr.cpID, cp.cpName, cpDesc, cpType, " +
                "       grID, grName, grDesc, grStage, grModus " +
                "  FROM GrList gr INNER JOIN CpList cp ON gr.cpID = cp.cpId " +
                " WHERE gr.cpID = ? ORDER BY grStage, grName";
        
        try {
            if ( (conn = getConnection()) == null )
                return new Group[0];
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                
                stmt.setInt(1, cp.cpID);
                try(ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        int idx = 0;
                        Group gr = new Group();
                        gr.cp.cpID = rs.getInt(++idx);
                        gr.cp.cpName = getString(rs, ++idx);
                        gr.cp.cpDesc = getString(rs, ++idx);
                        gr.cp.cpType = rs.getInt(++idx);
                        gr.grID = rs.getInt(++idx);
                        gr.grName = getString(rs, ++idx);
                        gr.grDesc = getString(rs, ++idx);
                        gr.grStage = getString(rs, ++idx);
                        gr.grModus = rs.getInt(++idx);

                        list.add(gr);
                    }
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        }
        
        return list.toArray(Group[]::new);
    }
    
    
    /**
     * List of all groups as array [scgID, grID, sent, manual, enabled]
     * @return array [scgID, grID, sent, manual, enabled]
     */
    synchronized public List<Integer[]> getGroups() {
        List<Integer[]> list = new java.util.ArrayList<>();
        
        Connection conn;
        String sql = "SELECT id, grID, state_sent, state_manual, state_enabled FROM smscenter_groups ";
        
        try {
            if ( (conn = getConnection()) == null )
                return list;
            
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    int id = rs.getInt(1);
                    int grID = rs.getInt(2);
                    int sent = rs.getInt(3);
                    int manual = rs.getInt(4);
                    int enabled = rs.getInt(5);
                    
                    list.add(new Integer[] {id, grID, sent, manual, enabled});
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        }
        
        return list;
    }
    
    
    synchronized public boolean setGroupEnabled(int grID) {
        Connection conn;
        String sql = "UPDATE smscenter_groups SET state_manual = 1, state_enabled = 1 WHERE grID = ?";
        
        try {
            if ( (conn = getConnection()) == null )
                return false;
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, grID);
                
                stmt.executeUpdate();
                conn.commit();
            }
            
            return true;
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        }                
        
        return false;
    }
    
    
    synchronized public boolean setGrougDisabled(int grID) {
        Connection conn;
        String sql = "UPDATE smscenter_groups SET state_manual = 1, state_enabled = 0 WHERE grID = ?";
        
        try {
            if ( (conn = getConnection()) == null )
                return false;
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, grID);
                
                stmt.executeUpdate();
                conn.commit();
            }
            
            return true;
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        }                
        
        return false;
    }
    
    
    synchronized public boolean setGroupAuto(int grID) {
        Connection conn;
        String sql = 
                "UPDATE smscenter_groups " +
                "   SET state_manual = 0, " +
                "       state_enabled = (SELECT grPublished FROM GrList WHERE grID = ?) WHERE grID = ?";
        
        try {
            if ( (conn = getConnection()) == null )
                return false;
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, grID);
                stmt.setInt(2, grID);
                
                stmt.executeUpdate();
                conn.commit();
            }
            
            return true;
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        }                
        
        return false;
    }
    
    
    synchronized public boolean setGroupSent(int grID) {
        Connection conn;
        String sql = "UPDATE smscenter_groups SET state_sent = 1 WHERE grID = ?";
        
        try {
            if ( (conn = getConnection()) == null )
                return false;
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, grID);
                
                stmt.executeUpdate();
                conn.commit();
            }
            
            return true;
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        }                
        
        return false;
    }
    
    
    synchronized public boolean clearGroupSent(int grID) {
        Connection conn;
        String sql = "UPDATE smscenter_groups SET state_sent = 0 WHERE grID = ?";
        
        try {
            if ( (conn = getConnection()) == null )
                return false;
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, grID);
                
                stmt.executeUpdate();
                conn.commit();
            }
            
            return true;
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        }                
        
        return false;
    }
    
    
    synchronized public boolean isGroupCombined(int grID) {
        Connection conn;
        String sql = "SELECT COUNT(DISTINCT mtDateTime) FROM MtList WHERE grID = ?";
        
        try {
            if ( (conn = getConnection()) == null )
                return false;
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, grID);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() && rs.getInt(1) == 1;
                }
            } 
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);                        
        }
        
        return false;
    }
    
    
    synchronized public boolean isGroupFinished(int grID) {
        Connection conn;
        String sql = 
                "SELECT COUNT(*) FROM MtList " +
                " WHERE mtResA = 0 AND mtResX = 0 AND mtTable IS NOT NULL AND " +
                "       tmA IS NOT NULL AND tmX IS NOT NULL AND " +
                "       mtWalkOverA = 0 AND mtWalkOverX = 0 AND " +
                "       mtInjuredA = 0 AND mtInjuredX = 0 AND " +
                "       mtDisqualifiedA = 0 AND mtDisqualifiedX = 0 AND " +
                "       mtDateTime IS NOT NULL AND grID = ?";
        
        try {
            if ( (conn = getConnection()) == null )
                return false;
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, grID);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() && rs.getInt(1) == 0;
                }
            } 
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);                        
        }
        
        return false;        
    }
    
    
    synchronized public boolean hasGropStarted(int grID) {
        Connection conn;
        String sql = 
                "SELECT COUNT(*) FROM MtList " +
                " WHERE (mtResA > 0 OR mtResX > 0) AND grID = ?";
        
        try {
            if ( (conn = getConnection()) == null )
                return false;
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, grID);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    return rs.next() && rs.getInt(1) > 0;
                }
            } 
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);                        
        }
        
        return false;        
    }
    
    
    synchronized public GroupEntry[] getUpdatePositions(int delay) {
        List<GroupEntry> list = new java.util.ArrayList<>();
        Connection conn = null;
        try {
            if ( (conn = getConnection()) == null )
                return new GroupEntry[0];
            
            String sql = 
                "SELECT cp.cpID, cp.cpName, cpDesc, cpType, " +
                "       gr.grID, grName, grDesc, grStage, grModus, " +
                "       st.plNr AS plplNr, psLast AS plpsLast, psFirst AS plpsFirst, naName AS plnaName, phonePl.phone as plphone, " +
                "       NULL AS bdplNr, NULL AS bdpsLast, NULL AS bdpsFirst, NULL AS bdnaName, NULL AS bdphone,  " +
                "       st.stID, stNr, stPos, scp.id, scp.ts, scg.state_enabled " +
                "  FROM StSingleList st " +
                "       INNER JOIN GrList gr ON st.grID = gr.grID " +
                "       INNER JOIN CpList cp ON gr.cpID = cp.cpID AND cp.cpType = 1 " +
                "       INNER JOIN smscenter_groups scg ON gr.grID = scg.grID " +
                "       INNER JOIN smscenter_positions scp ON st.stID = scp.stID " +
                "       LEFT OUTER JOIN smscenter_phones phonepl ON st.plNr = phonepl.plNr " +
                " WHERE scp.ts < DATEADD(second, " + (-delay) + ", CURRENT_TIMESTAMP) " +
                "UNION " +
                "SELECT cp.cpID, cp.cpName, cpDesc, cpType, " +
                "       gr.grID, grName, grDesc, grStage, grModus, " +
                "       st.plplNr, plpsLast, plpsFirst, plnaName, phonePl.phone AS plphone, " +
                "       st.bdplNr, bdpsLast, bdpsFirst, bdnaName, phoneBd.phone AS bdphone, " +
                "       st.stID, stNr, stPos, scp.id, scp.ts, scg.state_enabled " +
                "  FROM StDoubleList st " +
                "       INNER JOIN GrList gr ON st.grID = gr.grID " +
                "       INNER JOIN CpList cp ON gr.cpID = cp.cpID AND (cp.cpType = 2 OR cp.cpType = 3) " +
                "       INNER JOIN smscenter_groups scg ON gr.grID = scg.grID " +
                "       INNER JOIN smscenter_positions scp ON st.stID = scp.stID " +
                "       LEFT OUTER JOIN smscenter_phones phonepl ON st.plplNr = phonepl.plNr " +
                "       LEFT OUTER JOIN smscenter_phones phonebd ON st.bdplNr = phonebd.plNr " +
                " WHERE scp.ts < DATEADD(second, " + (-delay) + ", CURRENT_TIMESTAMP) ";
            
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    GroupEntry entry = new GroupEntry();
                    int idx = 0;
                    entry.gr.cp.cpID = rs.getInt(++idx);
                    entry.gr.cp.cpName = getString(rs, ++idx);
                    entry.gr.cp.cpDesc = getString(rs, ++idx);
                    entry.gr.cp.cpType = rs.getInt(++idx);
                    entry.gr.grID = rs.getInt(++idx);
                    entry.gr.grName = getString(rs, ++idx);
                    entry.gr.grDesc = getString(rs, ++idx);
                    entry.gr.grStage = getString(rs, ++idx);
                    entry.gr.grModus = rs.getInt(++idx);
                    entry.pl = new Player();
                    entry.pl.plNr = rs.getInt(++idx);
                    entry.pl.psLast = getString(rs, ++idx);
                    entry.pl.psFirst = getString(rs, ++idx);
                    entry.pl.naName = getString(rs, ++idx);
                    entry.pl.phone = getString(rs, ++idx);
                    if (entry.gr.cp.cpType == 2 || entry.gr.cp.cpType == 3) {
                        entry.bd = new Player();
                        entry.bd.plNr = rs.getInt(++idx);
                        entry.bd.psLast = getString(rs, ++idx);
                        entry.bd.psFirst = getString(rs, ++idx);
                        entry.bd.naName = getString(rs, ++idx);
                        entry.bd.phone = getString(rs, ++idx);
                    } else {
                        idx += 5;
                    }
                    
                    entry.stID = rs.getInt(++idx);
                    entry.stNr = rs.getInt(++idx);
                    entry.stPos = rs.getInt(++idx);
                    entry.scpID = rs.getInt(++idx);
                    entry.ts = rs.getTimestamp(++idx);
                    entry.scgEnabled = rs.getBoolean(++idx);
                    
                    list.add(entry);
                }
            } 
                    
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);                        
        }
            
        return list.toArray(GroupEntry[]::new);
    }
    
    
    synchronized public boolean removeUpdatePosition(int id) {
        Connection conn;
        String sql = "DELETE FROM smscenter_positions WHERE id = ?";
        
        try {
            if ( (conn = getConnection()) == null )
                return false;
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, id);
                
                stmt.executeUpdate();
                conn.commit();
            }
            
            return true;
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        }                        
        
        return false;
    }
    
    
    synchronized public boolean isStartNr(int nr) {
        Connection conn = null;
        
        try {
            if ( (conn = getConnection()) == null )
                return false;
            
            String sql = "SELECT plNr FROM PlList WHERE plNr = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, nr);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    return (rs.next() && rs.getInt(1) > 0 && !rs.wasNull());
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);                        
        }        
        
        return false;
    }
    
    synchronized public int getStartNrFromExternId(String externId) {
        Connection conn = null;
        
        try {
            if ( (conn = getConnection()) == null )
                return 0;
            
            String sql = "SELECT plNr FROM PlList WHERE plExtId = ?";
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, externId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    int startNr = rs.next() ? rs.getInt(1) : 0;
                    if (rs.wasNull())
                        startNr = 0;

                    return startNr;
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);                        
        }
        
        return 0;
    }
    
    synchronized public Player getPlayer(int startNr) {
        Connection conn = null;
          
        try {
            if ( (conn = getConnection()) == null )
                return null;
            
            String sql = 
                "SELECT pl.plNr, psLast, psFirst, naName, phone " +
                " FROM PlList pl LEFT OUTER JOIN smscenter_phones sms ON pl.plNr = sms.plNr " +
                "WHERE pl.plNr = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setInt(1, startNr);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (!rs.next())
                        return null;
                    
                    Player pl = new Player();
                    pl.plNr = rs.getInt(1);
                    pl.psLast = getString(rs, 2);
                    pl.psFirst = getString(rs, 3);
                    pl.naName = getString(rs, 4);
                    pl.phone = getString(rs, 5);
                    
                    return pl;
                }
            }
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);                        
        }
      
        return null;
    }
    
    
    synchronized public String getWelcomeMsg() {
        Connection conn = null;
          
        try {
            if ( (conn = getConnection()) == null )
                return null;
            
            String sql = "SELECT welcome FROM smscenter_settings";
            
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                rs.next();
                return rs.getString(1);
            }
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);                        
        }
      
        return null;        
    }
    
    
    synchronized public void setWelcomeMessage(String msg) {
        Connection conn = null;
          
        try {
            if ( (conn = getConnection()) == null )
                return;
            
            String sql = "UPDATE smscenter_settings SET welcome = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, msg);
                stmt.execute();
                conn.commit();
            }
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);                        
        }
    }

    
    synchronized public String getSponsorLine() {
        Connection conn = null;
          
        try {
            if ( (conn = getConnection()) == null )
                return null;
            
            String sql = "SELECT sponsor FROM smscenter_settings";
            
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                rs.next();
                return rs.getString(1);
            }
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);                        
        }
      
        return null;        
    }
    
    
    synchronized public void setSponsorLine(String line) {
        Connection conn = null;
          
        try {
            if ( (conn = getConnection()) == null )
                return;
            
            String sql = "UPDATE smscenter_settings SET sponsor = ?";
            
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, line);
                stmt.execute();
                conn.commit();
            }
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);                        
        }
    }


    synchronized public long getSettingsTimestamp() {
        Connection conn = null;
          
        try {
            if ( (conn = getConnection()) == null )
                return 0;
            
            String sql = "SELECT ts FROM smscenter_settings";
            
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                rs.next();
                java.sql.Date date = rs.getDate(1);
                return date == null ? 0 : date.getTime();
            }
        } catch (SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);                        
        }
      
        return 0;        
    }
    
    // If you change current version update smscenter_settings, too
    final int CURRENT_VERSION = 8;
            
    synchronized public boolean initializeDatabase(Properties props) {
        Connection conn = null;
        
        try {
            if ( (conn = getConnection()) == null )
                return false;

            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT * FROM smsserver_out")) {
                return updateDatabase(props);
                                
            } catch (SQLException ex) {
                
            }
            
            ScriptRunner runner = new ScriptRunner(conn, false, true);
            
            runner.setDelimiter("GO", true);
            
            String[] scripts = {
                "smscenter_settings",
                "smscenter_groups", 
                "smscenter_phones", 
                "smscenter_positions",
                "smscenter_results", 
                "smscenter_schedules", 
                "smsserver_calls", 
                "smsserver_in", 
                "smsserver_out",
                "smscenter_role",
                "grSmsInsertTrigger",
                "grSmsUpdateTrigger",
                "mtSmsUpdateTrigger", 
                "stSmsUpdateTrigger"
            };
            
            for (String script : scripts) {
                Reader reader = new java.io.InputStreamReader(getClass().getResourceAsStream("/smscenter/database/scripts/" + script + ".sql"));

                runner.runScript(reader);                        
            }       
            
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("UPDATE smscenter_settings SET version = " + CURRENT_VERSION);
            }
            
            conn.commit();
        } catch (IOException | SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
            try {
                if (conn != null)
                    conn.rollback();
            } catch (SQLException e) {
                
            }
            
            return false;
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);  
            
            return false;
        }
        
        return true;
    }
    
    
    synchronized public boolean updateDatabase(Properties props) {
        Connection conn = null;
        
        try {
            if ( (conn = getConnection()) == null )
                return false;

            int version = 0;
            List<String> updateScripts = new java.util.ArrayList<>();
            
            try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT version FROM smscenter_settings")) {
                rs.next();
                version = rs.getInt(1);
            } catch (SQLException ex) {

            }
            
            if (version == CURRENT_VERSION)
                return true;
                            
            for (int v = version; v < CURRENT_VERSION; v++)
                updateScripts.add("smscenter_update_" + v + "_to_" + (v+1));

            ScriptRunner runner = new ScriptRunner(conn, false, true);

            runner.setDelimiter("GO", true);

            for (String script : updateScripts) {
                Reader reader = new java.io.InputStreamReader(getClass().getResourceAsStream("/smscenter/database/scripts/" + script + ".sql"));
                runner.runScript(reader);                        
            }
            
            // Was: Update content (add welcome and sponsor from properties to database)
            
            // Update version
            try (Statement stmt = conn.createStatement()) {
                stmt.execute("UPDATE smscenter_settings SET version = " + CURRENT_VERSION);
            }
            
            conn.commit();
        } catch (IOException | SQLException ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);
            try {
                if (conn != null)
                    conn.rollback();
            } catch (SQLException e) {
                
            }
            
            return false;
        } catch (Exception ex) {
            Logger.getLogger(Database.class.getName()).log(Level.SEVERE, null, ex);            
            return false;
        }
        
        return true;
    }
    
    
    private Connection getConnection() throws SQLException {
        try {
            if (connection != null)
                connection.getMetaData();
        } catch (Exception e) {
            connection = null;
        }
        
        if (connection == null) {
            if (connectString == null)
                return null;
            
            if (!validConnection)
                return null;
            
            connection = DriverManager.getConnection(connectString);            
            connection.setAutoCommit(false);
        }
        
        return connection;
    }
    
    
    private void readMatch(Match mt, ResultSet rs) throws SQLException {
        int idx = 0;
        mt.gr.cp.cpID = rs.getInt(++idx);
        mt.gr.cp.cpName = getString(rs, ++idx);
        mt.gr.cp.cpDesc = getString(rs, ++idx);
        mt.gr.cp.cpType = rs.getInt(++idx);
        mt.gr.grID = rs.getInt(++idx);
        mt.gr.grName = getString(rs, ++idx);
        mt.gr.grDesc = getString(rs, ++idx);
        mt.gr.grStage = getString(rs, ++idx);
        mt.gr.grModus = rs.getInt(++idx);
        mt.mtNr = rs.getInt(++idx);
        mt.mtBestOf = rs.getInt(++idx);
        mt.mtTimestamp = rs.getTimestamp(++idx);
        mt.mtTable = rs.getInt(++idx);
        mt.mtDateTime = rs.getTimestamp(++idx);
        mt.mtResA = rs.getInt(++idx);
        mt.mtResX = rs.getInt(++idx);
        mt.mtWalkOverA = rs.getInt(++idx) > 0;
        mt.mtWalkOverX = rs.getInt(++idx) > 0;
        mt.mtInjuredA = rs.getInt(++idx) > 0;
        mt.mtInjuredX = rs.getInt(++idx) > 0;
        mt.mtDisqualifiedA = rs.getInt(++idx) > 0;
        mt.mtDisqualifiedX = rs.getInt(++idx) > 0;
        mt.stA = rs.getInt(++idx);
        mt.stX = rs.getInt(++idx);
        mt.tmA = rs.getInt(++idx);
        mt.tmX = rs.getInt(++idx);
        int plNr = rs.getInt(++idx);
        if (rs.wasNull()) {
            mt.plA = null;
            idx += 4;
        } else {
            mt.plA = new Player();
            mt.plA.plNr = plNr;
            mt.plA.psLast = getString(rs, ++idx);
            mt.plA.psFirst = getString(rs, ++idx);
            mt.plA.naName = getString(rs, ++idx);
            mt.plA.phone = getString(rs, ++idx);
        }
        plNr = rs.getInt(++idx);
        if (rs.wasNull()) {
            mt.plB = null;
            idx += 4;
        } else {
            mt.plB = new Player();
            mt.plB.plNr = plNr;
            mt.plB.psLast = getString(rs, ++idx);
            mt.plB.psFirst = getString(rs, ++idx);
            mt.plB.naName = getString(rs, ++idx);
            mt.plB.phone = getString(rs, ++idx);
        }
        plNr = rs.getInt(++idx);
        if (rs.wasNull()) {
            mt.plX = null;
            idx += 4;
        } else {
            mt.plX = new Player();
            mt.plX.plNr = plNr;
            mt.plX.psLast = getString(rs, ++idx);
            mt.plX.psFirst = getString(rs, ++idx);
            mt.plX.naName = getString(rs, ++idx);
            mt.plX.phone = getString(rs, ++idx);
        }
        plNr = rs.getInt(++idx);
        if (rs.wasNull()) {
            mt.plY = null;
            idx += 4;
        } else {
            mt.plY = new Player();
            mt.plY.plNr = plNr;
            mt.plY.psLast = getString(rs, ++idx);
            mt.plY.psFirst = getString(rs, ++idx);
            mt.plY.naName = getString(rs, ++idx);
            mt.plY.phone = getString(rs, ++idx);
        }
        mt.scrID = rs.getInt(++idx);
        mt.scsID = rs.getInt(++idx);
        mt.ts = rs.getTimestamp(++idx);   
        mt.scgID = rs.getInt(++idx);
        mt.scgEnabled = rs.getBoolean(++idx);
        mt.scsRescheduled = rs.getBoolean(++idx);
        mt.scsReminder = rs.getBoolean(++idx);
    }
    
    private String normalizeString(String str) {
        StringBuilder buffer = new StringBuilder();
        for (int i = 0; i < str.length(); i++) {
            int idx = -1;
            char c = str.charAt(i);
            if (c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z' || c >= '0' && c <= '9')
                buffer.append(c);
            else if (" @£$¥\r\n_!\"#%&'()*+,-./:;<=>?¡§¿".indexOf(c) != -1)
                buffer.append(c);
            else if ("èéùìòÇØøÅåÆæßÉ¤ÄÖÑÜäöñüà".indexOf(c) != -1)
                buffer.append(c);
            else if ( (idx = "ŁłÕõŽ".indexOf(c)) != -1 )
                buffer.append("LlOoYZ".charAt(idx));
            else {
                String s = str.substring(i, i+1);
                s = Normalizer.normalize(s, Normalizer.Form.NFD);
                for (int j = 0; j < s.length(); j++) {
                    System.out.println((int) s.charAt(j));
                    if ( (int) s.charAt(j) < 0x80 )
                        buffer.append(s.charAt(j));
                }
                // s = s.replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
                // buffer.append(s);
            }
        }
        
        if (false && !buffer.toString().equals(str)) {
            System.out.println(str);
            System.out.println(buffer.toString());
        }
        
        return buffer.toString();
    }
}
