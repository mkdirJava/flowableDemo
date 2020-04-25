package com.example.demo.bpnm.service.node;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Gateway;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.ServiceTask;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.ProcessDefinition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import com.example.demo.bpnm.service.node.handler.chain.BpnmActionChainImpl;
import com.example.demo.bpnm.service.node.handler.chain.actions.BaseBpnmHandlerAction;
import com.example.demo.bpnm.service.node.model.SearchContext;
import com.example.demo.bpnm.service.node.model.UserNode;

@Component
public class UserNodeService implements IGetBpnmModel {

	private RepositoryService repositoryService;

	@Autowired
	public UserNodeService(RepositoryService repositoryService) {
		this.repositoryService = repositoryService;
	}

	@Cacheable("UserNodeModel")
	@Override
	public UserNode getUserNodesInModel(String processDefinitionId) {
		
		
		
		BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinitionId);
		ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
				.processDefinitionId(processDefinitionId).singleResult();
		FlowElement startEvent = bpmnModel.getProcessById(processDefinition.getKey()).getFlowElements().stream()
				.filter(flow -> flow instanceof StartEvent).findFirst().orElseThrow();

		UserNode findNextNode = BpnmActionChainImpl.generateMapping(new SearchContext(UserNode.fromFlowElement(startEvent)), startEvent);
		
		return findNextNode(new SearchContext(UserNode.fromFlowElement(startEvent)), startEvent);
	}

	private UserNode findNextNode(SearchContext searchContext, FlowElement nextFlowElement) {

		if (nextFlowElement instanceof StartEvent == false & searchContext == null) {
			throw new IllegalArgumentException("argument is not a start event");
		}

		if (nextFlowElement instanceof StartEvent) {
			StartEvent startEvent = (StartEvent) nextFlowElement;
			UserNode userNode = new UserNode(startEvent.getId(), startEvent.getName(), searchContext);
			startEvent.getOutgoingFlows().stream().forEach((outflow) -> {
				findNextNode(new SearchContext(userNode), outflow);
			});
			return userNode;
		}

		if (nextFlowElement instanceof UserTask) {

			UserNode userNode = new UserNode(nextFlowElement.getId(), nextFlowElement.getName(), searchContext);

			if (userNode.getPreviousTask().isCyclic(userNode)) {
				System.out.println("FOUND CYCLIC "+ userNode.getUserNodeName());
				System.out.println("The conditions to get here " + searchContext.getFoundOutflowConditions() );
				userNode.getPreviousTask().setCyclicTask(userNode, searchContext);
				return null;
			}
			((UserTask) nextFlowElement).getOutgoingFlows().stream()
					.forEach((outFlow) -> findNextNode(new SearchContext(userNode), outFlow));

			((UserTask) nextFlowElement).getBoundaryEvents().stream().forEach((event) -> {
				userNode.getActionNames().add(event.getName());
			});

			return userNode;
		}
		
		if (nextFlowElement instanceof ServiceTask) {
			ServiceTask serviceTask = (ServiceTask) nextFlowElement;
			UserNode previousFoundUserNode = searchContext.getOriginUserNode();
			
			serviceTask.getOutgoingFlows().stream()
					.filter(outflow -> !previousFoundUserNode.isCyclic(UserNode.fromFlowElement(outflow)))
					.map((outflow) -> findNextNode(searchContext, outflow)).filter(nextNode -> {
						return nextNode != null && !previousFoundUserNode.isCyclic(nextNode);
					}).forEach((nextNode) -> {
						System.out.println(" Service Task adding "+ nextNode.getUserNodeName() + " to " + previousFoundUserNode.getUserNodeName());
						previousFoundUserNode.addToFutureTasks(nextNode);
					});
			return previousFoundUserNode;

		}

		if (nextFlowElement instanceof SequenceFlow) {
			UserNode previousFoundUserNode = searchContext.getOriginUserNode();
			SequenceFlow sequenceFlow = (SequenceFlow) nextFlowElement;
			if (previousFoundUserNode.isCyclic(UserNode.fromFlowElement(sequenceFlow))) {
				return previousFoundUserNode;
			}

			if (sequenceFlow.getConditionExpression() != null) {
				searchContext.addOutflowCondition(sequenceFlow.getConditionExpression());
			}
			return findNextNode(searchContext, sequenceFlow.getTargetFlowElement());

		}
		

		if (nextFlowElement instanceof Gateway) {
			Gateway gateway = (Gateway) nextFlowElement;
			UserNode previousFoundUserNode = searchContext.getOriginUserNode();
			gateway.getOutgoingFlows().stream()
					.filter(outflow -> !previousFoundUserNode.isCyclic(UserNode.fromFlowElement(outflow)))
					.map((outflow) -> {
						SearchContext newSearchContext = new SearchContext(searchContext.getOriginUserNode());
						return findNextNode(newSearchContext, outflow);	
					})
					.filter(nextNode -> {
						return nextNode != null && !previousFoundUserNode.isCyclic(nextNode);
					}).forEach((nextNode) -> {
						previousFoundUserNode.addToFutureTasks(nextNode);
					});
			return previousFoundUserNode;
		}
		if (nextFlowElement instanceof EndEvent) {
			return null;
		}
		throw new IllegalArgumentException("The element is not a sequence, gateway or user task it is a " + nextFlowElement.getClass().toString());

	}

}
