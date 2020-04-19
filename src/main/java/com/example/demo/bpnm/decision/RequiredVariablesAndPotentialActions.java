package com.example.demo.bpnm.decision;

import java.util.List;

public class RequiredVariablesAndPotentialActions {
	
	private String nodeName;
	private List<String> requiredOutgoingVariables;
	private List<String> actions;
	
	public RequiredVariablesAndPotentialActions(List<String> requiredOutgoingVariables,String nodeName, List<String> actions) {
		super();
		this.requiredOutgoingVariables = requiredOutgoingVariables;
		this.nodeName = nodeName;
		this.actions = actions;
	}

	public String getNodeName() {
		return nodeName;
	}

	public void setNodeName(String nodeName) {
		this.nodeName = nodeName;
	}

	public List<String> getRequiredIncomingVariables() {
		return requiredOutgoingVariables;
	}

	public void setRequiredIncomingVariables(List<String> requiredIncomingVariables) {
		this.requiredOutgoingVariables = requiredIncomingVariables;
	}
	public void setActions(List<String> actions) {
		this.actions = actions;
	}
	public List<String> getActions() {
		return actions;
	}
	
	
}
