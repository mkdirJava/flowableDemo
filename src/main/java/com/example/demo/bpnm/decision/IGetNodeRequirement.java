package com.example.demo.bpnm.decision;

import org.flowable.task.api.Task;

public interface IGetNodeRequirement {
	RequiredVariablesAndPotentialActions getNodeRequirements(Task task);
}
