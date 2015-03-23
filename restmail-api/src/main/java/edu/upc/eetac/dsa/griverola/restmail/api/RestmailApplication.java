package edu.upc.eetac.dsa.griverola.restmail.api;

import org.glassfish.jersey.linking.DeclarativeLinkingFeature;
import org.glassfish.jersey.server.ResourceConfig;
 
public class RestmailApplication extends ResourceConfig {
	public RestmailApplication() {
		super();
		register(DeclarativeLinkingFeature.class);
	}
}