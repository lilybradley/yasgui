package com.data2semantics.yasgui.server;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import com.data2semantics.yasgui.client.YasguiService;
import com.data2semantics.yasgui.shared.Output;
import com.data2semantics.yasgui.shared.RdfNodeContainer;
import com.data2semantics.yasgui.shared.ResultSetContainer;
import com.data2semantics.yasgui.shared.SolutionContainer;
import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecution;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;
import com.hp.hpl.jena.query.ResultSetFormatter;
import com.hp.hpl.jena.rdf.model.RDFNode;

/**
 * The server side implementation of the RPC service.
 */
@SuppressWarnings("serial")
public class YasguiServiceImpl extends RemoteServiceServlet implements YasguiService {


	public String queryGetText(String endpoint, String queryString, String format) throws IllegalArgumentException {
		String result = "";
		ResultSet resultSet = QueryService.query(endpoint, queryString);
		if (format.equals(Output.OUTPUT_JSON)) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ResultSetFormatter.outputAsJSON(baos, resultSet);
			try {
				result = baos.toString("UTF-8");
			} catch (UnsupportedEncodingException e) {
				
			}
		} else if (format.equals(Output.OUTPUT_XML)) {
			result = ResultSetFormatter.asXMLString(resultSet);
		} else if (format.equals(Output.OUTPUT_CSV)) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ResultSetFormatter.outputAsCSV(baos, resultSet);
			try {
				result = baos.toString("UTF-8");
			} catch (UnsupportedEncodingException e) {
				
			}
		} else {
			throw new IllegalArgumentException("No valid output format given as parameter");
		}
		
		return result;
	}
	
	public ResultSetContainer queryGetObject(String endpoint, String queryString) {
		ResultSetContainer resultSetContainer = new ResultSetContainer();
		ResultSet resultSet = QueryService.query(endpoint, queryString);
		resultSetContainer.setResultVars(resultSet.getResultVars());
		while (resultSet.hasNext()) {
			QuerySolution querySolution = resultSet.next();
			Iterator<String> varnames = querySolution.varNames();
			SolutionContainer solutionContainer = new SolutionContainer();
			while (varnames.hasNext()) {
				String varName = varnames.next();
				RDFNode rdfNode = querySolution.get(varName);
				String value = (String) rdfNode.visitWith(new CustomRdfVisitor());
				RdfNodeContainer rdfNodeContainer = new RdfNodeContainer();
				rdfNodeContainer.setValue(value);
				rdfNodeContainer.setVarName(varName);
				solutionContainer.addRdfNodeContainer(rdfNodeContainer);
			}
			resultSetContainer.addQuerySolution(solutionContainer);
		}
		return resultSetContainer;
	}
}