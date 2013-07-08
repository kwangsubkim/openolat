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
* Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
* University of Zurich, Switzerland.
* <hr>
* <a href="http://www.openolat.org">
* OpenOLAT - Online Learning and Training</a><br>
* This file has been modified by the OpenOLAT community. Changes are licensed
* under the Apache 2.0 license as the original file.
*/

package org.olat.course.nodes;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.Windows;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.generic.dtabs.DTab;
import org.olat.core.gui.control.generic.dtabs.DTabs;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.context.BusinessControlFactory;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.logging.AssertException;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.controllers.RepositoryDetailsController;
import org.olat.repository.handlers.RepositoryHandler;
import org.olat.repository.handlers.RepositoryHandlerFactory;

/**
 * Initial Date: 28.11.2003
 * 
 * @author Mike Stock
 * @author guido
 */
public class CourseNodeFactory {

	private static CourseNodeFactory INSTANCE;
	private Map<String, CourseNodeConfiguration> allCourseNodeConfigurations;

	/**
	 * [used by spring]
	 */
	private CourseNodeFactory() {
		INSTANCE = this;
	}


	/**
	 * @return an instance of the course node factory.
	 */
	public static CourseNodeFactory getInstance() {
		return INSTANCE;
	}

	/**
	 * @return the list of enabled aliases
	 */
	public List<String> getRegisteredCourseNodeAliases() {
		List<CourseNodeConfiguration> configList = new ArrayList<CourseNodeConfiguration>(getAllCourseNodeConfigurations().values());
		Collections.sort(configList, new OrderComparator());
		List<String> alias = new ArrayList<String>(configList.size());
		for(CourseNodeConfiguration config:configList) {
			if(config.isEnabled()) {
				alias.add(config.getAlias());
			}
		}
		return alias;
	}

	private synchronized Map<String,CourseNodeConfiguration> getAllCourseNodeConfigurations() {
		if(allCourseNodeConfigurations == null) {
			allCourseNodeConfigurations = new HashMap<String, CourseNodeConfiguration>();
			Map<String, CourseNodeConfiguration> courseNodeConfigurationMap = CoreSpringFactory.getBeansOfType(CourseNodeConfiguration.class);
			Collection<CourseNodeConfiguration> courseNodeConfigurationValues = courseNodeConfigurationMap.values();
			for (CourseNodeConfiguration courseNodeConfiguration : courseNodeConfigurationValues) {
				allCourseNodeConfigurations.put(courseNodeConfiguration.getAlias(), courseNodeConfiguration);
			}
		}
		return allCourseNodeConfigurations;
	}
	
	/**
	 * @param alias The node type or alias
	 * @return The instance of the desired type of node if enabled
	 */
	public CourseNodeConfiguration getCourseNodeConfiguration(String alias) {
		CourseNodeConfiguration config = getAllCourseNodeConfigurations().get(alias);
		if(config.isEnabled()) {
			return config;
		}
		return null;
	}

	/**
	 * @param alias The node type or alias
	 * @return The instance of the desired type of node if enabled or not
	 */
	public CourseNodeConfiguration getCourseNodeConfigurationEvenForDisabledBB(String alias) {
		return getAllCourseNodeConfigurations().get(alias);
	}
	
	/**
	 * Launch an editor for the repository entry which is referenced in the given
	 * course node. The editor is launched in a new tab.
	 * 
	 * @param ureq
	 * @param node
	 */
	public void launchReferencedRepoEntryEditor(UserRequest ureq, CourseNode node) {
		RepositoryEntry repositoryEntry = node.getReferencedRepositoryEntry();
		if (repositoryEntry == null) {
			// do nothing
			return;
		}
		RepositoryHandler typeToEdit = RepositoryHandlerFactory.getInstance().getRepositoryHandler(repositoryEntry);
		if (!typeToEdit.supportsEdit(repositoryEntry)){
			throw new AssertException("Trying to edit repository entry which has no assoiciated editor: "+ typeToEdit);
		}					
		// Open editor in new tab
		OLATResourceable ores = repositoryEntry.getOlatResource();
		DTabs dts = Windows.getWindows(ureq).getWindow(ureq).getDTabs();
		DTab dt = dts.getDTab(ores);
		if (dt == null) {
			// does not yet exist -> create and add
			//fxdiff BAKS-7 Resume function
			dt = dts.createDTab(ores, repositoryEntry, repositoryEntry.getDisplayname());
			if (dt == null){
				//null means DTabs are full -> warning is shown
				return;
			}
			//user activity logger is set by course factory
			Controller editorController = typeToEdit.createEditorController(ores, ureq, dt.getWindowControl());
			if(editorController == null){
				//editor could not be created -> warning is shown
				return;
			}
			dt.setController(editorController);
			dts.addDTab(ureq, dt);
		}
		List<ContextEntry> entries = BusinessControlFactory.getInstance().createCEListFromResourceType(RepositoryDetailsController.ACTIVATE_EDITOR);
		dts.activate(ureq, dt, entries);
	}
	
	private static class OrderComparator implements Comparator<CourseNodeConfiguration> {
		@Override
		public int compare(CourseNodeConfiguration c1, CourseNodeConfiguration c2) {
			if(c1 == null) return -1;
			if(c2 == null) return 1;
			
			int k1 = c1.getOrder();
			int k2 = c2.getOrder();
			int diff = (k1 < k2 ? -1 : (k1==k2 ? 0 : 1));
			if(diff == 0) {
				String a1 = c1.getAlias();
				String a2 = c2.getAlias();
				if(a1 == null) return -1;
				if(a2 == null) return 1;
				diff = a1.compareTo(a1);
			}
			return diff;
		}
	}
}