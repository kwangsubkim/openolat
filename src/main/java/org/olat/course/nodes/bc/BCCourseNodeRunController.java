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

package org.olat.course.nodes.bc;

import java.util.List;

import org.olat.core.commons.modules.bc.FolderRunController;
import org.olat.core.commons.modules.bc.vfs.OlatNamedContainerImpl;
import org.olat.core.commons.services.notifications.SubscriptionContext;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.control.DefaultController;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.dtabs.Activateable2;
import org.olat.core.id.Identity;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.id.context.StateEntry;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.callbacks.VFSSecurityCallback;
import org.olat.course.CourseFactory;
import org.olat.course.CourseModule;
import org.olat.course.ICourse;
import org.olat.course.groupsandrights.CourseRights;
import org.olat.course.nodes.BCCourseNode;
import org.olat.course.run.environment.CourseEnvironment;
import org.olat.course.run.userview.NodeEvaluation;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.util.logging.activity.LoggingResourceable;

/**
 * Initial Date: Apr 22, 2004
 * 
 * @author gnaegi
 */
public class BCCourseNodeRunController extends DefaultController implements Activateable2 {

	private FolderRunController frc;

	/**
	 * Constructor for a briefcase course building block runtime controller
	 * 
	 * @param ureq
	 * @param userCourseEnv
	 * @param wContr
	 * @param bcCourseNode
	 * @param scallback
	 */
	public BCCourseNodeRunController(NodeEvaluation ne, CourseEnvironment courseEnv, UserRequest ureq, WindowControl wContr) {
		super(wContr);

		boolean isOlatAdmin = ureq.getUserSession().getRoles().isOLATAdmin();
		boolean isGuestOnly = ureq.getUserSession().getRoles().isGuestOnly();
		// set logger on this run controller
		addLoggingResourceable(LoggingResourceable.wrap(ne.getCourseNode()));

		// offer subscription, but not to guests
		SubscriptionContext nodefolderSubContext = (isGuestOnly ? null : CourseModule.createSubscriptionContext(courseEnv, ne.getCourseNode()));

		OlatNamedContainerImpl namedContainer = BCCourseNode.getNodeFolderContainer((BCCourseNode) ne.getCourseNode(), courseEnv);
		VFSSecurityCallback scallback = new FolderNodeCallback(namedContainer.getRelPath(), ne, isOlatAdmin, isGuestOnly, nodefolderSubContext);
		namedContainer.setLocalSecurityCallback(scallback);
	
		// fxdiff VCRP-12: copy files from course folder
		// Allow copying of files from course folder if allowed to write and user
		// has course editor rights (course owner and users in a right group with
		// the author right)
		VFSContainer courseContainer = null;
		if(scallback.canWrite() && scallback.canCopy()) {
			Identity identity = ureq.getIdentity();
			ICourse course = CourseFactory.loadCourse(courseEnv.getCourseResourceableId());
			RepositoryManager rm = RepositoryManager.getInstance();
			RepositoryEntry entry = rm.lookupRepositoryEntry(course, true);
			if (isOlatAdmin || rm.isOwnerOfRepositoryEntry(identity, entry)
					|| courseEnv.getCourseGroupManager().hasRight(identity, CourseRights.RIGHT_COURSEEDITOR)) {
				// use course folder as copy source
				courseContainer = courseEnv.getCourseFolderContainer();
			}
		}
		frc = new FolderRunController(namedContainer, false, true, true, ureq, getWindowControl(), null, null, courseContainer);
		setInitialComponent(frc.getInitialComponent());
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	public void event(UserRequest ureq, Component source, Event event) {
	// no events to catch
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	protected void doDispose() {
		if (frc != null) {
			frc.dispose();
			frc = null;
		}
	}
	
	public void activatePath(UserRequest ureq, String path) {
		if (frc != null) {
			frc.activatePath(ureq, path);
		}
	}

	@Override
	public void activate(UserRequest ureq, List<ContextEntry> entries, StateEntry state) {
		if (frc != null) {
			frc.activate(ureq, entries, state);
		}
	}
}
