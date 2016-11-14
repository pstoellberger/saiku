package org.saiku.web.export;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.saiku.web.rest.objects.resultset.QueryResult;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JSConverter {

	public static String convertToHtml(QueryResult qr, boolean wrapcontent) throws IOException {
		if (qr.getCellset() == null) {
			return "No Data";
		}
		ObjectMapper om = new ObjectMapper();
		StringWriter sw = new StringWriter();
		Context context = Context.enter();
		Scriptable globalScope = context.initStandardObjects();
		Reader underscoreReader = new InputStreamReader(JSConverter.class.getResourceAsStream("underscore.js"));
		context.evaluateReader(globalScope, underscoreReader, "underscore.js", 1, null);
		Reader srReader = new InputStreamReader(JSConverter.class.getResourceAsStream("SaikuRenderer.js"));
		context.evaluateReader(globalScope, srReader, "SaikuRenderer.js", 1, null);
		Reader strReader = new InputStreamReader(JSConverter.class.getResourceAsStream("SaikuTableRenderer.js"));
		context.evaluateReader(globalScope, strReader, "SaikuTableRenderer.js", 1, null);

		String data = om.writeValueAsString(qr);
		Object wrappedQr = Context.javaToJS(data, globalScope);
		ScriptableObject.putProperty(globalScope, "data", wrappedQr);

		Object wrappedOut = Context.javaToJS(sw, globalScope);
		ScriptableObject.putProperty(globalScope, "out", wrappedOut);

		String code = "eval('var cellset = ' + data); \n"
		 + "var renderer = new SaikuTableRenderer(); \n"
		 + "var html = renderer.render(cellset, { wrapContent : " + wrapcontent + " }); \n"
		 + "if (html) { out.write(html); } else { out.write('No Data'); }";

		context.evaluateString(globalScope, code, "<mem>", 1, null);
		Context.exit();

		String content = sw.toString();
		return content;

	}

	public static String convertToHtml(QueryResult qr) throws IOException {
		return convertToHtml(qr, false);
	}

}
