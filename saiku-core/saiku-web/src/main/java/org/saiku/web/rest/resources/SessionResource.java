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

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringUtils;
import org.saiku.service.ISessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@Component
@Path("/saiku/session")
public class SessionResource  {


	private static final Logger log = LoggerFactory.getLogger(SessionResource.class);

	private ISessionService sessionService;

	public void setSessionService(ISessionService ss) {
		this.sessionService = ss;
	}

	@POST
	@Consumes("application/x-www-form-urlencoded")
	public Response login(
			@Context HttpServletRequest req,
			@FormParam("username") String username, 
			@FormParam("password") String password) 
	{
		try {
			sessionService.login(req, username, password);
			
			return Response.ok().build();
		}
		catch (Exception e) {
			log.debug("Error logging in:" + username, e);
			return Response.status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@GET
	@Consumes("application/x-www-form-urlencoded")
	@Produces(MediaType.APPLICATION_JSON)
	public Map<String,Object> getSession(@Context HttpServletRequest req) {
		
		Map<String, Object> sess = sessionService.getSession();
		try {
			String acceptLanguage = req.getLocale().getLanguage();
			if (StringUtils.isNotBlank(acceptLanguage)) {
				sess.put("language", acceptLanguage);
			}
		} catch (Exception e) {
			log.debug("Cannot get language!", e);
		}
		return sess;
	}

	@DELETE
	public Response logout(@Context HttpServletRequest req) 
	{
		sessionService.logout(req);
		//		NewCookie terminate = new NewCookie(TokenBasedRememberMeServices.SPRING_SECURITY_REMEMBER_ME_COOKIE_KEY, null);

		return Response.ok().build();

	}


}
