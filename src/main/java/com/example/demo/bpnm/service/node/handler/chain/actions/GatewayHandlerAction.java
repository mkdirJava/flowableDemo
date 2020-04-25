package com.example.demo.bpnm.service.node.handler.chain.actions;

import java.util.Map;

import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Gateway;

import com.example.demo.bpnm.service.node.model.SearchContext;
import com.example.demo.bpnm.service.node.model.UserNode;

public class GatewayHandlerAction extends BaseBpnmHandlerAction {

	public GatewayHandlerAction(Map<Class, BaseBpnmHandlerAction> registeredActions) {
		super.registeredActions = registeredActions;
	}

	@Override
	public UserNode findNextNode(SearchContext searchContext, FlowElement nextFlowElement) {

		Gateway gateway = (Gateway) nextFlowElement;
		UserNode previousFoundUserNode = searchContext.getOriginUserNode();
		gateway.getOutgoingFlows().stream()
				.filter(outflow -> !previousFoundUserNode.isCyclic(UserNode.fromFlowElement(outflow)))
				.map((outflow) -> {
					SearchContext newSearchContext = new SearchContext(searchContext.getOriginUserNode());
					return super.findNextNode(newSearchContext, outflow);
				}).filter(nextNode -> {
					return nextNode != null && !previousFoundUserNode.isCyclic(nextNode);
				}).forEach((nextNode) -> {
					previousFoundUserNode.addToFutureTasks(nextNode);
				});
		return previousFoundUserNode;
	};

}
