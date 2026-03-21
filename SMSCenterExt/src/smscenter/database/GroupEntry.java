/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package smscenter.database;

import java.sql.Timestamp;

/**
 *
 * @author chtheis
 */
public class GroupEntry {
    public Group    gr = new Group();
    public Player   pl;
    public Player   bd;
    public int      stID;
    public int      stNr;
    public int      stPos;
    public int      scpID;
    public Timestamp ts;
    public boolean  scgEnabled;
}
    
