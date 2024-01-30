package org.saiku.plugin.resources;

import jakarta.ws.rs.Path;

import org.saiku.web.rest.resources.ExporterResource;

@Path("/saiku/api/{username}/export")
public class PentahoExporterResource extends ExporterResource {

}
