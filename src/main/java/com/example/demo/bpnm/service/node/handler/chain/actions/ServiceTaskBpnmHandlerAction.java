package com.example.demo.bpnm.service.node.handler.chain.actions;

import java.util.Map;

import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.ServiceTask;

import com.example.demo.bpnm.service.node.model.SearchContext;
import com.example.demo.bpnm.service.node.model.UserNode;

public class ServiceTaskBpnmHandlerAction extends BaseBpnmHandlerAction {

	public ServiceTaskBpnmHandlerAction(Map<Class, BaseBpnmHandlerAction> registeredActions) {
		super.registeredActions = registeredActions;
	}

	@Override
	public UserNode findNextNode(SearchContext searchContext, FlowElement nextFlowElement) {
		ServiceTask serviceTask = (ServiceTask) nextFlowElement;
		UserNode previousFoundUserNode = searchContext.getOriginUserNode();
		serviceTask.getOutgoingFlows().stream()
				.filter(outflow -> !previousFoundUserNode.isCyclic(UserNode.fromFlowElement(outflow)))
				.map((outflow) -> super.findNextNode(searchContext, outflow)).filter(nextNode -> {
					return nextNode != null && !previousFoundUserNode.isCyclic(nextNode);
				}).forEach((nextNode) -> {
					previousFoundUserNode.addToFutureTasks(nextNode);
				});
		return previousFoundUserNode;
	}

}
