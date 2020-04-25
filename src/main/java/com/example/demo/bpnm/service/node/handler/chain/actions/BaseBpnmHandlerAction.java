package com.example.demo.bpnm.service.node.handler.chain.actions;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.flowable.bpmn.model.FlowElement;

import com.example.demo.bpnm.service.node.model.SearchContext;
import com.example.demo.bpnm.service.node.model.UserNode;

public abstract class BaseBpnmHandlerAction implements IBpnmHandlerActions {

	Map<Class, BaseBpnmHandlerAction> registeredActions;

	public UserNode findNextNode(SearchContext searchContext ,FlowElement nextFlowElement) {
		Optional<Entry<Class, BaseBpnmHandlerAction>> foundEntry = this.registeredActions.entrySet().stream()
				.filter((entrySet) -> {
					return entrySet.getKey().isInstance(nextFlowElement);
				}).findFirst();
				
		return foundEntry.get().getValue().findNextNode(searchContext, nextFlowElement);
	}

}
