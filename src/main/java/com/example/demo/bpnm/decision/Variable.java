package com.example.demo.bpnm.decision;

public class Variable {
	
	private String value;
	private String dataType;
	
	public Variable(String value, String dataType) {
		super();
		this.value = value;
		this.dataType = dataType;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public String getDataType() {
		return dataType;
	}
	public void setDataType(String dataType) {
		this.dataType = dataType;
	}
	
	

}
