package com.example.demo.bpnm.service.node;

import org.flowable.task.api.Task;

import com.example.demo.bpnm.service.node.model.UserNode;

public interface IGetBpnmModel {
	UserNode getUserNodesInModel(Task task);
}
