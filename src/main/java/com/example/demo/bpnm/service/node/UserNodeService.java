package com.example.demo.bpnm.service.node;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.EndEvent;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Gateway;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.StartEvent;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.task.api.Task;
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
	public UserNode getUserNodesInModel(Task task) {
		BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());
		ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
				.processDefinitionId(task.getProcessDefinitionId()).singleResult();
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
			UserNode userNode = new UserNode(startEvent.getId(), startEvent.getName(), previousFoundUserNode);
			startEvent.getOutgoingFlows().stream().forEach((outflow) -> {
				userNode.addToFutureTasks(findNextNode(userNode, outflow));
			});
			return userNode;
		} else {
			if (nextFlowElement instanceof UserTask) {
				UserTask userTask = (UserTask) nextFlowElement;
				UserNode userNode = new UserNode(userTask.getId(), userTask.getName(), previousFoundUserNode);

				if (userNode.isCyclic(userTask)) {
					return null;
				}
				userTask.getOutgoingFlows().stream().forEach((outFlow) -> {
					userNode.addToFutureTasks(findNextNode(userNode, outFlow));
				});

				userTask.getBoundaryEvents().stream().forEach((event) -> {
					userNode.getActionNames().add(event.getName());
					
				});
				
				return userNode;

			}

			if (nextFlowElement instanceof SequenceFlow) {
				// TODO handle cyclics
				SequenceFlow sequenceFlow = (SequenceFlow) nextFlowElement;
				if (previousFoundUserNode != null) {
					previousFoundUserNode.addConditionToThisNode(sequenceFlow.getConditionExpression());
				}
				if (!previousFoundUserNode.isCyclic(sequenceFlow)) {
					previousFoundUserNode.addToFutureTasks(findNextNode(previousFoundUserNode, sequenceFlow.getTargetFlowElement()));
					return previousFoundUserNode;
				}
				return previousFoundUserNode;
			}

			if (nextFlowElement instanceof Gateway) {
				Gateway gateway = (Gateway) nextFlowElement;
				// TODO handle cyclics

				gateway.getOutgoingFlows().stream().filter(outflow -> !previousFoundUserNode.isCyclic(outflow))
						.forEach((outFlow) -> {
							previousFoundUserNode.addToFutureTasks(findNextNode(previousFoundUserNode, outFlow));
						});
				return previousFoundUserNode;
			}
			if (nextFlowElement instanceof EndEvent) {
				return previousFoundUserNode;
			}
			throw new IllegalArgumentException("The element is not a sequence, gateway or user task");
		}

	}

}
