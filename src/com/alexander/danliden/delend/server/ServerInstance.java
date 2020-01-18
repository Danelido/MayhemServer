package com.alexander.danliden.delend.server;


import java.util.concurrent.CopyOnWriteArrayList;

public class ServerInstance {

	public CopyOnWriteArrayList<ServerClient> group = new CopyOnWriteArrayList<ServerClient>();
	public CopyOnWriteArrayList<ServerClient> worldList;
	public ServerInstance(CopyOnWriteArrayList<ServerClient> worldclients){
		worldList = worldclients;
	
	
	}
	public void removePlayer(ServerClient c){
		if(group.contains(c)){
			group.remove(c);
			if(!worldList.contains(c)){
				worldList.add(c);
			}
		}
	}
	
	
	public void clearGroup(){
		if(!group.isEmpty()){
		for(int i = 0; i < group.size(); i++){
			if(!worldList.contains(group.get(i))){
				worldList.add(group.get(i));
			}
			group.remove(i);
			
		}
		}
	}
	
	public void addPlayers(ServerClient playerOne, ServerClient playerTwo){
		if(group.size() < 2){
			if(!group.contains(playerOne))	
					group.add(playerOne);
			if(!group.contains(playerTwo))
					group.add(playerTwo);
		}
	}
	
	
	public void addPlayer(ServerClient player){
		if(group.size() < 2){
			if(!group.contains(player))
			group.add(player);
		}
	}
	
	public boolean isFull(){
		if(group.size() == 2){
			return true;
		}
		return false;
	}
	
	public ServerClient getPlayerOne(){
		if(!group.isEmpty()){
		ServerClient player = group.get(0);
		return player;
		}return null;
		
	}
	
	public ServerClient getPlayerTwo(){
		if(!group.isEmpty()){
		ServerClient player = group.get(1);
		return player;
		}return null;
		
	}
	
	public boolean containsPlayer(ServerClient c){
		if(!group.isEmpty()){
		if(group.contains(c)){
			return true;
		}
		}
		return false;
	}
	
	
}

