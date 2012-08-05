package com.data2semantics.yasgui.shared;

import java.io.Serializable;

public class RdfNodeContainer implements Serializable {
	private static final long serialVersionUID = 1L;
	private String varName;
	private String value;
	private boolean isUri;
	
	public RdfNodeContainer() {
		
	}
	
	public String getVarName() {
		return varName;
	}
	public void setVarName(String varName) {
		this.varName = varName;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}

	public boolean isUri() {
		return isUri;
	}

	public void setIsUri(boolean isUri) {
		this.isUri = isUri;
	}
		
		
}
