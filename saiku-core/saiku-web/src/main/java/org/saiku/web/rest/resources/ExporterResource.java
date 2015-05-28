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


import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

import org.apache.commons.lang.StringUtils;
import org.saiku.olap.query2.ThinQuery;
import org.saiku.service.util.export.excel.ExcelBuilderOptions;
import org.saiku.web.export.PdfReport;
import org.saiku.web.rest.objects.resultset.QueryResult;
import org.saiku.web.rest.util.ServletUtil;
import org.saiku.web.svg.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


/**
 * QueryServlet contains all the methods required when manipulating an OLAP Query.
 * @author Paul Stoellberger
 *
 */
@Component
@Path("/saiku/{username}/export")
@XmlAccessorType(XmlAccessType.NONE)
public class ExporterResource {

	private static final Logger log = LoggerFactory.getLogger(ExporterResource.class);

	private ISaikuRepository repository;

	private Query2Resource query2Resource;

	public void setQuery2Resource(Query2Resource qr){
		this.query2Resource = qr;
	}

	public void setRepository(ISaikuRepository repository){
		this.repository = repository;
	}


	@GET
	@Produces({"application/json" })
	@Path("/saiku/xls")
	public Response exportExcel(@QueryParam("file") String file, 
			@QueryParam("formatter") String formatter,
			@QueryParam("showTotals") @DefaultValue("false") boolean showTotals,
			@QueryParam("repeatValues") @DefaultValue("true") boolean repeatValues,
			@Context HttpServletRequest servletRequest) 
	{
		try {
			Response f = repository.getResource(file);
			String fileContent = new String( (byte[]) f.getEntity());
			String queryName = UUID.randomUUID().toString();			
			//fileContent = ServletUtil.replaceParameters(servletRequest, fileContent);
//			queryResource.createQuery(queryName,  null,  null, null, fileContent, queryName, null);
//			queryResource.execute(queryName, formatter, 0);
			Map<String, String> parameters = ServletUtil.getParameters(servletRequest);
			ThinQuery tq = query2Resource.createQuery(queryName, fileContent, null, null);
			if (parameters != null) {
				tq.getParameters().putAll(parameters);
			}
			if (StringUtils.isNotBlank(formatter)) {
				HashMap<String, Object> p = new HashMap<String, Object>();
				p.put("saiku.olap.result.formatter", formatter);
				if (tq.getProperties() == null) {
					tq.setProperties(p);
				} else {
					tq.getProperties().putAll(p);
				}
			}
			query2Resource.execute(tq);
			
			return query2Resource.getQueryExcelExport(queryName, formatter, showTotals, repeatValues);
		} catch (Exception e) {
			log.error("Error exporting XLS for file: " + file, e);
			return Response.serverError().entity(e.getMessage()).status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	@GET
	@Produces({"application/json" })
	@Path("/saiku/pdf")
	public Response exportPdf(@QueryParam("file") String file, 
			@QueryParam("formatter") String formatter,
			@Context HttpServletRequest servletRequest) 
	{
		try {
			Response f = repository.getResource(file);
			String fileContent = new String( (byte[]) f.getEntity());
			String queryName = UUID.randomUUID().toString();			
			Map<String, String> parameters = ServletUtil.getParameters(servletRequest);
			ThinQuery tq = query2Resource.createQuery(queryName, fileContent, null, null);
			if (parameters != null) {
				tq.getParameters().putAll(parameters);
			}
			if (StringUtils.isNotBlank(formatter)) {
				HashMap<String, Object> p = new HashMap<String, Object>();
				p.put("saiku.olap.result.formatter", formatter);
				if (tq.getProperties() == null) {
					tq.setProperties(p);
				} else {
					tq.getProperties().putAll(p);
				}
			}
			QueryResult qr = query2Resource.execute(tq);
			PdfReport pdf = new PdfReport();
			byte[] doc  = pdf.pdf(qr, null);
			return Response.ok(doc).type("application/pdf").header(
					"content-disposition",
					"attachment; filename = export.pdf").header(
							"content-length",doc.length).build();
		} catch (Exception e) {
			log.error("Error exporting PDF for file: " + file, e);
			return Response.serverError().entity(e.getMessage()).status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}


	@GET
	@Produces({"application/json" })
	@Path("/saiku/csv")
	public Response exportCsv(@QueryParam("file") String file, 
			@QueryParam("formatter") String formatter,
			@Context HttpServletRequest servletRequest) 
	{
		try {
			Response f = repository.getResource(file);
			String fileContent = new String( (byte[]) f.getEntity());
			//fileContent = ServletUtil.replaceParameters(servletRequest, fileContent);
			String queryName = UUID.randomUUID().toString();
//			query2Resource.createQuery(null,  null,  null, null, fileContent, queryName, null);
//			query2Resource.execute(queryName,formatter, 0);
			Map<String, String> parameters = ServletUtil.getParameters(servletRequest);
			ThinQuery tq = query2Resource.createQuery(queryName, fileContent, null, null);
			if (parameters != null) {
				tq.getParameters().putAll(parameters);
			}
			if (StringUtils.isNotBlank(formatter)) {
				HashMap<String, Object> p = new HashMap<String, Object>();
				p.put("saiku.olap.result.formatter", formatter);
				if (tq.getProperties() == null) {
					tq.setProperties(p);
				} else {
					tq.getProperties().putAll(p);
				}
			}
			query2Resource.execute(tq);
			return query2Resource.getQueryCsvExport(queryName);
		} catch (Exception e) {
			log.error("Error exporting CSV for file: " + file, e);
			return Response.serverError().entity(e.getMessage()).status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}



	@GET
	@Produces({"application/json" })
	@Path("/saiku/json")
	public Response exportJson(@QueryParam("file") String file, 
			@QueryParam("formatter") String formatter,
			@Context HttpServletRequest servletRequest) 
	{
		try {
			Response f = repository.getResource(file);
			String fileContent = new String( (byte[]) f.getEntity());
			fileContent = ServletUtil.replaceParameters(servletRequest, fileContent);
			String queryName = UUID.randomUUID().toString();
//			query2Resource.createQuery(null,  null,  null, null, fileContent, queryName, null);
//			QueryResult qr = query2Resource.execute(queryName, formatter, 0);
			Map<String, String> parameters = ServletUtil.getParameters(servletRequest);
			ThinQuery tq = query2Resource.createQuery(queryName, fileContent, null, null);
			if (parameters != null) {
				tq.getParameters().putAll(parameters);
			}
			if (StringUtils.isNotBlank(formatter)) {
				HashMap<String, Object> p = new HashMap<String, Object>();
				p.put("saiku.olap.result.formatter", formatter);
				if (tq.getProperties() == null) {
					tq.setProperties(p);
				} else {
					tq.getProperties().putAll(p);
				}
			}
			QueryResult qr = query2Resource.execute(tq);
			return Response.ok().entity(qr).build();
		} catch (Exception e) {
			log.error("Error exporting JSON for file: " + file, e);
			return Response.serverError().entity(e.getMessage()).status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
	
	@GET
	@Produces({"text/html" })
	@Path("/saiku/html")
	public Response exportHtml(@QueryParam("file") String file, 
			@QueryParam("formatter") String formatter,
			@QueryParam("css") @DefaultValue("false") Boolean css,
			@QueryParam("tableonly") @DefaultValue("false") Boolean tableonly,
			@QueryParam("wrapcontent") @DefaultValue("true") Boolean wrapcontent,
			@Context HttpServletRequest servletRequest) 
	{
		try {
			Response f = repository.getResource(file);
			String fileContent = new String( (byte[]) f.getEntity());
			fileContent = ServletUtil.replaceParameters(servletRequest, fileContent);
			String queryName = UUID.randomUUID().toString();
			Map<String, String> parameters = ServletUtil.getParameters(servletRequest);
			ThinQuery tq = query2Resource.createQuery(queryName, fileContent, null, null);
			if (parameters != null) {
				tq.getParameters().putAll(parameters);
			}
			if (StringUtils.isNotBlank(formatter)) {
				HashMap<String, Object> p = new HashMap<String, Object>();
				p.put("saiku.olap.result.formatter", formatter);
				if (tq.getProperties() == null) {
					tq.setProperties(p);
				} else {
					tq.getProperties().putAll(p);
				}
			}
			return query2Resource.exportHtml(tq, formatter, css, tableonly, wrapcontent);
		} catch (Exception e) {
			log.error("Error exporting JSON for file: " + file, e);
			return Response.serverError().entity(e.getMessage()).status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}

	@POST
	@Produces({"image/*" })
	@Path("/saiku/chart")
	public Response exportChart(
			@FormParam("type") @DefaultValue("png")  String type,
			@FormParam("svg") String svg,
			@FormParam("size") Integer size) 
	{
		try {
			final String imageType = type.toUpperCase();
			Converter converter = Converter.byType(imageType);
			if (converter == null)
			{
				throw new Exception("Image convert is null");
			}


			//		       resp.setContentType(converter.getContentType());
			//		       resp.setHeader("Content-disposition", "attachment; filename=chart." + converter.getExtension());
			//		       final Integer size = req.getParameter("size") != null? Integer.parseInt(req.getParameter("size")) : null;
			//		       final String svgDocument = req.getParameter("svg");
			//		       if (svgDocument == null)
			//		       {
			//		           resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Missing 'svg' parameter");
			//		           return;
			//		       }
			if (StringUtils.isBlank(svg)) {
				throw new Exception("Missing 'svg' parameter");
			}
			final InputStream in = new ByteArrayInputStream(svg.getBytes("UTF-8"));
			final ByteArrayOutputStream out = new ByteArrayOutputStream();
			converter.convert(in, out, size);
			out.flush();
			byte[] doc = out.toByteArray();
			return Response.ok(doc).type(converter.getContentType()).header(
					"content-disposition",
					"attachment; filename = chart." + converter.getExtension()).header(
							"content-length",doc.length).build();
		} catch (Exception e) {
			log.error("Error exporting Chart to  " + type, e);
			return Response.serverError().entity(e.getMessage()).status(Status.INTERNAL_SERVER_ERROR).build();
		}
	}
}
