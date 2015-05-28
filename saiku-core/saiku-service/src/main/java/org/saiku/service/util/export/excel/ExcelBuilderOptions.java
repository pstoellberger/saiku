package org.saiku.service.util.export.excel;

public class ExcelBuilderOptions {
	protected boolean repeatValues = true;
	protected String sheetName = null;
	protected boolean showTotals = false;
	public boolean bodyCellBorder = false;
	
	public ExcelBuilderOptions() {};
	
	public ExcelBuilderOptions(String sheetName, Boolean dataStyle, Boolean showTotals) {
		this.sheetName = sheetName;
		if (dataStyle != null) {
			repeatValues = dataStyle;
		}
		if (showTotals != null) {
			this.showTotals = showTotals;
		}
	}

	/**
	 * @return the repeatValues
	 */
	public boolean isRepeatValues() {
		return repeatValues;
	}

	/**
	 * @param repeatValues the repeatValues to set
	 */
	public void setRepeatValues(boolean repeatValues) {
		this.repeatValues = repeatValues;
	}

	/**
	 * @return the sheetName
	 */
	public String getSheetName() {
		return sheetName;
	}

	/**
	 * @param sheetName the sheetName to set
	 */
	public void setSheetName(String sheetName) {
		this.sheetName = sheetName;
	}

	/**
	 * @return the showTotals
	 */
	public boolean isShowTotals() {
		return showTotals;
	}

	/**
	 * @param showTotals the showTotals to set
	 */
	public void setShowTotals(boolean showTotals) {
		this.showTotals = showTotals;
	}

	/**
	 * @return the bodyCellBorder
	 */
	public boolean isBodyCellBorder() {
		return bodyCellBorder;
	}

	/**
	 * @param bodyCellBorder the bodyCellBorder to set
	 */
	public void setBodyCellBorder(boolean bodyCellBorder) {
		this.bodyCellBorder = bodyCellBorder;
	}
}
