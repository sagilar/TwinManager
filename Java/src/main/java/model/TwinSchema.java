package model;

import java.io.File;
import java.util.List;

public class TwinSchema {
	// TO DO: INITIALIZATION FROM FILES
	private String className;
	private File file;
	private List<Attribute> attributes;
	private List<Operation> operations;
	
	public TwinSchema() {
	}
	
	public TwinSchema(String className) {
		this.className = className;
	}	
	
	public TwinSchema(String className, File file) {
		this.file = file;
	}	
	
	public List<Attribute> getAttributes(){
		return this.attributes;
	}
	
	public List<Operation> getOperations(){
		return this.operations;
	}
	
	public class Attribute{
		String name;
		String type;
		
		public String getName() {
			return this.name;
		}
		
		public void setName(String name) {
			this.name = name;
		}
	}
	
	public class Operation{
		String name;
		List<String> parameters;
		
		public void setName(String name) {
			this.name = name;
		}
		
		public String getName() {
			return this.name;
		}
	}

}
