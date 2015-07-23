package org.saiku.olap.query2.util;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.Set;

import mondrian.olap.Annotation;
import mondrian.olap4j.SaikuMondrianHelper;
import mondrian.util.Format;

import org.apache.commons.lang.StringUtils;
import org.olap4j.Axis;
import org.olap4j.OlapException;
import org.olap4j.metadata.Cube;
import org.olap4j.metadata.Hierarchy;
import org.olap4j.metadata.Level;
import org.olap4j.metadata.Measure;
import org.saiku.olap.query2.ThinAxis;
import org.saiku.olap.query2.ThinCalculatedMeasure;
import org.saiku.olap.query2.ThinDetails;
import org.saiku.olap.query2.ThinHierarchy;
import org.saiku.olap.query2.ThinLevel;
import org.saiku.olap.query2.ThinMeasure;
import org.saiku.olap.query2.ThinMeasure.Type;
import org.saiku.olap.query2.ThinMember;
import org.saiku.olap.query2.ThinQuery;
import org.saiku.olap.query2.ThinQueryModel;
import org.saiku.olap.query2.ThinQueryModel.AxisLocation;
import org.saiku.olap.query2.common.ThinQuerySet;
import org.saiku.olap.query2.common.ThinSortableQuerySet;
import org.saiku.olap.query2.filter.ThinFilter;
import org.saiku.olap.util.SaikuDictionary;
import org.saiku.olap.util.SaikuDictionary.DateFlag;
import org.saiku.olap.util.SaikuProperties;
import org.saiku.query.IQuerySet;
import org.saiku.query.ISortableQuerySet;
import org.saiku.query.Query;
import org.saiku.query.QueryAxis;
import org.saiku.query.QueryDetails.Location;
import org.saiku.query.QueryHierarchy;
import org.saiku.query.QueryLevel;
import org.saiku.query.mdx.GenericFilter;
import org.saiku.query.mdx.IFilterFunction;
import org.saiku.query.mdx.IFilterFunction.MdxFunctionType;
import org.saiku.query.mdx.NFilter;
import org.saiku.query.mdx.NameFilter;
import org.saiku.query.mdx.NameLikeFilter;
import org.saiku.query.metadata.CalculatedMeasure;
import org.saiku.service.util.MondrianDictionary;

public class Fat {
	
	public static Query convert(ThinQuery tq, Cube cube) throws SQLException {
		
		Query q = new Query(tq.getName(), cube);
		if (tq.getParameters() != null) {
			q.setParameters(tq.getParameters());
		}
		
		if (tq.getQueryModel() == null)
			return q;

		ThinQueryModel model = tq.getQueryModel();
		convertAxes(q, tq.getQueryModel().getAxes(), tq);
		convertCalculatedMeasures(q, model.getCalculatedMeasures());
		convertDetails(q, model.getDetails());
		q.setVisualTotals(model.isVisualTotals());
		q.setVisualTotalsPattern(model.getVisualTotalsPattern());
		return q;
	}

	private static void convertCalculatedMeasures(Query q, List<ThinCalculatedMeasure> thinCms) {
		if (thinCms != null && thinCms.size() > 0) {
			for (ThinCalculatedMeasure qcm : thinCms) {
				// TODO improve this
				Hierarchy h = q.getCube().getMeasures().get(0).getHierarchy();
				
				CalculatedMeasure cm = 
						new CalculatedMeasure(
								h, 
								qcm.getName(), 
								null, 
								qcm.getFormula(),
								qcm.getProperties());
				
				q.addCalculatedMeasure(cm);
			}
		}
	}

	private static void convertDetails(Query query, ThinDetails details) {
		Location loc = Location.valueOf(details.getLocation().toString());
		query.getDetails().setLocation(loc);
		Axis ax = getLocation(details.getAxis());
		query.getDetails().setAxis(ax);
		
		if (details != null && details.getMeasures().size() > 0) {
			for (ThinMeasure m : details.getMeasures()) {
				if (Type.CALCULATED.equals(m.getType())) {
					Measure measure = query.getCalculatedMeasure(m.getName());
					query.getDetails().add(measure);
				} else if (Type.EXACT.equals(m.getType())) {
					Measure measure = query.getMeasure(m.getName());
					query.getDetails().add(measure);
				}
			}
		}
	}

	private static void convertAxes(Query q, Map<AxisLocation, ThinAxis> axes, ThinQuery tq) throws OlapException {
		if (axes != null) {
			for (AxisLocation axis : sortAxes(axes.keySet())) {
				if (axis != null) {
					convertAxis(q, axes.get(axis), tq);
				}
			}
		}
	}
	
	private static List<AxisLocation> sortAxes(Set<AxisLocation> axes) {
		List<AxisLocation> ax = new ArrayList<AxisLocation>();
		for (AxisLocation a : AxisLocation.values()) {
			if (axes.contains(a)){
				ax.add(a);
			}
		}
		return ax;
	}
	
	

	private static void convertAxis(Query query, ThinAxis thinAxis, ThinQuery tq) throws OlapException {
		Axis loc = getLocation(thinAxis.getLocation());
		QueryAxis qaxis = query.getAxis(loc);
		for (ThinHierarchy hierarchy : thinAxis.getHierarchies()) {
			QueryHierarchy qh = query.getHierarchy(hierarchy.getName());
			if (qh != null) {
				convertHierarchy(qh, hierarchy, tq);
				qaxis.addHierarchy(qh);
			}
		}
		qaxis.setNonEmpty(thinAxis.isNonEmpty());
		List<String> aggs = thinAxis.getAggregators();
		qaxis.getQuery().setAggregators(qaxis.getLocation().toString(), aggs);
		extendSortableQuerySet(query, qaxis, thinAxis);
	}
	
	private static void convertHierarchy(QueryHierarchy qh, ThinHierarchy th, ThinQuery tq) throws OlapException {
		for (ThinLevel tl : th.getLevels().values()) {
			QueryLevel ql = qh.includeLevel(tl.getName());
			
			List<String> aggs =  tl.getAggregators();
			qh.getQuery().setAggregators(ql.getUniqueName(), aggs);
			
			if (tl.getSelection() != null) {
				String parameter = tl.getSelection().getParameterName();
				if (StringUtils.isNotBlank(parameter)) {
					ql.setParameterName(parameter);
					ql.setParameterSelectionType(org.saiku.query.Parameter.SelectionType.INCLUSION);
				}
				switch(tl.getSelection().getType()) {
				case INCLUSION:
//					if (parameterValues != null) {
//						for (String m : parameterValues) {
//							qh.includeMember(m);
//						}
//
//					} else {
						for (ThinMember tm : tl.getSelection().getMembers()) {
							qh.includeMember(tm.getUniqueName());
						}
						ql.setParameterSelectionType(org.saiku.query.Parameter.SelectionType.INCLUSION);
//					}
					break;

				case EXCLUSION:
//					if (parameterValues != null) {
//						for (String m : parameterValues) {
//							qh.excludeMember(m);
//						}
//
//					} else {
						for (ThinMember tm : tl.getSelection().getMembers()) {
							qh.excludeMember(tm.getUniqueName());
						}
						ql.setParameterSelectionType(org.saiku.query.Parameter.SelectionType.EXCLUSION);
//					}
					break;
				case RANGE:
					List<ThinMember> members = tl.getSelection().getMembers();
					ThinMember start = members.size() > 0 ? members.get(0) : null;
					ThinMember end = members.size() > 1 ? members.get(1) : null;
					String startEx = start != null ? start.getUniqueName() : null;
					String endEx = end != null ? end.getUniqueName() : null;
					
					if (isFlag(startEx)) {
						ql.setRangeStartSynonym(startEx);
						List<String> exp = resolveFlag(startEx, ql);
						if (exp.size() > 1) {
							ql.setRangeExpressions(exp.get(0), exp.get(1));
						} else {
							ql.setRangeStartExpr(exp.get(0));
						}
					} else {
						ql.setRangeStartExpr(startEx);
					}
					
					if (ql.getRangeEndExpr() == null) {
						if (isFlag(endEx)) {
							ql.setRangeEndSynonym(endEx);
							List<String> exp = resolveFlag(endEx, ql);
							if (exp.size() > 1) {
								ql.setRangeExpressions(exp.get(0), exp.get(1));
							} else {
								ql.setRangeEndExpr(exp.get(0));
							}
						} else {
							ql.setRangeEndExpr(endEx);
						}
					}

					break;
				default:
					break;

				}
			}
			
			extendQuerySet(qh.getQuery(), ql, tl);
		}
		extendSortableQuerySet(qh.getQuery(), qh, th);
	}
	

	private static boolean isFlag(String expr) {
		return StringUtils.isNotBlank(expr) && expr.toUpperCase().startsWith("F:");
	}
	
	private static List<String> resolveFlag(String expr, QueryLevel ql) {
		List<String> exprs = new ArrayList<String>();
		String resolvedStartExpr = expr;
		String resolvedEndExpr = null;
		Level lvl = ql.getLevel();
		if (isFlag(resolvedStartExpr)) {
			String flag = expr.substring(2, expr.length()).trim();
			resolvedStartExpr = flag;
			boolean isDate = false;
			for (DateFlag df : SaikuDictionary.DateFlag.values()) {
				isDate = (flag.toUpperCase().endsWith(df.toString()));
				if (isDate)
					break;
			}
			if (isDate) {
				Annotation dateAn = SaikuMondrianHelper.hasAnnotation(lvl, MondrianDictionary.AnalyzerDateFormat) ?
						SaikuMondrianHelper.getAnnotation(lvl, MondrianDictionary.AnalyzerDateFormat)
						: SaikuMondrianHelper.getAnnotation(lvl, MondrianDictionary.SaikuDayFormat);

				if (dateAn != null) {
					String currDate;
					String formatString = dateAn.getValue().toString();
					final Format format = new Format(formatString, SaikuProperties.locale);
					currDate = lvl.getHierarchy().getUniqueName() + "." + format.format(Calendar.getInstance(SaikuProperties.locale).getTime());
					String escapedHierarchy = lvl.getHierarchy().getUniqueName().replaceAll("\\[", "[\"").replaceAll("\\]", "\"]");
					String currDateMember = "CurrentDateMember(" + lvl.getHierarchy().getUniqueName() + ", '" + escapedHierarchy + "\\." + formatString + "')";  
					//System.err.println("Current Date for DateFormat:" + formatString + "=" + currDate);

					if (SaikuDictionary.DateFlag.CURRENT.toString().equals(flag.toUpperCase())) {
						resolvedStartExpr = currDateMember;
					} else if (SaikuDictionary.DateFlag.LAST.toString().equals(flag.toUpperCase())) {
						resolvedStartExpr = currDateMember + ".Lag(1)";
					} else if (flag.toUpperCase().endsWith(SaikuDictionary.DateFlag.AGO.toString())){
						resolvedStartExpr = currDateMember + ".Lag(" + flag.replaceAll(SaikuDictionary.DateFlag.AGO.toString(), "") + ")";
					}
					
					if (Level.Type.TIME_DAYS.equals(lvl.getLevelType())) {
						if (SaikuDictionary.DateFlag.CURRENTMONTH.toString().equals(flag.toUpperCase())) {
							Calendar startC = Calendar.getInstance(SaikuProperties.locale);
							startC.set(Calendar.DAY_OF_MONTH, 1);
							String startDate =  lvl.getHierarchy().getUniqueName() + "." + format.format(startC.getTime());
							resolvedStartExpr = startDate; 
							resolvedEndExpr = currDate;
						} else if (SaikuDictionary.DateFlag.CURRENTWEEK.toString().equals(flag.toUpperCase())) {
							Calendar startC = Calendar.getInstance(SaikuProperties.locale);
							startC.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
							String startDate =  lvl.getHierarchy().getUniqueName() + "." + format.format(startC.getTime());
							resolvedStartExpr = startDate; 
							resolvedEndExpr = currDate;
						} else if (SaikuDictionary.DateFlag.CURRENTYEAR.toString().equals(flag.toUpperCase())) {
							Calendar startC = Calendar.getInstance(SaikuProperties.locale);
							startC.set(Calendar.DAY_OF_YEAR, 1);
							String startDate =  lvl.getHierarchy().getUniqueName() + "." + format.format(startC.getTime());
							resolvedStartExpr = startDate; 
							resolvedEndExpr = currDate;
						} else if (SaikuDictionary.DateFlag.LASTMONTH.toString().equals(flag.toUpperCase())) {
							Calendar startC = Calendar.getInstance(SaikuProperties.locale);
							startC.add(Calendar.MONTH, - 1);
							startC.set(Calendar.DAY_OF_MONTH, 1);
							String startDate = lvl.getHierarchy().getUniqueName() + "." + format.format(startC.getTime());
							startC.set(Calendar.DAY_OF_MONTH, startC.getActualMaximum(Calendar.DAY_OF_MONTH));
							String endDate = lvl.getHierarchy().getUniqueName() + "." + format.format(startC.getTime());
							resolvedStartExpr = startDate;
							resolvedEndExpr = endDate;
						} else if (SaikuDictionary.DateFlag.LASTWEEK.toString().equals(flag.toUpperCase())) {
							Calendar startC = Calendar.getInstance(SaikuProperties.locale);
							startC.add(Calendar.WEEK_OF_YEAR, - 1);
							startC.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
							String startDate = lvl.getHierarchy().getUniqueName() + "." + format.format(startC.getTime());
							startC.add(Calendar.DATE, 6);
							String endDate = lvl.getHierarchy().getUniqueName() + "." + format.format(startC.getTime());
							resolvedStartExpr = startDate;
							resolvedEndExpr = endDate;
						} else if (SaikuDictionary.DateFlag.LASTYEAR.toString().equals(flag.toUpperCase())) {
							Calendar startC = Calendar.getInstance(SaikuProperties.locale);
							startC.add(Calendar.YEAR, - 1);
							startC.set(Calendar.DAY_OF_YEAR, 1);
							String startDate = lvl.getHierarchy().getUniqueName() + "." + format.format(startC.getTime());
							startC.set(Calendar.DAY_OF_YEAR, startC.getActualMaximum(Calendar.DAY_OF_YEAR));
							String endDate = lvl.getHierarchy().getUniqueName() + "." + format.format(startC.getTime());
							resolvedStartExpr = startDate;
							resolvedEndExpr = endDate;
						}
					}
				}
			}
		}
		exprs.add(resolvedStartExpr);
		if (resolvedEndExpr != null) {
			exprs.add(resolvedEndExpr);
		}
		return exprs;
	}


	private static Axis getLocation(AxisLocation axis) {
		String ax = axis.toString();
		if (AxisLocation.ROWS.toString().equals(ax)) {
			return Axis.ROWS;
		} else if (AxisLocation.COLUMNS.toString().equals(ax)) {
			return Axis.COLUMNS;
		} else if (AxisLocation.FILTER.toString().equals(ax)) {
			return Axis.FILTER;
		} else if (AxisLocation.PAGES.toString().equals(ax)) {
			return Axis.PAGES;
		}
		return null;
	}

	private static void extendQuerySet(Query q, IQuerySet qs, ThinQuerySet ts) {
		qs.setMdxSetExpression(ts.getMdx());
		
		if (ts.getFilters() != null && ts.getFilters().size() > 0) {
			List<IFilterFunction> filters = convertFilters(q, ts.getFilters());
			qs.getFilters().addAll(filters);
		}
		
	}
	
	private static List<IFilterFunction> convertFilters(Query q, List<ThinFilter> filters) {
		List<IFilterFunction> qfs = new ArrayList<IFilterFunction>();
		for (ThinFilter f : filters) {
			switch(f.getFlavour()) {
				case Name:
					List<String> exp = f.getExpressions();
					if (exp != null && exp.size() > 1) {
						String hierarchyName = exp.remove(0);
						QueryHierarchy qh = q.getHierarchy(hierarchyName);
						NameFilter nf = new NameFilter(qh.getHierarchy(), exp);
						qfs.add(nf);
					}
					break;
				case NameLike:
					List<String> exp2 = f.getExpressions();
					if (exp2 != null && exp2.size() > 1) {
						String hierarchyName = exp2.remove(0);
						QueryHierarchy qh = q.getHierarchy(hierarchyName);
						NameLikeFilter nf = new NameLikeFilter(qh.getHierarchy(), exp2);
						qfs.add(nf);
					}
					break;
				case Generic:
					List<String> gexp = f.getExpressions();
					if (gexp != null && gexp.size() == 1) {
						GenericFilter gf = new GenericFilter(gexp.get(0));
						qfs.add(gf);
					}
					break;
				case Measure:
					// TODO Implement this
					break;
				case N:
					List<String> nexp = f.getExpressions();
					if (nexp != null && nexp.size() > 0) {
						MdxFunctionType mf = MdxFunctionType.valueOf(f.getFunction().toString());
						int n = Integer.parseInt(nexp.get(0));
						String expression = null;
						if (nexp.size() > 1) {
							expression = nexp.get(1);
						}
						NFilter nf = new NFilter(mf, n, expression);
						qfs.add(nf);
					}
					break;
				default:
					break;
			}
		}
		return qfs;
	}

	private static void extendSortableQuerySet(Query q, ISortableQuerySet qs, ThinSortableQuerySet ts) {
		extendQuerySet(q, qs, ts);
		if (ts.getHierarchizeMode() != null) {
			qs.setHierarchizeMode(org.saiku.query.ISortableQuerySet.HierarchizeMode.valueOf(ts.getHierarchizeMode().toString()));
		}
		if (ts.getSortOrder() != null) {
			qs.sort(org.saiku.query.SortOrder.valueOf(ts.getSortOrder().toString()), ts.getSortEvaluationLiteral());
		}
		
		
	}
	

}
