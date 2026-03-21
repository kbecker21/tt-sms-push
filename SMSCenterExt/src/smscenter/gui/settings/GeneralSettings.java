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
public class GeneralSettings extends Settings {

    public GeneralSettings() {
        
    }
    
    @Override
    public void readProperties(Properties props, String prefix) {
        setUpdateInterval(Integer.parseInt(getProperty(props, prefix, "update_interval", "5")));
        setUpdateDelay(Integer.parseInt(getProperty(props, prefix, "update_delay", "60")));
        setOutboundInterval(Integer.parseInt(getProperty(props, prefix, "outbound_interval", "10")));
        setInboundInterval(Integer.parseInt(getProperty(props, prefix, "inbound_interval", "60")));
        setSendModeAsync(getProperty(props, prefix, "send_mode", "async").equals("async"));
        setDeleteAfterProcessing(getProperty(props, prefix, "delete_after_processing", "yes").equals("yes"));
        setTimeframeHigh(getProperty(props, prefix, "timeframe.high", "0000-2359"));
        setTimeframeNormal(getProperty(props, prefix, "timeframe.normal", "0000-2359"));
        setTimeframeLow(getProperty(props, prefix, "timeframe.low", "0000-2359"));
        setReminderTime(Integer.parseInt(getProperty(props, prefix, "reminder_time", "15")));
        setReminderTimeNextDay(Integer.parseInt(getProperty(props, prefix, "reminder_time_next_day", "60")));
        setReminderCutoff(Integer.parseInt(getProperty(props, prefix, "reminder_cutoff", "90")));
    }
    
    
    @Override
    public void writeProperties(Properties props, String prefix) {
        setProperty(props, prefix, "update_interval", "" + getUpdateInterval());
        setProperty(props, prefix, "update_delay", "" + getUpdateDelay());
        setProperty(props, prefix, "outbound_interval", "" + getOutboundInterval());
        setProperty(props, prefix, "inbound_interval", "" + getInboundInterval());
        setProperty(props, prefix, "send_mode", isSendModeAsync() ? "async" : "sync");
        setProperty(props, prefix, "delete_after_processing", isDeleteAfterProcessing() ? "yes" : "no");
        setProperty(props, prefix, "timeframe.high", getTimeframeHigh());
        setProperty(props, prefix, "timeframe.normal", getTimeframeNormal());
        setProperty(props, prefix, "timeframe.low", getTimeframeLow());
        setProperty(props, prefix, "reminder_time", "" + getReminderTime());
        setProperty(props, prefix, "reminder_time_next_day", "" + getReminderTimeNextDay());
        setProperty(props, prefix, "reminder_cutoff", "" + getReminderCutoff());
    }
    
    private int updateInterval;
    private int updateDelay;
    private int outboundInterval;
    private int inboundInterval;
    private boolean sendModeAsync;
    private boolean deleteAfterProcessing;
    private String timeframeHigh;
    private String timeframeNormal;
    private String timeframeLow;
    private String welcomeMsg;
    private String sponsorLine;
    private int    reminder_cutoff;   // Ab wann eine Erinnerung schicken
    private int    reminder_time;     // Wieviel frueber die Erinnerung schicken
    private int    reminder_time_next_day;  // Dto., wenn Spiel am naechsten Tag

    /**
     * @return the outboundInterval
     */
    public int getOutboundInterval() {
        return outboundInterval;
    }

    /**
     * @param outboundInterval the outboundInterval to set
     */
    public void setOutboundInterval(int outboundInterval) {
        this.outboundInterval = outboundInterval;
    }

    /**
     * @return the sendModeAsync
     */
    public boolean isSendModeAsync() {
        return sendModeAsync;
    }

    /**
     * @param sendModeAsync the sendModeAsync to set
     */
    public void setSendModeAsync(boolean sendModeAsync) {
        this.sendModeAsync = sendModeAsync;
    }

    /**
     * @return the deleteAfterProcessing
     */
    public boolean isDeleteAfterProcessing() {
        return deleteAfterProcessing;
    }

    /**
     * @param deleteAfterProcessing the deleteAfterProcessing to set
     */
    public void setDeleteAfterProcessing(boolean deleteAfterProcessing) {
        this.deleteAfterProcessing = deleteAfterProcessing;
    }

    /**
     * @return the timeframeHigh
     */
    public String getTimeframeHigh() {
        return timeframeHigh;
    }

    /**
     * @param timeframeHigh the timeframeHigh to set
     */
    public void setTimeframeHigh(String timeframeHigh) {
        this.timeframeHigh = timeframeHigh;
    }

    /**
     * @return the timeframeNormal
     */
    public String getTimeframeNormal() {
        return timeframeNormal;
    }

    /**
     * @param timeframeNormal the timeframeNormal to set
     */
    public void setTimeframeNormal(String timeframeNormal) {
        this.timeframeNormal = timeframeNormal;
    }

    /**
     * @return the timeframeLow
     */
    public String getTimeframeLow() {
        return timeframeLow;
    }

    /**
     * @param timeframeLow the timeframeLow to set
     */
    public void setTimeframeLow(String timeframeLow) {
        this.timeframeLow = timeframeLow;
    }

    /**
     * @return the inboundInterval
     */
    public int getInboundInterval() {
        return inboundInterval;
    }

    /**
     * @param inboundInterval the inboundInterval to set
     */
    public void setInboundInterval(int inboundInterval) {
        this.inboundInterval = inboundInterval;
    }

    public int getUpdateInterval() {
        return updateInterval;
    }

    public void setUpdateInterval(int updateInterval) {
        this.updateInterval = updateInterval;
    }
    
    public int getUpdateDelay() {
        return updateDelay;
    }

    public void setUpdateDelay(int updateDelay) {
        this.updateDelay = updateDelay;
    }

    /**
     * @return the welcomeMsg
     */
    public String getWelcomeMsg() {
        return welcomeMsg;
    }

    /**
     * @param welcomeMsg the welcomeMsg to set
     */
    public void setWelcomeMsg(String welcomeMsg) {
        this.welcomeMsg = welcomeMsg;
    }

    /**
     * @return the sponsorLine
     */
    public String getSponsorLine() {
        return sponsorLine;
    }

    /**
     * @param sponsorLine the sponsorLine to set
     */
    public void setSponsorLine(String sponsorLine) {
        this.sponsorLine = sponsorLine;
    }
    
    public int getReminderCutoff() {
        return reminder_cutoff;
    }

    public void setReminderCutoff(int reminder_cutoff) {
        this.reminder_cutoff = reminder_cutoff;
    }

    public int getReminderTime() {
        return reminder_time;
    }

    public void setReminderTime(int reminder_time) {
        this.reminder_time = reminder_time;
    }

    public int getReminderTimeNextDay() {
        return reminder_time_next_day;
    }

    public void setReminderTimeNextDay(int reminder_time_next_day) {
        this.reminder_time_next_day = reminder_time_next_day;
    }
    
}
