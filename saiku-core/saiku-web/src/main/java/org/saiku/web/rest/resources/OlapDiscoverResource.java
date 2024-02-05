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

import org.saiku.olap.dto.*;
import org.saiku.service.olap.OlapDiscoverService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@RestController
@RequestMapping("/saiku/{username}/discover")
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
    
    @GetMapping
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
    @GetMapping("/{connection}")
     public List<SaikuConnection> getConnections(@PathVariable("connection") String connectionName) {
    	try {
			return olapDiscoverService.getConnection(connectionName);
		} catch (Exception e) {
			log.error(this.getClass().getName(),e);
			return new ArrayList<SaikuConnection>();
		}
    }


    @GetMapping("/refresh")
     public List<SaikuConnection> refreshConnections() {
    	try {
    		olapDiscoverService.refreshAllConnections();
			return olapDiscoverService.getAllConnections();
		} catch (Exception e) {
			log.error(this.getClass().getName(),e);
			return new ArrayList<SaikuConnection>();
		}
    }
    
    @GetMapping("/{connection}/refresh")
     public List<SaikuConnection> refreshConnection( @PathVariable("connection") String connectionName) {
    	try {
			olapDiscoverService.refreshConnection(connectionName);
			return olapDiscoverService.getConnection(connectionName);
		} catch (Exception e) {
			log.error(this.getClass().getName(),e);
			return new ArrayList<SaikuConnection>();
		}
    }
    
    
	@GetMapping("/{connection}/{catalog}/{schema}/{cube}/metadata")
     public SaikuCubeMetadata getMetadata(
    		 @PathVariable("connection") String connectionName,
    		 @PathVariable("catalog") String catalogName,
    		 @PathVariable("schema") String schemaName,
    		 @PathVariable("cube") String cubeName)
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

	@GetMapping("/{connection}/{catalog}/{schema}/{cube}/dimensions")
     public List<SaikuDimension> getDimensions(
    		 @PathVariable("connection") String connectionName,
    		 @PathVariable("catalog") String catalogName,
    		 @PathVariable("schema") String schemaName,
    		 @PathVariable("cube") String cubeName)
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

	@GetMapping("/{connection}/{catalog}/{schema}/{cube}/dimensions/{dimension}")
     public SaikuDimension getDimension(
    		 @PathVariable("connection") String connectionName,
    		 @PathVariable("catalog") String catalogName,
    		 @PathVariable("schema") String schemaName,
    		 @PathVariable("cube") String cubeName,
    		 @PathVariable("dimension") String dimensionName)
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

	@GetMapping("/{connection}/{catalog}/{schema}/{cube}/dimensions/{dimension}/hierarchies")
     public List<SaikuHierarchy> getDimensionHierarchies(@PathVariable("connection") String connectionName,
    		 									@PathVariable("catalog") String catalogName,
    		 									@PathVariable("schema") String schemaName,
    		 									@PathVariable("cube") String cubeName,
    		 									@PathVariable("dimension") String dimensionName) {
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

	@GetMapping("/{connection}/{catalog}/{schema}/{cube}/dimensions/{dimension}/hierarchies/{hierarchy}/levels")
	public List<SaikuLevel> getHierarchy(@PathVariable("connection") String connectionName,
				@PathVariable("catalog") String catalogName,
				@PathVariable("schema") String schemaName,
				@PathVariable("cube") String cubeName,
				@PathVariable("dimension") String dimensionName,
				@PathVariable("hierarchy") String hierarchyName)
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

	@GetMapping("/{connection}/{catalog}/{schema}/{cube}/dimensions/{dimension}/hierarchies/{hierarchy}/levels/{level}")
	public List<SimpleCubeElement> getLevelMembers(
			@PathVariable("connection") String connectionName,
			@PathVariable("catalog") String catalogName,
			@PathVariable("schema") String schemaName,
			@PathVariable("cube") String cubeName,
			@PathVariable("dimension") String dimensionName,
			@PathVariable("hierarchy") String hierarchyName,
			@PathVariable("level") String levelName)
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
   
	@GetMapping("/{connection}/{catalog}/{schema}/{cube}/hierarchies/{hierarchy}/rootmembers")
	public List<SaikuMember> getRootMembers(
			@PathVariable("connection") String connectionName,
			@PathVariable("catalog") String catalogName,
			@PathVariable("schema") String schemaName,
			@PathVariable("cube") String cubeName,
			@PathVariable("hierarchy") String hierarchyName)
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

	
	@GetMapping("/{connection}/{catalog}/{schema}/{cube}/hierarchies/")
    public List<SaikuHierarchy> getCubeHierarchies(@PathVariable("connection") String connectionName,
    		 									@PathVariable("catalog") String catalogName,
    		 									@PathVariable("schema") String schemaName,
    		 									@PathVariable("cube") String cubeName) {
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
	
	@GetMapping("/{connection}/{catalog}/{schema}/{cube}/measures/")
    public List<SaikuMember> getCubeMeasures(@PathVariable("connection") String connectionName,
    		 									@PathVariable("catalog") String catalogName,
    		 									@PathVariable("schema") String schemaName,
    		 									@PathVariable("cube") String cubeName) {
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

	@GetMapping("/{connection}/{catalog}/{schema}/{cube}/member/{member}")
	public SaikuMember getMember(
			@PathVariable("connection") String connectionName,
			@PathVariable("catalog") String catalogName,
			@PathVariable("schema") String schemaName,
			@PathVariable("cube") String cubeName,
			@PathVariable("member") String memberName)
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
	
	@GetMapping("/{connection}/{catalog}/{schema}/{cube}/member/{member}/children")
	public List<SaikuMember> getMemberChildren(
			@PathVariable("connection") String connectionName,
			@PathVariable("catalog") String catalogName,
			@PathVariable("schema") String schemaName,
			@PathVariable("cube") String cubeName,
			@PathVariable("member") String memberName)
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
