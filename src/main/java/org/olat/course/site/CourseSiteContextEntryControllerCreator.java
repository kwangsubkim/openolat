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
package org.olat.course.site;

import java.util.List;

import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.navigation.SiteDefinition;
import org.olat.core.gui.control.navigation.SiteDefinitions;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.id.context.ContextEntryControllerCreator;
import org.olat.core.id.context.DefaultContextEntryControllerCreator;
import org.olat.course.site.model.CourseSiteConfiguration;
import org.olat.course.site.model.LanguageConfiguration;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.repository.RepositoyUIFactory;

/**
 * <h3>Description:</h3>
 * <p>
 * This class can create run controllers for repository entries in the given
 * context
 * <p>
 * Initial Date: 19.08.2009 <br>
 * 
 * @author gnaegi, gnaegi@frentix.com, www.frentix.com
 */
public class CourseSiteContextEntryControllerCreator extends DefaultContextEntryControllerCreator {

	private RepositoryEntry repoEntry;
	private SiteDefinitions siteDefinitions;
	
	@Override
	public ContextEntryControllerCreator clone() {
		return new CourseSiteContextEntryControllerCreator();
	}

	/**
	 * @see org.olat.core.id.context.ContextEntryControllerCreator#createController(org.olat.core.id.context.ContextEntry,
	 *      org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl)
	 */
	@Override
	public Controller createController(ContextEntry ce, UserRequest ureq, WindowControl wControl) {
		RepositoryEntry re = getRepositoryEntry(ce);
		Controller ctrl = RepositoyUIFactory.createLaunchController(re, ureq, wControl);
		return ctrl;
	}

	/**
	 * @see org.olat.core.id.context.ContextEntryControllerCreator#getTabName(org.olat.core.id.context.ContextEntry)
	 */
	@Override
	public String getTabName(ContextEntry ce, UserRequest ureq) {
		RepositoryEntry re = getRepositoryEntry(ce);
		CourseSiteDef siteDef = getCourseSite(ureq, re);
		if(siteDef != null) {
			return "Hello";
		}
		return re == null ? "" : re.getDisplayname();
	}
	
	/**
	 * @see org.olat.core.id.context.ContextEntryControllerCreator#getSiteClassName(org.olat.core.id.context.ContextEntry)
	 */
	@Override
	public String getSiteClassName(ContextEntry ce, UserRequest ureq) {
		RepositoryEntry re = getRepositoryEntry(ce);
		CourseSiteDef siteDef = getCourseSite(ureq, re);
		if(siteDef != null) {
			return siteDef.getClass().getName().replace("Def", "");
		}
		return null;
	}
	
	@Override
	public boolean validateContextEntryAndShowError(ContextEntry ce, UserRequest ureq, WindowControl wControl) {
		return getRepositoryEntry(ce) != null;
	}

	private SiteDefinitions getSitesDefinitions() {
		if(siteDefinitions == null) {
			siteDefinitions = CoreSpringFactory.getImpl(SiteDefinitions.class);
		}
		return siteDefinitions;
	}
	
	private CourseSiteDef getCourseSite(UserRequest ureq, RepositoryEntry re) {
		if(re == null) return null;
		
		List<SiteDefinition> siteDefList = getSitesDefinitions().getSiteDefList();
		for(SiteDefinition siteDef:siteDefList) {
			if(siteDef instanceof CourseSiteDef) {
				CourseSiteDef courseSiteDef = (CourseSiteDef)siteDef;
				CourseSiteConfiguration config = courseSiteDef.getCourseSiteconfiguration();
				LanguageConfiguration langConfig = courseSiteDef.getLanguageConfiguration(ureq, config);
				if(langConfig == null) continue;
				
				String softKey = langConfig.getRepoSoftKey();
				if(re.getSoftkey() != null && re.getSoftkey().equals(softKey)) {
					return courseSiteDef;
				}
			}
		}
		return null;
	}
	
	private RepositoryEntry getRepositoryEntry(ContextEntry ce) {
		if(repoEntry == null) {
			if(ce.getOLATResourceable() instanceof RepositoryEntry) {
				repoEntry = (RepositoryEntry)ce.getOLATResourceable();
			} else {
				OLATResourceable ores = ce.getOLATResourceable();
				RepositoryManager rm = RepositoryManager.getInstance();
				repoEntry = rm.lookupRepositoryEntry(ores.getResourceableId());
			}
		}
		return repoEntry;
	}
}
