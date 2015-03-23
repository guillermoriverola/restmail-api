package edu.upc.eetac.dsa.griverola.restmail.api;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import edu.upc.eetac.dsa.griverola.restmail.api.model.RestmailRootAPI;
  
@Path("/")
public class RestmailRootAPIResource {
	@GET
	public RestmailRootAPI getRootAPI() {
		RestmailRootAPI api = new RestmailRootAPI();
		return api;
	}
}