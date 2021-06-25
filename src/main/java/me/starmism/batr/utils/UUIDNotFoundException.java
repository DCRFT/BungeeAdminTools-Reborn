package me.starmism.batr.utils;


public class UUIDNotFoundException extends RuntimeException{
	private static final long serialVersionUID = 1L;
	private final String player;
	
	public UUIDNotFoundException(String player){
		this.player = player;
	}
	
	public String getInvolvedPlayer(){
		return player;
	}
}
