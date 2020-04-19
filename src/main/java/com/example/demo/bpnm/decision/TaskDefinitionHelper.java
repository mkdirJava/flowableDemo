package com.example.demo.bpnm.decision;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.flowable.bpmn.model.BpmnModel;
import org.flowable.bpmn.model.FlowElement;
import org.flowable.bpmn.model.Gateway;
import org.flowable.bpmn.model.SequenceFlow;
import org.flowable.bpmn.model.UserTask;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.task.api.Task;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class TaskDefinitionHelper implements IGetNodeRequirement {

	private RepositoryService repositoryService;

	public TaskDefinitionHelper(RepositoryService repositoryService) {
		this.repositoryService = repositoryService;
	}

	@Cacheable("RequiredTaskVariables")
	@Override
	public RequiredVariablesAndPotentialActions getNodeRequirements(Task task) {
		BpmnModel bpmnModel = repositoryService.getBpmnModel(task.getProcessDefinitionId());
		ProcessDefinition processDefinition = repositoryService.createProcessDefinitionQuery()
				.processDefinitionId(task.getProcessDefinitionId()).singleResult();
		FlowElement foundFlowElement = findFlowElmentofTask(task, bpmnModel, processDefinition);
		validateFlowElement(foundFlowElement);
		List<String> actions = getSubscribedActions(task, bpmnModel, processDefinition);
		UserTask userTask = (UserTask) foundFlowElement;
		List<SequenceFlow> outgoingFlows = userTask.getOutgoingFlows();
		List<SequenceFlow> requiredConditions = getRequiredConditions(outgoingFlows);
		List<String> flowExpressions = requiredConditions.stream().map(flow -> flow.getConditionExpression())
				.collect(Collectors.toList());

		return new RequiredVariablesAndPotentialActions(flowExpressions, foundFlowElement.getName(), actions);
	}

	private void validateFlowElement(FlowElement task) {
		if (task instanceof UserTask == false) {
			throw new IllegalArgumentException("The task is not a user Task");
		}
	}

	private FlowElement findFlowElmentofTask(Task task, BpmnModel bpmnModel, ProcessDefinition processDefinition) {
		return bpmnModel.getProcessById(processDefinition.getKey()).getFlowElements().stream()
				.filter((flowElement) -> flowElement.getId().equals(task.getTaskDefinitionKey())).findFirst()
				.orElseThrow(() -> {
					throw new IllegalArgumentException("Cannot find the task");
				});
	}

	private List<SequenceFlow> getRequiredConditions(List<SequenceFlow> outgoingFlows) {

		return outgoingFlows.stream().map((flow) -> flow.getTargetFlowElement())
				.filter((target) -> target instanceof Gateway).flatMap(TaskDefinitionHelper::getOutSequenceFromGateway)
				.filter((result) -> result != null).collect(Collectors.toList());

	}

	private static Stream<SequenceFlow> getOutSequenceFromGateway(FlowElement flow) {
		if (flow instanceof Gateway) {
			Gateway gateway = (Gateway) flow;
			List<SequenceFlow> outgoingFlows = gateway.getOutgoingFlows();
			return outgoingFlows.stream().map(fromGatewaySequence -> {
				return fromGatewaySequence;
			});
		}
		return null;
	}

	private List<String> getSubscribedActions(Task task, BpmnModel bpmnModel, ProcessDefinition processDefinition) {
		FlowElement findFlowElmentofTask = findFlowElmentofTask(task, bpmnModel, processDefinition);
		if (findFlowElmentofTask instanceof UserTask) {
			UserTask userTask = (UserTask) findFlowElmentofTask;
			return userTask.getBoundaryEvents().stream().map((event) -> {
				return event.getName();
			}).collect(Collectors.toList());
		}
		return null;

	}

}
