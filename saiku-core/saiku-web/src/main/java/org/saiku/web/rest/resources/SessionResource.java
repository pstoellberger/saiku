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

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.saiku.service.ISessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@Component
@RestController
@RequestMapping("/saiku/session")
public class SessionResource  {


	private static final Logger log = LoggerFactory.getLogger(SessionResource.class);

	private ISessionService sessionService;

	public void setSessionService(ISessionService ss) {
		this.sessionService = ss;
	}

	@PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	public ResponseEntity login(
			HttpServletRequest req,
			@RequestParam("username") String username,
			@RequestParam("password") String password) 
	{
		try {
			sessionService.login(req, username, password);
			return getSession(req);
		}
		catch (Exception e) {
			log.debug("Error logging in:" + username, e);
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity getSession(HttpServletRequest req) {
		
		Map<String, Object> sess = sessionService.getSession();
		try {
			String acceptLanguage = req.getLocale().getLanguage();
			if (StringUtils.isNotBlank(acceptLanguage)) {
				sess.put("language", acceptLanguage);
			}
		} catch (Exception e) {
			log.debug("Cannot get language!", e);
		}
		return ResponseEntity.ok(sess);
	}

	@DeleteMapping
	public ResponseEntity logout(HttpServletRequest req)
	{
		sessionService.logout(req);
		//		NewCookie terminate = new NewCookie(TokenBasedRememberMeServices.SPRING_SECURITY_REMEMBER_ME_COOKIE_KEY, null);

		return ResponseEntity.ok().build();

	}


}
