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
 * Copyright (c) frentix GmbH<br>
 * http://www.frentix.com<br>
 * <p>
 */
package org.olat.user;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.olat.NewControllerFactory;
import org.olat.admin.user.delete.service.UserDeletionManager;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.id.Identity;
import org.olat.core.id.context.BusinessControl;
import org.olat.core.id.context.BusinessControlFactory;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.id.context.ContextEntryControllerCreator;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.manager.BasicManager;
import org.olat.core.util.StringHelper;
import org.olat.core.util.mail.manager.MailManager;
import org.olat.core.util.mail.model.DBMailImpl;
import org.olat.core.util.mail.ui.MailContextResolver;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupManagerImpl;
import org.olat.home.site.HomeSite;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;

/**
 * 
 * Description:<br>
 * The MailBoxExtensio for 
 * 
 * <P>
 * Initial Date:  25 mars 2011 <br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class MailBoxExtension extends BasicManager implements MailContextResolver, UserDataDeletable {
	
	private static final OLog log = Tracing.createLoggerFor(MailBoxExtension.class);

	private MailManager mailManager;
	
	public MailBoxExtension(UserDeletionManager userDeletionManager) {
		userDeletionManager.registerDeletableUserData(this);
	}
	
	public void init() {
		NewControllerFactory.getInstance().addContextEntryControllerCreator("Inbox", new ContextEntryControllerCreator(){
			@Override
			public Controller createController(ContextEntry ce, UserRequest ureq, WindowControl wControl) {
				return null;
			}

			@Override
			public String getTabName(ContextEntry ce) {
				// opens in home-tab
				return null;
			}

			@Override
			public String getSiteClassName(ContextEntry ce) {
				return HomeSite.class.getName();
			}

			@Override
			public boolean validateContextEntryAndShowError(ContextEntry ce, UserRequest ureq, WindowControl wControl) {
				return true;
			}
		});	
	}
	
	/**
	 * [used by Spring]
	 * @param mailManager
	 */
	public void setMailManager(MailManager mailManager) {
		this.mailManager = mailManager;
	}
	
	
	@Override
	public void deleteUserData(Identity identity, String newDeletedUserName) {
		//set as deleted all recpient
		logInfo("Delete intern messages");
		
		List<DBMailImpl> inbox = mailManager.getInbox(identity, null, 0, 0);
		for(DBMailImpl inMail:inbox) {
			mailManager.delete(inMail, identity, true);
		}
		
		List<DBMailImpl> outbox = mailManager.getOutbox(identity, 0, 0);
		for(DBMailImpl outMail:outbox) {
			mailManager.delete(outMail, identity, true);
		}
		
		logInfo("Delete " + inbox.size() + " messages in INBOX and " + outbox.size() + " in OUTBOX");
	}

	@Override
	public String getName(String businessPath, Locale locale) {
		if(!StringHelper.containsNonWhitespace(businessPath)) return null;
		
		try {
			List<ContextEntry> entries = BusinessControlFactory.getInstance().createCEListFromString(businessPath);
			Collections.reverse(entries);
			
			for(ContextEntry entry:entries) {
				String resourceTypeName = entry.getOLATResourceable().getResourceableTypeName();
				Long resourceId = entry.getOLATResourceable().getResourceableId();
				if("BusinessGroup".equals(resourceTypeName)) {
					BusinessGroup group = BusinessGroupManagerImpl.getInstance().loadBusinessGroup(resourceId, false);
					return group.getName();
				} else if ("RepositoryEntry".equals(resourceTypeName)) {
					RepositoryEntry re = RepositoryManager.getInstance().lookupRepositoryEntry(resourceId);
					return re.getDisplayname();
				}
			}
		} catch (Exception e) {
			log.error("", e);
		}
		return businessPath;
	}

	@Override
	public void open(UserRequest ureq, WindowControl wControl, String businessPath) {
		BusinessControl bc = BusinessControlFactory.getInstance().createFromString(businessPath);
	  WindowControl bwControl = BusinessControlFactory.getInstance().createBusinessWindowControl(bc, wControl);
	  NewControllerFactory.getInstance().launch(ureq, bwControl);
	}
}