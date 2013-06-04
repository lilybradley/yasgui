package com.data2semantics.yasgui.client.tab.results.output;

/*
 * #%L
 * YASGUI
 * %%
 * Copyright (C) 2013 Laurens Rietveld
 * %%
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * 
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 * #L%
 */

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import com.data2semantics.yasgui.client.View;
import com.data2semantics.yasgui.client.helpers.Helper;
import com.data2semantics.yasgui.client.tab.results.input.ResultsHelper;
import com.data2semantics.yasgui.client.tab.results.input.SparqlResults;
import com.data2semantics.yasgui.shared.Prefix;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.HTMLPane;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.SortNormalizer;

public class ResultGrid extends ListGrid {
	private static String SOLUTION_ATTRIBUTE = "yasgui___solution";
	private HashMap<Integer, HashMap<String, HashMap<String, String>>> solutions = new HashMap<Integer, HashMap<String, HashMap<String, String>>>();
	private View view;
	private SparqlResults sparqlResults;
	private HashMap<String, Prefix> queryPrefixes = new HashMap<String, Prefix>();
	public ResultGrid(View view, SparqlResults sparqlResults, HTMLPane html) {
		this.view = view;
		this.sparqlResults = sparqlResults;
		setWidth100();
		setHeight100();
		setShowRecordComponents(true);
		setShowRecordComponentsByCell(true);
		setShowRowNumbers(true);
		setFixedRecordHeights(false);
		setWrapCells(true);
		setCanResizeFields(true);
		queryPrefixes = Helper.getPrefixHashMapFromQuery(view.getSelectedTabSettings().getQueryString());
		drawQueryResults();
	}
	
	/**
	 * Take json string from query results, parse it, and draw in this table
	 * 
	 * @param jsonString
	 */
	public void drawQueryResults() {
		setVarsAsListGridFields(sparqlResults.getVariables());
		setSolutionsAsGridRecords(sparqlResults.getBindings());
	}
	
	/**
	 * Get solutions from json object, and add as object to listgridrecords (i.e. table row)
	 * 
	 * @param solutions
	 * @return
	 */
	private void setSolutionsAsGridRecords(ArrayList<HashMap<String, HashMap<String, String>>> solutions) {
		ArrayList<ListGridRecord> rows = new ArrayList<ListGridRecord>();
		for (HashMap<String, HashMap<String, String>> solution: solutions) {
			this.solutions.put(solution.hashCode(), solution);
			ListGridRecord row = new ListGridRecord();
			row.setAttribute(SOLUTION_ATTRIBUTE, solution.hashCode());
			for (Entry<String, HashMap<String, String>> entry : solution.entrySet()) {
			    String variable = entry.getKey();
			    row.setAttribute(variable, entry.getValue().get("value"));
			}
			rows.add(row);
		}
		setData(rows.toArray(new ListGridRecord[rows.size()]));
	}
	
	/**
	 * Get used vars from json object, and add them as variables (i.e. columns) to this listgrid 
	 * @param vars
	 * @return
	 */
	private void setVarsAsListGridFields(ArrayList<String> vars) {
		ArrayList<ListGridField> listGridFields = new ArrayList<ListGridField>();
		for(final String var: vars){
			ListGridField field = new ListGridField(var, var);
			field.setSortNormalizer(new SortNormalizer(){
				public Object normalize(ListGridRecord record, String fieldName) {
					HashMap<String, HashMap<String, String>> bindings = solutions.get(record.getAttributeAsInt(SOLUTION_ATTRIBUTE));
					HashMap<String, String> binding = bindings.get(fieldName);
					return binding.get("value");
				}});
			field.setCellFormatter(new CellFormatter(){
				@Override
				public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
					HashMap<String, HashMap<String, String>> bindings = solutions.get(record.getAttributeAsInt(SOLUTION_ATTRIBUTE));
					HashMap<String, String> binding = bindings.get(var);
					String type = binding.get("type");
					if (type.equals("uri")) {
						final String uri = binding.get("value");
						return new Canvas().linkHTML(ResultsHelper.getShortUri(uri, queryPrefixes), uri, "_blank", null, null, null);
					} else {
						String labelText;
						if (type.equals("literal") || binding.get("type").equals("typed-literal")) {
							labelText = ResultsHelper.getLiteralFromBinding(binding);
						} else {
							labelText = binding.get("value");
						}
						return labelText;
					}
					
				}});
			field.setCellAlign(Alignment.LEFT);
			field.setAlign(Alignment.CENTER); //for header
			
			listGridFields.add(field);
		}
		setFields(listGridFields.toArray(new ListGridField[listGridFields.size()]));
	}
	
	
	public String getDownloadFilename() {
		String filename = view.getSelectedTabSettings().getTabTitle() + ".csv";
		return filename;
	}
	
	
}
