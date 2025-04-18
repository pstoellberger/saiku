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
package org.saiku.web.rest.resources;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

import org.saiku.olap.dto.SaikuConnection;
import org.saiku.olap.dto.SaikuCube;
import org.saiku.olap.dto.SaikuCubeMetadata;
import org.saiku.olap.dto.SaikuDimension;
import org.saiku.olap.dto.SaikuHierarchy;
import org.saiku.olap.dto.SaikuLevel;
import org.saiku.olap.dto.SaikuMember;
import org.saiku.olap.dto.SimpleCubeElement;
import org.saiku.service.olap.OlapDiscoverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
@Path("/saiku/{username}/discover")
public class OlapDiscoverResource implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private OlapDiscoverService olapDiscoverService;
    
    private static final Logger log = LoggerFactory.getLogger(OlapDiscoverResource.class);
    
    public void setOlapDiscoverService(OlapDiscoverService olapds) {
        olapDiscoverService = olapds;
    }
    
    @GET
    @Produces({"application/json" })
     public List<SaikuConnection> getConnections() {
    	try {
			return olapDiscoverService.getAllConnections();
		} catch (Exception e) {
			log.error(this.getClass().getName(),e);
			return new ArrayList<SaikuConnection>();
		}
    }
    
    
    /**
     * Returns the datasources available.
     */
    @GET
    @Produces({"application/json" })
    @Path("/{connection}")
     public List<SaikuConnection> getConnections( @PathParam("connection") String connectionName) {
    	try {
			return olapDiscoverService.getConnection(connectionName);
		} catch (Exception e) {
			log.error(this.getClass().getName(),e);
			return new ArrayList<SaikuConnection>();
		}
    }


    @GET
    @Produces({"application/json" })
  	@Path("/refresh")
     public List<SaikuConnection> refreshConnections() {
    	try {
    		olapDiscoverService.refreshAllConnections();
			return olapDiscoverService.getAllConnections();
		} catch (Exception e) {
			log.error(this.getClass().getName(),e);
			return new ArrayList<SaikuConnection>();
		}
    }
    
    @GET
    @Produces({"application/json" })
    @Path("/{connection}/refresh")
     public List<SaikuConnection> refreshConnection( @PathParam("connection") String connectionName) {
    	try {
			olapDiscoverService.refreshConnection(connectionName);
			return olapDiscoverService.getConnection(connectionName);
		} catch (Exception e) {
			log.error(this.getClass().getName(),e);
			return new ArrayList<SaikuConnection>();
		}
    }
    
    
	@GET
    @Produces({"application/json" })
	@Path("/{connection}/{catalog}/{schema}/{cube}/metadata")
     public SaikuCubeMetadata getMetadata(
    		 @PathParam("connection") String connectionName, 
    		 @PathParam("catalog") String catalogName, 
    		 @PathParam("schema") String schemaName, 
    		 @PathParam("cube") String cubeName) 
    {
		if ("null".equals(schemaName)) {
			schemaName = "";
		}
		SaikuCube cube = new SaikuCube(connectionName, cubeName,cubeName,cubeName, catalogName, schemaName);
		try {
			List<SaikuDimension> dimensions = olapDiscoverService.getAllDimensions(cube);
			List<SaikuMember> measures = olapDiscoverService.getMeasures(cube);
			Map<String, Object> properties = olapDiscoverService.getProperties(cube);
			return new SaikuCubeMetadata(dimensions, measures, properties);
		} catch (Exception e) {
			log.error(this.getClass().getName(),e);
		}
		return new SaikuCubeMetadata(null, null, null);
	}
	
	@GET
    @Produces({"application/json" })
	@Path("/{connection}/{catalog}/{schema}/{cube}/dimensions")
     public List<SaikuDimension> getDimensions(
    		 @PathParam("connection") String connectionName, 
    		 @PathParam("catalog") String catalogName, 
    		 @PathParam("schema") String schemaName, 
    		 @PathParam("cube") String cubeName) 
    {
		if ("null".equals(schemaName)) {
			schemaName = "";
		}
		SaikuCube cube = new SaikuCube(connectionName, cubeName,cubeName,cubeName, catalogName, schemaName);
		try {
			return olapDiscoverService.getAllDimensions(cube);
		} catch (Exception e) {
			log.error(this.getClass().getName(),e);
		}
		return new ArrayList<SaikuDimension>();
	}
	
	@GET
    @Produces({"application/json" })
	@Path("/{connection}/{catalog}/{schema}/{cube}/dimensions/{dimension}")
     public SaikuDimension getDimension(
    		 @PathParam("connection") String connectionName, 
    		 @PathParam("catalog") String catalogName, 
    		 @PathParam("schema") String schemaName, 
    		 @PathParam("cube") String cubeName,
    		 @PathParam("dimension") String dimensionName) 
    {
		if ("null".equals(schemaName)) {
			schemaName = "";
		}
		SaikuCube cube = new SaikuCube(connectionName, cubeName,cubeName,cubeName, catalogName, schemaName);
		try {
			return olapDiscoverService.getDimension(cube, dimensionName);
		} catch (Exception e) {
			log.error(this.getClass().getName(),e);
		}
		return null;
	}
	
	@GET
    @Produces({"application/json" })
	@Path("/{connection}/{catalog}/{schema}/{cube}/dimensions/{dimension}/hierarchies")
     public List<SaikuHierarchy> getDimensionHierarchies(@PathParam("connection") String connectionName, 
    		 									@PathParam("catalog") String catalogName, 
    		 									@PathParam("schema") String schemaName, 
    		 									@PathParam("cube") String cubeName, 
    		 									@PathParam("dimension") String dimensionName) {
		if ("null".equals(schemaName)) {
			schemaName = "";
		}
		SaikuCube cube = new SaikuCube(connectionName, cubeName,cubeName,cubeName, catalogName, schemaName);
		try {
			return olapDiscoverService.getAllDimensionHierarchies(cube, dimensionName);
		} catch (Exception e) {
			log.error(this.getClass().getName(),e);
		}
		return new ArrayList<SaikuHierarchy>();
	}
	
	@GET
	@Produces({"application/json" })
	@Path("/{connection}/{catalog}/{schema}/{cube}/dimensions/{dimension}/hierarchies/{hierarchy}/levels")
	public List<SaikuLevel> getHierarchy(@PathParam("connection") String connectionName, 
				@PathParam("catalog") String catalogName, 
				@PathParam("schema") String schemaName, 
				@PathParam("cube") String cubeName, 
				@PathParam("dimension") String dimensionName, 
				@PathParam("hierarchy") String hierarchyName)
	{
		if ("null".equals(schemaName)) {
			schemaName = "";
		}
		SaikuCube cube = new SaikuCube(connectionName, cubeName,cubeName,cubeName, catalogName, schemaName);
		try {
			return olapDiscoverService.getAllHierarchyLevels(cube, dimensionName, hierarchyName);
		} catch (Exception e) {
			log.error(this.getClass().getName(),e);
		}
		return new ArrayList<SaikuLevel>();
	}

	@GET
	@Produces({"application/json" })
	@Path("/{connection}/{catalog}/{schema}/{cube}/dimensions/{dimension}/hierarchies/{hierarchy}/levels/{level}")
	public List<SimpleCubeElement> getLevelMembers(
			@PathParam("connection") String connectionName, 
			@PathParam("catalog") String catalogName, 
			@PathParam("schema") String schemaName, 
			@PathParam("cube") String cubeName, 
			@PathParam("dimension") String dimensionName, 
			@PathParam("hierarchy") String hierarchyName,
			@PathParam("level") String levelName)
	{
		if ("null".equals(schemaName)) {
			schemaName = "";
		}

		SaikuCube cube = new SaikuCube(connectionName, cubeName,cubeName,cubeName, catalogName, schemaName);

		try {
			return olapDiscoverService.getLevelMembers(cube, hierarchyName, levelName);
		} catch (Exception e) {
			log.error(this.getClass().getName(),e);
		}
		return new ArrayList<SimpleCubeElement>();
	}
   
	@GET
	@Produces({"application/json" })
	@Path("/{connection}/{catalog}/{schema}/{cube}/hierarchies/{hierarchy}/rootmembers")
	public List<SaikuMember> getRootMembers(
			@PathParam("connection") String connectionName, 
			@PathParam("catalog") String catalogName, 
			@PathParam("schema") String schemaName, 
			@PathParam("cube") String cubeName, 
			@PathParam("hierarchy") String hierarchyName)
		{
		if ("null".equals(schemaName)) {
			schemaName = "";
		}
		SaikuCube cube = new SaikuCube(connectionName, cubeName,cubeName,cubeName, catalogName, schemaName);
		try {
			return olapDiscoverService.getHierarchyRootMembers(cube, hierarchyName);
		} catch (Exception e) {
			log.error(this.getClass().getName(),e);
		}
		return null;
	}

	
	@GET
	@Path("/{connection}/{catalog}/{schema}/{cube}/hierarchies/")
    @Produces({"application/json" })
     public List<SaikuHierarchy> getCubeHierarchies(@PathParam("connection") String connectionName, 
    		 									@PathParam("catalog") String catalogName, 
    		 									@PathParam("schema") String schemaName, 
    		 									@PathParam("cube") String cubeName) {
		if ("null".equals(schemaName)) {
			schemaName = "";
		}
		SaikuCube cube = new SaikuCube(connectionName, cubeName,cubeName,cubeName, catalogName, schemaName);
		try {
			return olapDiscoverService.getAllHierarchies(cube);
		} catch (Exception e) {
			log.error(this.getClass().getName(),e);
		}
		return new ArrayList<SaikuHierarchy>();
	}
	
	@GET
	@Path("/{connection}/{catalog}/{schema}/{cube}/measures/")
    @Produces({"application/json" })
     public List<SaikuMember> getCubeMeasures(@PathParam("connection") String connectionName, 
    		 									@PathParam("catalog") String catalogName, 
    		 									@PathParam("schema") String schemaName, 
    		 									@PathParam("cube") String cubeName) {
		if ("null".equals(schemaName)) {
			schemaName = "";
		}
		SaikuCube cube = new SaikuCube(connectionName, cubeName,cubeName,cubeName, catalogName, schemaName);
		try {
			return olapDiscoverService.getMeasures(cube);
		} catch (Exception e) {
			log.error(this.getClass().getName(),e);
		}
		return new ArrayList<SaikuMember>();
	}
	
	@GET
	@Produces({"application/json" })
	@Path("/{connection}/{catalog}/{schema}/{cube}/member/{member}")
	public SaikuMember getMember(
			@PathParam("connection") String connectionName, 
			@PathParam("catalog") String catalogName, 
			@PathParam("schema") String schemaName, 
			@PathParam("cube") String cubeName, 
			@PathParam("member") String memberName)
	{
		if ("null".equals(schemaName)) {
			schemaName = "";
		}
		SaikuCube cube = new SaikuCube(connectionName, cubeName,cubeName,cubeName, catalogName, schemaName);
		try {
			return olapDiscoverService.getMember(cube, memberName);
		} catch (Exception e) {
			log.error(this.getClass().getName(),e);
		}
		return null;
	}
	
	@GET
	@Produces({"application/json" })
	@Path("/{connection}/{catalog}/{schema}/{cube}/member/{member}/children")
	public List<SaikuMember> getMemberChildren(
			@PathParam("connection") String connectionName, 
			@PathParam("catalog") String catalogName, 
			@PathParam("schema") String schemaName, 
			@PathParam("cube") String cubeName, 
			@PathParam("member") String memberName)
	{
		if ("null".equals(schemaName)) {
			schemaName = "";
		}
		SaikuCube cube = new SaikuCube(connectionName, cubeName,cubeName,cubeName, catalogName, schemaName);
		try {
			return olapDiscoverService.getMemberChildren(cube, memberName);
		} catch (Exception e) {
			log.error(this.getClass().getName(),e);
		}
		return new ArrayList<SaikuMember>();
	}

}
