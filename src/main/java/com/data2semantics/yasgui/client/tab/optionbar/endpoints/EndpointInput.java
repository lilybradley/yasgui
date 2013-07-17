package com.data2semantics.yasgui.client.tab.optionbar.endpoints;

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

import com.data2semantics.yasgui.client.View;
import com.data2semantics.yasgui.client.helpers.Helper;
import com.data2semantics.yasgui.client.helpers.JsMethods;
import com.data2semantics.yasgui.client.helpers.LocalStorageHelper;
import com.data2semantics.yasgui.client.settings.Imgs;
import com.data2semantics.yasgui.client.tab.QueryTab;
import com.data2semantics.yasgui.shared.Endpoints;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.data.AdvancedCriteria;
import com.smartgwt.client.data.Criterion;
import com.smartgwt.client.types.Autofit;
import com.smartgwt.client.types.OperatorId;
import com.smartgwt.client.types.Positioning;
import com.smartgwt.client.types.TextMatchStyle;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.util.StringUtil;
import com.smartgwt.client.widgets.Canvas;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.ImgButton;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.ComboBoxItem;
import com.smartgwt.client.widgets.form.fields.FormItemCriteriaFunction;
import com.smartgwt.client.widgets.form.fields.FormItemFunctionContext;
import com.smartgwt.client.widgets.form.fields.events.BlurEvent;
import com.smartgwt.client.widgets.form.fields.events.BlurHandler;
import com.smartgwt.client.widgets.form.fields.events.ChangedEvent;
import com.smartgwt.client.widgets.form.fields.events.ChangedHandler;
import com.smartgwt.client.widgets.form.fields.events.FocusEvent;
import com.smartgwt.client.widgets.form.fields.events.FocusHandler;
import com.smartgwt.client.widgets.grid.CellFormatter;
import com.smartgwt.client.widgets.grid.HoverCustomizer;
import com.smartgwt.client.widgets.grid.ListGrid;
import com.smartgwt.client.widgets.grid.ListGridField;
import com.smartgwt.client.widgets.grid.ListGridRecord;
import com.smartgwt.client.widgets.grid.events.SelectionUpdatedEvent;
import com.smartgwt.client.widgets.grid.events.SelectionUpdatedHandler;
import com.smartgwt.client.widgets.layout.HLayout;

public class EndpointInput extends DynamicForm {
	private View view;
	private ComboBoxItem endpoint;
	private String latestEndpointValue; //used to detect when to check for cors enabled. Not just on blur, but only on blur and when value has changed
	private QueryTab queryTab;
	private ListGrid pickListProperties;
	private static int WIDTH = 420;
	private static int COL_WIDTH_DATASET_TITLE = 150;
	private static int COL_WIDTH_MORE_INFO = 22;
	private static int BUTTON_OFFSET_Y = 18;
	private static int BUTTON_OFFSET_X = WIDTH - 39;
	private boolean pickListRecordSelected = false;
	private ArrayList<String> cols = new ArrayList<String>();
	private HLayout buttonSpace = new HLayout();
	public EndpointInput(View view, QueryTab queryTab) {
		this.queryTab = queryTab;
		this.view = view;
		setTitleOrientation(TitleOrientation.TOP);
		createTextInput();
		addButtonSpace();
//		getProperties();
	}

	
	public EndpointInput() {
		setTitleOrientation(TitleOrientation.TOP);
		createTextInput();
		//Init value
		latestEndpointValue = getEndpoint();
		
	}
	
	private void addButtonSpace() {
		Scheduler.get().scheduleDeferred(new Command() {
			public void execute() {
				//initialize button space
				buttonSpace.setAutoWidth();
				buttonSpace.setPosition(Positioning.ABSOLUTE);
				buttonSpace.setLeft(getDOM().getAbsoluteLeft() + BUTTON_OFFSET_X);
				buttonSpace.setTop(getDOM().getAbsoluteTop() + BUTTON_OFFSET_Y);
				buttonSpace.setAutoHeight();
				buttonSpace.setAutoWidth();
				buttonSpace.draw();
				addFetchAutocompletionsButton();
			}
		});
	}
	
	private void addFetchAutocompletionsButton() {
		resetButtonSpace();
		ImgButton imgButton = new ImgButton();
		imgButton.setSrc(Imgs.get(Imgs.DOWNLOAD_ROUND));
		imgButton.setHeight(16);
		imgButton.setWidth(16);
		imgButton.setShowOverCanvas(false);
		imgButton.setShowDownIcon(false);
		imgButton.setTooltip("Fetch predicate autocompletion information for this endpoint");
		imgButton.addClickHandler(new ClickHandler(){
			@Override
			public void onClick(ClickEvent event) {
				getProperties();
			}});
		buttonSpace.addMember(imgButton);
		
	}
	private void addFetchingAutocompletionsIcon() {
		resetButtonSpace();
		Img img = new Img(Imgs.get(Imgs.LOADING));
		img.setHeight(16);
		img.setWidth(16);
		img.setTooltip("Fetching predicate autocompletion information for this endpoint");
		buttonSpace.addMember(img);
	}
	
	private void addAutocompletionsFetchedIcon() {
		resetButtonSpace();
		Img img = new Img(Imgs.get(Imgs.CHECKMARK));
		img.setHeight(16);
		img.setWidth(16);
		img.setTooltip("Autocompletion information fetched for this endpoint");
		buttonSpace.addMember(img);
	}
	private void addFetchingFailedIcon() {
		resetButtonSpace();
		ImgButton imgButton = new ImgButton();
		imgButton.setSrc(Imgs.get(Imgs.CROSS));
		imgButton.setHeight(16);
		imgButton.setWidth(16);
		imgButton.setShowOverCanvas(false);
		imgButton.setShowDownIcon(false);
		imgButton.setTooltip("Unable to fetch predicate autocompletion information for this endpoint. Click to retry");
		imgButton.addClickHandler(new ClickHandler(){
			@Override
			public void onClick(ClickEvent event) {
				getProperties();
			}});
		buttonSpace.addMember(imgButton);
	}
	
	private void resetButtonSpace() {
		Canvas[] contents = buttonSpace.getMembers();
		for (Canvas content: contents) {
			content.destroy();
		}
	}
	private void createTextInput() {
		endpoint = new ComboBoxItem("endpoint", "Endpoint");
		endpoint.setValueField(Endpoints.KEY_ENDPOINT);
		endpoint.setAddUnknownValues(true);
		endpoint.setCompleteOnTab(true);
		endpoint.setWidth(WIDTH);
		endpoint.setOptionDataSource(view.getEndpointDataSource());
		endpoint.setHideEmptyPickList(true);
		endpoint.setDefaultValue(getQueryTab().getTabSettings().getEndpoint());
		

		initPickList();
		
        endpoint.setPickListProperties(pickListProperties);
        endpoint.setPickListFilterCriteriaFunction(new FormItemCriteriaFunction(){
			@Override
			public AdvancedCriteria getCriteria(FormItemFunctionContext itemContext) {
				String value = getEndpoint();
				AdvancedCriteria criteria = new AdvancedCriteria(OperatorId.OR, new Criterion[]{
						new Criterion(Endpoints.KEY_TITLE, OperatorId.ICONTAINS, value),
						new Criterion(Endpoints.KEY_ENDPOINT, OperatorId.ICONTAINS, value)
				});
				return criteria;
			}});

        setFields(endpoint);
		endpoint.addFocusHandler(new FocusHandler() {
			@Override
			public void onFocus(FocusEvent event) {
				latestEndpointValue = getEndpoint();
			}
		});
		endpoint.addBlurHandler(new BlurHandler() {
			@Override
			public void onBlur(BlurEvent event) {
				String endpoint = getEndpoint();
				if (!latestEndpointValue.equals(endpoint)) {
					setEndpoint(endpoint, true);
				}
			}
		});
		endpoint.addChangedHandler(new ChangedHandler(){

			@Override
			public void onChanged(ChangedEvent event) {
				if (pickListRecordSelected == true) {
					//only perform when item from listgrid was selected. Otherwise on every keypress we'll do lots of processing
					pickListRecordSelected = false;
					setEndpoint(getEndpoint(), true);
				}
			}});
	}
	
	private void setPickListFieldsForComboBox(ArrayList<ListGridField> fields) {
		//Also store index and field key in arraylist. 
		//Need this because somehow using the listgrid 'getField' method in the setcellformatter causes stack overflow (maybe due to different initialization of listgrid)
		for (ListGridField field: fields) {
			cols.add(field.getName());
		}
		endpoint.setPickListFields(fields.toArray(new ListGridField[fields.size()]));
	}
	
	public String getEndpoint() {
		String endpointString = endpoint.getValueAsString();
		if (endpointString == null) {
			endpointString = "";
		}
		return endpointString;
	}
	
	
	/**
	 * set endpoint string
	 * @param endpointString
	 */
	public void setEndpoint(String endpointString) {
		setEndpoint(endpointString, false);
	}
	
	/**
	 * set endpoint, with parameter to allow for executing callback function (e.g. store in cache and setting object)
	 * @param endpointString
	 */
	public void setEndpoint(String endpointString, boolean execCallback) {
		endpoint.setValue(endpointString);
		if (execCallback) {
			setEndpointCallback(endpointString);
		}
	}
	
	/**
	 * Callback executed after setting the endpoint. Stores the endpoint in settings, and checks cors
	 * @param endpointString
	 */
	public void setEndpointCallback(String endpointString) {
		JsMethods.checkCorsEnabled(endpointString);
		view.getSelectedTabSettings().setEndpoint(endpointString);
		LocalStorageHelper.storeSettingsInCookie(view.getSettings());
		getProperties();
	}
	
	private QueryTab getQueryTab() {
		return this.queryTab;
	}
	
	/**
	 * Initiate the dropdown picklist (listgrid)
	 */
	private void initPickList() {
		pickListProperties = new ListGrid();
		
		pickListProperties.setAutoFitData(Autofit.VERTICAL);
		pickListProperties.setHoverWidth(300);
		ArrayList<ListGridField> fields = new ArrayList<ListGridField>();
		ListGridField datasetTitle = new ListGridField(Endpoints.KEY_TITLE, "Dataset", COL_WIDTH_DATASET_TITLE);
		
		fields.add(datasetTitle);
		fields.add(new ListGridField(Endpoints.KEY_ENDPOINT, "Endpoint"));
		fields.add(new ListGridField(Endpoints.KEY_DATASETURI, " ", COL_WIDTH_MORE_INFO));
		
		setPickListFieldsForComboBox(fields);
		pickListProperties.setShowHeaderContextMenu(false);
		pickListProperties.setFixedRecordHeights(false);
		pickListProperties.setWrapCells(true);
		pickListProperties.setAutoFetchTextMatchStyle(TextMatchStyle.SUBSTRING);
        pickListProperties.setCanHover(true);  
        pickListProperties.setShowHover(true);
        pickListProperties.addSelectionUpdatedHandler(new SelectionUpdatedHandler(){

			@Override
			public void onSelectionUpdated(SelectionUpdatedEvent event) {
				//Thing is: the current value of the combobox is still the old one.
				//The new value (selected record) is not retrievable (getting it from 'selectedRecord' explodes our stack size... (bug?)
				//And we don't want to use the onchanged handler of the combobox itself (because then on every keypress we check for cors stuff)
				//Therefor, set this flag. The onchanged handler of the combobox can then check and reset this flag and store the endpoint
				pickListRecordSelected = true;
			}});
        pickListProperties.setCellFormatter(new CellFormatter() {
			@Override
			public String format(Object value, ListGridRecord record, int rowNum, int colNum) {
				if (rowNum == 0 && colNum == 0 && Helper.recordIsEmpty(record)) {
					return "Empty";
				}
				String colName = cols.get(colNum);
				String cellValue = record.getAttribute(colName);
				
				if (cellValue != null) {
					if (colName.equals(Endpoints.KEY_TITLE) || colName.equals(Endpoints.KEY_ENDPOINT)) {
						return "<span style='cursor:pointer;'>" + cellValue + "</span>";
					} else if (colName.equals(Endpoints.KEY_DATASETURI) && cellValue.length() > 0) {
						return "<a href='" + cellValue + "' target='_blank'><img src='" + Imgs.OTHER_IMAGES_DIR + Imgs.get(Imgs.INFO) + "' width='16' height='16'></a>";
					}
				}
                return null;
            }  
        });  
        pickListProperties.setHoverCustomizer(new HoverCustomizer() {  
            @Override  
            public String hoverHTML(Object value, ListGridRecord record, int rowNum, int colNum) {  
                return StringUtil.asHTML(record.getAttribute(Endpoints.KEY_DESCRIPTION)); 
            }  
        });  
	}
	public void getProperties() {
		final String endpoint = view.getSelectedTabSettings().getEndpoint();
		if (JsMethods.propertiesRetrieved(endpoint)) {
			addAutocompletionsFetchedIcon();
		} else {
			addFetchingAutocompletionsIcon();
			view.getRemoteService().fetchProperties(endpoint, false, new AsyncCallback<String>() {
				public void onFailure(Throwable caught) {
					view.getLogger().severe("failure in getting properties");
					view.getElements().onError(caught);
					addFetchingFailedIcon();
				}
	
				public void onSuccess(String properties) {
					if (properties.length() > 0) {
						JsMethods.setAutocompleteProperties(endpoint, properties);
						view.getLogger().severe("we've retrieved properties");
						addAutocompletionsFetchedIcon();
					} else {
						addFetchingFailedIcon();
						view.getLogger().severe("empty properties returned");
					}
				}
			});
		}
	}
	
	
}