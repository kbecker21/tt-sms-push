/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package smscenter.database;

import java.sql.Timestamp;
import java.util.Date;

/**
 *
 * @author chtheis
 */
public class Match {
    public Group gr = new Group();
    public int mtNr;
    public int mtBestOf;
    public Date mtTimestamp;
    public int mtTable;
    public Date mtDateTime;
    public int mtResA;
    public int mtResX;
    public boolean mtWalkOverA;
    public boolean mtWalkOverX;
    public boolean mtInjuredA;
    public boolean mtInjuredX;
    public boolean mtDisqualifiedA;
    public boolean mtDisqualifiedX;
    public Player plA;
    public Player plB;
    public Player plX;
    public Player plY;
    public int stA;
    public int stX;
    public int tmA;
    public int tmX;
    public int scsID;
    public int scrID;
    public int scgID;
    public boolean scgEnabled;
    public Timestamp ts;    // Time entered into table, compare with now  
    public boolean scsRescheduled;
    public boolean scsReminder;
}
