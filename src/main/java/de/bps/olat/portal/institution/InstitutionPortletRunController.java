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
 * BPS Bildungsportal Sachsen GmbH, http://www.bps-system.de
 * <p>
 */
package de.bps.olat.portal.institution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.olat.ControllerFactory;
import org.olat.NewControllerFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.Windows;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.dtabs.DTab;
import org.olat.core.gui.control.generic.dtabs.DTabs;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.UserConstants;
import org.olat.core.logging.Tracing;
import org.olat.core.util.StringHelper;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;

public class InstitutionPortletRunController extends BasicController {
	
	private VelocityContainer portletVC;
	private List<String> polyLinks;
	private Map<Link, PolymorphLink> mapLinks;
	private InstitutionPortletEntry ipe;

	protected InstitutionPortletRunController(UserRequest ureq, WindowControl wControl) {
		super(ureq, wControl);
		this.portletVC = createVelocityContainer("institutionPortlet");
		
		String userinst = ureq.getIdentity().getUser().getProperty(UserConstants.INSTITUTIONALNAME, ureq.getLocale());
		if(StringHelper.containsNonWhitespace(userinst)) {
			ipe = InstitutionPortlet.getInstitutionPortletEntry(userinst.toLowerCase());
		} else {
			userinst = "unknown";
			ipe = null;
		}
		
		this.portletVC.contextPut("hasInstitution", new Boolean(ipe != null));
		if (ipe == null) {
			logWarn("unknown institution (" + userinst + ") for user " + ureq.getIdentity().getName(), null);
		} else {
			this.portletVC.contextPut("iname", ipe.getInstitutionName());
			this.portletVC.contextPut("iurl", ipe.getInstitutionUrl());
			this.portletVC.contextPut("ilogo", ipe.getInstitutionLogo());
			// --> just read first supervisor element:
			InstitutionPortletSupervisorEntry ipse = ipe.getSupervisors().get(0);
			this.portletVC.contextPut("sperson", ipse.getSupervisorPerson());

			Boolean showphone = Boolean.FALSE;
			Boolean showemail = Boolean.FALSE;
			Boolean showurl = Boolean.FALSE;
			Boolean showblog = Boolean.FALSE;
			String sphone = ipse.getSupervisorPhone();
			String semail = ipse.getSupervisorMail();
			String surl = ipse.getSupervisorURL();
			String sblog = ipse.getSupervisorBlog();
			this.portletVC.contextPut("sphone", sphone);
			this.portletVC.contextPut("semail", semail);
			this.portletVC.contextPut("surl", surl);
			this.portletVC.contextPut("sblog", sblog);

			if (sphone != null && sphone.length() > 0) {
				showphone = Boolean.TRUE;
			}

			if (semail != null && semail.length() > 0) {
				showemail = Boolean.TRUE;
			}

			if (surl != null && surl.length() > 0) {
				showurl = Boolean.TRUE;
			}
			
			if (sblog != null && sblog.length() > 0) {
				showblog = Boolean.TRUE;
			}
			
			this.portletVC.contextPut("showphone", showphone);
			this.portletVC.contextPut("showemail", showemail);
			this.portletVC.contextPut("showurl", showurl);
			this.portletVC.contextPut("showblog", showblog);

			this.portletVC.contextPut("surl", ipse.getSupervisorURL());

			this.portletVC.contextPut("hasPolymorphLink", Boolean.FALSE);

			polyLinks = new ArrayList<String>();
			mapLinks = new HashMap<Link, PolymorphLink>();

			List<PolymorphLink> polyList = ipe.getPolymorphLinks();
			if (polyList != null && polyList.size() > 0) {
				int i = 0;
				for (PolymorphLink polymorphLink : polyList) {
					if ((polymorphLink.hasConditions() && (polymorphLink.getResultIDForUser(ureq) != null)) || !(polymorphLink.hasConditions())) {
						Link polyLink = LinkFactory.createCustomLink("institutionPortlet.polymorphLink." + i, "none", polymorphLink.getLinkText(),
								Link.TOOLENTRY_DEFAULT + Link.NONTRANSLATED, portletVC, this);
						polyLink.setCustomEnabledLinkCSS(polymorphLink.getLinkType().equals("course") ? "o_institutionportlet_course" : "o_institutionportlet_coursefolder");
						polyLink.setCustomDisabledLinkCSS(polymorphLink.getLinkType().equals("course") ? "o_institutionportlet_course" : "o_institutionportlet_coursefolder");
						polyLinks.add(polyLink.getComponentName());
						mapLinks.put(polyLink, polymorphLink);
						i++;
					}
				}
				if (polyLinks.size() > 0) {
					this.portletVC.contextPut("polyLinks", polyLinks);
					this.portletVC.contextPut("numPolyLinks", polyLinks.size());
					this.portletVC.contextPut("hasPolymorphLink", Boolean.TRUE);
				}
			}
		}
		putInitialPanel(this.portletVC);
	}

	/**
	 * @see org.olat.gui.control.DefaultController#event(org.olat.gui.UserRequest,
	 *      org.olat.gui.components.Component, org.olat.gui.control.Event)
	 */
	public void event(UserRequest ureq, Component source, Event event) {
		// nothing to catch
		if (source instanceof Link) {
			Link link = (Link) source;
			PolymorphLink polyLink = mapLinks.get(link);

			Long resultIDForUser = null;
			Long defaultID = null;
			String resultIDForUserS = polyLink.getResultIDForUser(ureq);

			if (resultIDForUserS != null) {
				try {
					resultIDForUser = Long.parseLong(resultIDForUserS);
				} catch (NumberFormatException e) {
					Tracing.createLoggerFor(InstitutionPortletRunController.class).error(e.getMessage());
				}
			}
			try {
				defaultID = Long.parseLong(polyLink.getDefaultLink());
			} catch (NumberFormatException e) {
				Tracing.createLoggerFor(InstitutionPortletRunController.class).error(e.getMessage());
			}

			if (polyLink.getLinkType().equals(InstitutionPortlet.TYPE_COURSE)) {
				RepositoryManager rm = RepositoryManager.getInstance();
				RepositoryEntry re = null;

				// id corresponding to the conditions set for this user
				if (polyLink != null && resultIDForUser != null) re = rm.lookupRepositoryEntry(resultIDForUser);

				// if ressource is not available choose default link
				if (re == null && defaultID != null) re = rm.lookupRepositoryEntry(defaultID);

				if (re != null) {
					if (!rm.isAllowedToLaunch(ureq, re)) {
						getWindowControl().setWarning(translate("warn.cantlaunch"));
					} else {
						OLATResourceable ores = re.getOlatResource();
						DTabs dts = Windows.getWindows(ureq).getWindow(ureq).getDTabs();
						DTab dt = dts.getDTab(ores);
						if (dt == null) {
							// does not yet exist -> create and add
							dt = dts.createDTab(ores, re, re.getDisplayname());
							if (dt == null) return;
							Controller launchController = ControllerFactory.createLaunchController(ores, ureq, dt.getWindowControl(), true);
							dt.setController(launchController);
							dts.addDTab(ureq, dt);
						}
						dts.activate(ureq, dt, null); // null: do not activate a certain
																					// view
					}
				} else {
					getWindowControl().setWarning(translate("warn.cantlaunch"));
				}
			} else if (polyLink.getLinkType().equals(InstitutionPortlet.TYPE_CATALOG)) {
				try {
					Long ceKey = resultIDForUser != null ? resultIDForUser : defaultID;
					String businessPath = "[CatalogEntry:" + ceKey + "]";
					NewControllerFactory.getInstance().launch(businessPath, ureq, getWindowControl());
				} catch (Exception e) {
					Tracing.createLoggerFor(InstitutionPortletRunController.class).error(e.getMessage());
					getWindowControl().setWarning(translate("warn.cantlaunch"));
				}
			}
		}
	}

	/**
	 * @see org.olat.gui.control.DefaultController#doDispose(boolean)
	 */
	protected void doDispose() {
		if(portletVC != null) portletVC = null;
		if(polyLinks != null) polyLinks = null;
		if(mapLinks != null) mapLinks = null;
		if(ipe != null) ipe = null;
	}

}
