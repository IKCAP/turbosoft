package org.earthcube.geosoft.portal.servlets;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFWriter;
import com.hp.hpl.jena.sparql.resultset.ResultsFormat;
import com.hp.hpl.jena.tdb.TDB;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.update.UpdateAction;
import com.hp.hpl.jena.update.UpdateFactory;
import com.hp.hpl.jena.update.UpdateRequest;

import org.earthcube.geosoft.portal.classes.Config;
import org.earthcube.geosoft.portal.classes.WriteLock;

/**
 * Servlet implementation class SparqlEndpoint
 */
public class SparqlEndpoint extends HttpServlet {
	private static final long serialVersionUID = 1L;
       
	private ServletOutputStream out;
    /**
     * @see HttpServlet#HttpServlet()
     */
    public SparqlEndpoint() {
        super();
        // TODO Auto-generated constructor stub
    }

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		this.out = response.getOutputStream();
		String queryString = request.getParameter("query");
		String updateString = request.getParameter("update");
		if (queryString == null && updateString == null) {
			response.setContentType("text/html");
			out.print("<form>"
					+ "<h1>Turbosoft Portal Sparql endpoint</h1>"
					+ "<h4>Enter select query below</h4>"
					+ "<textarea name='query' rows='20' cols='100'></textarea>"
					/*+ "<h4>Enter update query below</h4>"
					+ "<textarea name='update' rows='20' cols='100'></textarea>"*/
					+ "<br/>"
					+ "<input type='submit'/>"
					+ "</form>");
			return;
		}
		synchronized (WriteLock.Lock) {
  		try {
  			if(queryString != null && !queryString.equals(""))
  				this.showQueryResults(queryString, request, response);
  			else if(updateString != null && !updateString.equals(""))
  				this.updateDataset(updateString, request, response);
  		}
  		catch (Exception e) {
  			response.getOutputStream().print(e.getMessage());
  		}
  		response.getOutputStream().flush();
		}
	}

	private void showQueryResults(String queryString, HttpServletRequest request, HttpServletResponse response) 
			throws IOException {
		Query query = QueryFactory.create(queryString);
		if(query.isSelectType() || query.isDescribeType()) {
			Config config = new Config(request);
			ResultsFormat fmt = ResultsFormat.lookup(request.getParameter("format"));
			Dataset tdbstore = TDBFactory.createDataset(config.getTripleStoreDir());
			QueryExecution qexec = QueryExecutionFactory.create(query, tdbstore);
			qexec.getContext().set(TDB.symUnionDefaultGraph, true);
			if(query.isSelectType()) {
  			ResultSet results = qexec.execSelect();
  			if(fmt == null) {
  				out.print(queryString+"\n");
  				ResultSetFormatter.out(out, results, query);
  			}
  			else
  				ResultSetFormatter.output(out, results, fmt);
			}
			else {
			  Model model = qexec.execDescribe();
        RDFWriter rdfWriter = model.getWriter("RDF/XML-ABBREV");
        rdfWriter.setProperty("showXmlDeclaration", "true");
        rdfWriter.setProperty("tab", "6");
        rdfWriter.write(model, out, null);
			}
		}
		else {
			out.print("Only select or describe queries allowed");
		}
	}
	
	private void updateDataset(String updateString, HttpServletRequest request, HttpServletResponse response) 
			throws IOException {
		Config config = new Config(request);
		Dataset tdbstore = TDBFactory.createDataset(config.getTripleStoreDir());
		UpdateRequest update = UpdateFactory.create(updateString);
		UpdateAction.execute(update, tdbstore);
		out.print("Updated");
		TDB.sync(tdbstore);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

}
