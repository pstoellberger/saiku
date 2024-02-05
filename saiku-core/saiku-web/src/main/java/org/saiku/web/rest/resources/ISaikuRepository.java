package org.saiku.web.rest.resources;

import java.util.List;

import org.saiku.web.rest.objects.repository.IRepositoryObject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

public interface ISaikuRepository {

	/**
	 * Get Saved Queries.
	 * @return A list of SavedQuery Objects.
	 */

	@GetMapping(value = "/", produces = {MediaType.APPLICATION_JSON_VALUE})
	public List<IRepositoryObject> getRepository(
			@RequestParam("path") String path, @RequestParam("type") String type);

	/**
	 * Load a resource.
	 * @param file - The name of the repository file to load.
	 * @return A Repository File Object.
	 */

	@GetMapping(value = "/resource", produces = {MediaType.TEXT_PLAIN_VALUE})
	public ResponseEntity getResource(@RequestParam("file") String file);

	/**
	 * Save a resource.
	 *
	 * @param file    - The name of the repository file to load.
	 * @param content - The content to save.
	 * @return Status
	 */

	@PostMapping(value = "/resource")
	public ResponseEntity saveResource(@RequestParam("file") String file,
									   @RequestParam("content") String content);

	/**
	 * Delete a resource.
	 *
	 * @param file - The name of the repository file to load.
	 * @return Status
	 */

	@DeleteMapping(value = "/resource")
	public ResponseEntity deleteResource(@RequestParam("file") String file);

}