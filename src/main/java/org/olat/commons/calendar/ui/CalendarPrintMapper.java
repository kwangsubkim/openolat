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
package org.olat.commons.calendar.ui;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.olat.commons.calendar.CalendarUtils;
import org.olat.commons.calendar.model.KalendarEvent;
import org.olat.commons.calendar.ui.components.KalendarEventDateComparator;
import org.olat.commons.calendar.ui.components.KalendarEventRenderWrapper;
import org.olat.commons.calendar.ui.components.KalendarRenderWrapper;
import org.olat.core.dispatcher.mapper.Mapper;
import org.olat.core.gui.media.MediaResource;
import org.olat.core.gui.media.StringMediaResource;
import org.olat.core.gui.translator.Translator;
import org.olat.core.util.Formatter;
import org.olat.core.util.StringHelper;

/**
 * 
 * Initial date: 17.05.2013<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class CalendarPrintMapper implements Mapper {

	private final String themeBaseUri;
	private final Translator translator;
	
	private Date from, to;
	private List<KalendarRenderWrapper> calendarWrappers;
	private List<KalendarRenderWrapper> importedCalendarWrappers;
	
	public CalendarPrintMapper(String themeBaseUri, Translator translator) {
		this.themeBaseUri = themeBaseUri;
		this.translator = translator;
	}

	public Date getFrom() {
		return from;
	}

	public void setFrom(Date from) {
		this.from = from;
	}

	public Date getTo() {
		return to;
	}

	public void setTo(Date to) {
		this.to = to;
	}

	public List<KalendarRenderWrapper> getCalendarWrappers() {
		return calendarWrappers;
	}

	public void setCalendarWrappers(List<KalendarRenderWrapper> calendarWrappers) {
		this.calendarWrappers = calendarWrappers;
	}

	public List<KalendarRenderWrapper> getImportedCalendarWrappers() {
		return importedCalendarWrappers;
	}

	public void setImportedCalendarWrappers(List<KalendarRenderWrapper> importedCalendarWrappers) {
		this.importedCalendarWrappers = importedCalendarWrappers;
	}

	@Override
	public MediaResource handle(String relPath, HttpServletRequest request) {
		StringBuilder sb = new StringBuilder();
		sb.append("<html><head><title>")
		  .append("Calendar")
		  .append("</title>")
		  .append("<meta http-equiv='Content-type' content='text/html; charset=utf-8' />")
		  .append("<meta http-equiv='cache-control' content='no-cache' />")
		  .append("<meta http-equiv='pragma' content='no-cache' />")
		  .append("<meta http-equiv='expires' content='0' />")
		  .append("<meta http-equiv='pragma-directive' content='no-cache' />")
		  .append("<meta http-equiv='cache-directive' content='no-cache' />")
		  .append("<link href='").append(themeBaseUri).append("all/content.css' rel='stylesheet' type='text/css' />\n")
		  .append("<link href='").append(themeBaseUri).append("layout.css' rel='stylesheet' type='text/css' />\n")
		  .append("</head><body class='o_cal_print' onload='window.focus();window.print()'>");
		
		//collect all events
		List<KalendarEventRenderWrapper> sortedEventsWithin = new ArrayList<KalendarEventRenderWrapper>();
		collectEvents(sortedEventsWithin, calendarWrappers);
		collectEvents(sortedEventsWithin, importedCalendarWrappers);
		Collections.sort(sortedEventsWithin, KalendarEventDateComparator.getInstance());

		//list of events
		renderEvents(sb, sortedEventsWithin, from, to);
		//list of calendars
		renderCalendars(sb) ;
		sb.append("</body></html>");
		
		StringMediaResource smr = new StringMediaResource();
		smr.setContentType("text/html");
		smr.setEncoding("UTF-8");
		smr.setData(sb.toString());
		return smr;
	}
	
	private void collectEvents(List<KalendarEventRenderWrapper> eventList, List<KalendarRenderWrapper> wrappers) {
		for (KalendarRenderWrapper calendarWrapper:wrappers) {
			if (calendarWrapper.getKalendarConfig().isVis()) {
				List<KalendarEvent> events = CalendarUtils.listEventsForPeriod(calendarWrapper.getKalendar(), from, to);
				for (KalendarEvent event : events) {
					//private filter???
					eventList.add(new KalendarEventRenderWrapper(event, calendarWrapper));
				}
			}
		}
	}
	
	/*
<div class="o_cal_wv_print">
	<ul class="o_cal_wv_list">
		<li>
			<span class="o_cal_date">Monday, May 13, 2013</span>
			<ul class="o_cal_events">
				<li class="o_cal_event">
					<div class="o_cal_time"><span>4:00 AM - 9:00 AM</span></div>
					<div class="o_cal_subject o_cal_blue"><p>Test</p></div>
				</li>
			</ul>
		</li>
		<li>
			<span class="o_cal_date">Tuesday, May 14, 2013</span>
			<ul class="o_cal_events">
				<li class="o_cal_event">
					<div class="o_cal_time"><span>2:00 PM - 3:00 PM</span></div>
					<div class="o_cal_subject o_cal_blue"><p>Test 3</p></div>
				</li>
			</ul>
		</li>
	</ul>
	<div class="b_clearfix">&nbsp;</div>
</div>
	*/
	
	private void renderEvents(StringBuilder sb, List<KalendarEventRenderWrapper> eventList, Date from , Date to) {
		sb.append("<div class='o_cal_wv_print'><fieldset>")
		  .append("<legend>").append(StringHelper.formatLocaleDateFull(from, translator.getLocale()))
		  .append(" - ").append(StringHelper.formatLocaleDateFull(to, translator.getLocale()))
		  .append("</legend>")
		  .append("<ul class='o_cal_wv_list'>");
		
		Collections.sort(eventList, new KalendarEventDateComparator());
		
		Date currentDate = null;
		List<KalendarEventRenderWrapper> eventByDayList = new ArrayList<KalendarEventRenderWrapper>();
		for(KalendarEventRenderWrapper event:eventList) {
			Date begin = event.getEvent().getBegin();
			Date normalizedBegin = CalendarUtils.removeTime(begin);
			//same day?
			if(currentDate == null || currentDate.before(normalizedBegin)) {
				renderDay(sb, currentDate, eventByDayList);
				eventByDayList.clear();
				currentDate = normalizedBegin;
			}
			eventByDayList.add(event);
		}
		renderDay(sb, currentDate, eventByDayList);

		sb.append("</ul></fieldset><div class='b_clearfix'>&nbsp;</div></div>");
	}
	
	private void renderDay(StringBuilder sb, Date date, List<KalendarEventRenderWrapper> eventList) {
		if(eventList.isEmpty()) return;
		
		Date dayStart = date;
		Calendar cal = Calendar.getInstance();
		cal.setTime(dayStart);
		cal.add(Calendar.DAY_OF_YEAR, 1);
		Date dayEnd = cal.getTime();

		sb.append("<li><span class='o_cal_date'>")
		  .append(StringHelper.formatLocaleDateFull(dayStart, translator.getLocale()))
		  .append("</span><ul class='o_cal_events'>");
		for(KalendarEventRenderWrapper event:eventList) {
			renderEvent(sb, event, dayStart, dayEnd);
		}
		sb.append("</ul></li>");
	}

	private void renderEvent(StringBuilder sb, KalendarEventRenderWrapper eventWrapper, Date dayStart, Date dayEnd) {
		KalendarEvent event = eventWrapper.getEvent();
		boolean hidden = eventWrapper.getCalendarAccess() == KalendarRenderWrapper.ACCESS_READ_ONLY
				&& !eventWrapper.getKalendarRenderWrapper().isImported() && event.getClassification() != KalendarEvent.CLASS_PUBLIC;
		String escapedSubject = Formatter.escWithBR(event.getSubject()).toString();
		escapedSubject = escapedSubject.replace('\r', ' ');
		sb.append("<li class=\"o_cal_event\">\n");
		// time
		sb.append("<div class=\"o_cal_time\"><span>\n");
		if (event.isAllDayEvent()) {
			sb.append(translator.translate("cal.form.allday"));
		} else {
			// set start and end times for events spanning more than one day
			Date begin = event.getBegin();
			Date end = event.getEnd();
			if (begin.before(dayStart)) {
				begin = dayStart;
			}
			if (end.after(dayEnd)) {
				end = dayEnd;
			}
			sb.append(StringHelper.formatLocaleTime(begin, translator.getLocale()));
			sb.append(" - ");
			sb.append(StringHelper.formatLocaleTime(end, translator.getLocale()));
		}
		sb.append("</span></div>\n");
		// event name (subject)
		//fxdiff BAKS-13: firefox doesn't break lines with only <br />, we need <p>
		sb.append("<div class=\"o_cal_subject " + eventWrapper.getCssClass() + "\"><p>");
		if (hidden) {
			sb.append("-");
		} else {
			sb.append(escapedSubject.replace("<br />", "</p><p>"));
		}
		sb.append("</p></div>\n");
		// location
		if (StringHelper.containsNonWhitespace(event.getLocation())) {
			sb.append("<div class=\"o_cal_location\"><span>\n");
			sb.append(translator.translate("cal.form.location") + ": ");
			if (!hidden) {
				sb.append(StringHelper.escapeHtml(event.getLocation()));
			}
			sb.append("</span></div>\n");
		}
		sb.append("</li>\n");
	}

	/*
<div id="o_cal_config">
	<fieldset>
		<legend>List of calendars</legend>
		<div class="o_cal_config_row">
			<div class="o_cal_config_calendar o_cal_blue">
				kanu
			</div>
		</div>
	</fieldset>
</div>
	 */
	private void renderCalendars(StringBuilder sb) {
		sb.append("<div id='o_cal_config'>")
		  .append("<fieldset><legend>").append(translator.translate("cal.list")).append("</legend>");
		renderCalendar(sb, calendarWrappers);
		sb.append("</fieldset>");
		//list of imported calendars
		sb.append("<fieldset><legend>").append(translator.translate("cal.import.list")).append("</legend>");
		renderCalendar(sb, importedCalendarWrappers);
		sb.append("</fieldset>")
		  .append("</div>");
	}
	
	private void renderCalendar(StringBuilder sb, List<KalendarRenderWrapper> calendarWrappers) {
		for(KalendarRenderWrapper calendarWrapper:calendarWrappers) {
			String cssClass = calendarWrapper.getKalendarConfig().getCss();
			sb.append("<div class='o_cal_config_row'><div class='o_cal_config_calendar ").append(cssClass).append("'>")
			  .append(StringHelper.escapeHtml(calendarWrapper.getKalendarConfig().getDisplayName()))
			  .append("</div></div>");
		}
	}
}