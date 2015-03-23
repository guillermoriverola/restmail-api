package edu.upc.eetac.dsa.griverola.restmail.api.model;

public class RestmailError {
	private int status;
	private String message;
 
	public RestmailError() {
		super();
	}
 
	public RestmailError(int status, String message) {
		super();
		this.status = status;
		this.message = message;
	}
 
	public int getStatus() {
		return status;
	}
 
	public void setStatus(int status) {
		this.status = status;
	}
 
	public String getMessage() {
		return message;
	}
 
	public void setMessage(String message) {
		this.message = message;
	}
}
