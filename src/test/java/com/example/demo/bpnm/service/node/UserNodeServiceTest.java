package com.example.demo.bpnm.service.node;

import static org.junit.jupiter.api.Assertions.fail;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.demo.bpnm.service.node.model.UserNode;

@SpringBootTest
class UserNodeServiceTest {

	private RepositoryService repositoryService;
	private RuntimeService runtimeService;
	private TaskService taskService;

	private UserNodeService userNodeService;

	@Autowired
	public UserNodeServiceTest(RepositoryService repositoryService, RuntimeService runtimeService,
			TaskService taskService, UserNodeService userNodeService) {
		this.repositoryService = repositoryService;
		this.runtimeService = runtimeService;
		this.taskService = taskService;
		this.userNodeService =userNodeService;
	}

	@Test
	void test() {
		createProcessInstance();
		Task activeTask = this.taskService.createTaskQuery().singleResult();
		
		UserNode userNodesInModel = this.userNodeService.getUserNodesInModel(activeTask.getProcessDefinitionId());
		
		
		
		fail("Not yet implemented");
	}

	private ProcessInstance createProcessInstance() {
		ProcessDefinition processDefinition = this.repositoryService.createProcessDefinitionQuery().processDefinitionKey("corrective-action").singleResult();
		return this.runtimeService.startProcessInstanceByKey(processDefinition.getKey());
	}

}
