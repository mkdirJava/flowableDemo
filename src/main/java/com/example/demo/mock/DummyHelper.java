package com.example.demo.mock;

import org.flowable.engine.impl.persistence.entity.ExecutionEntity;
import org.springframework.stereotype.Component;

@Component
public class DummyHelper{
		
	public void doSomthing(ExecutionEntity executionEntity) {
		System.out.println("I am moving through the execution "+ executionEntity.getName());
	}

}