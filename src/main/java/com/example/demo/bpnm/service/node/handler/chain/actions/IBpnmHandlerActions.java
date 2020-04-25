package com.example.demo.bpnm.service.node.handler.chain.actions;

import org.flowable.bpmn.model.FlowElement;

import com.example.demo.bpnm.service.node.model.SearchContext;
import com.example.demo.bpnm.service.node.model.UserNode;

public interface IBpnmHandlerActions {
	UserNode findNextNode(SearchContext searchContext, FlowElement nextFlowElement);
}
