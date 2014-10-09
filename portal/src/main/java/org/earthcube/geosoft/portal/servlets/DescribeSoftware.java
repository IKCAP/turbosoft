package org.earthcube.geosoft.portal.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.earthcube.geosoft.portal.classes.Config;
import org.earthcube.geosoft.portal.controllers.SoftwareController;

/**
 * Servlet implementation class ManageSoftwares
 */
public class DescribeSoftware extends HttpServlet {
	private static final long serialVersionUID = 1L;
	
	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public DescribeSoftware() {
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
		if (path == null) path = "/";
		String[] args = path.split("\\/");
		String op = args.length > 1 ? args[1] : null;
		
		if (op != null && op.equals("intro")) {
			PrintWriter out = response.getWriter();
			out.println("<div class='x-toolbar x-toolbar-default highlightIcon' " + 
					"style='padding:10px;font-size:1.5em;border-width:0px 0px 1px 0px'>"
					+ "How to describe Software Components</div>\n" + 
					"<div style='padding:5px; line-height:1.5em'>\n" + 
					"With this interface, you can:\n" + 
					"<ul>\n" + 
					"   <li>View/Edit Software components by clicking on the links in the tree on the left</li>\n" +
					"   <li>Import software descriptions from CSDMS and then add to them</li>\n" +
					"   <li>After opening a Software you may edit its properties</li>\n" + 
					"</ul>\n" + 
					"</div>");
			return;
		}

		int guid = 1;

		SoftwareController mc = new SoftwareController(guid, config);
		String softwareid = request.getParameter("softwareid");

		PrintWriter out = response.getWriter();
		if (op == null || op.equals("")) {
		  response.setContentType("text/html");
		  mc.show(out);
		  return;
		} else if (op.equals("getSoftwareJSON")) {
		  out.print(mc.getSoftwareJSON(softwareid));
		} else if (op.equals("getSoftwareTypeJSON")) {
		  String typeid = request.getParameter("typeid");
		  out.print(mc.getSoftwareTypeJSON(typeid));
		}

		if (op.equals("saveSoftwareJSON")) {
		  String software_json = request.getParameter("software_json");
		  if (!config.isSandboxed())
		    if (mc.saveSoftwareJSON(softwareid, software_json))
		      out.print("OK");
		} else if (op.equals("saveSoftwareTypeJSON")) {
		  String typeid = request.getParameter("typeid");
		  String type_json = request.getParameter("json");
		  if (!config.isSandboxed())
		    if (mc.saveSoftwareTypeJSON(typeid, type_json))
		      out.print("OK");
		} else if (op.equals("getInferredSoftware")) {
		  String software_json = request.getParameter("software_json");
		  if (!config.isSandboxed())
		    out.print(mc.getInferredSoftware(softwareid, software_json));
		} else if (op.equals("checkCode")){
		  String id = request.getParameter("softwareid");
		  if (!config.isSandboxed()) {
		    out.print(mc.checkCode(id));
		  }
		} else if (op.equals("runAuditTool")) {
		  if (!config.isSandboxed())
		    if(mc.runAuditTool(softwareid))
		      out.print("OK");
		} else if (op.equals("addSoftware")) {
		  String software_typeid = request.getParameter("software_typeid");
		  if (!config.isSandboxed())
		    if (mc.addSoftware(softwareid, software_typeid))
		      out.print("OK");
		} else if (op.equals("addSoftwareType")) {
		  String typeid = request.getParameter("typeid");
		  String parentid = request.getParameter("parentid");
		  if (!config.isSandboxed())
		    if (mc.addSoftwareType(typeid, parentid))
		      out.print("OK");
		} else if (op.equals("importSoftware")) {
		  String repo_softwareid = request.getParameter("repo_softwareid");
		  String repo_id = request.getParameter("repo_id");
		  String software_typeid = request.getParameter("software_typeid");
		  if(mc.importSoftware(softwareid, software_typeid, repo_softwareid, repo_id))
		    out.print("OK");
		} else if (op.equals("delSoftware")) {
		  if (!config.isSandboxed())
		    if (mc.delSoftware(softwareid))
		      out.print("OK");
		} else if (op.equals("delSoftwareType")) {
		  String typeid = request.getParameter("typeid");
		  if (!config.isSandboxed())
		    if (mc.delSoftwareType(typeid))
		      out.print("OK");
		} else if (op.equals("renameSoftware")) {
		  String newid = request.getParameter("newid");
		  if (!config.isSandboxed())
		    if(mc.renameSoftware(softwareid, newid))
		      out.print("OK");
		} else if (op.equals("renameSoftwareType")) {
		  String typeid = request.getParameter("typeid");
		  String newid = request.getParameter("newid");
		  if (!config.isSandboxed())
		    if (mc.renameSoftwareType(typeid, newid))
		      out.print("OK");
		} else if (op.equals("moveSoftware")) {
		  softwareid = request.getParameter("id");
		  String typeid = request.getParameter("parentid");
		  if (!config.isSandboxed())
		    if (mc.setSoftwareType(softwareid, typeid))
		      out.print("OK");
		} else if (op.equals("moveSoftwareType")) {
		  String typeid = request.getParameter("id");
		  String parentid = request.getParameter("parentid");
		  if (!config.isSandboxed())
		    if (mc.setSoftwareTypeParent(typeid, parentid))
		      out.print("OK");
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
