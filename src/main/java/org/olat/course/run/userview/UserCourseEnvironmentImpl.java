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

package org.olat.course.run.userview;

import java.util.List;

import org.olat.core.commons.persistence.PersistenceHelper;
import org.olat.core.id.IdentityEnvironment;
import org.olat.course.condition.interpreter.ConditionInterpreter;
import org.olat.course.editor.CourseEditorEnv;
import org.olat.course.groupsandrights.CourseGroupManager;
import org.olat.course.run.environment.CourseEnvironment;
import org.olat.course.run.scoring.ScoreAccounting;
import org.olat.group.BusinessGroup;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.repository.model.RepositoryEntryLifecycle;
import org.olat.resource.OLATResource;

/**
 * Initial Date:  Feb 6, 2004
 * @author Felix Jost
 *
 */
public class UserCourseEnvironmentImpl implements UserCourseEnvironment {
	private IdentityEnvironment identityEnvironment;
	private CourseEnvironment courseEnvironment;
	private ConditionInterpreter conditionInterpreter;
	private ScoreAccounting scoreAccounting;
	private RepositoryEntryLifecycle lifecycle;
	private RepositoryEntry courseRepoEntry;
	private List<BusinessGroup> coachedGroups;
	private List<BusinessGroup> participatingGroups;
	private List<BusinessGroup> waitingLists;
	
	private Boolean coach;
	private Boolean admin;
	private Boolean participant;
	
	public UserCourseEnvironmentImpl(IdentityEnvironment identityEnvironment, CourseEnvironment courseEnvironment) {
		this(identityEnvironment, courseEnvironment, null, null, null, null, null, null);
	}
	
	public UserCourseEnvironmentImpl(IdentityEnvironment identityEnvironment, CourseEnvironment courseEnvironment,
			List<BusinessGroup> coachedGroups, List<BusinessGroup> participatingGroups, List<BusinessGroup> waitingLists,
			Boolean coach, Boolean admin, Boolean participant) {
		this.courseEnvironment = courseEnvironment;
		this.identityEnvironment = identityEnvironment;
		this.scoreAccounting = new ScoreAccounting(this);
		this.conditionInterpreter = new ConditionInterpreter(this);
		this.coachedGroups = coachedGroups;
		this.participatingGroups = participatingGroups;
		this.waitingLists = waitingLists;
		this.coach = coach;
		this.admin = admin;
		this.participant = participant;
	}

	/**
	 * @return Returns the courseEnvironment.
	 */
	@Override
	public CourseEnvironment getCourseEnvironment() {
		return courseEnvironment;
	}

	@Override
	public IdentityEnvironment getIdentityEnvironment() {
		return identityEnvironment;
	}

	@Override
	public ConditionInterpreter getConditionInterpreter() {
		return conditionInterpreter;
	}

	@Override
	public ScoreAccounting getScoreAccounting() {
		return scoreAccounting;
	}

	@Override
	public CourseEditorEnv getCourseEditorEnv() {
		// return null signalling this is real user environment
		return null;
	}
	
	@Override
	public boolean isIdentityInCourseGroup(Long groupKey) {
		if(coachedGroups != null && participatingGroups != null) {
			return PersistenceHelper.listContainsObjectByKey(participatingGroups, groupKey)
					|| PersistenceHelper.listContainsObjectByKey(coachedGroups, groupKey);
		}
		CourseGroupManager cgm = courseEnvironment.getCourseGroupManager();
		return cgm.isIdentityInGroup(identityEnvironment.getIdentity(), groupKey);
	}

	@Override
	public boolean isCoach() {
		if(coach != null) {
			return coach.booleanValue();
		}
		//lazy loading
		CourseGroupManager cgm = courseEnvironment.getCourseGroupManager();
		boolean coachLazy = cgm.isIdentityCourseCoach(identityEnvironment.getIdentity());
		coach = new Boolean(coachLazy);
		return coachLazy;
	}

	@Override
	public boolean isAdmin() {
		if(admin != null) {
			return admin.booleanValue();
		}
		//lazy loading
		CourseGroupManager cgm = courseEnvironment.getCourseGroupManager();
		boolean admiLazy = cgm.isIdentityCourseAdministrator(identityEnvironment.getIdentity());
		admin = new Boolean(admiLazy);
		return admiLazy;
	}

	@Override
	public boolean isParticipant() {
		if(participant != null) {
			return participant.booleanValue();
		}
		//lazy loading
		CourseGroupManager cgm = courseEnvironment.getCourseGroupManager();
		boolean partLazy = cgm.isIdentityCourseParticipant(identityEnvironment.getIdentity());
		participant = new Boolean(partLazy);
		return partLazy;
	}

	@Override
	public RepositoryEntryLifecycle getLifecycle() {
		if(lifecycle == null) {
			RepositoryEntry re = getCourseRepositoryEntry();
			if(re != null) {
				lifecycle = re.getLifecycle();
			}
		}
		return lifecycle;
	}

	public RepositoryEntry getCourseRepositoryEntry() {
		if(courseRepoEntry == null) {
			CourseGroupManager cgm = courseEnvironment.getCourseGroupManager();
			OLATResource courseResource = cgm.getCourseResource();
			courseRepoEntry = RepositoryManager.getInstance().lookupRepositoryEntry(courseResource, false);
		}
		return courseRepoEntry;
	}

	public List<BusinessGroup> getCoachedGroups() {
		return coachedGroups;
	}

	public List<BusinessGroup> getParticipatingGroups() {
		return participatingGroups;
	}

	public List<BusinessGroup> getWaitingLists() {
		return waitingLists;
	}
	
	public void setGroupMemberships(RepositoryEntry repoEntry, List<BusinessGroup> coachedGroups,
			List<BusinessGroup> participatingGroups, List<BusinessGroup> waitingLists) {
		this.coachedGroups = coachedGroups;
		this.participatingGroups = participatingGroups;
		this.waitingLists = waitingLists;
	}
	
	public void setUserRoles(boolean admin, boolean coach) {
		this.admin = new Boolean(admin);
		this.coach = new Boolean(coach);
		this.participant = null;//reset it
	}
}
