/*******************************************************************************
 * Copyright 2012 Anteros Tecnologia
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 *******************************************************************************/
package br.com.anteros.persistence.session.configuration;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.sql.DataSource;

import br.com.anteros.cloud.integration.filesharing.CloudFileManager;
import br.com.anteros.core.configuration.AnterosCoreProperties;
import br.com.anteros.core.log.Logger;
import br.com.anteros.core.log.LoggerProvider;
import br.com.anteros.core.scanner.ClassFilter;
import br.com.anteros.core.scanner.ClassPathScanner;
import br.com.anteros.core.utils.ResourceUtils;
import br.com.anteros.core.utils.StringUtils;
import br.com.anteros.persistence.metadata.EntityCacheManager;
import br.com.anteros.persistence.metadata.accessor.PropertyAccessorFactory;
import br.com.anteros.persistence.metadata.annotation.Converter;
import br.com.anteros.persistence.metadata.annotation.Entity;
import br.com.anteros.persistence.metadata.annotation.EnumValues;
import br.com.anteros.persistence.metadata.comparator.DependencyComparator;
import br.com.anteros.persistence.metadata.configuration.PersistenceModelConfiguration;
import br.com.anteros.persistence.session.SQLSessionFactory;
import br.com.anteros.persistence.session.configuration.exception.AnterosConfigurationException;
import br.com.anteros.persistence.sql.dialect.DatabaseDialect;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;

public abstract class AbstractPersistenceConfiguration extends AnterosBasicConfiguration implements PersistenceConfiguration {

	protected static Logger LOG = LoggerProvider.getInstance().getLogger(AbstractPersistenceConfiguration.class);

	public static final String SECURITY_PACKAGE = "br.com.anteros.security.model.*";
	
	public static final String CONVERTERS_PACKAGE = "br.com.anteros.persistence.metadata.converter.converters";

	public static final String ANNOTATED_CLASSES = "annotatedClasses";

	public static final String PROPERTIES = "properties/property";

	public static final String LOCATION = "location";

	public static final String ID = "id";

	public static final String CLASS_NAME = "className";

	public static final String VALUE = "value";

	public static final String NAME = "name";

	public static final String PROPERTY = "property";

	public static final String DATA_SOURCE_PROPERTY = "dataSource/property";

	public static final String INCLUDE_SECURITY_MODEL = "include-security-model";

	public static final String DATA_SOURCES = "dataSources/dataSource";

	public static final String DATA_SOURCE = "dataSource";

	public static final String PACKAGE_NAME = "package-name";

	public static final String PACKAGE_SCAN_ENTITY = "package-scan-entity";

	public static final String PLACEHOLDER = "placeholder";

	public static String SESSION_FACTORY_PATH = "anteros-configuration/session-factory";

	protected EntityCacheManager entityCacheManager;
	protected DataSource dataSource;
	protected PersistenceModelConfiguration modelConfiguration;
	protected CloudFileManager externalFileManager;
	protected boolean enableImageCompression = false;
	protected Map<Object,Class<?>> entityListeners = new LinkedHashMap<Object,Class<?>>();

	public AbstractPersistenceConfiguration() {
		entityCacheManager = new EntityCacheManager();
	}

	public AbstractPersistenceConfiguration(DataSource dataSource, CloudFileManager externalFileManager, boolean enableImageCompression) {
		this();
		this.dataSource = dataSource;
		this.externalFileManager = externalFileManager;
		this.enableImageCompression = enableImageCompression;
	}

	public AbstractPersistenceConfiguration(PersistenceModelConfiguration modelConfiguration, CloudFileManager externalFileManager, boolean enableImageCompression) {
		this();
		this.modelConfiguration = modelConfiguration;
		this.externalFileManager = externalFileManager;
		this.enableImageCompression = enableImageCompression;
	}

	public AbstractPersistenceConfiguration(DataSource dataSource, PersistenceModelConfiguration modelConfiguration, CloudFileManager externalFileManager, boolean enableImageCompression) {
		super();
		this.dataSource = dataSource;
		this.modelConfiguration = modelConfiguration;
		this.externalFileManager = externalFileManager;
		this.enableImageCompression = enableImageCompression;
	}

	public SessionFactoryConfiguration getSessionFactoryConfiguration() { 
		if (sessionFactory == null)
			sessionFactory = new SessionFactoryConfiguration();
		return sessionFactory;
	}

	public void setSessionFactory(SessionFactoryConfiguration value) {
		this.sessionFactory = value;
	}

	public AbstractPersistenceConfiguration addAnnotatedClass(Class<?> clazz) {
		getSessionFactoryConfiguration().getAnnotatedClasses().getClazz().add(clazz.getName());
		return this;
	}

	public AbstractPersistenceConfiguration addAnnotatedClass(String clazz) {
		getSessionFactoryConfiguration().getAnnotatedClasses().getClazz().add(clazz);
		return this;
	}

	public AbstractPersistenceConfiguration setLocationPlaceHolder(String location) {
		getSessionFactoryConfiguration().getPlaceholder().setLocation(location);
		return this;
	}

	public AbstractPersistenceConfiguration addDataSource(DataSourceConfiguration dataSource) {
		getSessionFactoryConfiguration().getDataSources().getDataSources().add(dataSource);
		return this;
	}

	public AbstractPersistenceConfiguration addDataSource(String id, Class<?> clazz, PropertyConfiguration[] properties) {
		return addDataSource(id, clazz.getName(), properties);
	}

	public AbstractPersistenceConfiguration addDataSource(String id, String clazz, PropertyConfiguration[] properties) {
		DataSourceConfiguration dataSource = new DataSourceConfiguration(id, clazz);
		for (PropertyConfiguration propertyConfiguration : properties) {
			dataSource.getProperties().add(propertyConfiguration);
		}
		getSessionFactoryConfiguration().getDataSources().getDataSources().add(dataSource);
		return this;
	}

	public AbstractPersistenceConfiguration addDataSource(String id, Class<?> clazz, Map<String, String> properties) {
		return addDataSource(id, clazz.getName(), properties);
	}

	public AbstractPersistenceConfiguration addDataSource(String id, String clazz, Map<String, String> properties) {
		List<PropertyConfiguration> props = new ArrayList<PropertyConfiguration>();
		for (String property : properties.keySet()) {
			props.add(new PropertyConfiguration().setName(property).setValue(properties.get(property)));
		}
		return addDataSource(id, clazz, props.toArray(new PropertyConfiguration[] {}));
	}

	public AbstractPersistenceConfiguration addDataSource(String id, Class<?> clazz, Properties properties) {
		return addDataSource(id, clazz.getName(), properties);
	}

	public AbstractPersistenceConfiguration addDataSource(String id, String clazz, Properties properties) {
		List<PropertyConfiguration> props = new ArrayList<PropertyConfiguration>();
		for (Object property : properties.keySet()) {
			props.add(new PropertyConfiguration().setName((String) property).setValue((String) properties.get(property)));
		}
		return addDataSource(id, clazz, props.toArray(new PropertyConfiguration[] {}));
	}

	public AbstractPersistenceConfiguration addProperty(PropertyConfiguration property) {
		getSessionFactoryConfiguration().getProperties().getProperties().add(property);
		return this;
	}

	public AbstractPersistenceConfiguration addProperties(Properties properties) {
		for (Object property : properties.keySet()) {
			addProperty(new PropertyConfiguration().setName((String) property).setValue((String) properties.get(property)));
		}
		return this;
	}

	public AbstractPersistenceConfiguration addProperties(PropertyConfiguration[] properties) {
		for (PropertyConfiguration property : properties) {
			addProperty(property);
		}
		return this;
	}

	public AbstractPersistenceConfiguration addProperty(String name, String value) {
		addProperty(new PropertyConfiguration().setName(name).setValue(value));
		return this;
	}
	
	public AbstractPersistenceConfiguration addEntityListener(Class<?> entity, Object listener) {
		entityListeners.put(listener, entity);
		return this;
	}

	protected void prepareClassesToLoad() throws ClassNotFoundException {
		LOG.debug("Preparando classes para ler entidades.");
		if ((getSessionFactoryConfiguration().getPackageToScanEntity() != null)
				&& (!"".equals(getSessionFactoryConfiguration().getPackageToScanEntity().getPackageName()))) {
			if (getSessionFactoryConfiguration().isIncludeSecurityModel())
				getSessionFactoryConfiguration().getPackageToScanEntity().setPackageName(
						getSessionFactoryConfiguration().getPackageToScanEntity().getPackageName() + ", " + SECURITY_PACKAGE);
			String[] packages = StringUtils.tokenizeToStringArray(getSessionFactoryConfiguration().getPackageToScanEntity().getPackageName(), ", ;");
			List<Class<?>> scanClasses = new ArrayList<Class<?>>();
			
			try (ScanResult scanResult = new ClassGraph().enableAllInfo().acceptPackages(packages).acceptPackages(CONVERTERS_PACKAGE)
		            .scan()) {
				
				ClassInfoList filtered = scanResult.getAllClasses()
			            .filter(classInfo -> {			            	
			            	boolean result = (classInfo.hasAnnotation(Entity.class.getName())
				                    || classInfo.hasAnnotation(EnumValues.class.getName())
				                    || classInfo.hasAnnotation(Converter.class.getName()));
			            	
			                return result;
			               
			               });
			            
				scanClasses = filtered.loadClasses();					
			}		
			
			
			if (LOG.isDebugEnabled()) {
				for (Class<?> cl : scanClasses) {
					LOG.debug("Encontrado classe scaneada " + cl.getName());
				}
			}
			getSessionFactoryConfiguration().addToAnnotatedClasses(scanClasses);
		}

		if ((getSessionFactoryConfiguration().getClasses() == null) || (getSessionFactoryConfiguration().getClasses().size() == 0))
			LOG.debug("Não foram encontradas classes representando entidades. Informe o pacote onde elas podem ser localizadas ou informe manualmente cada uma delas.");

		LOG.debug("Preparação das classes concluída.");
	}

	public abstract SQLSessionFactory buildSessionFactory() throws Exception;

	public EntityCacheManager loadEntities(DatabaseDialect databaseDialect) throws Exception {
		List<Class<? extends Serializable>> classes = getSessionFactoryConfiguration().getClasses();
		Collections.sort(classes, new DependencyComparator());

		if (modelConfiguration != null)
			this.entityCacheManager.load(modelConfiguration, getPropertyAccessorFactory(), databaseDialect, entityListeners);
		else
			this.entityCacheManager.load(classes, true, getPropertyAccessorFactory(), databaseDialect, entityListeners);
		return this.entityCacheManager;
	}

	@Override
	public AbstractPersistenceConfiguration configure() throws AnterosConfigurationException {
		return configure(AnterosCoreProperties.XML_CONFIGURATION);
	}

	@Override
	public AbstractPersistenceConfiguration configure(String xmlFile) throws AnterosConfigurationException {
		
		try {
			InputStream is;
			final List<URL> resources = ResourceUtils.getResources(xmlFile, getClass());
			if ((resources != null) && (resources.size() > 0)) {
				final URL url = resources.get(0);
				is = url.openStream();
				configure(is);
				is.close();
				return this;
			}
		} catch (final Exception e) {
			throw new AnterosConfigurationException("Impossível realizar a leitura " + xmlFile + " " + e);
		}

		throw new AnterosConfigurationException("Arquivo de configuração " + xmlFile + " não encontrado.");
	}

	@Override
	public AbstractPersistenceConfiguration configure(InputStream xmlConfiguration) throws AnterosConfigurationException {
		try {
			final AbstractPersistenceConfiguration result = parseXmlConfiguration(xmlConfiguration);
			this.setSessionFactory(result.getSessionFactoryConfiguration());
			this.dataSource = null;
			this.buildDataSource();
			return this;
		} catch (final InvocationTargetException e) {
			throw new AnterosConfigurationException("Impossível realizar a leitura do arquivo de configuração." + e.getTargetException());
		} catch (final Exception e) {
			throw new AnterosConfigurationException("Impossível realizar a leitura do arquivo de configuração." + e);
		}
	}

	protected abstract AbstractPersistenceConfiguration parseXmlConfiguration(InputStream xmlConfiguration) throws Exception;

	protected abstract void buildDataSource() throws Exception;

	public DataSource getDataSource() {
		return dataSource;
	}

	public AbstractPersistenceConfiguration dataSource(DataSource dataSource) {
		this.dataSource = dataSource;
		return this;
	}

	public AbstractPersistenceConfiguration modelConfiguration(PersistenceModelConfiguration modelConfiguration) {
		this.modelConfiguration = modelConfiguration;
		return this;
	}

	public EntityCacheManager getEntityCacheManager() {
		return entityCacheManager;
	}

	public String getProperty(String name) {
		return getSessionFactoryConfiguration().getProperties().getProperty(name);
	}

	public AbstractPersistenceConfiguration setPlaceHolder(InputStream placeHolder) throws IOException {
		if (placeHolder != null) {
			Properties props = new Properties();
			props.load(placeHolder);
			getSessionFactoryConfiguration().getPlaceholder().setProperties(props);
		}
		return this;
	}

	public AbstractPersistenceConfiguration setProperties(Properties props) {
		getSessionFactoryConfiguration().getProperties().setProperties(props);
		return this;
	}

	@Override
	public AbstractPersistenceConfiguration configure(InputStream xmlConfiguration, InputStream placeHolder) throws AnterosConfigurationException {
		try {
			final AnterosBasicConfiguration result = parseXmlConfiguration(xmlConfiguration);
			result.setPlaceHolder(placeHolder);
			this.setSessionFactory(result.getSessionFactoryConfiguration());
			this.dataSource = null;
			this.buildDataSource();

			return this;
		} catch (final Exception e) {
			e.printStackTrace();
			throw new AnterosConfigurationException("Impossível realizar a leitura do arquivo de configuração." + e);
		}
	}

	public abstract PropertyAccessorFactory getPropertyAccessorFactory();

	public boolean isEnableImageCompression() {
		return enableImageCompression;
	}

	public void setEnableImageCompression(boolean enableImageCompression) {
		this.enableImageCompression = enableImageCompression;
	}

}
