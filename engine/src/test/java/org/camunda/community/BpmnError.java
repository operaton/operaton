package org.camunda.community;

public class BpmnError extends org.operaton.bpm.engine.delegate.BpmnError {
	
	private static final long serialVersionUID = 1L;

	public BpmnError(String errorCode) {
		super(errorCode);
	}

}
