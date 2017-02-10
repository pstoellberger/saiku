package org.saiku.olap.dto;

public class SaikuMeasure extends SaikuMember {
	
	private Boolean calculated;
	private Boolean isDefaultMeasure;

	public SaikuMeasure() {}

	public SaikuMeasure(
			String name, 
			String uniqueName, 
			String caption, 
			String description, 
			String dimensionUniqueName, 
			String hierarchyUniqueName, 
			String levelUniqueName,
			boolean visible,
			boolean calculated,
			boolean defaultMeasure)
	{
		super(name, uniqueName, caption, description, dimensionUniqueName, hierarchyUniqueName, levelUniqueName);
		this.calculated = calculated;
		this.isDefaultMeasure = defaultMeasure;
	}

	/**
	 * @return the calculated
	 */
	public Boolean isCalculated() {
		return calculated;
	}
	public Boolean isDefaultMeasure() {
		return isDefaultMeasure;
	}


}
