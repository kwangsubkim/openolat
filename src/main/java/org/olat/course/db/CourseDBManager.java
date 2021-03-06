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
package org.olat.course.db;

import java.util.List;

import org.olat.core.CoreSpringFactory;
import org.olat.core.configuration.AbstractOLATModule;
import org.olat.core.configuration.ConfigOnOff;
import org.olat.core.id.Identity;
import org.olat.course.ICourse;

/**
 * 
 * Description:<br>
 * TODO: srosse Class Description for CourseDBManager
 * 
 * <P>
 * Initial Date:  7 apr. 2010 <br>
 * @author srosse, stephane.rosse@frentix.com
 */
public abstract class CourseDBManager extends AbstractOLATModule implements ConfigOnOff {

	public static CourseDBManager getInstance() {
		return (CourseDBManager)CoreSpringFactory.getBean("courseDBManager");
	}
	
	public abstract boolean isEnabled();
	
	public abstract Long getCourseId(Long key);
	
	public abstract List<String> getUsedCategories(ICourse course);
	
	public abstract void reset(ICourse course, String category);
	
	public abstract CourseDBEntry getValue(ICourse course, Identity identity, String category, String name);
	
	public abstract CourseDBEntry getValue(Long courseResourceId, Identity identity, String category, String name);
	
	public abstract boolean deleteValue(ICourse course, Identity identity, String category, String name);
	
	public abstract CourseDBEntry setValue(ICourse course, Identity identity, String category, String name, Object value);
	
	public abstract List<CourseDBEntry> getValues(ICourse course, Identity identity, String category, String name);
}
