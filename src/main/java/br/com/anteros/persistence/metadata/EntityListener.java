package br.com.anteros.persistence.metadata;

import java.lang.reflect.Method;

import br.com.anteros.persistence.metadata.annotation.EventType;

public class EntityListener {
	Object targetObject;
	Method method;
	EventType eventType;

	private EntityListener(Object targetObject, Method method, EventType eventType) {
		this.targetObject = targetObject;
		this.method = method;
		this.eventType = eventType;
	}

	public static EntityListener of(Object targetObject, Method method, EventType eventType) {
		return new EntityListener(targetObject, method, eventType);
	}

	public Object getTargetObject() {
		return targetObject;
	}

	public Method getMethod() {
		return method;
	}

	public EventType getEventType() {
		return eventType;
	}
	

}
