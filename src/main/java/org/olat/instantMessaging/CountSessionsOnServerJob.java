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
package org.olat.instantMessaging;

import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.scheduler.JobWithDB;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

/**
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class CountSessionsOnServerJob extends JobWithDB {
	
	private OLog log = Tracing.createLoggerFor(CountSessionsOnServerJob.class);

	@Override
	public void executeWithDB(JobExecutionContext arg0) throws JobExecutionException {
		try {
			SmackInstantMessagingImpl instantMessaging = CoreSpringFactory.getImpl(SmackInstantMessagingImpl.class);
			int count = instantMessaging.getSessionCountService().countSessions();
			instantMessaging.setSessionCount(count);
		} catch (Exception e) {
			log.error("Cannot count the users online", e);
		}
	}
}
