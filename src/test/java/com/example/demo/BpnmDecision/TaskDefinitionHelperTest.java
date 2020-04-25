package com.example.demo.BpnmDecision;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.impl.persistence.entity.ExecutionEntityImpl;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.demo.bpnm.decision.RequiredVariablesAndPotentialActions;
import com.example.demo.bpnm.decision.TaskDefinitionHelper;

@SpringBootTest
class TaskDefinitionHelperTest {

	private RepositoryService repositoryService;
	private RuntimeService runtimeService;
	private TaskService taskService;

	private TaskDefinitionHelper taskDefinitionHelper;

	@Autowired
	public TaskDefinitionHelperTest(RepositoryService repositoryService, RuntimeService runtimeService,
			TaskService taskService, TaskDefinitionHelper taskDefinitionHelper) {
		this.repositoryService = repositoryService;
		this.runtimeService = runtimeService;
		this.taskService = taskService;
		this.taskDefinitionHelper = taskDefinitionHelper;
	}

	@AfterEach
	public void cleanUp() {
		this.runtimeService.createProcessInstanceQuery().list().stream().forEach(processInstance -> {
			this.runtimeService.deleteProcessInstance(processInstance.getId(), "clean up after each test");
		});
	}

	private ProcessInstance createProcessInstance() {
		ProcessDefinition processDefinition = this.repositoryService.createProcessDefinitionQuery().processDefinitionKey("test").singleResult();
		return this.runtimeService.startProcessInstanceByKey(processDefinition.getKey());
	}
    private void raiseInvestigation(ProcessInstance processInstance,String eventName){
        Execution execution = this.runtimeService.createExecutionQuery()
                .processInstanceId(processInstance.getId())
                .signalEventSubscriptionName(eventName)
                .singleResult();
        this.runtimeService.signalEventReceived(eventName,execution.getId());
    }

	@Test
	void getNodeRequirements() {
		createProcessInstance();
		Task task = taskService.createTaskQuery().singleResult();
		RequiredVariablesAndPotentialActions reAndPotentialActions = taskDefinitionHelper.getNodeRequirements(task);
		List<String> requiredVariables = reAndPotentialActions.getRequiredIncomingVariables();
		List<String> actions = reAndPotentialActions.getActions();

		assertAll("getting a saftey office should return two required fields plus the name of the task",
				() -> assertEquals("Safety Office", reAndPotentialActions.getNodeName()),
				// TODO these are just strings, data type is needed
				() -> assertEquals(1, actions.size()),
				() -> assertEquals("raiseInvestigation", actions.get(0)),
				() -> assertEquals(2, reAndPotentialActions.getRequiredIncomingVariables().size()),
				() -> assertEquals("${shouldWaitInvestigations ==false}", requiredVariables.get(0)),
				() -> assertEquals("${shouldWaitInvestigations ==true}", requiredVariables.get(1)));
	}

	@Test
	void getNodeRequirementsAfterCompletingFirstTask() {
		
		ProcessInstance processInstance = createProcessInstance(); 
		// complete safety office task with no investigation
		Task safetyOfficeTask = this.taskService.createTaskQuery().singleResult();
		RequiredVariablesAndPotentialActions safteyOfficeRequiredTaskVariables = taskDefinitionHelper.getNodeRequirements(safetyOfficeTask);
		this.taskService.setVariables(safetyOfficeTask.getId(), Map.of("shouldWaitInvestigations", "true"));
		raiseInvestigation(processInstance,safteyOfficeRequiredTaskVariables.getActions().get(0));
		this.taskService.complete(safetyOfficeTask.getId());
		
		
		this.taskService.createTaskQuery().list().forEach((task)-> this.taskService.complete(task.getId()));
		
		Task safetyOfficeReview = taskService.createTaskQuery().singleResult();
		RequiredVariablesAndPotentialActions safetyOfficeReviewRequiredTaskVariables = taskDefinitionHelper.getNodeRequirements(safetyOfficeReview);

		List<String> requiredVariables = safetyOfficeReviewRequiredTaskVariables.getRequiredIncomingVariables();
		List<String> actions = safetyOfficeReviewRequiredTaskVariables.getActions();

		assertAll("This should be asserting against Saftey Office Review",
				() -> assertEquals("Safety Office Review", safetyOfficeReviewRequiredTaskVariables.getNodeName()),
				() -> assertTrue(actions.isEmpty()),
				() -> assertEquals(2, safetyOfficeReviewRequiredTaskVariables.getRequiredIncomingVariables().size()),
				() -> assertEquals("${isSafetyOfficeFinished == true}", requiredVariables.get(0)),
				() -> assertEquals("${isSafetyOfficeFinished == false}", requiredVariables.get(1)));
	}

	// This is really testing spring
	@Test
	void getNodeRequirementsFromCache() {
		createProcessInstance();
		Task task = taskService.createTaskQuery().singleResult();
		RequiredVariablesAndPotentialActions requiredTaskVariables = taskDefinitionHelper.getNodeRequirements(task);
		RequiredVariablesAndPotentialActions requiredTaskVariablesFromCache = taskDefinitionHelper.getNodeRequirements(task);

		assertAll(
				"Doing a second call to the same method with the same task should result in the same RequiredTaskVariables object coing back ",
				() -> assertEquals(requiredTaskVariables.hashCode(), requiredTaskVariablesFromCache.hashCode()));
	}

}
