package edu.upc.eetac.dsa.griverola.beeter.api.model;

public class Sting {
	private int stingid;
	private String username;
	private String subject;
	private String body;
	private long lastModified;
	private long creationTimestamp;

	public int getStingid() {
		return stingid;
	}

	public void setStingid(int stingid) {
		this.stingid = stingid;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	
	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getBody() {
		return content;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public long getLastModified() {
		return lastModified;
	}

	public void setLastModified(long lastModified) {
		this.lastModified = lastModified;
	}

	public long getCreationTimestamp() {
		return creationTimestamp;
	}

	public void setCreationTimestamp(long creationTimestamp) {
		this.creationTimestamp = creationTimestamp;
	}

}
