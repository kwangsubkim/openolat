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

package org.olat.commons.servlets;

import java.io.IOException;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.ValidationException;

import org.apache.poi.util.IOUtils;
import org.olat.commons.calendar.CalendarManager;
import org.olat.commons.calendar.CalendarManagerFactory;
import org.olat.commons.calendar.ICalTokenGenerator;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.dispatcher.DispatcherModule;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.i18n.I18nManager;


/**
 * Description:<BR>
 * Servlet that serves the ical document.
 * <P>
 * Initial Date:  June 1, 2008
 *
 * @author Udit Sajjanhar
 */
public class ICalServlet extends HttpServlet {

	private static final long serialVersionUID = -155266285395912535L;
	private static final OLog log = Tracing.createLoggerFor(ICalServlet.class);
	private static int outputBufferSize = 2048;
	private static int inputBufferSize = 2048;

	/**
	 * Default constructor.
	 */
	public ICalServlet() {
	}

	/**
	 * @see javax.servlet.Servlet#init(javax.servlet.ServletConfig)
	 */
	public void init(ServletConfig servletConfig) throws ServletException {
		super.init(servletConfig);
		log.info("init statics servlet");
		try {
			String bufSize = servletConfig.getInitParameter("input");
			inputBufferSize = Integer.parseInt(bufSize);
			bufSize = servletConfig.getInitParameter("output");
			inputBufferSize = Integer.parseInt(bufSize);
		} catch (Exception e) {
			log.warn("problem with config parameters for ical servlets:", e);
		}
		log.info("input buffer size: " + inputBufferSize);
		log.info("output buffer size: " + inputBufferSize);
	}

	/**
	 * @see javax.servlet.http.HttpServlet#service(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
		Tracing.setUreq(req);
		String method = req.getMethod();
		try {
		if (method.equals("GET")) {
			doGet(req, resp);
		} else {
			super.service(req, resp);
		}
		}finally {
			//consume the userrequest.
			Tracing.setUreq(null);
			I18nManager.remove18nInfoFromThread();
		}
	}

	/**
	 * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) {
		Calendar icalDoc = null;
		
		ServletOutputStream ostream = null;
		final boolean debug = log.isDebug();
		try {
			String pathInfo = request.getPathInfo();
			if(debug) log.debug("doGet pathInfo=" + pathInfo);
			if ((pathInfo == null) || (pathInfo.equals(""))) { 
				return; // error
			}
	
			if (checkPath(pathInfo)) {
				icalDoc = getIcalDocument(pathInfo);
				if (icalDoc == null) {
					DispatcherModule.sendNotFound(pathInfo, response);
					return;
				}
			} else {
				DispatcherModule.sendNotFound(pathInfo, response);
				return;
			}
			
			// OLAT-5243 related: sending back the reply can take arbitrary long,
			// considering slow end-user connections for example - or a sudden death of the connection
			// on the client-side which remains unnoticed (network partitioning)
			DBFactory.getInstance().intermediateCommit();
			
      // get the output stream 
			response.setBufferSize(outputBufferSize);
			ostream = response.getOutputStream();

      // output the calendar to the stream
      CalendarOutputter calOut = new CalendarOutputter(false);
			calOut.output(icalDoc, ostream);
		} catch (ValidationException e) {
			// throw olat exception for nice logging
			log.warn("Validation Error when generate iCal stream for path::" + request.getPathInfo(), e);
			DispatcherModule.sendNotFound("none", response);
		} catch (IOException e) {
			// throw olat exception for nice logging
			log.warn("IOException Error when generate iCal stream for path::" + request.getPathInfo(), e);
			DispatcherModule.sendNotFound("none", response);
		} catch (Exception e) {
			log.warn("Unknown Error in icalservlet", e);
			DispatcherModule.sendNotFound("none", response);
		} finally {
			IOUtils.closeQuietly(ostream);
			DBFactory.getInstance(false).commitAndCloseSession();
		}
	}

  /**
   * Checks the path information to match the prefixs in ICalTokenGenerator.ICAL_PREFIX_COLLECTION
   * @param icalFeedPath
   * @return boolean
   */
  private boolean checkPath(String icalFeedPath) {
	  // pathInfo is like /user/<user_name>/AUTH_TOKEN.ics
    //                  /group/<user_name>/AUTH_TOKEN/<group_id>.ics
    //                  /course/<user_name>/AUTH_TOKEN/<course_unique_id>.ics

  	// check the type of calendar
  	boolean calendarTypeMatched = false;
    for (int prefixIndex  = 0; prefixIndex < ICalTokenGenerator.ICAL_PREFIX_COLLECTION.length; prefixIndex++) {
      if (icalFeedPath.indexOf(ICalTokenGenerator.ICAL_PREFIX_COLLECTION[prefixIndex]) == 0) {
        calendarTypeMatched = true;
      }
    }
    if (!calendarTypeMatched) {
    	return false;
    }
    
    // check the number of tokens in the icalFeedPath
    int numberOfTokens = icalFeedPath.split("/").length - ICalTokenGenerator.ICAL_PATH_SHIFT;
    if (isRequestForPersonalCalendarFeed(icalFeedPath)) {
    	return (numberOfTokens == ICalTokenGenerator.ICAL_PERSONAL_PATH_TOKEN_LENGTH);
    } else { 
    	return (numberOfTokens == ICalTokenGenerator.ICAL_PATH_TOKEN_LENGTH);
    }
  }

  /** 
   * checks whether the iCal feed request is for a personal calendar
   * @param  icalFeedPath
   * @return boolean
   */
  private boolean isRequestForPersonalCalendarFeed(String icalFeedPath) {
  	if (icalFeedPath.indexOf(ICalTokenGenerator.ICAL_PREFIX_PERSONAL) != 0) {
      return false;
    } else {
    	return true;
    }
  }
  
  /**
   * Reads in the appropriate ics file, depending upon the pathInfo 
   * @param pathInfo
   * @return Calendar
   */
  private Calendar getIcalDocument(String pathInfo) {
	  // pathInfo is like /user/<user_name>/AUTH_TOKEN.ics
    //                  /group/<user_name>/AUTH_TOKEN/<group_id>.ics
    //                  /course/<user_name>/AUTH_TOKEN/<course_unique_id>.ics

    // get the individual path tokens
  	pathInfo = pathInfo.replaceAll(".ics", "");
    String[] pathInfoTokens = pathInfo.split("/");
    String calendarType = pathInfoTokens[0 + ICalTokenGenerator.ICAL_PATH_SHIFT];
    String userName = pathInfoTokens[1 + ICalTokenGenerator.ICAL_PATH_SHIFT];
    String authToken = pathInfoTokens[2 + ICalTokenGenerator.ICAL_PATH_SHIFT];
    String calendarID = userName;
    if (!isRequestForPersonalCalendarFeed(pathInfo)) {
    	calendarID = pathInfoTokens[3 + ICalTokenGenerator.ICAL_PATH_SHIFT];
    }
    
    // check the authentication token
    if (!checkPathAuthenticity(calendarType, userName, authToken, calendarID)) {
  		log.warn("Authenticity Check failed for the ical feed path: " + pathInfo);
      return null;
    }

    CalendarManager calendarManager = CalendarManagerFactory.getInstance().getCalendarManager();
    
    // check if the calendar exists (calendars are only persisted when an event is created)
    if (calendarManager.calendarExists(calendarType, calendarID)) {
    	// read and return the calendar file
    	return calendarManager.readCalendar(calendarType, calendarID);
    } else {
    	// return an empty calendar file
    	return new Calendar();
    }
    	
    
  }

  /**
   * checks the AUTH_TOKEN in the iCal feed path
   * @param type Type of calendar, i.e. User, Group or Course
   * @param userName Name of the User
   * @param authToken Authentication token for the calendar
   * @param icsFileName Name of the ics file
   * @return boolean
   */
  private boolean checkPathAuthenticity(String calendarType, String userName, String authToken, String calendarID) {

  	// get the authentication token stored in the database
  	String authTokenFromDb = ICalTokenGenerator.getIcalAuthToken(calendarType, calendarID, userName, false);
  	
  	// check if the token from db matches the token in the url
  	if (authTokenFromDb == null) {
  		return false;
  	} else {
  		return authTokenFromDb.equals(authToken);
  	}
  }
  
}
