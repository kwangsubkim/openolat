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
* <p>
*/
package org.olat.course.assessment;

import java.util.List;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.dtabs.Activateable;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.util.notifications.ContextualSubscriptionController;
import org.olat.core.util.notifications.PublisherData;
import org.olat.core.util.notifications.SubscriptionContext;
import org.olat.course.ICourse;
import org.olat.ims.qti.export.QTIArchiveWizardController;

/**
 * Description:<br>
 * TODO: patrickb Class Description for AssessmentControllerCreatorOlat
 * 
 * <P>
 * Initial Date:  29.06.2010 <br>
 * @author patrickb
 */
public class AssessmentControllerCreatorOlat implements AssessmentControllerCreator {

	
	
	@Override
	public Activateable createAssessmentMainController(UserRequest ureq, WindowControl wControl, OLATResourceable ores, IAssessmentCallback assessmentCallback) {
		return new AssessmentMainController(ureq, wControl, ores, assessmentCallback);
	}
	
	/**
	 * @see org.olat.course.assessment.AssessmentControllerCreator#createQTIArchiveWizardController(boolean, org.olat.core.gui.UserRequest, java.util.List, org.olat.course.ICourse, org.olat.core.gui.control.WindowControl)
	 */
	@Override
	public Controller createQTIArchiveWizardController(boolean dummyMode, UserRequest ureq, List nodesTableObjectArrayList, ICourse course,
			WindowControl wControl) {
		return new QTIArchiveWizardController(dummyMode, ureq, nodesTableObjectArrayList, course, wControl);
	}

	
	public Controller createContextualSubscriptionController(UserRequest ureq, WindowControl wControl, ICourse course){

		AssessmentNotificationsHandler anh = AssessmentNotificationsHandler.getInstance();
		SubscriptionContext subsContext = anh.getAssessmentSubscriptionContext(ureq.getIdentity(), course);
		if (subsContext != null) {
			PublisherData pData = anh.getAssessmentPublisherData(course, wControl.getBusinessControl().getAsString());
			return new ContextualSubscriptionController(ureq, wControl, subsContext, pData);
		}
		
		return null;
		
	}
	
}