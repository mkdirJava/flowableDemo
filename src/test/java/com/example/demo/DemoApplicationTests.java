package com.example.demo;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.runtime.Execution;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.task.api.Task;
import org.flowable.task.api.TaskInfo;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.test.util.AssertionErrors.assertTrue;

@SpringBootTest
class DemoApplicationTests {

    private RepositoryService repositoryService;
    private RuntimeService runtimeService;
    private TaskService taskService;

    @Autowired
    public DemoApplicationTests(RepositoryService repositoryService,RuntimeService runtimeService, TaskService taskService){
        this.repositoryService = repositoryService;
        this.runtimeService = runtimeService;
        this.taskService = taskService;
    }

    @AfterEach
    public void cleanUp() {
        this.runtimeService.createProcessInstanceQuery().list().stream().forEach(processInstance -> {
            this.runtimeService.deleteProcessInstance(processInstance.getId(), "clean up after each test");
        });
    }
    @Test
    void I_SHOULD_BE_ABLE_TO_PROGRESS_WITHOUT_AN_INVESTIGATION(){
        //create new process
        ProcessInstance createdProcessInstance = createProcessInstance();
        testOneSafetyOfficeTaskCreated();

        //complete safety office task with no investigation
        Task safetyOfficeTask = this.taskService.createTaskQuery().singleResult();
        runtimeService.setVariableLocal(safetyOfficeTask.getExecutionId(),"shouldWaitInvestigations","false");
        this.taskService.complete(safetyOfficeTask.getId());

        //assert no more tasks to be completed
        List<Task> totalTasks = this.taskService.createTaskQuery().list();
        assertTrue("There should only be one task ", totalTasks.size() == 0);

    }

    @Test
    void I_SHOULD_BE_ABLE_TO_RAISE_INVESTIGATION_TASKS_AND_HAVE_A_SAFETY_OFFICE_TASK_OPEN_AT_ONE_TIME() {
        // create a new process
        ProcessInstance createdProcessInstance = createProcessInstance();
        testOneSafetyOfficeTaskCreated();

        raiseInvestigation(createdProcessInstance);
        List<Task> tasksAfterSignalForInvestigation = this.taskService.createTaskQuery().list();
        assertTrue("There should be a safety office and investigation tasks created ", tasksAfterSignalForInvestigation.size() == 2);
        assertTrue("There be one processes running" , this.runtimeService.createProcessInstanceQuery().list().size() ==1);
    }

    @Test
    void I_SHOULD_BE_ABLE_TO_RAISE_MANY_INVESTIGATION_TASKS_AND_HAVE_A_SAFETY_OFFICE_TASK_OPEN_AT_ONE_TIME() {
        //create a new process
        ProcessInstance createdProcessInstance = createProcessInstance();
        testOneSafetyOfficeTaskCreated();

        //raise two investigations and check there should be two investigations and one safety office
        raiseInvestigation(createdProcessInstance);
        raiseInvestigation(createdProcessInstance);

        //Assert there are two investigations and one safety office task.
        List<Task> tasksAfterSignalForInvestigation = this.taskService.createTaskQuery().list();
        assertTrue("There should be a safety office and investigation tasks created ", tasksAfterSignalForInvestigation.size() == 3);
        Map<String, List<Task>> groupByTaskName = tasksAfterSignalForInvestigation.stream().collect(Collectors.groupingBy(TaskInfo::getName));
        assertTrue("There should be two investigations",groupByTaskName.get("Investigation").size() ==2);
        assertTrue("There should be one Safety Office task",groupByTaskName.get("Safety Office").size() ==1);

    }

    @Test
    void I_SHOULD_BE_ABLE_TO_CLOSE_A_SAFETY_OFFICE_TASK_HAVE_OUTSTANDING_INVESTIGATIONS_WITHOUT_MOVING_TO_THE_NEXT_STAGE(){
        // create a process
        ProcessInstance createdProcessInstance = createProcessInstance();

        // list out the initial tasks
        testOneSafetyOfficeTaskCreated();
        Task safetyOffice = this.taskService.createTaskQuery().singleResult();

        //raise two investigations that are concurrent  with the safety office task
        raiseInvestigation(createdProcessInstance);
        raiseInvestigation(createdProcessInstance);
        List<Task> tasksAfterTwoInvestigationSignals = this.taskService.createTaskQuery().list();
        assertTrue("There should be three tasks", tasksAfterTwoInvestigationSignals.size() == 3);
        Map<String, List<Task>> tasksByName = tasksAfterTwoInvestigationSignals.stream().collect(Collectors.groupingBy(TaskInfo::getName));
        assertTrue("There should be one safety office task ", tasksByName.get("Safety Office").size() == 1);
        assertTrue("There should be two investigation task ", tasksByName.get("Investigation").size() == 2);


        //complete the safety office task and check there is still two investigations going on
        runtimeService.setVariableLocal(safetyOffice.getExecutionId(),"shouldWaitInvestigations","true");
        this.taskService.complete(safetyOffice.getId());
        List<Task> tasksAfterSignalForInvestigation = this.taskService.createTaskQuery().list();
        Map<String, List<Task>> tasksByNameAfterSafetyOfficeTaskComplete = tasksAfterSignalForInvestigation.stream().collect(Collectors.groupingBy(TaskInfo::getName));
        assertTrue("There should not be any safety office task ", !tasksByNameAfterSafetyOfficeTaskComplete.containsKey("Safety Office"));
        assertTrue("There should not be any Down stream task ", !tasksByNameAfterSafetyOfficeTaskComplete.containsKey("Safety Office Review"));
        assertTrue("There should be two investigation task ", tasksByNameAfterSafetyOfficeTaskComplete.get("Investigation").size() == 2 );

    }

    @Test
    void I_NEED_TO_COMPLETE_ALL_INVESTIGATION_AND_SAFETY_OFFICE_TASKS_BEFORE_GETTING_TO_SAFETY_OFFICE_TASK(){
        //Create a new process instance
        ProcessInstance createdProcessInstance = createProcessInstance();

        //test that one safety office is created
        testOneSafetyOfficeTaskCreated();
        Task safetyOffice = this.taskService.createTaskQuery().singleResult();

        // raise two investigations and test that there are three tasks, one safety office
        raiseInvestigation(createdProcessInstance);
        raiseInvestigation(createdProcessInstance);
        List<Task> tasksAfterTwoInvestigationSignals = this.taskService.createTaskQuery().list();
        assertTrue("There should be three tasks", tasksAfterTwoInvestigationSignals.size() == 3);
        Map<String, List<Task>> tasksByName = tasksAfterTwoInvestigationSignals.stream().collect(Collectors.groupingBy(TaskInfo::getName));
        assertTrue("There should be one safety office task ", tasksByName.get("Safety Office").size() == 1);
        assertTrue("There should be two investigation task ", tasksByName.get("Investigation").size() == 2);

        // complete the safety office task and check there are two investigations tasks and no Safety Office Review tasks
        runtimeService.setVariableLocal(safetyOffice.getExecutionId(),"shouldWaitInvestigations","true");
        this.taskService.complete(safetyOffice.getId());
        List<Task> tasksAfterSignalForInvestigation = this.taskService.createTaskQuery().list();
        Map<String, List<Task>> tasksByNameAfterSafetyOfficeTaskComplete = tasksAfterSignalForInvestigation.stream().collect(Collectors.groupingBy(TaskInfo::getName));
        assertTrue("There should not be any safety office task ", !tasksByNameAfterSafetyOfficeTaskComplete.containsKey("Safety Office"));
        assertTrue("There should not be any Down stream task ", !tasksByNameAfterSafetyOfficeTaskComplete.containsKey("Safety Office Review"));
        assertTrue("There should be two investigation task ", tasksByNameAfterSafetyOfficeTaskComplete.get("Investigation").size() == 2 );

        //Complete remaining investigation tasks which should cause the Safety Office Review task to appear
        tasksAfterSignalForInvestigation.stream().filter(task -> task.getName().equals("Investigation")).forEach(investigationTask -> this.taskService.complete(investigationTask.getId()));
        List<Task> tasksAfterCompletingSafetyOfficeAndInvestigations = this.taskService.createTaskQuery().list();
        Map<String, List<Task>> endTaskNames = tasksAfterCompletingSafetyOfficeAndInvestigations.stream().collect(Collectors.groupingBy(TaskInfo::getName));
        assertTrue("There should be one Safety Office Review task ", endTaskNames.get("Safety Office Review").size() == 1);
        assertTrue("There should be no investigation task ", !endTaskNames.containsKey("Investigation"));
        assertTrue("There should be no safety task ", !endTaskNames.containsKey("Safety Office"));

        //complete the Safety Office Review task
        List<Task> endTaskList = this.taskService.createTaskQuery().list();
        assertTrue("There is one task left", endTaskList.size() == 1);
        assertTrue("task left is called Safety Office Review", endTaskList.get(0).getName().equals("Safety Office Review"));

    }

    @Test
    void I_CAN_RE_ENTER_SAFTEY_OFFICE_AFTER_FINISHING_A_REVIEW(){
        //Create a new process instance
        ProcessInstance createdProcessInstance = createProcessInstance();

        //test that one safety office is created
        testOneSafetyOfficeTaskCreated();
        Task safetyOffice = this.taskService.createTaskQuery().singleResult();

        // raise two investigations and test that there are three tasks, one safety office
        raiseInvestigation(createdProcessInstance);
        raiseInvestigation(createdProcessInstance);
        List<Task> tasksAfterTwoInvestigationSignals = this.taskService.createTaskQuery().list();
        assertTrue("There should be three tasks", tasksAfterTwoInvestigationSignals.size() == 3);
        Map<String, List<Task>> tasksByName = tasksAfterTwoInvestigationSignals.stream().collect(Collectors.groupingBy(TaskInfo::getName));
        assertTrue("There should be one safety office task ", tasksByName.get("Safety Office").size() == 1);
        assertTrue("There should be two investigation task ", tasksByName.get("Investigation").size() == 2);

        // complete the safety office task and check there are two investigations tasks and no Safety Office Review tasks
        runtimeService.setVariableLocal(safetyOffice.getExecutionId(),"shouldWaitInvestigations","true");
        this.taskService.complete(safetyOffice.getId());
        List<Task> tasksAfterSignalForInvestigation = this.taskService.createTaskQuery().list();
        Map<String, List<Task>> tasksByNameAfterSafetyOfficeTaskComplete = tasksAfterSignalForInvestigation.stream().collect(Collectors.groupingBy(TaskInfo::getName));
        assertTrue("There should not be any safety office task ", !tasksByNameAfterSafetyOfficeTaskComplete.containsKey("Safety Office"));
        assertTrue("There should not be any Down stream task ", !tasksByNameAfterSafetyOfficeTaskComplete.containsKey("Safety Office Review"));
        assertTrue("There should be two investigation task ", tasksByNameAfterSafetyOfficeTaskComplete.get("Investigation").size() == 2 );

        //Complete remaining investigation tasks which should cause the Safety Office Review task to appear
        tasksAfterSignalForInvestigation.stream().filter(task -> task.getName().equals("Investigation")).forEach(investigationTask -> this.taskService.complete(investigationTask.getId()));
        List<Task> tasksAfterCompletingSafetyOfficeAndInvestigations = this.taskService.createTaskQuery().list();
        Map<String, List<Task>> endTaskNames = tasksAfterCompletingSafetyOfficeAndInvestigations.stream().collect(Collectors.groupingBy(TaskInfo::getName));
        assertTrue("There should be one Safety Office Review task ", endTaskNames.get("Safety Office Review").size() == 1);
        assertTrue("There should be no investigation task ", !endTaskNames.containsKey("Investigation"));
        assertTrue("There should be no safety task ", !endTaskNames.containsKey("Safety Office"));

        //complete the Safety Office Review task
        List<Task> endTaskList = this.taskService.createTaskQuery().list();
        assertTrue("There is one task left", endTaskList.size() == 1);
        assertTrue("task left is called Safety Office Review", endTaskList.get(0).getName().equals("Safety Office Review"));
        this.runtimeService.setVariableLocal(endTaskList.get(0).getExecutionId(),"isSafetyOfficeFinished","false");
        this.taskService.complete(endTaskList.get(0).getId());

        List<Task> restartTaskList = this.taskService.createTaskQuery().list();
        assertTrue("There should be one task left ", restartTaskList.size() == 1);
        assertTrue("The task should be Safety Office", restartTaskList.get(0).getName().equals("Safety Office"));

    }

    @Test
    void I_CAN_FINISH_THE_TASKS_AND_REACH_THE_END(){
        //Create a new process instance
        ProcessInstance createdProcessInstance = createProcessInstance();

        //test that one safety office is created
        testOneSafetyOfficeTaskCreated();
        Task safetyOffice = this.taskService.createTaskQuery().singleResult();

        // raise two investigations and test that there are three tasks, one safety office
        raiseInvestigation(createdProcessInstance);
        raiseInvestigation(createdProcessInstance);
        List<Task> tasksAfterTwoInvestigationSignals = this.taskService.createTaskQuery().list();
        assertTrue("There should be three tasks", tasksAfterTwoInvestigationSignals.size() == 3);
        Map<String, List<Task>> tasksByName = tasksAfterTwoInvestigationSignals.stream().collect(Collectors.groupingBy(TaskInfo::getName));
        assertTrue("There should be one safety office task ", tasksByName.get("Safety Office").size() == 1);
        assertTrue("There should be two investigation task ", tasksByName.get("Investigation").size() == 2);

        // complete the safety office task and check there are two investigations tasks and no Safety Office Review tasks
        runtimeService.setVariableLocal(safetyOffice.getExecutionId(),"shouldWaitInvestigations","true");
        this.taskService.complete(safetyOffice.getId());
        List<Task> tasksAfterSignalForInvestigation = this.taskService.createTaskQuery().list();
        Map<String, List<Task>> tasksByNameAfterSafetyOfficeTaskComplete = tasksAfterSignalForInvestigation.stream().collect(Collectors.groupingBy(TaskInfo::getName));
        assertTrue("There should not be any safety office task ", !tasksByNameAfterSafetyOfficeTaskComplete.containsKey("Safety Office"));
        assertTrue("There should not be any Down stream task ", !tasksByNameAfterSafetyOfficeTaskComplete.containsKey("Safety Office Review"));
        assertTrue("There should be two investigation task ", tasksByNameAfterSafetyOfficeTaskComplete.get("Investigation").size() == 2 );

        //Complete remaining investigation tasks which should cause the Safety Office Review task to appear
        tasksAfterSignalForInvestigation.stream().filter(task -> task.getName().equals("Investigation")).forEach(investigationTask -> this.taskService.complete(investigationTask.getId()));
        List<Task> tasksAfterCompletingSafetyOfficeAndInvestigations = this.taskService.createTaskQuery().list();
        Map<String, List<Task>> endTaskNames = tasksAfterCompletingSafetyOfficeAndInvestigations.stream().collect(Collectors.groupingBy(TaskInfo::getName));
        assertTrue("There should be one Safety Office Review task ", endTaskNames.get("Safety Office Review").size() == 1);
        assertTrue("There should be no investigation task ", !endTaskNames.containsKey("Investigation"));
        assertTrue("There should be no safety task ", !endTaskNames.containsKey("Safety Office"));

        //complete the Safety Office Review task
        List<Task> endTaskList = this.taskService.createTaskQuery().list();
        assertTrue("There is one task left", endTaskList.size() == 1);
        assertTrue("task left is called Safety Office Review", endTaskList.get(0).getName().equals("Safety Office Review"));
        this.runtimeService.setVariableLocal(endTaskList.get(0).getExecutionId(),"isSafetyOfficeFinished","true");
        this.taskService.complete(endTaskList.get(0).getId());

        List<Task> restartTaskList = this.taskService.createTaskQuery().list();
        assertTrue("There should be no more tasks left", restartTaskList.size() == 0);

    }


    private void testOneSafetyOfficeTaskCreated(){
        List<Task> initialTask = this.taskService.createTaskQuery().list();
        Task safetyOffice = initialTask.get(0);
        assertTrue("The task should be safety office ", safetyOffice.getName().equals("Safety Office"));
        assertTrue("There should only be one task created", initialTask.size() ==1);
    }

    private ProcessInstance createProcessInstance() {
        ProcessDefinition processDefinition = this.repositoryService.createProcessDefinitionQuery().singleResult();
        return this.runtimeService.startProcessInstanceByKey(processDefinition.getKey());
    }

    private void raiseInvestigation(ProcessInstance processInstance){
        Execution execution = this.runtimeService.createExecutionQuery()
                .processInstanceId(processInstance.getId())
                .signalEventSubscriptionName("raiseInvestigation")
                .singleResult();
        this.runtimeService.signalEventReceived("raiseInvestigation",execution.getId());
    }


}
