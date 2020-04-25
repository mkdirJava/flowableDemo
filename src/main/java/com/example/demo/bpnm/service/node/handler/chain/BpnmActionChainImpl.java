package com.example.demo.bpnm.service.node.handler.chain;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Gateway;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.UserTask;

import com.example.demo.bpnm.service.node.handler.chain.actions.BaseBpnmHandlerAction;
import com.example.demo.bpnm.service.node.handler.chain.actions.EndEventBpnmHandlerAction;
import com.example.demo.bpnm.service.node.handler.chain.actions.GatewayHandlerAction;
import com.example.demo.bpnm.service.node.handler.chain.actions.SequenceBpnmHandlerAction;
import com.example.demo.bpnm.service.node.handler.chain.actions.ServiceTaskBpnmHandlerAction;
import com.example.demo.bpnm.service.node.handler.chain.actions.StartEventBpnmHandlerAction;
import com.example.demo.bpnm.service.node.handler.chain.actions.UserTaskBpnmHandlerAction;
import com.example.demo.bpnm.service.node.model.SearchContext;
import com.example.demo.bpnm.service.node.model.UserNode;



public class BpnmActionChainImpl implements IBpnmActionChain {

	private static Map<Class, BaseBpnmHandlerAction> getDefaultChain() {

		Map<Class, BaseBpnmHandlerAction> registeredActions = new HashMap<Class, BaseBpnmHandlerAction>();
		
		StartEventBpnmHandlerAction startEventBpnmHandlerAction = new StartEventBpnmHandlerAction(registeredActions);
		registeredActions.put(StartEvent.class, startEventBpnmHandlerAction);
		
		SequenceBpnmHandlerAction sequenceBpnmHandlerAction = new SequenceBpnmHandlerAction(registeredActions);
		registeredActions.put(SequenceFlow.class, sequenceBpnmHandlerAction);
		
		GatewayHandlerAction gatewayHandlerAction = new GatewayHandlerAction(registeredActions);
		registeredActions.put(Gateway.class, gatewayHandlerAction);
		
		ServiceTaskBpnmHandlerAction serviceTaskBpnmHandlerAction = new ServiceTaskBpnmHandlerAction(registeredActions);
		registeredActions.put(ServiceTask.class, serviceTaskBpnmHandlerAction);
		
		UserTaskBpnmHandlerAction userTaskBpnmHandlerAction = new UserTaskBpnmHandlerAction(registeredActions);
		registeredActions.put(UserTask.class, userTaskBpnmHandlerAction);
		
		EndEventBpnmHandlerAction endEventBpnmHandlerAction = new EndEventBpnmHandlerAction(registeredActions);
		registeredActions.put(EndEvent.class, endEventBpnmHandlerAction);

		return registeredActions;
	}
	
	public static UserNode generateMapping(SearchContext searchContext, FlowElement flowElement) {
		Map<Class, BaseBpnmHandlerAction> defaultChain = getDefaultChain();
		Entry<Class, BaseBpnmHandlerAction> foundEntry = defaultChain.entrySet().stream()
				.filter((entrySet) -> entrySet.getKey().isInstance(flowElement)).findFirst()
				.orElseThrow(IllegalArgumentException::new);
		return foundEntry.getValue().findNextNode(searchContext, flowElement);
	}


}
