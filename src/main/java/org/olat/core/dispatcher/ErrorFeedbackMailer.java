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
* <p>
*/

package org.olat.core.dispatcher;

import java.util.Collection;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.olat.basesecurity.BaseSecurity;
import org.olat.core.CoreSpringFactory;
import org.olat.core.id.Identity;
import org.olat.core.logging.LogFileParser;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.WebappHelper;
import org.olat.core.util.mail.MailBundle;
import org.olat.core.util.mail.MailManager;

/**
 * Description:<br>
 * Send an Email to the support address
 * <P>
 * Initial Date: Jan 31, 2006 <br>
 * 
 * @author guido
 */
public class ErrorFeedbackMailer implements Dispatcher {
	
	private static final OLog log = Tracing.createLoggerFor(ErrorFeedbackMailer.class);

	private static final ErrorFeedbackMailer INSTANCE = new ErrorFeedbackMailer();

	private ErrorFeedbackMailer() {
		// private since singleton
	}

	protected static ErrorFeedbackMailer getInstance() {
		return INSTANCE;
	}

	/**
	 * send email to olat support with user submitted error informaition
	 * 
	 * @param request
	 */
	public void sendMail(HttpServletRequest request) {
		String feedback = request.getParameter("textarea");
		// fxdiff: correctly get the error-number
		// was : String errorNr = feedback.substring(0, feedback.indexOf("\n") -
		// 1);
		String errorNr = request.getParameter("fx_errnum");
		String username = request.getParameter("username");
		try {
			BaseSecurity im = CoreSpringFactory.getImpl(BaseSecurity.class);
			Identity ident = im.findIdentityByName(username);
			// if null, user may crashed befor getting a valid session, try with
			// guest user instead
			if (ident == null)
				ident = im.findIdentityByName("guest");
			Collection<String> logFileEntries = LogFileParser.getErrorToday(errorNr, false);
			StringBuilder out = new StringBuilder();
			if (logFileEntries != null) {
				for (Iterator<String> iter = logFileEntries.iterator(); iter.hasNext();) {
					out.append(iter.next());
				}
			}

			String body = feedback + "\n------------------------------------------\n\n --- from user: " + username
					+ " ---" + out.toString();
			
			MailBundle bundle = new MailBundle();
			bundle.setFromId(ident);
			bundle.setTo(WebappHelper.getMailConfig("mailError"));
			bundle.setContent("Feedback from Error Nr.: " + errorNr, body);
			CoreSpringFactory.getImpl(MailManager.class).sendExternMessage(bundle, null);
		} catch (Exception e) {
			// error in recipient email address(es)
			handleException(request, e);
			return;
		}
	}


	private void handleException(HttpServletRequest request, Exception e) {
		String feedback = request.getParameter("textarea");
		String username = request.getParameter("username");
		log.error("Error sending error feedback mail to OpenOLAT error support (" + WebappHelper.getMailConfig("mailError") + ") from: "
				+ username + " with content: " + feedback, e);
	}

	/**
	 * @see org.olat.core.dispatcher.Dispatcher#execute(javax.servlet.http.HttpServletRequest,
	 *      javax.servlet.http.HttpServletResponse, java.lang.String)
	 */
	@Override
	public void execute(HttpServletRequest request, HttpServletResponse response) {
		sendMail(request);
		DispatcherModule.redirectToDefaultDispatcher(response);
	}

}
