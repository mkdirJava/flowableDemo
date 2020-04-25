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

		return findNextNode(null, startEvent);
	}

	private UserNode findNextNode(UserNode previousFoundUserNode, FlowElement nextFlowElement) {

		if (nextFlowElement instanceof StartEvent == false & previousFoundUserNode == null) {
			throw new IllegalArgumentException("argument is not a start event");
		}

		if (previousFoundUserNode == null) {
			StartEvent startEvent = (StartEvent) nextFlowElement;
			UserNode startNode = new UserNode(startEvent.getId(), "START EVENT", previousFoundUserNode);
			startEvent.getOutgoingFlows().stream()
			.filter(outflow -> !startNode.isCyclic(UserNode.fromFlowElement(outflow)))
			.forEach((outflow) -> findNextNode(startNode, outflow));
			return startNode;
		} else {

			if (nextFlowElement instanceof UserTask) {
				UserNode userNode = new UserNode(nextFlowElement.getId(), nextFlowElement.getName(),
						previousFoundUserNode);

				if (userNode.getPreviousTask().isCyclic(UserNode.fromFlowElement(nextFlowElement))) {
					return null;
				}
				UserTask userTask = ((UserTask) nextFlowElement);
				userTask.getOutgoingFlows().stream()
						.filter(outflow -> !userNode.isCyclic(UserNode.fromFlowElement(outflow)))
						.map((outflow) -> findNextNode(userNode, outflow)).filter(nextNode -> {
							return nextNode != null && !nextNode.isCyclic(nextNode);
						}).forEach((nextNode) -> {
							System.out.println(" User Task adding "+ nextNode.getUserNodeName() + " to " + previousFoundUserNode.getUserNodeName());
							userNode.addToFutureTasks(nextNode);
						});
				((UserTask) nextFlowElement).getBoundaryEvents().stream().forEach((event) -> {
					userNode.getActionNames().add(event.getName());

				});

				return userNode;

			}

			if (nextFlowElement instanceof ServiceTask) {
				ServiceTask serviceTask = (ServiceTask) nextFlowElement;

//				if (previousFoundUserNode.isCyclic(UserNode.fromFlowElement(nextFlowElement))) {
//					return previousFoundUserNode;
//				}
				serviceTask.getOutgoingFlows().stream()
						.filter(outflow -> !previousFoundUserNode.isCyclic(UserNode.fromFlowElement(outflow)))
						.map((outflow) -> findNextNode(previousFoundUserNode, outflow)).filter(nextNode -> {
							return nextNode != null && !previousFoundUserNode.isCyclic(nextNode);
						}).forEach((nextNode) -> {
							System.out.println(" Service Task adding "+ nextNode.getUserNodeName() + " to " + previousFoundUserNode.getUserNodeName());
							previousFoundUserNode.addToFutureTasks(nextNode);
						});
				return previousFoundUserNode;

			}

			if (nextFlowElement instanceof SequenceFlow) {
				SequenceFlow sequenceFlow = (SequenceFlow) nextFlowElement;
				if (previousFoundUserNode.isCyclic(UserNode.fromFlowElement(sequenceFlow))) {
					return previousFoundUserNode;
				}

				if (previousFoundUserNode != null && sequenceFlow.getConditionExpression() != null) {
					previousFoundUserNode.addOngoingConditions(sequenceFlow.getConditionExpression());
				}
				return findNextNode(previousFoundUserNode, sequenceFlow.getTargetFlowElement());

			}

			if (nextFlowElement instanceof Gateway) {
				Gateway gateway = (Gateway) nextFlowElement;

				gateway.getOutgoingFlows().stream()
						.filter(outflow -> !previousFoundUserNode.isCyclic(UserNode.fromFlowElement(outflow)))
						.map((outflow) -> findNextNode(previousFoundUserNode, outflow)).filter(nextNode -> {
							return nextNode != null && !previousFoundUserNode.isCyclic(nextNode);
						}).forEach((nextNode) -> {
							System.out.println(" Gateway adding "+ nextNode.getUserNodeName() + " to " + previousFoundUserNode.getUserNodeName());
							previousFoundUserNode.addToFutureTasks(nextNode);
						});
				return previousFoundUserNode;
			}
			if (nextFlowElement instanceof EndEvent) {
				return null;
			}
			throw new IllegalArgumentException("The element is not a sequence, gateway or user task");
		}

	}

}
