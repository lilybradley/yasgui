package com.data2semantics.yasgui.client.openid;

import java.util.ArrayList;

import com.data2semantics.yasgui.client.View;
import com.data2semantics.yasgui.client.helpers.properties.ZIndexes;
import com.data2semantics.yasgui.shared.LoginResult;
import com.data2semantics.yasgui.shared.StaticConfig;
import com.data2semantics.yasgui.shared.UserDetails;
import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.smartgwt.client.types.Alignment;
import com.smartgwt.client.types.Cursor;
import com.smartgwt.client.types.Overflow;
import com.smartgwt.client.types.Positioning;
import com.smartgwt.client.types.TitleOrientation;
import com.smartgwt.client.widgets.Button;
import com.smartgwt.client.widgets.HTMLFlow;
import com.smartgwt.client.widgets.Img;
import com.smartgwt.client.widgets.Label;
import com.smartgwt.client.widgets.events.ClickEvent;
import com.smartgwt.client.widgets.events.ClickHandler;
import com.smartgwt.client.widgets.form.DynamicForm;
import com.smartgwt.client.widgets.form.fields.TextItem;
import com.smartgwt.client.widgets.layout.HLayout;
import com.smartgwt.client.widgets.layout.LayoutSpacer;
import com.smartgwt.client.widgets.layout.VLayout;

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

public class OpenId {
	View view;
	private static int WINDOW_WIDTH = 800;
	private static int WINDOW_HEIGHT = 200;
	private boolean loggedIn = false;
	private ArrayList<OpenIdProvider> providers;
	public OpenId(View view) {
		getProviders();
		for (OpenIdProvider provider: providers) {
			//prefetch these. otherwise, somehow, on first load the icons arent shown..
			com.google.gwt.user.client.ui.Image.prefetch("images/" + provider.getImageLocation());
		}
		
		this.view = view;
		drawSessionWidgetLogin();
		// on initiation, check whether we are logged in..
		updateLoginStatus();
	}

	private void updateLoginStatus() {
		view.getOpenIdService().getCurrentUser(
		new AsyncCallback<UserDetails>() {

			public void onFailure(Throwable caught) {
				view.getElements().onError(caught);
			}

			public void onSuccess(UserDetails details) {
				if (details.isLoggedIn()) {
					drawSessionWidgetLoggedIn(details);
					view.loggedInCallback();
					loggedIn = true;
					
				} else {
					drawSessionWidgetLogin();
					loggedIn = false;
				}
			}

		});

	}

	public void login(String openIdService) {
		view.getOpenIdService().login(GWT.getHostPageBaseURL(),
				!GWT.isProdMode(), openIdService,
				new AsyncCallback<LoginResult>() {

					public void onFailure(Throwable caught) {
						view.getElements().onError("error " + caught.getMessage());
					}

					public void onSuccess(LoginResult result) {
						if (result.isLoggedIn()) {
							view.getLogger().severe("already logged in");
						} else {
							// redirect user to login page
							Window.Location.assign(result
									.getAuthenticationLink());
						}

					}

				});
	}

	public void logOut() {
		String url;
		url = StaticConfig.OPEN_ID_SERVLET + "?logOut=1&appBaseUrl="
				+ URL.encode(GWT.getHostPageBaseURL());
		if (!GWT.isProdMode()) {
			url += StaticConfig.DEBUG_FILE + "&" + StaticConfig.DEBUG_ARGUMENT_KEY + "="
					+ StaticConfig.DEBUG_ARGUMENT_VALUE;
		}
		Window.Location.assign(url);
		loggedIn = false;
	}

	public void updateSessionInfo() {
		drawSessionWidgetLogin();
	}

	private void drawSessionWidgetLoggedIn(UserDetails details) {
		loggedIn = true;
		String text = details.getDisplayName();
		
		String html = "<span class='footerText'>" + text + "</span>&nbsp;<span class='footerTextLink'>(log out)</span>";
		HTMLFlow htmlFlow = new HTMLFlow(html);
		htmlFlow.setAlign(Alignment.CENTER);
		htmlFlow.setMargin(3);
		htmlFlow.setCanSelectText(true);
		htmlFlow.setWidth(text.length() * 8);
		htmlFlow.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				view.getOpenId().logOut();
			}
		});
		view.getElements().updateSessionCanvas(htmlFlow);
	}
	private void drawSessionWidgetLogin() {
		String text = "login";
		Label sessionLabel = new Label(text);
		sessionLabel.setAlign(Alignment.CENTER);
		sessionLabel.setMargin(3);
		sessionLabel.setStyleName("footerTextLink");
		sessionLabel.setWrap(false);
		sessionLabel.setCanSelectText(true);
		sessionLabel.setWidth(text.length() * 8);

		sessionLabel.setPosition(Positioning.ABSOLUTE);
		sessionLabel.addClickHandler(new ClickHandler() {
			@Override
			public void onClick(ClickEvent event) {
				view.getOpenId().showOpenIdProviders();
			}
		});
		view.getElements().updateSessionCanvas(sessionLabel);
	}

	public void showOpenIdProviders() {
		// Window window = new Window();
		com.smartgwt.client.widgets.Window window = new com.smartgwt.client.widgets.Window();
		window.setOverflow(Overflow.HIDDEN);
		window.setZIndex(ZIndexes.MODAL_WINDOWS);
		window.setIsModal(true);
		window.setDismissOnOutsideClick(true);
		window.setShowTitle(false);
		window.setWidth(WINDOW_WIDTH);
		window.setHeight(WINDOW_HEIGHT);
		window.setShowMinimizeButton(false);
		window.setAutoCenter(true);
		window.addItem(drawProviders());
		window.draw();
	}

	private HLayout drawProviders() {
		HLayout hlayout = new HLayout();
		hlayout.setHeight(WINDOW_HEIGHT - 40);
		hlayout.setAlign(Alignment.CENTER);
		LayoutSpacer iconSpacer = new LayoutSpacer();
		iconSpacer.setWidth(15);
		iconSpacer.setHeight(1);
		boolean hasItemBefore = false;
		for (final OpenIdProvider provider : providers) {
			if (hasItemBefore) {
				hlayout.addMember(iconSpacer);
			}
			hasItemBefore = true;
			//use core google img class to get size of image. then pass it on the smartgwt (blegh)
			com.google.gwt.user.client.ui.Image gwtImg = new com.google.gwt.user.client.ui.Image("images/" + provider.getImageLocation());
			final VLayout providerContainer = new VLayout();
			
			
			if (gwtImg.getWidth() > 0 && gwtImg.getHeight() > 0) {
				//to avoid dividing by zero
				int width = provider.getMaxIconHeight() * (gwtImg.getWidth() / gwtImg.getHeight()) ;
				int height = provider.getMaxIconHeight();
				providerContainer.setPadding(15);
				providerContainer.setMargin(10);
				providerContainer.setWidth(width + 5);
				providerContainer.setHeight(WINDOW_HEIGHT - 100);
				providerContainer.setAlign(Alignment.CENTER);
				providerContainer.setStyleName("providerContainer");
				Img providerImg = new Img(provider.getImageLocation(), width, height);
				if (provider.getUrl() == null) {
					
					providerContainer.addClickHandler(new ClickHandler(){
						public void onClick(ClickEvent event) {
							if (providerContainer.getMembers().length == 2) {//might be added already
								HLayout formLayout = new HLayout();
								DynamicForm form = new DynamicForm();  
						        
						        final TextItem textItem = new TextItem();
						        textItem.setHint("http://openIdProvider.org");
						        textItem.setShowHintInField(true);
						        textItem.setWidth(150);
						        textItem.setShowTitle(false);
						        form.setTitleOrientation(TitleOrientation.TOP);
						        form.setItems(textItem);
								
								Button authenticateButton = new Button("submit");
								authenticateButton.setWidth(40);
								authenticateButton.addClickHandler(new ClickHandler(){
									public void onClick(ClickEvent event) {
										view.getOpenId().login(textItem.getValueAsString());
									}});
								formLayout.addMember(form);
								formLayout.addMember(authenticateButton);
								providerContainer.addMember(formLayout);
							}
						}
					});
			        
				} else {
					providerImg.addClickHandler(new ClickHandler(){
						public void onClick(ClickEvent event) {
							view.getOpenId().login(provider.getUrl());
						}
					});
				}
				providerImg.setCursor(Cursor.POINTER);
				providerContainer.setCursor(Cursor.POINTER);
				
				HTMLFlow text = new HTMLFlow("log in with");
				text.setCursor(Cursor.POINTER);
				providerContainer.addMember(text);
				providerContainer.addMember(providerImg);
				hlayout.addMember(providerContainer);
			}
		}
		return hlayout;
	}

	private void getProviders() {
		providers = new ArrayList<OpenIdProvider>();
		providers.add(new ProviderGoogle());
		providers.add(new ProviderYahoo());
		providers.add(new ProviderOpenId());
	}
	
	public boolean isLoggedIn() {
		return loggedIn;
	}
}
