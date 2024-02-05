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

import org.saiku.datasources.datasource.SaikuDatasource;
import org.saiku.service.datasource.DatasourceService;
import org.saiku.service.util.exception.SaikuServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Collection;

@Component
@RestController
@RequestMapping(value = "/saiku/{username}/datasources", produces = MediaType.APPLICATION_JSON_VALUE)
public class DataSourceResource {

    DatasourceService datasourceService;
    
    private static final Logger log = LoggerFactory.getLogger(DataSourceResource.class);
    
    public void setDatasourceService(DatasourceService ds) {
    	datasourceService = ds;
    }
    
    /**
     * Get Data Sources.
     * @return A Collection of SaikuDatasource's.
     */
    @GetMapping
	@ResponseBody
    public Collection<SaikuDatasource> getDatasources() {
    	try {
			return datasourceService.getDatasources().values();
		} catch (SaikuServiceException e) {
			log.error(this.getClass().getName(),e);
			return new ArrayList<SaikuDatasource>();
		}
    }
    
    /**
     * Delete Data Source.
     * @param datasourceName - The name of the data source.
     * @return A GONE Status.
     */

	@DeleteMapping(path = "/{datasource}")
	public ResponseEntity deleteDatasource(@PathVariable("datasource") String datasourceName){
    	datasourceService.removeDatasource(datasourceName);
		return ResponseEntity.status(HttpStatus.GONE).build();
    }

    @GetMapping(path = "/{datasource}")
	@ResponseBody
	public SaikuDatasource getDatasource(@PathVariable("datasource") String datasourceName){
    	return datasourceService.getDatasource(datasourceName);
    }

//    @POST
//    @Consumes({"application/json" })
//	@Path("/{datasource}")
//	public Status addDatasource(@PathParam("datasource") String datasourceName , @Context SaikuDatasource ds){
//    	System.out.println("ds not null:" + (ds != null));
//    	System.out.println("ds name:"+ds.getName());
//    	datasourceService.addDatasource(ds);
//    	return Status.OK;
//    }

}
