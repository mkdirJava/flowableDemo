package com.example.demo.bpnm.service.node.model;

import java.util.ArrayList;
import java.util.List;

public class SearchContext {
	
	private UserNode originUserNode;
	private List<String> foundOutflowConditions = new ArrayList<>();
	
	public SearchContext(UserNode originUserNode) {
		this.originUserNode = originUserNode;
	}
	
	public void addOutflowCondition(String condition) {
		this.foundOutflowConditions.add(condition);
	}
	
	public List<String> getFoundOutflowConditions() {
		return new ArrayList<String>(foundOutflowConditions);
	}
	
	public UserNode getOriginUserNode() {
		return originUserNode;
	}

}
