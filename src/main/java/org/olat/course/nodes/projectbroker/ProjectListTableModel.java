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

package org.olat.course.nodes.projectbroker;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.olat.basesecurity.BaseSecurityManager;
import org.olat.core.gui.components.table.DefaultTableDataModel;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.course.nodes.projectbroker.datamodel.CustomField;
import org.olat.course.nodes.projectbroker.datamodel.Project;
import org.olat.course.nodes.projectbroker.service.ProjectBrokerManagerFactory;
import org.olat.course.nodes.projectbroker.service.ProjectBrokerModuleConfiguration;

/**
 * 
 * @author guretzki
 */

public class ProjectListTableModel extends DefaultTableDataModel {
	private static final int COLUMN_COUNT = 6;
	private Identity identity;
	private Translator translator;
	private ProjectBrokerModuleConfiguration moduleConfig;
	private int numberOfCustomFieldInTable;
	private int numberOfEventInTable;
	private int nbrSelectedProjects;
	private List<Project.EventType> enabledEventList;
	private boolean isParticipantInAnyProject;
	// Array with numbers of the customfields [0...MAX_NBR_CUSTOMFIELDS] which are enabled for table-view 
	private int[] enabledCustomFieldNumbers;
	
	private OLog log = Tracing.createLoggerFor(this.getClass());

	/**
	 * @param owned list of projects 
	 */
	public ProjectListTableModel(List owned, Identity identity, Translator translator, ProjectBrokerModuleConfiguration moduleConfig, 
			                         int numberOfCustomFieldInTable, int numberOfEventInTable, int nbrSelectedProjects, boolean isParticipantInAnyProject) {
		super(owned);
		this.identity = identity;
		this.translator = translator;
		this.moduleConfig = moduleConfig;
		this.numberOfCustomFieldInTable = numberOfCustomFieldInTable;
		this.numberOfEventInTable = numberOfEventInTable;
		this.nbrSelectedProjects = nbrSelectedProjects;
		this.enabledEventList = getEnabledEvents(moduleConfig);
		this.isParticipantInAnyProject = isParticipantInAnyProject;
		this.enabledCustomFieldNumbers = new int[numberOfCustomFieldInTable];
		// loop over all custom fields
		int index = 0;
		int customFiledIndex = 0;
		for (Iterator<CustomField> iterator = moduleConfig.getCustomFields().iterator(); iterator.hasNext();) {
			CustomField customField = iterator.next();
			if (customField.isTableViewEnabled()) {
				enabledCustomFieldNumbers[index++] = customFiledIndex;			
			}
			customFiledIndex++;
		}
	}

	/**
	 * @see org.olat.core.gui.components.table.TableDataModel#getColumnCount()
	 */
	public int getColumnCount() {
		return COLUMN_COUNT + numberOfCustomFieldInTable + numberOfEventInTable;
	}

	/**
	 * @see org.olat.core.gui.components.table.TableDataModel#getValueAt(int, int)
	 */
	public Object getValueAt(int row, int col) {
		Project project = (Project) objects.get(row);
		if (col == 0) {
			log.debug("project=" + project); // debug-output only once for each project
			String name = project.getTitle();
			return name;
		} else if (col == 1) {
			// get identity_date list sorted by AddedDate
		  List<Object[]> identities = BaseSecurityManager.getInstance().getIdentitiesAndDateOfSecurityGroup(project.getProjectLeaderGroup(), true);
			if (identities.isEmpty()) {
				return "-";
			} else {
				// return all proj-leaders
				ArrayList<Identity> allIdents = new ArrayList<Identity>();
				for (Object[] idobj : identities) {
					allIdents.add((Identity)idobj[0]);
				}
				return allIdents;
			}
		} else if (col == (numberOfCustomFieldInTable + numberOfEventInTable + 2)) {
			return ProjectBrokerManagerFactory.getProjectBrokerManager().getStateFor(project,identity,moduleConfig);
		} else if (col == (numberOfCustomFieldInTable + numberOfEventInTable + 3)) {
			StringBuilder buf = new StringBuilder();
			buf.append(project.getSelectedPlaces());
			if (project.getMaxMembers() != Project.MAX_MEMBERS_UNLIMITED) {
				buf.append(" ");
				buf.append(translator.translate("projectlist.numbers.delimiter"));
				buf.append(" ");
				buf.append(project.getMaxMembers());
			}
			return buf.toString();
		}	else if (col == (numberOfCustomFieldInTable + numberOfEventInTable + 4)) { // enroll
			return ProjectBrokerManagerFactory.getProjectBrokerManager().canBeProjectSelectedBy(identity, project, moduleConfig, nbrSelectedProjects, isParticipantInAnyProject);
		} else if (col == (numberOfCustomFieldInTable + numberOfEventInTable + 5)) { // cancel enrollment
			return ProjectBrokerManagerFactory.getProjectBrokerManager().canBeCancelEnrollmentBy(identity,project,moduleConfig);
		} else if ( (col == 2) && (numberOfCustomFieldInTable > 0) ) {
			return project.getCustomFieldValue(enabledCustomFieldNumbers[0]);
		} else if ( (col == 3) && (numberOfCustomFieldInTable > 1) ) {
			return project.getCustomFieldValue(enabledCustomFieldNumbers[1]);
		} else if ( (col == 4) && (numberOfCustomFieldInTable > 2) ) {
			return project.getCustomFieldValue(enabledCustomFieldNumbers[2]);
		} else if ( (col == 5) && (numberOfCustomFieldInTable > 3) ) {
			return project.getCustomFieldValue(enabledCustomFieldNumbers[3]);
		} else if ( (col == 6) && (numberOfCustomFieldInTable > 4) ) {
			return project.getCustomFieldValue(enabledCustomFieldNumbers[4]);
		} else if ( col == (2 + numberOfCustomFieldInTable) ) {
			return project.getProjectEvent(enabledEventList.get(0));
		} else if ( col == (3 + numberOfCustomFieldInTable) ) {
			return project.getProjectEvent(enabledEventList.get(1));
		} else if ( col == (4 + numberOfCustomFieldInTable) ) {
			return project.getProjectEvent(enabledEventList.get(2));
		} else {
			return "ERROR";
		}
	}

	private List<Project.EventType> getEnabledEvents(ProjectBrokerModuleConfiguration moduleConfig) {
		List<Project.EventType> enabledEventList = new ArrayList<Project.EventType>();
		for (Project.EventType eventType : Project.EventType.values()) {
			if (moduleConfig.isProjectEventEnabled(eventType) && moduleConfig.isProjectEventTableViewEnabled(eventType)) {
				enabledEventList.add(eventType);
			}
		}
		return enabledEventList;
	}

	/**
	 * @param owned
	 */
	public void setEntries(List owned) {
		this.objects = owned;
	}

	/**
	 * @param row
	 * @return the project at the given row
	 */
	public Project getProjectAt(int row) {
		return (Project) objects.get(row);
	}

	public Object createCopyWithEmptyList() {
		ProjectListTableModel copy = new ProjectListTableModel(new ArrayList(), identity, translator, moduleConfig, numberOfCustomFieldInTable, numberOfEventInTable, nbrSelectedProjects, isParticipantInAnyProject);
		return copy;
	}

}