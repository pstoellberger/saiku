/*  
 *   Copyright 2012 OSBI Ltd
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package org.saiku.plugin;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import mondrian.olap.MondrianProperties;
import mondrian.olap.Util;
import mondrian.rolap.RolapConnectionProperties;
import mondrian.util.Pair;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.pentaho.platform.api.engine.IPentahoSession;
import org.pentaho.platform.engine.core.system.PentahoSessionHolder;
import org.pentaho.platform.engine.core.system.PentahoSystem;
import org.pentaho.platform.plugin.action.mondrian.catalog.IMondrianCatalogService;
import org.pentaho.platform.plugin.action.mondrian.catalog.MondrianCatalog;
import org.pentaho.platform.util.messages.LocaleHelper;
import org.saiku.datasources.connection.ISaikuConnection;
import org.saiku.datasources.datasource.SaikuDatasource;
import org.saiku.service.datasource.IDatasourceManager;

public class PentahoDatasourceManager implements IDatasourceManager {

	private static final Log LOG = LogFactory.getLog(PentahoDatasourceManager.class);

	private Map<String,SaikuDatasource> datasources = Collections.synchronizedMap(new HashMap<String,SaikuDatasource>());

	private String saikuDatasourceProcessor;

	private String saikuConnectionProcessor;

	private String dynamicSchemaProcessor;

	private IPentahoSession session;

	private IMondrianCatalogService catalogService;

	private String datasourceResolver;

	public void setDatasourceResolverClass(String datasourceResolverClass) {
		this.datasourceResolver = datasourceResolverClass;
	}

	public void setSaikuDatasourceProcessor(String datasourceProcessor) {
		this.saikuDatasourceProcessor = datasourceProcessor;
	}

	public void setSaikuConnectionProcessor(String connectionProcessor) {
		this.saikuConnectionProcessor = connectionProcessor;
	}

	public void setDynamicSchemaProcessor(String dynamicSchemaProcessor) {
		this.dynamicSchemaProcessor = dynamicSchemaProcessor;
	}
	
	public void setSession(IPentahoSession session) {
		this.session = session;
	}

	public PentahoDatasourceManager() {
	}

	public PentahoDatasourceManager(IPentahoSession session) {
		this.session = session;
	}


	public void init() {
		load();
	}

	public void load() {
		datasources.clear();
		loadDatasources();
	}

	private Map<String, SaikuDatasource> loadDatasources() {
		try {
			if (this.session == null) {
				this.session = PentahoSessionHolder.getSession();
			}

			ClassLoader cl = this.getClass().getClassLoader();
			ClassLoader cl2 = this.getClass().getClassLoader().getParent();

			Thread.currentThread().setContextClassLoader(cl2);
			this.catalogService = PentahoSystem.get(IMondrianCatalogService.class,
					session);

			List<MondrianCatalog> catalogs = catalogService.listCatalogs(session, true);
			Thread.currentThread().setContextClassLoader(cl);
			if (StringUtils.isNotBlank(this.datasourceResolver)) {
				MondrianProperties.instance().DataSourceResolverClass.setString(this.datasourceResolver);
			}

			for (MondrianCatalog catalog : catalogs) {
				String name = catalog.getName();
				Util.PropertyList parsedProperties = Util.parseConnectString(catalog
						.getDataSourceInfo());

				String dynProcName = parsedProperties.get(
						RolapConnectionProperties.DynamicSchemaProcessor.name());
				if (StringUtils.isNotBlank(dynamicSchemaProcessor) && StringUtils.isBlank(dynProcName)) {
					parsedProperties.put(RolapConnectionProperties.DynamicSchemaProcessor.name(), dynamicSchemaProcessor);

				}

				StringBuilder builder = new StringBuilder();
				builder.append("jdbc:mondrian:");
				builder.append("Catalog=");
				builder.append(catalog.getDefinition());
				builder.append("; ");

				Iterator<Pair<String, String>> it = parsedProperties.iterator();

				while (it.hasNext()) {
					Pair<String, String> pair = it.next();
					builder.append(pair.getKey());
					builder.append("=");
					builder.append(pair.getValue());
					builder.append("; ");
				}

				//				builder.append("PoolNeeded=false; ");

				builder.append("Locale=");
		        if (session != null && session.getLocale() != null) {
					builder.append(session.getLocale().toString());
				} else {
					builder.append(LocaleHelper.getLocale().toString());
				}
				builder.append(";");

				String url = builder.toString();

				LOG.debug("NAME: " + catalog.getName() + " DSINFO: " + url + "  ###CATALOG: " + catalog.getName());

				Properties props = new Properties();
				props.put("driver", "mondrian.olap4j.MondrianOlap4jDriver");
				props.put("location",url);

				if (saikuDatasourceProcessor != null) {
					props.put(ISaikuConnection.DATASOURCE_PROCESSORS, saikuDatasourceProcessor);
				}
				if (saikuConnectionProcessor != null) {
					props.put(ISaikuConnection.CONNECTION_PROCESSORS, saikuConnectionProcessor);
				}
				props.list(System.out);

				SaikuDatasource sd = new SaikuDatasource(name, SaikuDatasource.Type.OLAP, props);
				datasources.put(name, sd);


			}
			return datasources;
		} catch(Exception e) {
			e.printStackTrace();
			LOG.error(e);
		}
		return new HashMap<String, SaikuDatasource>();
	}



	public SaikuDatasource addDatasource(SaikuDatasource datasource) {
		throw new UnsupportedOperationException();
	}

	public SaikuDatasource setDatasource(SaikuDatasource datasource) {
		throw new UnsupportedOperationException();
	}

	public List<SaikuDatasource> addDatasources(List<SaikuDatasource> datasources) {
		throw new UnsupportedOperationException();
	}

	public boolean removeDatasource(String datasourceName) {
		throw new UnsupportedOperationException();
	}

	public Map<String,SaikuDatasource> getDatasources() {
		return loadDatasources();
	}

	public SaikuDatasource getDatasource(String datasourceName) {
		return loadDatasources().get(datasourceName);
	}




}
