package com.example.demo.bpnm.service.node.handler.chain.actions;

import java.util.Map;

import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.UserTask;

import com.example.demo.bpnm.service.node.model.SearchContext;
import com.example.demo.bpnm.service.node.model.UserNode;

public class UserTaskBpnmHandlerAction extends BaseBpnmHandlerAction {

	public UserTaskBpnmHandlerAction(Map<Class, BaseBpnmHandlerAction> registeredActions) {
		super.registeredActions = registeredActions;
	}

	@Override
	public UserNode findNextNode(SearchContext searchContext, FlowElement nextFlowElement) {
		UserNode userNode = new UserNode(nextFlowElement.getId(), nextFlowElement.getName(), searchContext);

		if (userNode.getPreviousTask().isCyclic(userNode)) {
			System.out.println("FOUND CYCLIC "+ userNode.getUserNodeName());
			System.out.println("The conditions to get here " + searchContext.getFoundOutflowConditions() );
			userNode.getPreviousTask().setCyclicTask(userNode, searchContext);
			return null;
		}
		((UserTask) nextFlowElement).getOutgoingFlows().stream()
				.forEach((outFlow) -> super.findNextNode(new SearchContext(userNode), outFlow));

		((UserTask) nextFlowElement).getBoundaryEvents().stream().forEach((event) -> {
			userNode.getActionNames().add(event.getName());
		});

		return userNode;
	}
	
	

}
