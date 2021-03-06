/**
 * <a href="http://www.openolat.org">
 * OpenOLAT - Online Learning and Training</a><br>
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License"); <br>
 * you may not use this file except in compliance with the License.<br>
 * You may obtain a copy of the License at the
 * <a href="http://www.apache.org/licenses/LICENSE-2.0">Apache homepage</a>
 * <p>
 * Unless required by applicable law or agreed to in writing,<br>
 * software distributed under the License is distributed on an "AS IS" BASIS, <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Initial code contributed and copyrighted by<br>
 * frentix GmbH, http://www.frentix.com
 * <p>
 */

package org.olat.resource.accesscontrol.provider.token.ui;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.Form;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.resource.accesscontrol.model.AccessMethod;
import org.olat.resource.accesscontrol.model.OfferImpl;
import org.olat.resource.accesscontrol.model.OfferAccess;
import org.olat.resource.accesscontrol.ui.AbstractConfigurationMethodController;

/**
 * 
 * Description:<br>
 * Configuration for a token
 * 
 * <P>
 * Initial Date:  15 avr. 2011 <br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class TokenAccessConfigurationController extends AbstractConfigurationMethodController {

	private TextElement descEl;
	private TextElement tokenEl;
	private final OfferAccess link;
	
	public TokenAccessConfigurationController(UserRequest ureq, WindowControl wControl, OfferAccess link) {
		super(ureq, wControl);
		this.link = link;
		initForm(ureq);
	}

	public TokenAccessConfigurationController(UserRequest ureq, WindowControl wControl, OfferAccess link, Form form) {
		super(ureq, wControl, LAYOUT_DEFAULT, null, form);
		this.link = link;
		initForm(ureq);
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		formLayout.setElementCssClass("o_sel_accesscontrol_token_form");
		
		descEl = uifactory.addTextAreaElement("offer-desc", "offer.description", 2000, 6, 80, false, null, formLayout);
		descEl.setElementCssClass("o_sel_accesscontrol_description");
		
		String token = "";
		if(link.getOffer() instanceof OfferImpl) {
			token = ((OfferImpl)link.getOffer()).getToken();
		}
		tokenEl = uifactory.addTextElement("token", "accesscontrol.token", 255, token, formLayout);
		tokenEl.setElementCssClass("o_sel_accesscontrol_token");
		
		super.initForm(formLayout, listener, ureq);
	}
	
	@Override
	public AccessMethod getMethod() {
		return link.getMethod();
	}

	@Override
	public OfferAccess commitChanges() {
		if(link.getOffer() instanceof OfferImpl) {
			((OfferImpl)link.getOffer()).setToken(tokenEl.getValue());
		}
		link.getOffer().setDescription(descEl.getValue());
		return link;
	}

	@Override
	protected boolean validateFormLogic(UserRequest ureq) {
		boolean allOk = true;
		
		String token = tokenEl.getValue();
		tokenEl.clearError();
		if(token == null || token.length() < 2) {
			tokenEl.setErrorKey("invalid.token.format", null);
			allOk = false;
		}
		
		return allOk && super.validateFormLogic(ureq);
	}
}
