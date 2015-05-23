package org.saiku.olap.query2;

public class ThinMember {
	
	private String name;
	private String uniqueName;
	private String caption;
	
	public ThinMember() {};

	public ThinMember(String name, String uniqueName, String caption) {
		this.name = name;
		this.uniqueName = uniqueName;
		this.caption = caption;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the uniqueName
	 */
	public String getUniqueName() {
		return uniqueName;
	}

	/**
	 * @param uniqueName the uniqueName to set
	 */
	public void setUniqueName(String uniqueName) {
		this.uniqueName = uniqueName;
	}

	/**
	 * @return the caption
	 */
	public String getCaption() {
		return caption;
	}

	/**
	 * @param caption the caption to set
	 */
	public void setCaption(String caption) {
		this.caption = caption;
	}

	@Override
	public String toString() {
		return uniqueName.toString();
	}
	

}
