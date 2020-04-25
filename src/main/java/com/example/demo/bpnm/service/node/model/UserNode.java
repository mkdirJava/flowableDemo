package com.example.demo.bpnm.service.node.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.flowable.bpmn.model.FlowElement;
import org.flowable.task.api.Task;

public class UserNode {

	private String userNodeId;
	private String userNodeName;
	private UserNode previousTask;
	private List<UserNode> futureTasks = new ArrayList<UserNode>();
	private List<String> conditionsToArrive = new ArrayList<String>();
	private List<String> actionNames = new ArrayList<String>();

	public UserNode(String userNodeId, String userNodeName, SearchContext context) {
		super();
		this.userNodeId = userNodeId;
		this.userNodeName = userNodeName;
		if (context != null && context.getOriginUserNode() != null) {
			this.previousTask = context.getOriginUserNode();
			this.conditionsToArrive = context.getFoundOutflowConditions();
		}
	}

	public UserNode(String userNodeId, String userNodeName) {
		super();
		this.userNodeId = userNodeId;
		this.userNodeName = userNodeName;
	}

	public boolean isCyclic(UserNode userNode) {
		UserNode node = searchForUserNodeBefore(userNode);
		return node != null;
	}

	public void setCyclicTask(UserNode userNode, SearchContext searchContext) {
		UserNode node = searchForUserNodeBefore(userNode);
		if (node == null) {
			throw new IllegalStateException();
		}
		userNode.conditionsToArrive.addAll(searchContext.getFoundOutflowConditions());
		node.addToFutureTasks(userNode);
	}

	public UserNode getUserNode(Task task) {

		Optional<UserNode> shallowFind = this.futureTasks.stream().filter(userNode -> {
			return task.getTaskDefinitionKey().equals(this.userNodeId);
		}).findFirst();

		return shallowFind.orElseGet(() -> {
			return this.futureTasks.stream().filter(userNode -> {
				return userNode.getUserNode(task) != null;
			}).findFirst().orElseThrow(IllegalArgumentException::new);
		});

	}

	private UserNode searchForUserNodeBefore(UserNode userNode) {
		if (this.previousTask != null) {
			if (userNode.getUserNodeId().equals(this.userNodeId)) {
				return this;
			} else {
				return this.previousTask.searchForUserNodeBefore(userNode);
			}
		}
		return null;
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
		return new ArrayList<UserNode>(futureTasks);
	}

	public synchronized void addToFutureTasks(UserNode userNode) {
		this.futureTasks.add(userNode);
	}

	public List<String> getConditionsToArrive() {
		return conditionsToArrive;
	}

	@Override
	public String toString() {
		return this.getUserNodeName();
	}

	public static UserNode fromFlowElement(FlowElement flowElement) {
		return new UserNode(flowElement.getId(), flowElement.getName());
	}

}
