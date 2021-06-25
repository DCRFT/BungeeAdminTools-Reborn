package me.starmism.batr.modules;

public class InvalidModuleException extends Exception {
	private static final long serialVersionUID = 1L;
	private final String message;
	
	public InvalidModuleException(final String message){
		this.message = message;
	}

	public String getMessage() {
		return this.message;
	}
}
