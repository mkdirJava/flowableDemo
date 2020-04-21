package com.example.demo.bpnm.service.node.model;

import java.util.ArrayList;
import java.util.List;

import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.UserTask;

public class UserNode {
	private String userNodeId;
	private String userNodeName;
	
	private UserNode previousTask;
	private List<UserNode> futureTasks = new ArrayList<UserNode>();
	
	private List<String> conditionsToGetToThisUserNode = new ArrayList<String>();
	
	private List<String> actionNames = new ArrayList<String>();
	
	public UserNode(String userNodeId, String userNodeName, UserNode previousTask) {
		super();
		this.userNodeId = userNodeId;
		this.userNodeName = userNodeName;
		this.previousTask = previousTask;
	}
	public boolean isCyclic(UserNode userNode) {
		if(this.previousTask != null) {
			if(userNode.getUserNodeId().equals(this.userNodeId)) {
				return true;
			}else {
				return this.previousTask.isCyclic(userNode);
			}
		}
		return false;
	}
	public String getUserNodeId() {
		return userNodeId;
	}
	public void setUserNodeId(String userNodeId) {
		this.userNodeId = userNodeId;
	}
	public String getUserNodeName() {
		return userNodeName;
	}
	public List<String> getActionNames() {
		return actionNames;
	}
	public synchronized void addToActionNames(String actionName) {
		this.actionNames.add(actionName);
	}
	public void setUserNodeName(String userNodeName) {
		this.userNodeName = userNodeName;
	}
	public UserNode getPreviousTask() {
		return previousTask;
	}
	public void setPreviousTask(UserNode previousTask) {
		this.previousTask = previousTask;
	}
	public List<UserNode> getFutureTasks() {
		return futureTasks;
	}
	public synchronized void addToFutureTasks(UserNode userNode) {
		this.futureTasks.add(userNode);
	}
	public synchronized List<String> getConditionsToGetToThisUserNode() {
		return conditionsToGetToThisUserNode;
	}
	public synchronized void addConditionToThisNode(String condition) {
		this.conditionsToGetToThisUserNode.add(condition);
	}
	
	@Override
	public String toString() {
		return this.getUserNodeName(); 
	}
	
	public static UserNode fromFlowElement(FlowElement flowElement) {
		return new UserNode(flowElement.getId(), flowElement.getName(), null);
	}
	
}
