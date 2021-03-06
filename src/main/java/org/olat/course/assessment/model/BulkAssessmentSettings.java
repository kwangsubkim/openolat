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
package org.olat.course.assessment.model;

import java.io.Serializable;

import org.olat.course.nodes.AssessableCourseNode;
import org.olat.course.nodes.ProjectBrokerCourseNode;
import org.olat.course.nodes.TACourseNode;

/**
 * 
 * Initial date: 21.11.2013<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class BulkAssessmentSettings implements Serializable {

	private static final long serialVersionUID = 2274923942324831139L;
	private final boolean hasUserComment;
	private final boolean hasScore;
	private final boolean hasPassed;
	private final boolean hasReturnFiles;
	private final Float min;
	private final Float max;
	private final Float cut;

	public BulkAssessmentSettings(AssessableCourseNode courseNode) {
		hasUserComment = courseNode.hasCommentConfigured();
		hasScore = courseNode.hasScoreConfigured();
		hasPassed = courseNode.hasPassedConfigured();
		
		if (courseNode instanceof TACourseNode) {
			Boolean hasReturnBox = (Boolean)courseNode.getModuleConfiguration().get(TACourseNode.CONF_RETURNBOX_ENABLED);
			hasReturnFiles = hasReturnBox.booleanValue();				
		} else if (courseNode instanceof ProjectBrokerCourseNode) {
			Boolean hasReturnBox = (Boolean)courseNode.getModuleConfiguration().get(ProjectBrokerCourseNode.CONF_RETURNBOX_ENABLED);
			hasReturnFiles = hasReturnBox.booleanValue();				
		} else {
			hasReturnFiles = false;			
		}

		if (hasScore) {
			min = courseNode.getMinScoreConfiguration();
			max = courseNode.getMaxScoreConfiguration();
		} else {
			min = max = null;
		}
		if (hasPassed) {
			cut = courseNode.getCutValueConfiguration();
		} else {
			cut = null;
		}
	}

	public boolean isHasUserComment() {
		return hasUserComment;
	}

	public boolean isHasScore() {
		return hasScore;
	}

	public boolean isHasPassed() {
		return hasPassed;
	}

	public boolean isHasReturnFiles() {
		return hasReturnFiles;
	}

	public Float getMin() {
		return min;
	}

	public Float getMax() {
		return max;
	}

	public Float getCut() {
		return cut;
	}
}
