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


import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.apache.commons.vfs2.*;
import org.saiku.service.ISessionService;
import org.saiku.service.util.Env;
import org.saiku.service.util.exception.SaikuServiceException;
import org.saiku.service.util.export.CsvExporter;
import org.saiku.web.rest.objects.acl.Acl;
import org.saiku.web.rest.objects.acl.AclEntry;
import org.saiku.web.rest.objects.acl.enumeration.AclMethod;
import org.saiku.web.rest.objects.repository.IRepositoryObject;
import org.saiku.web.rest.objects.repository.RepositoryFileObject;
import org.saiku.web.rest.objects.repository.RepositoryFolderObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;


/**
 * QueryServlet contains all the methods required when manipulating an OLAP Query.
 * @author Paul Stoellberger
 *
 */
@Component
@RestController
@RequestMapping("/saiku/api/repository")
public class BasicRepositoryResource2 implements ISaikuRepository {

	private static final Logger log = LoggerFactory.getLogger(BasicRepositoryResource2.class);

	private FileObject repo;
	private FileObject accessLogDir;
	private boolean hasAccessLog = false;
	private String ACCESS_LOG_NAME = "saiku_repo_access";

	private ISessionService sessionService;

	private Acl acl;

	private FileObject resolve(String path) throws Exception {
		FileSystemManager fileSystemManager;
		if (!path.endsWith("" + File.separatorChar)) {
			path += File.separatorChar;
		}
		path = Env.resolve(path);
		fileSystemManager = VFS.getManager();
		FileObject fileObject;
		fileObject = fileSystemManager.resolveFile(path);
		if (fileObject == null) {
			throw new IOException("File cannot be resolved: " + path);
		}
		if(!fileObject.exists()) {
			throw new IOException("File does not exist: " + path);
		}
		return fileObject;
	}

	public void setPath(String path) throws Exception {
		try {
			repo = resolve(path);
		} catch (Exception e) {
			log.error("Error setting path for repository: " + path, e);
		}
	}

	public void setAccessLogDir(String path) throws Exception {
		try {
			FileObject f = resolve(path);
			if (!f.isWriteable()) {
				throw new IOException("Directory is not writeable: " +  path);
			}
			if (!f.getType().equals(FileType.FOLDER)) {
				throw new IOException("Access Log path is not a directory: " +  path);
			}
			hasAccessLog = true;
			accessLogDir = f;
		} catch (Exception e) {
			log.error("Error setting path for access log dir: " + path, e);
		}
	}

	public void setAcl(Acl acl) {
		this.acl = acl;
	}

	/**
	 * Sets the sessionService
	 * @param sessionService
	 */
	public void setSessionService(ISessionService sessionService){
		this.sessionService = sessionService;
	}

	/* (non-Javadoc)
	 * @see org.saiku.web.rest.resources.ISaikuRepository#getRepository(java.lang.String, java.lang.String)
	 */
	@GetMapping
	public List<IRepositoryObject> getRepository (
			@Nullable @RequestParam("path") String path,
			@Nullable @RequestParam("type") String type)
	{
		List<IRepositoryObject> objects = new ArrayList<IRepositoryObject>();
		try {
			if (path != null && (path.startsWith("/") || path.startsWith("."))) {
				throw new IllegalArgumentException("Path cannot be null or start with \"/\" or \".\" - Illegal Path: " + path);
			}

			if (repo != null) {
				FileObject folder = repo;
				if (path != null) {
					folder = repo.resolveFile(path);
				} else {
					path = repo.getName().getRelativeName(folder.getName());
				}

				String username = sessionService.getAllSessionObjects().get("username").toString();
				List<String> roles = (List<String> ) sessionService.getAllSessionObjects().get("roles");

				//TODO : shall throw an exception ???
				if ( !acl.canRead(path,username, roles) ) {
					return new ArrayList<IRepositoryObject>(); // empty
				} else {
					objects = getRepositoryObjects(folder, type);
				}
			}
			else {
				throw new Exception("repo URL is null");
			}
		} catch (Exception e) {
			log.error(this.getClass().getName(),e);
		}
		return objects;
	}


	@GetMapping(value = "/resource/acl", produces = {MediaType.APPLICATION_JSON_VALUE})
	public AclEntry getResourceAcl(@RequestParam("file") String file) {
		try {
			if (file == null || file.startsWith("/") || file.startsWith(".")) {
				throw new IllegalArgumentException("Path cannot be null or start with \"/\" or \".\" - Illegal Path: " + file);
			}
			String username = sessionService.getAllSessionObjects().get("username").toString();
			List<String> roles = (List<String> ) sessionService.getAllSessionObjects().get("roles");
			if (acl.canGrant(file, username, roles) ) {
				return getAcl(file);
			}
		} catch (Exception e) {
			log.error("Error retrieving ACL for file: " + file, e);
		}
		throw new SaikuServiceException("You dont have permission to retrieve ACL for file: " + file);
	}



	@PostMapping(value = "/resource/acl", produces = {MediaType.APPLICATION_JSON_VALUE})
	public ResponseEntity setResourceAcl(
			@RequestParam("file") String file,
			@RequestParam("acl") String aclEntry)
	{
		try {
			if (file == null || file.startsWith("/") || file.startsWith(".")) {
				throw new IllegalArgumentException("Path cannot be null or start with \"/\" or \".\" - Illegal Path: " + file);
			}
			ObjectMapper mapper = new ObjectMapper();
			log.debug("Set ACL to " + file + " : " + aclEntry);
			AclEntry ae = mapper.readValue(aclEntry, AclEntry.class);
			String username = sessionService.getAllSessionObjects().get("username").toString();
			List<String> roles = (List<String> ) sessionService.getAllSessionObjects().get("roles");
			FileObject repoFile = repo.resolveFile(file);
			if (repoFile.exists() && acl.canGrant(file, username, roles) ) {
				acl.addEntry(file, ae);
				return ResponseEntity.ok().build();
			}
			log.debug("Repo file does not exist or cannot grant access. repo file:" + repoFile + " - file: " + file);
		} catch (Exception e) {
			log.error("An error occured while setting permissions to file: " + file, e);
		}
		return ResponseEntity.internalServerError().build();
	}


	/* (non-Javadoc)
	 * @see org.saiku.web.rest.resources.ISaikuRepository#getResource(java.lang.String)
	 */
	@GetMapping(value = "/resource", produces = {MediaType.TEXT_PLAIN_VALUE})
	public ResponseEntity getResource (@RequestParam("file") String file)
	{
		try {
			if (file == null || file.startsWith("/") || file.startsWith(".")) {
				throw new IllegalArgumentException("Path cannot be null or start with \"/\" or \".\" - Illegal Path: " + file);
			}
			String username = sessionService.getAllSessionObjects().get("username").toString();
			List<String> roles = (List<String> ) sessionService.getAllSessionObjects().get("roles");
			FileObject repoFile = repo.resolveFile(file);
			if ( !acl.canRead(file, username, roles) ) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
			}
//			System.out.println("path:" + repo.getName().getRelativeName(repoFile.getName()));
			if (repoFile.exists()) {
				writeToLog(username, file);
				InputStreamReader reader = new InputStreamReader(repoFile.getContent().getInputStream());
				BufferedReader br = new BufferedReader(reader);
				String chunk ="",content ="";
				while ((chunk = br.readLine()) != null) {
					content += chunk + "\n";
				}
				byte[] doc = content.getBytes("UTF-8");
				return ResponseEntity.ok().contentLength(doc.length).contentType(MediaType.TEXT_PLAIN).body(doc);
			}
			else {
				throw new Exception("File does not exist:" + repoFile.getName().getPath());
			}
		} catch(Exception e){
			log.error("Cannot load query (" + file + ")",e);
			String error = ExceptionUtils.getRootCauseMessage(e);
			return ResponseEntity.internalServerError().body(error);
		}
	}

	/* (non-Javadoc)
	 * @see org.saiku.web.rest.resources.ISaikuRepository#saveResource(java.lang.String, java.lang.String)
	 */
	@PostMapping("/resource")
	public ResponseEntity saveResource (
			@RequestParam("file") String file,
			@RequestParam("content") String content)
	{
		try {
			if (file == null || file.startsWith("/") || file.startsWith(".")) {
				throw new IllegalArgumentException("Path cannot be null or start with \"/\" or \".\" - Illegal Path: " + file);
			}

			String username = sessionService.getAllSessionObjects().get("username").toString();
			List<String> roles = (List<String> ) sessionService.getAllSessionObjects().get("roles");
			FileObject repoFile = repo.resolveFile(file);

			if ( !acl.canWrite(file,username, roles) ) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN).contentType(MediaType.TEXT_PLAIN)
						.body("You don't have permissions to save here!");

			}

			if (repoFile == null) throw new Exception("Repo File not found");

			if (repoFile.exists()) {
				repoFile.delete();
			}
			if (!StringUtils.isNotBlank(content)) {
				repoFile.createFolder();
			} else {
				repoFile.createFile();
				OutputStreamWriter ow = new OutputStreamWriter(repoFile.getContent().getOutputStream());
				BufferedWriter bw = new BufferedWriter(ow);
				bw.write(content);
				bw.close();
			}
			return ResponseEntity.ok().build();
		} catch(Exception e){
			log.error("Cannot save resource to ( file: " + file + ")",e);
		}
		return ResponseEntity.internalServerError().contentType(MediaType.TEXT_PLAIN)
				.body("Cannot save resource to ( file: " + file + ")");
	}

	/* (non-Javadoc)
	 * @see org.saiku.web.rest.resources.ISaikuRepository#deleteResource(java.lang.String)
	 */
	@DeleteMapping("/resource")
	public ResponseEntity deleteResource (
			@RequestParam("file") String file)
	{
		try {
			if (file == null || file.startsWith("/") || file.startsWith(".")) {
				throw new IllegalArgumentException("Path cannot be null or start with \"/\" or \".\" - Illegal Path: " + file);
			}


			String username = sessionService.getAllSessionObjects().get("username").toString();
			List<String> roles = (List<String> ) sessionService.getAllSessionObjects().get("roles");
				FileObject repoFile = repo.resolveFile(file);

				if (repoFile != null && repoFile.exists() ) {
					if ( acl.canWrite(file, username, roles) ){
						if (repoFile.getType().equals(FileType.FILE)) {
							repoFile.delete();
						} else {
							repoFile.delete(new AllFileSelector());
						}
						return ResponseEntity.ok().build();
					} else {
						return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
					}
				}
		} catch(Exception e){
			log.error("Cannot save resource to (file: " + file + ")",e);
		}
		return ResponseEntity.internalServerError().build();
	}

	/* (non-Javadoc)
	 * @see org.saiku.web.rest.resources.ISaikuRepository#saveResource(java.lang.String, java.lang.String)
	 */
	@PostMapping("/resource/move")
	public ResponseEntity moveResource(
			@RequestParam("source") String source,
			@RequestParam("target") String target)
	{
		try {
			if (source == null || source.startsWith("/") || source.startsWith(".")) {
				throw new IllegalArgumentException("Path cannot be null or start with \"/\" or \".\" - Illegal Path: " + source);
			}
			if (target == null || target.startsWith("/") || target.startsWith(".")) {
				throw new IllegalArgumentException("Path cannot be null or start with \"/\" or \".\" - Illegal Path: " + target);
			}

			String username = sessionService.getAllSessionObjects().get("username").toString();
			List<String> roles = (List<String> ) sessionService.getAllSessionObjects().get("roles");
			FileObject targetFile = repo.resolveFile(target);

			if ( !acl.canWrite(target,username, roles) ) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN)
						.contentType(MediaType.TEXT_PLAIN)
						.body("You don't have permissions to save here!");
			}

			if (targetFile == null) throw new Exception("Repo File not found");

			if (targetFile.exists()) {
				throw new Exception("Target file exists already. Cannot write: " + target);
			}

			FileObject sourceFile = repo.resolveFile(source);
			if ( !acl.canRead(source, username, roles) ) {
				return ResponseEntity.status(HttpStatus.FORBIDDEN)
						.contentType(MediaType.TEXT_PLAIN)
						.body("You don't have permissions to read the source file: " + source);
			}

			if (!sourceFile.exists()) {
				throw new Exception("Source file does not exist: " + source);
			}
			if (!sourceFile.canRenameTo(targetFile)) {
				throw new Exception("Cannot rename " + source + " to " + target);
			}
			sourceFile.moveTo(targetFile);
			return ResponseEntity.ok().build();
		} catch(Exception e){
			log.error("Cannot move resource from " + source + " to " + target ,e);
			return ResponseEntity.internalServerError().body("Cannot move resource from " + source + " to " + target + " ( " + e.getMessage() + ")");
		}

	}


	@GetMapping("/zip")
	public ResponseEntity getResourcesAsZip (
			@RequestParam("directory") String directory,
			@RequestParam("files") String files)
	{
		try {
			if (StringUtils.isBlank(directory))
				return ResponseEntity.ok().build();

			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			ZipOutputStream zos = new ZipOutputStream(bos);

			String[] fileArray = null;
			if (StringUtils.isBlank(files)) {
				FileObject dir = repo.resolveFile(directory);
				for (FileObject fo : dir.getChildren()) {
					if (fo.getType().equals(FileType.FILE)) {
						String entry = fo.getName().getBaseName();
						if ("saiku".equals(fo.getName().getExtension())) {
							byte[] doc = FileUtil.getContent(fo);
							ZipEntry ze = new ZipEntry(entry);
							zos.putNextEntry(ze);
							zos.write(doc);
						}
					}
				}
			} else {
				fileArray = files.split(",");
				for (String f : fileArray) {
					String resource = directory + "/" + f;
					ResponseEntity r = getResource(resource);
					if (HttpStatus.OK.equals(r.getStatusCode())) {
						byte[] doc = (byte[]) r.getBody();
						ZipEntry ze = new ZipEntry(f);
						zos.putNextEntry(ze);
						zos.write(doc);
					}
				}
			}
			zos.closeEntry();
			zos.close();
			byte[] zipDoc = bos.toByteArray();

			return ResponseEntity.ok().contentType(MediaType.APPLICATION_OCTET_STREAM)
					.header("content-disposition",
							"attachment; filename = " + directory + ".zip")
					.header("content-length", String.valueOf(zipDoc.length)).body(zipDoc);


		} catch(Exception e){
			log.error("Cannot zip resources " + files ,e);
			String error = ExceptionUtils.getRootCauseMessage(e);
			return ResponseEntity.internalServerError().body(error);
		}

	}


	@PostMapping("/zipupload")
	public ResponseEntity uploadArchiveZip(
			@RequestParam("directory") String directory,
			@RequestParam("file") MultipartFile file)
	{
		String zipFile = file.getOriginalFilename();
		String output = "";
		try {
			if (StringUtils.isBlank(zipFile))
				throw new Exception("You must specify a zip file to upload");

			output = "Uploding file: " + zipFile + " ...\r\n";
			ZipInputStream zis = new ZipInputStream(file.getInputStream());
		    ZipEntry ze = zis.getNextEntry();
		    byte[] doc = null;
		    boolean isFile = false;
		    if (ze == null) {
		    	doc = IOUtils.toByteArray(file.getInputStream());
		    	isFile = true;
		    }
			while (ze != null || doc != null) {
					String fileName = null;
				   if (!isFile) {
					   fileName = ze.getName();
					   doc = IOUtils.toByteArray(zis);
				   } else {
					   fileName = zipFile;
				   }

		    	   output += "Saving " + fileName + "... ";
		    	   String fullPath = (StringUtils.isNotBlank(directory)) ? directory + "/" + fileName : fileName;

		    	   String content = new String(doc);
		    	   ResponseEntity r = saveResource(fullPath, content);
		    	   doc = null;

		    	   if (HttpStatus.OK != r.getStatusCode()) {
		    		   output += " ERROR: " + r.getBody().toString() + "\r\n";
		    	   } else {
		    		   output += " OK\r\n";
		    	   }
		    	   if (!isFile)
		    		   ze = zis.getNextEntry();
		    	}

				if (!isFile) {
					zis.closeEntry();
					zis.close();
				}
				file.getInputStream().close();

		    	output += " SUCCESSFUL!\r\n";
		    	return ResponseEntity.ok(output);

		} catch(Exception e){
			log.error("Cannot unzip resources " + zipFile ,e);
			String error = ExceptionUtils.getRootCauseMessage(e);
			return ResponseEntity.internalServerError().body(output + "\r\n" + error);
		}


	}

	private List<IRepositoryObject> getRepositoryObjects(FileObject root, String fileType) throws Exception {
		List<IRepositoryObject> repoObjects = new ArrayList<IRepositoryObject>();
		FileObject[] objects = new FileObject[0];
		if (root.getType().equals(FileType.FOLDER)) {
			objects = root.getChildren();
		} else {
			objects = new FileObject[]{ root };
		}


		for (FileObject file : objects) {
			if (!file.isHidden()) {
				String filename = file.getName().getBaseName();
				String relativePath = repo.getName().getRelativeName(file.getName());

				String username = sessionService.getAllSessionObjects().get("username").toString();
				List<String> roles = (List<String> ) sessionService.getAllSessionObjects().get("roles");
				if ( acl.canRead(relativePath,username, roles) ) {
					List<AclMethod> acls = acl.getMethods(relativePath, username, roles);
					if (file.getType().equals(FileType.FILE)) {
						if (StringUtils.isNotEmpty(fileType) && !filename.endsWith(fileType)) {
							continue;
						}
						String extension = file.getName().getExtension();

						repoObjects.add(new RepositoryFileObject(filename, "#" + relativePath, extension, relativePath, acls));
					}
					if (file.getType().equals(FileType.FOLDER)) {
						repoObjects.add(new RepositoryFolderObject(filename, "#" + relativePath, relativePath, acls, getRepositoryObjects(file, fileType)));
					}
					Collections.sort(repoObjects, new Comparator<IRepositoryObject>() {

						public int compare(IRepositoryObject o1, IRepositoryObject o2) {
							if (o1.getType().equals(IRepositoryObject.Type.FOLDER) && o2.getType().equals(IRepositoryObject.Type.FILE))
								return -1;
							if (o1.getType().equals(IRepositoryObject.Type.FILE) && o2.getType().equals(IRepositoryObject.Type.FOLDER))
								return 1;
							return o1.getName().toLowerCase().compareTo(o2.getName().toLowerCase());

						}

					});
				}
			}
		}
		return repoObjects;
	}



	private AclEntry getAcl(String path){
		AclEntry entry = this.acl.getEntry(path);
		if ( entry == null ) entry = new AclEntry();
		return entry;
	}

	private FileObject getCurrentLogFile() throws FileSystemException {
		if (hasAccessLog) {
			String filename = ACCESS_LOG_NAME + "." + DateFormatUtils.format(new Date(), "yyyy-MM-dd");
			return accessLogDir.resolveFile(filename);
		}
		return null;
	}

	private void writeToLog(String username, String file) {
		try {
			if (hasAccessLog) {
				FileObject logFile = getCurrentLogFile();
				if (!logFile.exists()) {
					logFile.createFile();
				}
				OutputStreamWriter ow = new OutputStreamWriter(logFile.getContent().getOutputStream(true));
				String timeStamp =  DateFormatUtils.format(new Date(), "yyyy-MM-dd HH:mm:ss.SSS");
				String line = CsvExporter.convertCsvLine(";", "\"", timeStamp, username, file);
				log.debug("Writing line:" + line);
				BufferedWriter bw = new BufferedWriter(ow);
				bw.write(line);
				bw.newLine();
				bw.flush();
				bw.close();

			}
		} catch (Exception e) {
			log.debug("Could not write to access log", e);
		}
	}

}
