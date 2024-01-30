package org.saiku.web.rest.resources;

import java.util.List;

import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import org.saiku.web.rest.objects.repository.IRepositoryObject;

public interface ISaikuRepository {

	/**
	 * Get Saved Queries.
	 * @return A list of SavedQuery Objects.
	 */
	@GET
	@Produces({ "application/json" })
	public List<IRepositoryObject> getRepository(
			@QueryParam("path") String path, @QueryParam("type") String type);

	/**
	 * Load a resource.
	 * @param file - The name of the repository file to load.
	 * @return A Repository File Object.
	 */
	@GET
	@Produces({ "text/plain" })
	@Path("/resource")
	public Response getResource(@QueryParam("file") String file);

	/**
	 * Save a resource.
	 * @param file - The name of the repository file to load.
	 * @param content - The content to save.
	 * @return Status
	 */
	@POST
	@Path("/resource")
	public Response saveResource(@FormParam("file") String file,
			@FormParam("content") String content);

	/**
	 * Delete a resource.
	 * @param file - The name of the repository file to load.
	 * @return Status
	 */
	@DELETE
	@Path("/resource")
	public Response deleteResource(@QueryParam("file") String file);

}