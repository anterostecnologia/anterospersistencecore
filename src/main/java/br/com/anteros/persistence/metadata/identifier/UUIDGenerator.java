package br.com.anteros.persistence.metadata.identifier;

import java.io.Serializable;

import br.com.anteros.core.utils.ReflectionUtils;
import br.com.anteros.persistence.metadata.descriptor.DescriptionGenerator;

public class UUIDGenerator implements IdentifierGenerator {
	private Class<UUIDProvider> uuidClassGenerator; 
	private UUIDProvider generator;
	private Class<?> uuidType;

	@SuppressWarnings("unchecked")
	public UUIDGenerator(DescriptionGenerator descriptionGenerator) throws UUIDGeneratorException, InstantiationException, IllegalAccessException {
		if (!(ReflectionUtils.isImplementsInterface(descriptionGenerator.getUuidClassGenerator(), UUIDProvider.class))){
			throw new UUIDGeneratorException("Classe "+descriptionGenerator.getUuidClassGenerator()+" não implementa a interface UUIDProvider.");
		}
		this.uuidType = descriptionGenerator.getUuidType();
		this.uuidClassGenerator = (Class<UUIDProvider>) descriptionGenerator.getUuidClassGenerator();
		this.generator = uuidClassGenerator.newInstance();
	}

	@Override
	public Serializable generate() throws Exception {
		return generator.generateValue(uuidType);
	}

}
