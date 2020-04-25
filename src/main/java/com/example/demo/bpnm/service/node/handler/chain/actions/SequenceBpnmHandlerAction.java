package com.example.demo.bpnm.service.node.handler.chain.actions;

import java.util.Map;

import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.SequenceFlow;

import com.example.demo.bpnm.service.node.model.SearchContext;
import com.example.demo.bpnm.service.node.model.UserNode;

public class SequenceBpnmHandlerAction extends BaseBpnmHandlerAction{

	public SequenceBpnmHandlerAction(Map<Class, BaseBpnmHandlerAction> registeredActions) {
		super.registeredActions = registeredActions;
	}

	@Override
	public UserNode findNextNode(SearchContext searchContext, FlowElement nextFlowElement) {
		UserNode previousFoundUserNode = searchContext.getOriginUserNode();
		SequenceFlow sequenceFlow = (SequenceFlow) nextFlowElement;
		if (previousFoundUserNode.isCyclic(UserNode.fromFlowElement(sequenceFlow))) {
			return previousFoundUserNode;
		}

		if (sequenceFlow.getConditionExpression() != null) {
			searchContext.addOutflowCondition(sequenceFlow.getConditionExpression());
		}
		return super.findNextNode(searchContext, sequenceFlow.getTargetFlowElement());
	}
	
	

}
