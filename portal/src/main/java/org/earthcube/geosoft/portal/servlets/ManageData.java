package org.earthcube.geosoft.portal.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.*;

import org.earthcube.geosoft.portal.classes.Config;
import org.earthcube.geosoft.portal.classes.WriteLock;
import org.earthcube.geosoft.portal.controllers.DataController;

/**
 * Servlet implementation class ManageData
 */
public class ManageData extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public ManageData() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		Config config = new Config(request);

		String path = request.getPathInfo();
		if (path == null)
			path = "/";
		String[] args = path.split("\\/");
		String op = args.length > 1 ? args[1] : null;
		
		if (op != null && op.equals("intro")) {
			PrintWriter out = response.getWriter();
			out.println("<div class='x-toolbar x-toolbar-default highlightIcon' "
					+ "style='padding:10px;font-size:1.5em;border-width:0px 0px 1px 0px'>Describe File Types</div>\n"
					+ "<div style='padding:5px; line-height:1.5em'>\n"
					+ "With this interface, you can:\n"
					+ "<ul>\n"
					+ "   <li>View File types and files by clicking on items in the tree on the left</li>\n"
					+ "   <li>Add new File types by clicking on the Add File Type button</li>\n"
					+ "   <li>After opening a File Type you may:\n"
					+ "   <ul>\n"
					+ "      <li>Add/Remove Metadata Properties for that file type</li>\n"
					+ "      <li>Add files for the file type</li>\n"
					+ "      <li>Delete the file type and all files associated with it</li>\n"
					+ "   </ul>\n" 
					+ "   </li>\n" 
					+ "   <li>After opening a File you may:\n"
					+ "   <ul>\n"
					+ "      <li>Add Metadata property Values for the file</li>\n"
					+ "      <li>View the file / or get the file url</li>\n"
					+ "      <li>Delete the file (Delete button on left)</li>\n" + "   </ul>\n"
					+ "   </li>\n" 
					+ "</ul>\n" 
					+ "</div>\n");
			return;
		}
		
		int guid = 1;

		synchronized (WriteLock.Lock) {
		  DataController dv = new DataController(guid, config);

  		String dtype = request.getParameter("data_type");
  		String dataid = request.getParameter("data_id");
  		if (op != null && op.equals("fetch")) {
  			dv.streamData(dataid, response, this.getServletContext());
  			return;
  		}
  
  		PrintWriter out = response.getWriter();
  		// Reader functions
  		if (op == null || op.equals("")) {
  			response.setContentType("text/html");
  			dv.show(out);
  			return;
  		} else if (op.equals("getDataJSON")) {
  			out.println(dv.getDataJSON(dataid));
  		} else if (op.equals("getDataTypeJSON")) {
  			out.println(dv.getDatatypeJSON(dtype));
  		} else if (op.equals("getDataHierarchyJSON")) {
  			out.println(dv.getDataHierarchyJSON());
  		}
		
  		// Writer functions
			if (op.equals("saveDataJSON")) {
				String propvals_json = request.getParameter("propvals_json");
				if (dv.saveDataJSON(dataid, propvals_json))
					out.print("OK");
			} else if (op.equals("saveDataTypeJSON")) {
				String props_json = request.getParameter("props_json");
				if (!config.isSandboxed())
					out.print(dv.saveDatatypeJSON(dtype, props_json));
			} else if (op.equals("newDataType")) {
				String ptype = request.getParameter("parent_type");
				if (!config.isSandboxed())
					if (dv.addDatatype(ptype, dtype))
						out.print("OK");
			} else if (op.equals("delDataTypes")) {
				Gson gson = new Gson();
				String[] dtypes = gson.fromJson(dtype, String[].class);
				if (!config.isSandboxed())
					if (dv.delDatatypes(dtypes))
						out.print("OK");
			} else if (op.equals("moveDatatypeTo")) {
				String fromtype = request.getParameter("from_parent_type");
				String totype = request.getParameter("to_parent_type");
				if (!config.isSandboxed())
					if (dv.moveDatatypeTo(dtype, fromtype, totype))
						out.print("OK");
			} else if (op.equals("addDataForType")) {
				if (dv.addDataForDatatype(dataid, dtype))
					out.print("OK");
			} else if (op.equals("delData")) {
				if (dv.delData(dataid))
					out.print("OK");
			} else if (op.equals("renameData")) {
				String newid = request.getParameter("newid");
				if (dv.renameData(dataid, newid))
					out.print("OK");
			} else if (op.equals("renameDataType")) {
				String newid = request.getParameter("newid");
				if (dv.renameDataType(dtype, newid))
					out.print("OK");
			} else if (op.equals("setDataLocation")) {
				String location = request.getParameter("location");
				if (dv.setDataLocation(dataid, location))
					out.print("OK");
			} else if (op.equals("addBatchData")) {
				Gson gson = new Gson();
				String[] dids = gson.fromJson(request.getParameter("data_ids"), String[].class);
				String[] locs = gson.fromJson(request.getParameter("data_locations"),
						String[].class);
				if (dv.addBatchData(dtype, dids, locs))
					out.print("OK");
			} else if (op.equals("importFromExternalCatalog")) {
				String propvals_json = request.getParameter("propvals_json");
				String location = request.getParameter("location");
				if(dv.importFromExternalCatalog(dataid, dtype, propvals_json, location))
					out.print("OK");
			} else if (op.equals("registerData")) {
        String newname = request.getParameter("newname");
			  String metadata_json = request.getParameter("metadata_json");
			  out.print(dv.registerData(dataid, newname, metadata_json));
			}
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doGet(request, response);
	}
}
