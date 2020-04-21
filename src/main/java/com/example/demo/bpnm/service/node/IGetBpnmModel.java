package com.example.demo.bpnm.service.node;

import com.example.demo.bpnm.service.node.model.UserNode;

public interface IGetBpnmModel {
	UserNode getUserNodesInModel(String processDefinitionId);
}
