/**
* OLAT - Online Learning and Training<br>
* http://www.olat.org
* <p>
* Licensed under the Apache License, Version 2.0 (the "License"); <br>
* you may not use this file except in compliance with the License.<br>
* You may obtain a copy of the License at
* <p>
* http://www.apache.org/licenses/LICENSE-2.0
* <p>
* Unless required by applicable law or agreed to in writing,<br>
* software distributed under the License is distributed on an "AS IS" BASIS, <br>
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
* See the License for the specific language governing permissions and <br>
* limitations under the License.
* <p>
* Copyright (c) 2008 frentix GmbH, Switzerland<br>
* <p>
*/

package org.olat.core.util.mail.ui;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.id.Identity;
import org.olat.core.id.User;
import org.olat.core.id.UserConstants;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.core.util.mail.MailModule;
import org.olat.core.util.mail.manager.MailManager;
import org.olat.core.util.mail.model.DBMailAttachment;
import org.olat.core.util.mail.model.DBMailImpl;
import org.olat.core.util.mail.model.DBMailRecipient;

/**
 * 
 * Description:<br>
 * 
 * <P>
 * Initial Date:  24 mars 2011 <br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class MailController extends FormBasicController {
	
	private FormLink backLink;
	
	private String mapperBaseURI;
	private final DBMailImpl mail;
	private final List<DBMailAttachment> attachments;
	private final MailManager mailManager;
	
	public MailController(UserRequest ureq, WindowControl wControl, DBMailImpl mail) {
		super(ureq, wControl, LAYOUT_VERTICAL);
		setTranslator(Util.createPackageTranslator(MailModule.class, ureq.getLocale()));
		this.mail = mail;
		mailManager = MailManager.getInstance();
		attachments = MailManager.getInstance().getAttachments(mail);
		if(!attachments.isEmpty()) {
			mapperBaseURI = registerMapper(new MailAttachmentMapper(mail, mailManager));
		}
		initForm(ureq);
	}

	@Override
	protected void initForm(FormItemContainer mainLayout, Controller listener, UserRequest ureq) {
		setTranslator(Util.createPackageTranslator(MailModule.class, ureq.getLocale()));
		String page = Util.getPackageVelocityRoot(MailModule.class) + "/mail.html";
		FormLayoutContainer vcLayout = FormLayoutContainer.createCustomFormLayout("wrapper", getTranslator(), page);
		vcLayout.setRootForm(mainForm);
		mainLayout.add(vcLayout);
		
		if(!StringHelper.containsNonWhitespace(mail.getMetaId())) {
			backLink = uifactory.addFormLink("back", vcLayout, Link.LINK_BACK);
			vcLayout.add("back", backLink);
		}

		FormLayoutContainer formLayout = FormLayoutContainer.createDefaultFormLayout("mainCmp", getTranslator());
		formLayout.setRootForm(mainForm);
		vcLayout.add("mainCmp", formLayout);
		
		String from = getFullName(mail.getFrom());
		uifactory.addStaticTextElement("from", "mail.from", from, formLayout);
		
		String recipients = getRecipients();
		uifactory.addStaticTextElement("recipients", "mail.recipients", recipients, formLayout);

		uifactory.addStaticTextElement("subject", "mail.subject", mail.getSubject(), formLayout);
		uifactory.addStaticTextElement("body", "mail.body", formattedBody(), formLayout);
		
		if(!attachments.isEmpty()) {
			String attachmentsPage = Util.getPackageVelocityRoot(MailModule.class) + "/attachments.html";
			FormLayoutContainer container = FormLayoutContainer.createCustomFormLayout("attachments", getTranslator(), attachmentsPage);
			container.setLabel("mail.attachments", null);
			container.setRootForm(mainForm);
			container.contextPut("attachments", attachments);
			container.contextPut("mapperBaseURI", mapperBaseURI);
			formLayout.add(container);
		}
	}
	
	private String getRecipients() {
		StringBuilder sb = new StringBuilder();
		Set<String> groups = new HashSet<String>();
		for(DBMailRecipient recipient:mail.getRecipients()) {
			String group = recipient.getGroup();
			if(StringHelper.containsNonWhitespace(group) && !groups.contains(group)) {
				if(sb.length() > 0) sb.append(", ");
				sb.append(group);
				groups.add(group);
			}	
		}
		return sb.toString();
	}
	
	private String getFullName(DBMailRecipient recipient) {
		if(recipient == null) return "";
		return getFullName(recipient.getRecipient());
	}
	
	private String getFullName(Identity identity) {
		StringBuilder sb = new StringBuilder();
		if(identity != null) {
			User user = identity.getUser();
			sb.append(user.getProperty(UserConstants.LASTNAME, null))
				.append(" ")
				.append(user.getProperty(UserConstants.FIRSTNAME, null));
		}
		return sb.toString();
	}
	
	private String formattedBody() {
		String body = mail.getBody();
		if(!StringHelper.containsNonWhitespace(body)) return "";
		
		if(body.indexOf("<") >= 0 && body.indexOf("/>") >= 0) {
			//html
			return body;
		}

		body = body.replace("\n\r", "<br />");//if windows
		body = body.replace("\n", "<br />");
		return body;
	}
	

	@Override
	protected void doDispose() {
		//
	}

	@Override
	protected void formOK(UserRequest ureq) {
		//
	}

	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if(source == backLink) {
			fireEvent(ureq, Event.BACK_EVENT);
		} else {
			super.formInnerEvent(ureq, source, event);
		}
	}
}