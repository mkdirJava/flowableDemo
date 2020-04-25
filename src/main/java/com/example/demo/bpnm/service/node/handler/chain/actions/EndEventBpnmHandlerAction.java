package com.example.demo.bpnm.service.node.handler.chain.actions;

import java.util.Map;

import org.flowable.bpmn.model.FlowElement;

import com.example.demo.bpnm.service.node.model.SearchContext;
import com.example.demo.bpnm.service.node.model.UserNode;

public class EndEventBpnmHandlerAction extends BaseBpnmHandlerAction {

	public EndEventBpnmHandlerAction(Map<Class, BaseBpnmHandlerAction> registeredActions) {
		super.registeredActions = registeredActions;
	}

	@Override
	public UserNode findNextNode(SearchContext searchContext, FlowElement nextFlowElement) {
		return null;
	}
}
