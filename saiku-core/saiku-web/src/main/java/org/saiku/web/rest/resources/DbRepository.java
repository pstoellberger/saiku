package org.saiku.web.rest.resources;

import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.List;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.sql.DataSource;
import javax.ws.rs.DELETE;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.commons.dbutils.DbUtils;
import org.apache.commons.dbutils.QueryRunner;
import org.saiku.service.util.exception.SaikuServiceException;
import org.saiku.web.rest.objects.repository.IRepositoryObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DbRepository implements ISaikuRepository {


	private String dataSourceName;

	private Properties sqlMap = new Properties();
	
	private static final Logger log = LoggerFactory.getLogger(DbRepository.class);
	
	private enum SQL {
		check_stale,
		fetch_root,
		fetch_children,
		
		
	}

	public void setDataSourceName(String dataSourceName) {
		this.dataSourceName = dataSourceName;
	}
	
	public void setSqlProperties(String propertiesName) throws IOException {
		Properties props = new Properties();
		try {
			props.load(this.getClass().getClassLoader().getResourceAsStream(propertiesName));
			sqlMap.putAll(props);
		} catch (IOException ioe) {
			log.error("Cannot load sql properties file: " + propertiesName, ioe);
		}
	}

	@GET
	@Produces("application/json")
	public List<IRepositoryObject> getRepository(
			@QueryParam("path") String path, @QueryParam("type") String type) {
		// TODO Auto-generated method stub
		return null;
	}

	@GET
	@Produces("text/plain")
	@Path("/resource")
	public Response getResource(@QueryParam("file") String file) {
		// TODO Auto-generated method stub
		return null;
	}

	@POST
	@Path("/resource")
	public Response saveResource(@FormParam("file") String file,
			@FormParam("content") String content) {
		// TODO Auto-generated method stub
		return null;
	}

	@DELETE
	@Path("/resource")
	public Response deleteResource(@QueryParam("file") String file) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	private DataSource getDataSource() throws Exception {
		InitialContext ctx = new InitialContext();
		DataSource ds = (DataSource) ctx.lookup("java:comp/env/jdbc/" + dataSourceName);
		if (ds == null) {
			throw new SaikuServiceException("Cannot find datasource with name: " + dataSourceName);
		}
		return ds;
	}
	
	private String getSql(SQL stmt) {
		String key = stmt.toString().toLowerCase();
		if (sqlMap.containsKey(key)) {
			return sqlMap.getProperty(key);
		}
		throw new SaikuServiceException("Cannto fetch statement with key: " + stmt);
	}
	

	private List<IRepositoryObject> getRepositoryObjects(int parent, String fileType) throws Exception {
		final Connection con = null;
		try {
				QueryRunner run = new QueryRunner(getDataSource());
				run.
				Statement hibStmt = con.createStatement();
				ResultSet rs = hibStmt.executeQuery("SELECT Country, DataTableName from markets where DataTableName is not null;" );
				while (rs.next()) {
					String country = rs.getString(1);
					String table = rs.getString(2);
					log.debug("Generating schema for country: " + country + " table " + table);
				}
				rs.close();
				hibStmt.close();	
		} catch (Exception e) {
			log.error("Something went wrong generating schemas per country DSP using the database", e);
		} finally {
			DbUtils.closeQuietly(con);
		}
	}


}
