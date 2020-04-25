package com.example.demo.bpnm.service.node.handler.chain.actions;

import java.util.Map;

import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.StartEvent;

import com.example.demo.bpnm.service.node.model.SearchContext;
import com.example.demo.bpnm.service.node.model.UserNode;

public class StartEventBpnmHandlerAction extends BaseBpnmHandlerAction {

	public StartEventBpnmHandlerAction(Map<Class, BaseBpnmHandlerAction> registeredActions) {
		super.registeredActions= registeredActions;
	}

	@Override
	public UserNode findNextNode(SearchContext searchContext, FlowElement nextFlowElement) {
		StartEvent startEvent = (StartEvent) nextFlowElement;
		UserNode userNode = new UserNode(startEvent.getId(), startEvent.getName(), searchContext);
		startEvent.getOutgoingFlows().stream().forEach((outflow) -> {
			super.findNextNode(new SearchContext(userNode), outflow);
		});
		return userNode;
	}
	

}
