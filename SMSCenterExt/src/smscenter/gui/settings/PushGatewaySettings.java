package smscenter.gui.settings;

import java.util.Properties;

/**
 * Settings for the Push Notification Gateway.
 */
public class PushGatewaySettings extends GatewaySettings {

    private String serviceUrl;
    private String apiKey;
    private boolean outbound;
    private String description;

    @Override
    public SettingsPanel getPanel() {
        return new PushGatewaySettingsPanel(this);
    }

    @Override
    public GatewayType getType() {
        return GatewayType.PushGateway;
    }

    @Override
    public void readProperties(Properties props, String prefix) {
        setServiceUrl(getProperty(props, prefix, "serviceUrl", "http://localhost:8080"));
        setApiKey(getProperty(props, prefix, "apiKey", "changeme"));
        setOutbound(getProperty(props, prefix, "outbound", "yes").equals("yes"));
        setDescription(getProperty(props, prefix, "description", "Default Push Gateway"));
    }

    @Override
    public void writeProperties(Properties props, String prefix) {
        setProperty(props, prefix, "serviceUrl", getServiceUrl());
        setProperty(props, prefix, "apiKey", getApiKey());
        setProperty(props, prefix, "outbound", isOutbound() ? "yes" : "no");
        setProperty(props, prefix, "description", getDescription());
    }

    public String getServiceUrl() {
        return serviceUrl;
    }

    public void setServiceUrl(String serviceUrl) {
        this.serviceUrl = serviceUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public boolean isOutbound() {
        return outbound;
    }

    public void setOutbound(boolean outbound) {
        this.outbound = outbound;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
