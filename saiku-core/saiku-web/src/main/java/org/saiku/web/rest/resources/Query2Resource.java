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

import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.CollectionType;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.saiku.olap.dto.SimpleCubeElement;
import org.saiku.olap.dto.resultset.CellDataSet;
import org.saiku.olap.query2.ThinQuery;
import org.saiku.olap.util.SaikuProperties;
import org.saiku.service.olap.ThinQueryService;
import org.saiku.service.util.exception.SaikuServiceException;
import org.saiku.service.util.export.excel.ExcelBuilderOptions;
import org.saiku.web.export.JSConverter;
import org.saiku.web.export.PdfReport;
import org.saiku.web.rest.objects.resultset.QueryResult;
import org.saiku.web.rest.util.RestUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.util.MultiValueMap;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Component
@RestController
@RequestMapping("/saiku/api/query")
public class Query2Resource {

	private static final Logger log = LoggerFactory.getLogger(Query2Resource.class);

	private ThinQueryService thinQueryService;

	public void setThinQueryService(ThinQueryService tqs) {
		thinQueryService = tqs;
	}

	private ISaikuRepository repository;

	public void setRepository(ISaikuRepository repository){
		this.repository = repository;
	}


	@DeleteMapping("/{queryname}")
	public ResponseEntity deleteQuery(@PathVariable("queryname") String queryName){
		if (log.isDebugEnabled()) {
			log.debug("TRACK\t"  + "\t/query/" + queryName + "\tDELETE");
		}
		try{
			thinQueryService.deleteQuery(queryName);
			return ResponseEntity.ok().body(HttpStatus.GONE);
		}
		catch(Exception e){
			log.error("Cannot delete query (" + queryName + ")",e);
			return ResponseEntity.internalServerError().build();
		}
	}

	@PostMapping("/{queryname}")
	public ThinQuery createQuery(
			@PathVariable("queryname") String queryName,
			@Nullable @RequestParam("json") String jsonRequestParam,
			@Nullable @RequestParam("file") String fileRequestParam,
			@Nullable @RequestParam MultiValueMap<String, String> formParams)
			{
		try {
			ThinQuery tq = null;
			String file = fileRequestParam,
					json = jsonRequestParam;
			if (formParams != null) {
				json = formParams.containsKey("json") ? formParams.getFirst("json") : jsonRequestParam;
				file = formParams.containsKey("file") ? formParams.getFirst("file") : fileRequestParam;
			}
			String filecontent = null;
			if (StringUtils.isNotBlank(json)) {
				filecontent = json;
			} else if (StringUtils.isNotBlank(file)) {
				ResponseEntity f = repository.getResource(file);
				filecontent = new String( (byte[]) f.getBody());
			}
			if (StringUtils.isBlank(filecontent)) {
				throw new SaikuServiceException("Cannot create new query. Empty file content " + StringUtils.isNotBlank(json) + " or read from file:" + file);
			}
			if (thinQueryService.isOldQuery(filecontent)) {
				tq = thinQueryService.convertQuery(filecontent);
			} else {
				ObjectMapper om = new ObjectMapper();
				tq = om.readValue(filecontent, ThinQuery.class);
			}

			if (log.isDebugEnabled()) {
				log.debug("TRACK\t"  + "\t/query/" + queryName + "\tPOST\t tq:" + (tq == null) + " file:" + (file));
			}

			if (tq == null) {
				throw new SaikuServiceException("Cannot create blank query (ThinQuery object = null)");
			}
			tq.setName(queryName);

			//			SaikuCube cube = tq.getCube();
			//			if (StringUtils.isNotBlank(xml)) {
			//				String query = ServletUtil.replaceParameters(formParams, xml);
			//				return thinQueryService.createNewOlapQuery(queryName, query);
			//			}
			return thinQueryService.createQuery(tq);
		} catch (Exception e) {
			log.error("Error creating new query", e);
			throw new ErrorResponseException(HttpStatus.UNPROCESSABLE_ENTITY);
		}
			}


	@PostMapping("/execute")
	public QueryResult execute(@RequestBody ThinQuery tq) {
		try {
			if (thinQueryService.isMdxDrillthrough(tq)) {
				Long start = (new Date()).getTime();
				ResultSet rs = thinQueryService.drillthrough(tq);
				QueryResult rsc = RestUtil.convert(rs);
				rsc.setQuery(tq);
				Long runtime = (new Date()).getTime()- start;
				rsc.setRuntime(runtime.intValue());
				return rsc;
			}

			QueryResult qr = RestUtil.convert(thinQueryService.execute(tq));
			ThinQuery tqAfter = thinQueryService.getContext(tq.getName()).getOlapQuery();
			qr.setQuery(tqAfter);
			return qr;
		}
		catch (Exception e) {
			log.error("Cannot execute query (" + tq + ")",e);
			String error = ExceptionUtils.getRootCauseMessage(e);
			return new QueryResult(error);
		}
	}

	@DeleteMapping("/{queryname}/cancel")
	public ResponseEntity cancel(@PathVariable("queryname") String queryName){
		if (log.isDebugEnabled()) {
			log.debug("TRACK\t"  + "\t/query/" + queryName + "\tDELETE");
		}
		try{
			thinQueryService.cancel(queryName);
			return ResponseEntity.ok().body(HttpStatus.GONE);
		}
		catch(Exception e){
			log.error("Cannot cancel query (" + queryName + ")",e);
			String error = ExceptionUtils.getRootCauseMessage(e);
			return ResponseEntity.internalServerError().body(error);
		}
	}

	@PostMapping("/enrich")
	public ThinQuery enrich(@RequestBody ThinQuery tq) {
		try {
			ThinQuery tqAfter = thinQueryService.updateQuery(tq);
			return tqAfter;
		}
		catch (Exception e) {
			log.error("Cannot enrich query (" + tq + ")",e);
			String error = ExceptionUtils.getRootCauseMessage(e);
			throw new ErrorResponseException(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/{queryname}/result/metadata/hierarchies/{hierarchy}/levels/{level}")
	public List<SimpleCubeElement> getLevelMembers(
			@PathVariable("queryname") String queryName,
			@PathVariable("hierarchy") String hierarchyName,
			@PathVariable("level") String levelName,
			@RequestParam(name = "result", defaultValue ="true") boolean result,
			@Nullable @RequestParam(name = "search") String searchString,
			@RequestParam(name = "searchlimit", defaultValue = "-1") int searchLimit)
			{
		if (log.isDebugEnabled()) {
			log.debug("TRACK\t"
					+ "\t/query/" + queryName + "/result/metadata"
					+ "/hierarchies/" + hierarchyName + "/levels/" + levelName + "\tGET");
		}
		try {
			List<SimpleCubeElement> ms = thinQueryService.getResultMetadataMembers(queryName, result, hierarchyName, levelName, searchString, searchLimit);
			return ms;
		}
		catch (Exception e) {
			log.error("Cannot execute query (" + queryName + ")",e);
			String error = ExceptionUtils.getRootCauseMessage(e);
			throw new ErrorResponseException(HttpStatus.INTERNAL_SERVER_ERROR);
		}
			}



	@GetMapping(value = "/{queryname}/export/xls", produces = "application/vnd.ms-excel")
	public ResponseEntity getQueryExcelExport(@PathVariable("queryname") String queryName){
		if (log.isDebugEnabled()) {
			log.debug("TRACK\t"  + "\t/query/" + queryName + "/export/xls/\tGET");
		}
		return getQueryExcelExport(queryName, "flattened", null,null);
	}


	@GetMapping(value = "/{queryname}/export/xls/{format}", produces = "application/vnd.ms-excel")
	public ResponseEntity getQueryExcelExport(
			@PathVariable("queryname") String queryName,
			@PathVariable("format") String format,
			@RequestParam(name = "showTotals", defaultValue = "false") Boolean showTotals,
			@RequestParam(name = "repeatValues", defaultValue = "true") Boolean repeatValues){
		if (log.isDebugEnabled()) {
			log.debug("TRACK\t"  + "\t/query/" + queryName + "/export/xls/"+format+"\tGET");
		}
		try {
			ExcelBuilderOptions exb = new ExcelBuilderOptions(null, repeatValues, showTotals);
			byte[] doc = thinQueryService.getExport(queryName,"xls",format, exb);
			String name = SaikuProperties.webExportExcelName + "." + SaikuProperties.webExportExcelFormat;
			return ResponseEntity.ok().contentLength(doc.length).contentType(MediaType.APPLICATION_OCTET_STREAM)
					.header("content-disposition","attachment; filename = " + name)
					.body(doc);
		}
		catch (Exception e) {
			log.error("Cannot get excel for query (" + queryName + ")",e);
			String error = ExceptionUtils.getRootCauseMessage(e);
			throw new ErrorResponseException(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}


	@GetMapping(value = "/{queryname}/export/csv", produces = "text/csv")
	public ResponseEntity getQueryCsvExport(@PathVariable("queryname") String queryName) {
		if (log.isDebugEnabled()) {
			log.debug("TRACK\t"  + "\t/query/" + queryName + "/export/csv\tGET");
		}
		return getQueryCsvExport(queryName, "flattened");
	}

	@GetMapping(value = "/{queryname}/export/csv/{format}", produces = "text/csv")
	public ResponseEntity getQueryCsvExport(
			@PathVariable("queryname") String queryName,
			@PathVariable("format") String format){
		if (log.isDebugEnabled()) {
			log.debug("TRACK\t"  + "\t/query/" + queryName + "/export/csv/"+format+"\tGET");
		}
		try {
			byte[] doc = thinQueryService.getExport(queryName,"csv",format, null);
			String name = SaikuProperties.webExportCsvName;
			return ResponseEntity.ok().contentLength(doc.length).contentType(MediaType.APPLICATION_OCTET_STREAM)
					.header("content-disposition","attachment; filename = " + name + ".csv")
					.body(doc);
		}
		catch (Exception e) {
			log.error("Cannot get csv for query (" + queryName + ")",e);
			String error = ExceptionUtils.getRootCauseMessage(e);
			throw new ErrorResponseException(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}


	@PostMapping(path = "/{queryname}/zoomin")
	public ThinQuery zoomIn(
			@PathVariable("queryname") String queryName,
			@RequestParam("selections") String positionListString) {
		try {

			if (log.isDebugEnabled()) {
				log.debug("TRACK\t"  + "\t/query/" + queryName + "/zoomIn\tPUT");
			}
			List<List<Integer>> realPositions = new ArrayList<List<Integer>>();
			if (StringUtils.isNotBlank(positionListString)) {
				ObjectMapper mapper = new ObjectMapper();

				String[] positions = mapper.readValue(positionListString,
						mapper.getTypeFactory().constructArrayType(String.class));
				if (positions != null && positions.length > 0) {
					for (String position : positions) {
						String[] rPos = position.split(":");
						List<Integer> cellPosition = new ArrayList<Integer>();

						for (String p : rPos) {
							Integer pInt = Integer.parseInt(p);
							cellPosition.add(pInt);
						}
						realPositions.add(cellPosition);
					}
				}
			}
			ThinQuery tq = thinQueryService.zoomIn(queryName, realPositions);
			return tq;

		} catch (Exception e){
			log.error("Cannot zoom in on query (" + queryName + ")",e);
			throw new ErrorResponseException(HttpStatus.INTERNAL_SERVER_ERROR);
		}
	}

	@GetMapping("/{queryname}/drillthrough")
	public QueryResult drillthrough(
			@PathVariable("queryname") String queryName,
			@RequestParam(name = "maxrows", defaultValue = "100") Integer maxrows,
			@Nullable @RequestParam("position") String position,
			@Nullable @RequestParam("returns") String returns)
	{
		if (log.isDebugEnabled()) {
			log.debug("TRACK\t"  + "\t/query/" + queryName + "/drillthrough\tGET");
		}
		QueryResult rsc;
		ResultSet rs = null;
		try {
			Long start = (new Date()).getTime();
			if (position == null) {
				rs = thinQueryService.drillthrough(queryName, maxrows, returns);
			} else {
				String[] positions = position.split(":");
				List<Integer> cellPosition = new ArrayList<Integer>();

				for (String p : positions) {
					Integer pInt = Integer.parseInt(p);
					cellPosition.add(pInt);
				}

				rs = thinQueryService.drillthrough(queryName, cellPosition, maxrows, returns);
			}
			rsc = RestUtil.convert(rs);
			Long runtime = (new Date()).getTime()- start;
			rsc.setRuntime(runtime.intValue());

		}
		catch (Exception e) {
			log.error("Cannot execute query (" + queryName + ")",e);
			String error = ExceptionUtils.getRootCauseMessage(e);
			rsc =  new QueryResult(error);

		}
		finally {
			if (rs != null) {
				Statement statement = null;
				Connection con = null;
				try {
					statement = rs.getStatement();
					con = rs.getStatement().getConnection();
				} catch (Exception e) {
					throw new SaikuServiceException(e);
				} finally {
					try {
						rs.close();
						if (statement != null) {
							statement.close();
						}
					} catch (Exception ee) {};

					rs = null;
				}
			}
		}
		return rsc;

	}


	@GetMapping(value = "/{queryname}/drillthrough/export/csv", produces = "text/csv")
	public ResponseEntity<byte[]> getDrillthroughExport(
			@PathVariable("queryname") String queryName,
			@RequestParam(name = "maxrows", defaultValue = "100") Integer maxrows,
			@Nullable @RequestParam("position") String position,
			@Nullable @RequestParam("returns") String returns)
	{
		if (log.isDebugEnabled()) {
			log.debug("TRACK\t"  + "\t/query/" + queryName + "/drillthrough/export/csv (maxrows:" + maxrows + " position" + position + ")\tGET");
		}
		ResultSet rs = null;

		try {
			if (position == null) {
				rs = thinQueryService.drillthrough(queryName, maxrows, returns);
			} else {
				String[] positions = position.split(":");
				List<Integer> cellPosition = new ArrayList<Integer>();

				for (String p : positions) {
					Integer pInt = Integer.parseInt(p);
					cellPosition.add(pInt);
				}

				rs = thinQueryService.drillthrough(queryName, cellPosition, maxrows, returns);
			}
			byte[] doc = thinQueryService.exportResultSetCsv(rs);
			String name = SaikuProperties.webExportCsvName;
			return ResponseEntity.ok().contentLength(doc.length).contentType(MediaType.APPLICATION_OCTET_STREAM)
					.header("content-disposition","attachment; filename = " + name + "-drillthrough.csv")
					.body(doc);


		} catch (Exception e) {
			log.error("Cannot export drillthrough query (" + queryName + ")",e);
			return ResponseEntity.internalServerError().build();
		}
		finally {
			if (rs != null) {
				try {
					Statement statement = rs.getStatement();
					statement.close();
					rs.close();
				} catch (SQLException e) {
					throw new SaikuServiceException(e);
				} finally {
					rs = null;
				}
			}
		}


	}

	@PostMapping(value = "/{queryname}/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
	public ResponseEntity<byte[]> exportPdfWithChart(
			@PathVariable("queryname")  String queryName,
			@RequestParam(value = "svg", defaultValue = "") String svg)
	{
		return exportPdfWithChartAndFormat(queryName, null, svg);
	}

	@GetMapping(value = "/{queryname}/export/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
	public ResponseEntity<byte[]> exportPdf(@PathVariable("queryname")  String queryName)
	{
		return exportPdfWithChartAndFormat(queryName, null, null);
	}

	@GetMapping(value = "/{queryname}/export/pdf/{format}", produces = MediaType.APPLICATION_PDF_VALUE)
	public ResponseEntity<byte[]> exportPdfWithFormat(
			@PathVariable("queryname")  String queryName,
			@PathVariable("format") String format)
	{
		return exportPdfWithChartAndFormat(queryName, format, null);
	}

	@PostMapping(value = "/{queryname}/export/pdf/{format}", produces = MediaType.APPLICATION_PDF_VALUE)
	public ResponseEntity<byte[]> exportPdfWithChartAndFormat(
			@PathVariable("queryname")  String queryName,
			@PathVariable("format") String format,
			@RequestParam(name = "svg", defaultValue = "") String svg)
	{

		try {
			CellDataSet cs = thinQueryService.getFormattedResult(queryName, format);
			QueryResult qr = RestUtil.convert(cs);
			PdfReport pdf = new PdfReport();
			byte[] doc  = pdf.pdf(qr, svg);
			return ResponseEntity.ok().contentLength(doc.length).contentType(MediaType.APPLICATION_PDF)
					.header("content-disposition","attachment; filename = export.pdf")
					.body(doc);
		} catch (Exception e) {
			log.error("Error exporting query to  PDF", e);
			return ResponseEntity.internalServerError().build();
		}
	}
	
	@GetMapping(path = "/{queryname}/export/html", produces = MediaType.TEXT_HTML_VALUE)
	public ResponseEntity exportHtml(
			@PathVariable("queryname") String queryname,
			@RequestParam("format") String format,
			@RequestParam(name = "css" , defaultValue = "false") Boolean css,
			@RequestParam(name = "tableonly"  , defaultValue = "false") Boolean tableonly,
			@RequestParam(name = "wrapcontent" , defaultValue = "true") Boolean wrapcontent)
	{
		ThinQuery tq = thinQueryService.getContext(queryname).getOlapQuery();
		return exportHtml(tq, format, css, tableonly, wrapcontent);
	}

	@PostMapping(value = "/export/html", produces = MediaType.TEXT_HTML_VALUE)
	public ResponseEntity exportHtml(
			ThinQuery tq,
			@RequestParam("format") String format,
			@RequestParam(name = "css" , defaultValue = "false") Boolean css,
			@RequestParam(name = "tableonly" , defaultValue = "false") Boolean tableonly,
			@RequestParam(name = "wrapcontent" , defaultValue = "true") Boolean wrapcontent)
	{

		try {
			CellDataSet cs = null;
			if (StringUtils.isNotBlank(format)) {
				cs = thinQueryService.execute(tq, format);
			} else {
				cs = thinQueryService.execute(tq);
			}
			QueryResult qr = RestUtil.convert(cs);
			String content = JSConverter.convertToHtml(qr, wrapcontent);
			String html = "";
			if (!tableonly) {
				html += "<!DOCTYPE html><html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">\n";
				if (css) {
					html += "<style>\n";
					InputStream is = JSConverter.class.getResourceAsStream("saiku.table.full.css");
					String cssContent = IOUtils.toString(is);
					html += cssContent;
					html += "</style>\n";
				}
				html += "</head>\n<body><div class='workspace_results'>\n";
			}
			html += content;
			if (!tableonly) {
				html += "\n</div></body></html>";
			}
			return ResponseEntity.ok(html);
		} catch (Exception e) {
			log.error("Error exporting query to  HTML", e);
			return ResponseEntity.internalServerError().build();
		}
	}

	@PostMapping("/{queryname}/drillacross")
	public ThinQuery drillacross(
			@PathVariable("queryname") String queryName,
			@RequestParam("position") String position,
			@Nullable @RequestParam("drill") String returns)
	{
		if (log.isDebugEnabled()) {
			log.debug("TRACK\t"  + "\t/query/" + queryName + "/drillacross\tPOST");
		}

		try {
			String[] positions = position.split(":");
			List<Integer> cellPosition = new ArrayList<Integer>();
			for (String p : positions) {
				Integer pInt = Integer.parseInt(p);
				cellPosition.add(pInt);
			}
			ObjectMapper mapper = new ObjectMapper();
			CollectionType ct = mapper.getTypeFactory().constructCollectionType(ArrayList.class, String.class);
			JavaType st = mapper.getTypeFactory().uncheckedSimpleType(String.class);
			Map<String, List<String>> levels = mapper.readValue(returns, mapper.getTypeFactory().constructMapType(Map.class, st, ct));
			ThinQuery q = thinQueryService.drillacross(queryName, cellPosition, levels);
			return q;

		}
		catch (Exception e) {
			log.error("Cannot execute query (" + queryName + ")",e);
			String error = ExceptionUtils.getRootCauseMessage(e);
			throw new ErrorResponseException(HttpStatus.INTERNAL_SERVER_ERROR);

		}
	}





}
