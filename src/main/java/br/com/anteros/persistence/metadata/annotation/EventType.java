package br.com.anteros.persistence.metadata.annotation;

public enum EventType {
	PrePersist,
	PreRemove,
	PostPersist,
	PostRemove,
	PreUpdate,
	PostUpdate,
	PreValidate,
	PostValidate
}
