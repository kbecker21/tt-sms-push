package smscenter.smsserver.gateways;

import java.util.Properties;
import smscenter.smsserver.interfaces.Interface;

/**
 * SMSServer Application Gateway for Push Notifications.
 * Wraps PushHTTPGateway and configures it from properties.
 */
public class PushGateway extends AGateway
{
	public PushGateway(String myGatewayId, Properties myProps, smscenter.smsserver.SMSServer myServer)
	{
		super(myGatewayId, myProps, myServer);
	}

	@Override
	public String getDescription()
	{
		return getProperties().getProperty(getGatewayId() + "." + "description", "Default Push Gateway");
	}

	@Override
	public void create() throws Exception
	{
		String propName = getGatewayId() + ".";
		String serviceUrl = getProperties().getProperty(propName + "serviceUrl", "http://localhost:8080");
		String apiKey = getProperties().getProperty(propName + "apiKey", "changeme");

		PushHTTPGateway pushGateway = new PushHTTPGateway(getGatewayId(), serviceUrl, apiKey);

		if (getProperties().getProperty(propName + "outbound", "yes").equalsIgnoreCase("yes"))
			pushGateway.setOutbound(true);
		else
			pushGateway.setOutbound(false);

		pushGateway.setInbound(false);

		// Configure database access for recipient mapping (phone → plNr).
		// Find the Database interface ID from the server's interface list.
		String dbInterfaceId = findDatabaseInterfaceId();
		if (dbInterfaceId != null)
		{
			pushGateway.configureDatabase(getProperties(), dbInterfaceId);
		}

		setGateway(pushGateway);
	}

	/**
	 * Find the ID of the Database interface in the SMSServer configuration.
	 * Looks through registered interfaces to find one of type Database.
	 */
	private String findDatabaseInterfaceId()
	{
		if (getServer() != null && getServer().getInfList() != null)
		{
			for (Interface<?> inf : getServer().getInfList())
			{
				if (inf instanceof smscenter.smsserver.interfaces.Database)
				{
					return inf.getId();
				}
			}
		}

		// Fallback: scan properties for interface entries referencing Database
		Properties props = getProperties();
		for (int i = 0; i < 100; i++)
		{
			String value = props.getProperty("interface." + i, "");
			if (value.isEmpty()) break;
			if (value.contains("Database"))
			{
				// Format: "db0, Database, inoutbound" — first token is the ID
				String id = value.split(",")[0].trim();
				return id;
			}
		}

		return null;
	}
}
