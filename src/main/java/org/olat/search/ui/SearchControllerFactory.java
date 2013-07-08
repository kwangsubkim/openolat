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

package org.olat.search.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.olat.basesecurity.BaseSecurityManager;
import org.olat.basesecurity.IdentityShort;
import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.impl.Form;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.context.BusinessControl;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.Util;
import org.olat.course.CourseFactory;
import org.olat.course.ICourse;
import org.olat.course.nodes.CourseNode;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupService;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.search.SearchServiceUIFactory;
import org.olat.search.model.ResultDocument;
import org.olat.search.service.document.ContextHelpDocument;
import org.olat.user.UserManager;

/**
 * Description:<br>
 * 
 * <P>
 * Initial Date:  3 dec. 2009 <br>
 * @author srosse, stephane.rosse@frentix.com
 */
public class SearchControllerFactory implements SearchServiceUIFactory {
	private static final OLog log = Tracing.createLoggerFor(SearchControllerFactory.class);
	
	private final Map<String,ResultControllerCreator> resultControllerCreator = new HashMap<String,ResultControllerCreator>();
	
	public void addResultControllerCreator(ResultControllerCreator creator) {
		resultControllerCreator.put(creator.getFileType(), creator);
	}
	
	@Override
	public SearchInputController createInputController(UserRequest ureq, WindowControl wControl, DisplayOption displayOption, Form mainForm) {
		String resourceUrl = null;
		BusinessControl bc = wControl.getBusinessControl();
		if (bc != null) {
			resourceUrl = bc.getAsString();
		}
		
		SearchInputController searchCtrl;
		if (mainForm == null) {
			searchCtrl = new SearchInputController(ureq, wControl, resourceUrl, displayOption);
		} else {
			searchCtrl = new SearchInputController(ureq, wControl, resourceUrl, displayOption, mainForm);
		}
		return searchCtrl;
	}

	@Override
	public SearchInputController createSearchController(UserRequest ureq, WindowControl wControl) {
		String resourceUrl = null;
		BusinessControl bc = wControl.getBusinessControl();
		if (bc != null) {
			resourceUrl = bc.getAsString();
		}
		return new ResultsSearchController(ureq, wControl, resourceUrl);
	}
	
	@Override
	public ResultController createController(UserRequest ureq, WindowControl wControl, Form mainForm, ResultDocument document) {
		String documentType = document.getDocumentType();
		if(resultControllerCreator.containsKey(documentType)) {
			ResultControllerCreator creator = resultControllerCreator.get(documentType);
			return creator.createController(ureq, wControl, mainForm, document);
		}
		
		ResultController ctrl;
		if(ContextHelpDocument.TYPE.equals(documentType)) {
			ctrl = new ContextHelpResultController(ureq, wControl, mainForm, document);
		} else {
			ctrl = new StandardResultController(ureq, wControl, mainForm, document);
		}
		return ctrl;
	}
	
	
	
	@Override
	public String getBusinessPathLabel(String token, List<String> allTokens, Locale locale) {
		try {
			String[] splitted = token.split("[:]");
			if(splitted != null && splitted.length == 2) {
				String tokenType = splitted[0];
				String tokenKey = splitted[1];
				if ("RepositoryEntry".equals(tokenType)) {
					RepositoryEntry re = RepositoryManager.getInstance().lookupRepositoryEntry(Long.parseLong(tokenKey));
					return re.getDisplayname();
				}
				if ("CourseNode".equals(tokenType)) {
					String repoKey = allTokens.get(0).split("[:]")[1];
					RepositoryEntry re = RepositoryManager.getInstance().lookupRepositoryEntry(Long.parseLong(repoKey));
					ICourse course = CourseFactory.loadCourse(re.getOlatResource());
					CourseNode courseNode = course.getRunStructure().getNode(tokenKey);
					return courseNode.getShortTitle();
				}
				if ("Identity".equals(tokenType)) {
					IdentityShort identity  = BaseSecurityManager.getInstance().loadIdentityShortByKey(Long.parseLong(tokenKey));
					return UserManager.getInstance().getUserDisplayName(identity);
				}
				if ("BusinessGroup".equals(tokenType)) {
					BusinessGroup bg = CoreSpringFactory.getImpl(BusinessGroupService.class).loadBusinessGroup(Long.parseLong(tokenKey));
					return bg == null ? "" : bg.getName();
				}
				Translator translator = Util.createPackageTranslator(this.getClass(), locale);
				if("userfolder".equals(tokenType)) {
					return translator.translate("type.identity.publicfolder");
				}
				String translated = translator.translate(tokenType);
				if (translated == null || translated.length() > 64) {
					return token;//no translation, translator return an error
				}
				return translated;
			}
		} catch (Exception ex) {
			log.warn("Problem to decipher business path token: " + token, ex);
		}
		return token;
	}
}
