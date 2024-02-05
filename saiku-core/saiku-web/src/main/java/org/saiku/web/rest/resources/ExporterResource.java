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
import org.saiku.olap.query2.ThinQuery;
import org.saiku.web.export.PdfReport;
import org.saiku.web.rest.objects.resultset.QueryResult;
import org.saiku.web.rest.util.ServletUtil;
import org.saiku.web.svg.Converter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


/**
 * QueryServlet contains all the methods required when manipulating an OLAP Query.
 * @author Paul Stoellberger
 *
 */
@Component
@RestController
@RequestMapping("/saiku/{username}/export")
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


	@GetMapping(value = "/saiku/xls", produces = "application/vnd.ms-excel")
	public ResponseEntity exportExcel(@RequestParam("file") String file,
									  @RequestParam(name = "showTotals", defaultValue ="false") boolean showTotals,
									  @RequestParam(name = "repeatValues", defaultValue ="true") boolean repeatValues,
									  @Nullable @RequestParam("formatter") String formatter,
									  HttpServletRequest servletRequest)
	{
		try {
			log.debug("Exporting XLS for file: " + file);
			ResponseEntity f = repository.getResource(file);
			String fileContent = new String( (byte[]) f.getBody());
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
			return ResponseEntity.internalServerError().build();
		}
	}

	@GetMapping(value = "/saiku/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
	public ResponseEntity exportPdf(@RequestParam("file") String file,
			@Nullable @RequestParam("formatter") String formatter,
			HttpServletRequest servletRequest)
	{
		try {
			log.debug("Exporting PDF for file: " + file);
			ResponseEntity f = repository.getResource(file);
			String fileContent = new String( (byte[]) f.getBody());
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
			return ResponseEntity.ok().contentLength(doc.length).contentType(MediaType.APPLICATION_PDF)
					.header("content-disposition","attachment; filename = export.pdf")
					.body(doc);

		} catch (Exception e) {
			log.error("Error exporting PDF for file: " + file, e);
			return ResponseEntity.internalServerError().build();
		}
	}


	@GetMapping(value = "/saiku/csv", produces = "text/csv")
	public ResponseEntity exportCsv(@RequestParam("file") String file,
			@Nullable @RequestParam("formatter") String formatter,
			HttpServletRequest servletRequest)
	{
		try {
			log.debug("Exporting CSV for file: " + file);
			ResponseEntity f = repository.getResource(file);
			String fileContent = new String( (byte[]) f.getBody());
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
			return ResponseEntity.internalServerError().build();
		}
	}



	@GetMapping("/saiku/json")
	public ResponseEntity exportJson(@RequestParam("file") String file,
			@Nullable @RequestParam("formatter") String formatter,
			HttpServletRequest servletRequest)
	{
		try {
			log.debug("Exporting JSON for file: " + file);
			ResponseEntity f = repository.getResource(file);
			String fileContent = new String( (byte[]) f.getBody());
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
			return ResponseEntity.ok(qr);
		} catch (Exception e) {
			log.error("Error exporting JSON for file: " + file, e);
			return ResponseEntity.internalServerError().build();
		}
	}

	@GetMapping(value = "/saiku/html", produces = MediaType.TEXT_HTML_VALUE)
	public ResponseEntity exportHtml(
			@RequestParam("file") String file,
			@RequestParam(name = "css", defaultValue ="false") Boolean css,
			@RequestParam(name = "tableonly", defaultValue ="false") Boolean tableonly,
			@RequestParam(name = "wrapcontent", defaultValue ="true") Boolean wrapcontent,
			@Nullable @RequestParam("formatter") String formatter,
			HttpServletRequest servletRequest)
	{
		try {
			log.debug("Exporting HTML for file: " + file);
			ResponseEntity f = repository.getResource(file);
			String fileContent = new String( (byte[]) f.getBody());
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
			return ResponseEntity.internalServerError().build();
		}
	}


	@PostMapping(value = "/saiku/chart", produces = "image/*")
	public ResponseEntity exportChart(
			@RequestParam(value = "type", defaultValue = "png")  String type,
			@Nullable @RequestParam("svg") String svg,
			@Nullable @RequestParam("size") Integer size)
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
			return ResponseEntity.ok().contentLength(doc.length)
					.header("Content-Type", converter.getContentType())
					.header("content-disposition","attachment; filename = chart." + converter.getExtension())
					.body(doc);

		} catch (Exception e) {
			log.error("Error exporting Chart to  " + type, e);
			return ResponseEntity.internalServerError().build();
		}
	}
}
