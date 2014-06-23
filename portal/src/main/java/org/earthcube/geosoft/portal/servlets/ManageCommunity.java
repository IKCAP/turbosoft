package org.earthcube.geosoft.portal.servlets;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.earthcube.geosoft.portal.classes.Config;
import org.earthcube.geosoft.portal.classes.WriteLock;
import org.earthcube.geosoft.portal.controllers.CommunityController;

/**
 * Servlet implementation class Community
 */
public class ManageCommunity extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
    /**
     * @see HttpServlet#HttpServlet()
     */
    public ManageCommunity() {
        super();
        // TODO Auto-generated constructor stub
    }

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
            + "style='padding:10px;font-size:1.5em;border-width:0px 0px 1px 0px'>Community</div>\n"
            + "<div style='padding:5px; line-height:1.5em'>\n"
            + "With this interface, you can:\n"
            + "<ul>\n"
            + "   <li>View User information</li>\n"
            + "   <li>Edit your own information</li>\n"
            + "</ul>\n" 
            + "</div>\n");
        return;
      }
      
      int guid = 1;

      synchronized (WriteLock.Lock) {
        CommunityController uc = new CommunityController(guid, config);

        String userid = request.getParameter("userid");
  
        PrintWriter out = response.getWriter();
        // Reader functions
        if (op == null || op.equals("")) {
          response.setContentType("text/html");
          uc.show(out);
          return;
        } else if (op.equals("getUserJSON")) {
          out.println(uc.getUserJSON(userid));
        }
      
        // Writer functions
        if (op.equals("saveUserJSON")) {
          String uservals_json = request.getParameter("json");
          if (uc.saveUserJSON(userid, uservals_json))
            out.print("OK");
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
