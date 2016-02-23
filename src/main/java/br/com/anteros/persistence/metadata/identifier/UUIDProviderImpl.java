package br.com.anteros.persistence.metadata.identifier;

import java.io.Serializable;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.util.UUID;

public class UUIDProviderImpl implements UUIDProvider {

	@Override
	public Serializable generateValue(Class<?> uuidType) {
		if (uuidType == Long.class) {
			return generateUniqueId();
		} else if (uuidType == String.class) {
			return UUID.randomUUID().toString();
		} else {
			throw new UUIDProviderException(
					"Esta implementação suporta apenas String e Long. Caso necessite poderá criar um novo provider que gere o UUID no formato desejado. Formato inválido "
							+ uuidType.getSimpleName());
		}
	}

	private Long generateUniqueId() {
		long val = -1;
		do {
			UUID uuid = java.util.UUID.randomUUID();
			final ByteBuffer buffer = ByteBuffer.wrap(new byte[16]);
			buffer.putLong(uuid.getLeastSignificantBits());
			buffer.putLong(uuid.getMostSignificantBits());
			final BigInteger bi = new BigInteger(buffer.array());
			val = bi.longValue();
		} while (val < 0);
		return val;
	}

}
