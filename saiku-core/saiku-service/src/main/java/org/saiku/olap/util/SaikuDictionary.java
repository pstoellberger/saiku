package org.saiku.olap.util;

import org.apache.commons.lang.StringUtils;

public class SaikuDictionary {
	
	public enum DateFlag {
		CURRENT,
		LAST,
		AGO,
		CURRENTWEEK,
		LASTWEEK,
		CURRENTMONTH,
		LASTMONTH
	};
	
	public final static String DateFlags = "DateFlags"; 
	
	public static String getAllDateFlags() {
		return getDateFlags(DateFlag.values());
	}
	
	public static String getAllPeriodFlags() {
		return getDateFlags(DateFlag.CURRENT, DateFlag.LAST, DateFlag.AGO);
	}
	
	public static String getDateFlags(DateFlag... flags) {
		if (flags != null) {
			return StringUtils.join(flags, "|");
		}
		return StringUtils.join(DateFlag.values(), "|");
	}

}
