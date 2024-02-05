package org.saiku.web.rest.resources;

import mondrian.olap.MondrianServer;
import mondrian.olap.MondrianServer.MondrianVersion;
import mondrian.server.monitor.ConnectionInfo;
import mondrian.server.monitor.Monitor;
import mondrian.server.monitor.ServerInfo;
import mondrian.server.monitor.StatementInfo;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.List;

@Component
@RequestMapping("/saiku/statistics")
public class StatisticsResource {

//	StringWriter sqlWriter = new StringWriter();
//	StringWriter mdxWriter = new StringWriter();
//	StringWriter profileWriter = new StringWriter();
//	StringWriter saikuWriter = new StringWriter();
//	
//	public StatisticsResource() {
//		setupLog("mondrian.sql", "DEBUG", sqlWriter);
//		setupLog("mondrian.mdx", "DEBUG", mdxWriter);
//		setupLog("mondrian.profile", "DEBUG", profileWriter);
//		setupLog("org.saiku.service", "INFO", saikuWriter);
//		System.out.println("##########################SETUP LOG");
//		
//		
//	}
	
//	private void setupLog(String category, String level, StringWriter writer) {
//		Logger pkgLogger = Logger.getRootLogger().getLoggerRepository().getLogger(category);
//		pkgLogger.setLevel(Level.toLevel(level));
//		WriterAppender appender = new WriterAppender(new PatternLayout("%d{ISO8601} %p - %m%n"), writer);
//		appender.setName("CONSOLE_APPENDER");
//		appender.setThreshold(Level.ERROR);
//		pkgLogger.addAppender(appender);
//		Logger.getRootLogger().addAppender(appender);
//	}
	
	
	@GetMapping(value = "/mondrian", produces = MediaType.APPLICATION_JSON_VALUE)
	public MondrianStats getMondrianStats() {
		
		MondrianServer mondrianServer = MondrianServer.forId(null);
		if (mondrianServer != null) {
			MondrianVersion mv = mondrianServer.getVersion();
			
			final Monitor monitor = mondrianServer.getMonitor();
	        final ServerInfo server = monitor.getServer();
	        
	        int statementCurrentlyOpenCount = server.statementEndCount - server.statementStartCount;
	        int connectionCurrentlyOpenCount = server.connectionEndCount - server.connectionStartCount;
	        int sqlStatementCurrentlyOpenCount = server.sqlStatementEndCount - server.sqlStatementStartCount;
	        int statementCurrentlyExecutingCount = server.statementEndCount - server.statementStartCount;
	        float avgCellDimensionality =  ((float) server.cellCoordinateCount / (float) server.cellCount);
	        
	        final List<ConnectionInfo> connections = monitor.getConnections();
	        final List<StatementInfo> statements = monitor.getStatements();
	        
	        return new MondrianStats(
	        		server,
	        		mv,
	        		statementCurrentlyOpenCount,
	        		connectionCurrentlyOpenCount,
	        		sqlStatementCurrentlyOpenCount,
	        		statementCurrentlyExecutingCount,
	        		avgCellDimensionality,
	        		connections,
	        		statements
	        		);
		}
		
		return null;
	}


	@GetMapping(value = "/mondrian/server", produces = MediaType.APPLICATION_JSON_VALUE)
	public ServerInfo getMondrianServer() {
		MondrianServer mondrianServer = MondrianServer.forId(null);
		if (mondrianServer != null) {
			MondrianVersion mv = mondrianServer.getVersion();
			
			final Monitor monitor = mondrianServer.getMonitor();
	        final ServerInfo server = monitor.getServer();
	        return server;
		}
		return null;
	}
	
//	@GET
//	@Produces({"text/plain" })
//	@Path("/log/sql")
//	public String getSqlLog() {
//		return sqlWriter.toString();
//	}
//	
//	@GET
//	@Produces({"text/plain" })
//	@Path("/log/mdx")
//	public String getMdxLog() {
//		return mdxWriter.toString();
//	}
//	
//	@GET
//	@Produces({"text/plain" })
//	@Path("/log/profile")
//	public String getProfileLog() {
//		return profileWriter.toString();
//	}
//	
//	@GET
//	@Produces({"text/plain" })
//	@Path("/log/saiku")
//	public String getSaikuLog() {
//		return saikuWriter.toString();
//	}
	
	
}
