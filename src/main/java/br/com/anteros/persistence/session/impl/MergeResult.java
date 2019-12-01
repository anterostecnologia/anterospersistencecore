package br.com.anteros.persistence.session.impl;

public class MergeResult {

	private Object oldObject;
	private Object newObject;
	
	private MergeResult(Object oldObject, Object newObject) {
		this.oldObject = oldObject;
		this.newObject = newObject;
	}
	
	public static MergeResult of(Object oldObject, Object newObject) {
		return new MergeResult(oldObject, newObject);
	}

	public Object getOldObject() {
		return oldObject;
	}

	public Object getNewObject() {
		return newObject;
	}
}
