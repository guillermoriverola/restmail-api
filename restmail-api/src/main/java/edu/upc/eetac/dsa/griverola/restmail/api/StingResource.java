package edu.upc.eetac.dsa.griverola.restmail.api;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import javax.sql.DataSource;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.ForbiddenException;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.ServerErrorException;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.SecurityContext;

import edu.upc.eetac.dsa.griverola.restmail.api.model.Sting;
import edu.upc.eetac.dsa.griverola.restmail.api.model.StingCollection;
 
@Path("/stings")
public class StingResource {
	private DataSource ds = DataSourceSPA.getInstance().getDataSource();
	
	private String GET_STINGS_QUERY = "select s.*, u.name from stings s, users u where u.username=s.username and s.creation_timestamp < ifnull(?, now())  order by creation_timestamp desc limit ?";
	private String GET_STINGS_QUERY_FROM_LAST = "select s.*, u.name from stings s, users u where u.username=s.username and s.creation_timestamp > ? order by creation_timestamp desc";
		
	
	@GET
@Produces(MediaType.RESTMAIL_API_STING_COLLECTION)
public StingCollection getStings(@QueryParam("length") int length,
		@QueryParam("before") long before, @QueryParam("after") long after) {
	StingCollection stings = new StingCollection();
 
	Connection conn = null;
	try {
		conn = ds.getConnection();
	} catch (SQLException e) {
		throw new ServerErrorException("Could not connect to the database",
				Response.Status.SERVICE_UNAVAILABLE);
	}
 
	PreparedStatement stmt = null;
	try {
		boolean updateFromLast = after > 0;
		stmt = updateFromLast ? conn
				.prepareStatement(GET_STINGS_QUERY_FROM_LAST) : conn
				.prepareStatement(GET_STINGS_QUERY);
		if (updateFromLast) {
			stmt.setTimestamp(1, new Timestamp(after));
		} else {
			if (before > 0)
				stmt.setTimestamp(1, new Timestamp(before));
			else
				stmt.setTimestamp(1, null);
			length = (length <= 0) ? 5 : length;
			stmt.setInt(2, length);
		}
		ResultSet rs = stmt.executeQuery();
		boolean first = true;
		long oldestTimestamp = 0;
		while (rs.next()) {
			Sting sting = new Sting();
			sting.setStingid(rs.getInt("stingid"));
			sting.setUsername(rs.getString("username"));
			sting.setSubject(rs.getString("subject"));
			sting.setLastModified(rs.getTimestamp("last_modified").getTime());
			sting.setCreationTimestamp(rs.getTimestamp("creation_timestamp").getTime()); 
			oldestTimestamp = rs.getTimestamp("creation_timestamp").getTime();
			sting.setLastModified(oldestTimestamp);
			if (first) {
				first = false;
				stings.setNewestTimestamp(sting.getCreationTimestamp());
			}
			stings.addSting(sting);
		}
		stings.setOldestTimestamp(oldestTimestamp);
	} catch (SQLException e) {
		throw new ServerErrorException(e.getMessage(),
				Response.Status.INTERNAL_SERVER_ERROR);
	} finally {
		try {
			if (stmt != null)
				stmt.close();
			conn.close();
		} catch (SQLException e) {
		}
	}
 
	return stings;
}
private String INSERT_STING_QUERY = "insert into stings (username, subject, body) values (?, ?, ?)";

@POST
@Consumes(MediaType.RESTMAIL_API_STING)
@Produces(MediaType.RESTMAIL_API_STING)
public Sting createSting(Sting sting) {
	validateSting(sting);
	Connection conn = null;
	try {
		conn = ds.getConnection();
	} catch (SQLException e) {
		throw new ServerErrorException("Could not connect to the database",
				Response.Status.SERVICE_UNAVAILABLE);
	}
 
	PreparedStatement stmt = null;
	try {
		stmt = conn.prepareStatement(INSERT_STING_QUERY,
				Statement.RETURN_GENERATED_KEYS);
 
		//stmt.setString(1, sting.getUsername());
		stmt.setString(1, security.getUserPrincipal().getName());
		stmt.setString(2, sting.getSubject());
		stmt.setString(3, sting.getBody());
		stmt.executeUpdate();
		ResultSet rs = stmt.getGeneratedKeys();
		if (rs.next()) {
			int stingid = rs.getInt(1);
 
			sting = getStingFromDatabase(Integer.toString(stingid));
		} else {
			// Something has failed...
		}
	} catch (SQLException e) {
		throw new ServerErrorException(e.getMessage(),
				Response.Status.INTERNAL_SERVER_ERROR);
	} finally {
		try {
			if (stmt != null)
				stmt.close();
			conn.close();
		} catch (SQLException e) {
		}
	}
 
	return sting;
}
private String GET_STING_BY_ID_QUERY = "select s.*, u.name from stings s, users u where u.username=s.username and s.stingid=?";

private String DELETE_STING_QUERY = "delete from stings where stingid=?";

@DELETE
@Path("/{stingid}")
public void deleteSting(@PathParam("stingid") String stingid) {
	Connection conn = null;
	try {
		conn = ds.getConnection();
	} catch (SQLException e) {
		throw new ServerErrorException("Could not connect to the database",
				Response.Status.SERVICE_UNAVAILABLE);
	}
 
	PreparedStatement stmt = null;
	try {
		stmt = conn.prepareStatement(DELETE_STING_QUERY);
		stmt.setInt(1, Integer.valueOf(stingid));
 
		int rows = stmt.executeUpdate();
		if (rows == 0)
			throw new NotFoundException("There's no sting with stingid="
					+ stingid);			
	} catch (SQLException e) {
		throw new ServerErrorException(e.getMessage(),
				Response.Status.INTERNAL_SERVER_ERROR);
	} finally {
		try {
			if (stmt != null)
				stmt.close();
			conn.close();
		} catch (SQLException e) {
		}
	}
}

private String UPDATE_STING_QUERY = "update stings set subject=ifnull(?, subject), body=ifnull(?, body) where stingid=?";

@PUT
@Path("/{stingid}")
@Consumes(MediaType.RESTMAIL_API_STING)
@Produces(MediaType.RESTMAIL_API_STING)
public Sting updateSting(@PathParam("stingid") String stingid, Sting sting) {
	validateUpdateSting(sting);
	Connection conn = null;
	try {
		conn = ds.getConnection();
	} catch (SQLException e) {
		throw new ServerErrorException("Could not connect to the database",
				Response.Status.SERVICE_UNAVAILABLE);
	}
 
	PreparedStatement stmt = null;
	try {
		
		stmt = conn.prepareStatement(UPDATE_STING_QUERY);
		stmt.setString(1, sting.getSubject());
		stmt.setString(2, sting.getBody());
		stmt.setInt(3, Integer.valueOf(stingid));
 
		int rows = stmt.executeUpdate();
		if (rows == 1)
			sting = getStingFromDatabase(stingid);
		else {
			throw new NotFoundException("There's no sting with stingid="
					+ stingid);
		}
 
	} catch (SQLException e) {
		throw new ServerErrorException(e.getMessage(),
				Response.Status.INTERNAL_SERVER_ERROR);
	} finally {
		try {
			if (stmt != null)
				stmt.close();
			conn.close();
		} catch (SQLException e) {
		}
	}
 
	return sting;
}
 
private void validateUpdateSting(Sting sting) {
	if (sting.getSubject() != null && sting.getSubject().length() > 100)
		throw new BadRequestException(
				"Subject can't be greater than 100 characters.");
	if (sting.getBody() != null && sting.getBody().length() > 500)
		throw new BadRequestException(
				"Body can't be greater than 500 characters.");
}
	

private void validateSting(Sting sting) {
	if (sting.getSubject() == null)
		throw new BadRequestException("Subject can't be null.");
	if (sting.getBody() == null)
		throw new BadRequestException("Body can't be null.");
	if (sting.getSubject().length() > 100)
		throw new BadRequestException("Subject can't be greater than 100 characters.");
	if (sting.getBody().length() > 500)
		throw new BadRequestException("Body can't be greater than 500 characters.");
}

private Sting getStingFromDatabase(String stingid) {
	Sting sting = new Sting();
 
	Connection conn = null;
	try {
		conn = ds.getConnection();
	} catch (SQLException e) {
		throw new ServerErrorException("Could not connect to the database",
				Response.Status.SERVICE_UNAVAILABLE);
	}
 
	PreparedStatement stmt = null;
	try {
		stmt = conn.prepareStatement(GET_STING_BY_ID_QUERY);
		stmt.setInt(1, Integer.valueOf(stingid));
		ResultSet rs = stmt.executeQuery();
		if (rs.next()) {
			sting.setStingid(rs.getInt("stingid"));
			sting.setUsername(rs.getString("username"));			
			sting.setSubject(rs.getString("subject"));
			sting.setBody(rs.getString("body"));
			sting.setLastModified(rs.getTimestamp("last_modified")
					.getTime());
			sting.setCreationTimestamp(rs
					.getTimestamp("creation_timestamp").getTime());
		} else {
			throw new NotFoundException("There's no sting with stingid="
					+ stingid);
		}
	} catch (SQLException e) {
		throw new ServerErrorException(e.getMessage(),
				Response.Status.INTERNAL_SERVER_ERROR);
	} finally {
		try {
			if (stmt != null)
				stmt.close();
			conn.close();
		} catch (SQLException e) {
		}
	}
 
	return sting;
}

@GET
@Path("/{stingid}")
@Produces(MediaType.RESTMAIL_API_STING)
public Response getSting(@PathParam("stingid") String stingid,
		@Context Request request) {
	// Create CacheControl
	CacheControl cc = new CacheControl();
 
	Sting sting = getStingFromDatabase(stingid);
 
	// Calculate the ETag on last modified date of user resource
	EntityTag eTag = new EntityTag(Long.toString(sting.getLastModified()));
 
	// Verify if it matched with etag available in http request
	Response.ResponseBuilder rb = request.evaluatePreconditions(eTag);
 
	// If ETag matches the rb will be non-null;
	// Use the rb to return the response without any further processing
	if (rb != null) {
		return rb.cacheControl(cc).tag(eTag).build();
	}
 
	// If rb is null then either it is first time request; or resource is
	// modified
	// Get the updated representation and return with Etag attached to it
	rb = Response.ok(sting).cacheControl(cc).tag(eTag);
 
	return rb.build();
}

@Context
private SecurityContext security;

private void validateUser(String stingid) {
    Sting sting = getStingFromDatabase(stingid);
    String username = sting.getUsername();
	if (!security.getUserPrincipal().getName()
			.equals(username))
		throw new ForbiddenException(
				"You are not allowed to modify this sting.");
}


}

