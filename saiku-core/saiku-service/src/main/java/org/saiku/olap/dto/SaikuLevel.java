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
package org.saiku.olap.dto;

import java.util.Map;

public class SaikuLevel extends AbstractSaikuObject {

	private String caption;
	private String hierarchyUniqueName;
	private String dimensionUniqueName;
	private boolean visible;
	private String description;
	
	private String levelType;
	//private final Map<String, String> annotations;
	private final Map<String, Object> flags;

	public SaikuLevel() {
		super(null, null);
		throw new RuntimeException("Unsupported Constructor. Serialization only");
	}


	public SaikuLevel(
			String name,
			String uniqueName,
			String caption,
			String description,
			String dimensionUniqueName,
			String hierarchyUniqueName,
			boolean visible,
			String levelType,
			Map<String, Object> flags) {
		super(uniqueName, name);
		this.caption = caption;
		this.hierarchyUniqueName = hierarchyUniqueName;
		this.dimensionUniqueName = dimensionUniqueName;
		this.visible = visible;
		this.description = description;
		this.flags = flags;		
		//this.annotations = annotations;
		this.levelType = levelType;
	}

	public String getCaption() {
		return caption;
	}

	public String getHierarchyUniqueName() {
		return hierarchyUniqueName;
	}

	public String getDimensionUniqueName() {
		return dimensionUniqueName;
	}

	public boolean isVisible() {
		return visible;
	}

	public String getDescription() {
		return description;
	}
	
	//public Map<String, String> getAnnotations() {
	//	return annotations;
	//}
	
	public Map<String, Object> getFlags() {
		return flags;
	}
	
	public String getLevelType() {
		return levelType;
	}
}
