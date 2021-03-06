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

package org.olat.collaboration;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;

import org.olat.admin.quota.QuotaConstants;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.basesecurity.Constants;
import org.olat.commons.calendar.CalendarManager;
import org.olat.commons.calendar.CalendarManagerFactory;
import org.olat.commons.calendar.ui.CalendarController;
import org.olat.commons.calendar.ui.WeeklyCalendarController;
import org.olat.commons.calendar.ui.components.KalendarRenderWrapper;
import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.modules.bc.FolderRunController;
import org.olat.core.commons.modules.bc.vfs.OlatNamedContainerImpl;
import org.olat.core.commons.modules.bc.vfs.OlatRootFolderImpl;
import org.olat.core.commons.services.notifications.SubscriptionContext;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.title.TitleInfo;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.context.BusinessControl;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.FileUtils;
import org.olat.core.util.Util;
import org.olat.core.util.ZipUtil;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.coordinate.SyncerCallback;
import org.olat.core.util.coordinate.SyncerExecutor;
import org.olat.core.util.mail.ContactMessage;
import org.olat.core.util.vfs.LocalFolderImpl;
import org.olat.core.util.vfs.Quota;
import org.olat.core.util.vfs.QuotaManager;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.core.util.vfs.callbacks.VFSSecurityCallback;
import org.olat.course.CourseFactory;
import org.olat.course.CourseModule;
import org.olat.course.ICourse;
import org.olat.course.run.calendar.CourseLinkProviderController;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupService;
import org.olat.instantMessaging.InstantMessagingModule;
import org.olat.instantMessaging.ui.ChatToolController;
import org.olat.modules.co.ContactFormController;
import org.olat.modules.fo.Forum;
import org.olat.modules.fo.ForumCallback;
import org.olat.modules.fo.ForumManager;
import org.olat.modules.fo.ForumUIFactory;
import org.olat.modules.fo.archiver.ForumArchiveManager;
import org.olat.modules.fo.archiver.formatters.ForumFormatter;
import org.olat.modules.fo.archiver.formatters.ForumRTFFormatter;
import org.olat.modules.openmeetings.OpenMeetingsModule;
import org.olat.modules.openmeetings.manager.OpenMeetingsException;
import org.olat.modules.openmeetings.manager.OpenMeetingsManager;
import org.olat.modules.openmeetings.model.OpenMeetingsRoom;
import org.olat.modules.openmeetings.model.RoomType;
import org.olat.modules.openmeetings.ui.OpenMeetingsRunController;
import org.olat.modules.wiki.WikiManager;
import org.olat.modules.wiki.WikiSecurityCallback;
import org.olat.modules.wiki.WikiSecurityCallbackImpl;
import org.olat.modules.wiki.WikiToZipUtils;
import org.olat.portfolio.EPSecurityCallback;
import org.olat.portfolio.EPSecurityCallbackImpl;
import org.olat.portfolio.EPUIFactory;
import org.olat.portfolio.manager.EPFrontendManager;
import org.olat.portfolio.model.structel.PortfolioStructureMap;
import org.olat.portfolio.ui.structel.EPCreateMapController;
import org.olat.properties.NarrowedPropertyManager;
import org.olat.properties.Property;
import org.olat.properties.PropertyManager;
import org.olat.resource.OLATResource;
import org.olat.testutils.codepoints.server.Codepoint;

/**
 * Description:<BR>
 * The singleton used for retrieving a collaboration tools suite associated with
 * the supplied OLATResourceable.
 * <P>
 * Description: <br>
 * The CollaborationTools represents a suite of collaborative tools addeable to
 * any OLATResourceable. To get an instance of this suite, one has to use the
 * collaboration tools factory.
 * <p>
 * This collaboration tools class exposes the possibility to retrieve the
 * appropriate controllers for the desired tools. And also provides the means to
 * manage the configuration of the provided tools. Moreover it is already
 * shipped with a controller which can be used to display an administrative view
 * for enabling/disabling such tools for the supplied OLATResourceable.
 * <p>
 * All the future collaborative tools will be found here.
 * 
 * @see org.olat.collaboration.CollaborationToolsFactory
 * @author Felix Jost
 * @author guido
 */
public class CollaborationTools implements Serializable {

	private static final long serialVersionUID = -155629068939748789L;
	boolean dirty = false;
	private final static String TRUE = "true";
	private final static String FALSE = "false";
	public final static String KEY_FORUM = "forumKey";
	public final static String KEY_PORTFOLIO = "portfolioMapKey";
	public final static String KEY_OPENMEETINGS = "openMeetingsKey";

	/**
	 * <code>PROP_CAT_BG_COLLABTOOLS</code> identifies properties concerning
	 * Collaboration Tools
	 */
	public final static String PROP_CAT_BG_COLLABTOOLS = "collabtools";
	/**
	 * constant used to identify the calendar for a BuddyGroup
	 */
	public final static String TOOL_CALENDAR = "hasCalendar";
	/**
	 * constant used to identify the forum for a BuddyGroup
	 */
	public final static String TOOL_FORUM = "hasForum";
	/**
	 * constant used to identify the folder for a BuddyGroup
	 */
	public final static String TOOL_FOLDER = "hasFolder";
	/**
	 * constant used to identify the chat for a BuddyGroup
	 */
	public final static String TOOL_CHAT = "hasChat";
	/**
	 * constant used to identify the contact form for a BuddyGroup
	 */
	public final static String TOOL_CONTACT = "hasContactForm";
	/**
	 * constant used to identify the contact form for a BuddyGroup
	 */
	public final static String TOOL_NEWS = "hasNews";
	/**
	 * constant used to identify the wiki for a BuddyGroup
	 */
	public final static String TOOL_WIKI = "hasWiki";
	
	/**
	 * constant used to identify the portfolio for a BuddyGroup
	 */
	public final static String TOOL_PORTFOLIO = "hasPortfolio";
	
	/**
	 * constant used to identify the open meetings for a group
	 */
	public final static String TOOL_OPENMEETINGS = "hasOpenMeetings";
	
	/**
	 * Only owners have write access to the calendar.
	 */
	public static final int CALENDAR_ACCESS_OWNERS = 0;
	/**
	 * Owners and members have write access to the calendar.
	 */
	public static final int CALENDAR_ACCESS_ALL = 1;
	/**
	 * Only owners have write access to the folder.
	 */
	//fxdiff VCRP-8: collaboration tools folder access control
	public static final int FOLDER_ACCESS_OWNERS = 0;
	/**
	 * Owners and members have write access to the folder.
	 */
	public static final int FOLDER_ACCESS_ALL = 1;
	
	
	/**
	 * cache for Boolean Objects representing the State
	 */
	private final static String KEY_NEWS = "news";
	public final static String KEY_CALENDAR_ACCESS = "cal";
	//fxdiff VCRP-8: collaboration tools folder access control
	public final static String KEY_FOLDER_ACCESS = "folder";

	//o_clusterOK by guido
	private Hashtable<String, Boolean> cacheToolStates;
	private final BusinessGroup ores;
	
	private static OLog log = Tracing.createLoggerFor(CollaborationTools.class);
	private transient CoordinatorManager coordinatorManager;

	/**
	 * package local constructor only
	 * 
	 * @param ores
	 */
	CollaborationTools(CoordinatorManager coordinatorManager, BusinessGroup ores) {
		this.coordinatorManager = coordinatorManager;
		this.ores = ores;
		this.cacheToolStates = new Hashtable<String, Boolean>();
	}

	/**
	 * @param ureq
	 * @return a news controller
	 */
	public Controller createNewsController(UserRequest ureq, WindowControl wControl) {
		String news = lookupNews();
		return new SimpleNewsController(ureq, wControl, news);
	}

	/**
	 * TODO: rename to getForumController and save instance?
	 * 
	 * @param ureq
	 * @param wControl
	 * @param isAdmin
	 * @param subsContext the subscriptioncontext if subscriptions to this forum
	 *          should be possible
	 * @return a forum controller
	 */
	public Controller createForumController(UserRequest ureq, WindowControl wControl, boolean isAdmin, boolean isGuestOnly,
			final SubscriptionContext subsContext) {
		Codepoint.codepoint(CollaborationTools.class, "createForumController-init");
		
		final boolean isAdm = isAdmin;
		final boolean isGuest = isGuestOnly;
		Forum forum = getForum();
		
		Translator trans = Util.createPackageTranslator(this.getClass(), ureq.getLocale());
		TitleInfo titleInfo = new TitleInfo(null, trans.translate("collabtools.named.hasForum"));
		titleInfo.setSeparatorEnabled(true);
		Controller forumController = ForumUIFactory.getTitledForumController(ureq, wControl, forum, new ForumCallback() {

			public boolean mayOpenNewThread() {
				return true;
			}

			public boolean mayReplyMessage() {
				return true;
			}

			public boolean mayEditMessageAsModerator() {
				return isAdm;
			}

			public boolean mayDeleteMessageAsModerator() {
				return isAdm;
			}

			public boolean mayArchiveForum() {
				return !isGuest;
			}
			
			public boolean mayFilterForUser() {
				return isAdm;
			}

			public SubscriptionContext getSubscriptionContext() {
				return subsContext;
			}
		}, titleInfo);
		return forumController;
	}
	
	public Forum getForum() {
		
		final ForumManager fom = ForumManager.getInstance();
		final NarrowedPropertyManager npm = NarrowedPropertyManager.getInstance(ores);
		
		// TODO: is there a nicer solution without setting an instance variable
		//final List<Forum> forumHolder = new ArrayList<Forum>();
		
		Codepoint.codepoint(CollaborationTools.class, "pre_sync_enter");
	//TODO gsync
		Forum forum = coordinatorManager.getCoordinator().getSyncer().doInSync(ores, new SyncerCallback<Forum>(){
			public Forum execute() {
				
				Codepoint.codepoint(CollaborationTools.class, "sync_enter");
				
				//was: synchronized (CollaborationTools.class) {
				Forum aforum;
				Long forumKey;
				Property forumKeyProperty = npm.findProperty(null, null, PROP_CAT_BG_COLLABTOOLS, KEY_FORUM);
				if (forumKeyProperty == null) {
					// First call of forum, create new forum and save
					aforum = fom.addAForum();
					forumKey = aforum.getKey();
					if (log.isDebug()) {
						log.debug("created new forum in collab tools: foid::" + forumKey.longValue() + " for ores::"
								+ ores.getResourceableTypeName() + "/" + ores.getResourceableId());
					}
					forumKeyProperty = npm.createPropertyInstance(null, null, PROP_CAT_BG_COLLABTOOLS, KEY_FORUM, null, forumKey, null, null);
					npm.saveProperty(forumKeyProperty);
				} else {
					// Forum does already exist, load forum with key from properties
					forumKey = forumKeyProperty.getLongValue();
					aforum = fom.loadForum(forumKey);
					if (aforum == null) { throw new AssertException("Unable to load forum with key " + forumKey.longValue() + " for ores "
							+ ores.getResourceableTypeName() + " with key " + ores.getResourceableId()); }
					if (log.isDebug()) {
						log.debug("loading forum in collab tools from properties: foid::" + forumKey.longValue() + " for ores::"
								+ ores.getResourceableTypeName() + "/" + ores.getResourceableId());
					}
				}
				Codepoint.codepoint(CollaborationTools.class, "sync_exit");
				return aforum;
			}});
		return forum;
	}

	public String getFolderRelPath() {
		return "/cts/folders/" + ores.getResourceableTypeName() + "/" + ores.getResourceableId();
	}

	/**
	 * Creates a folder run controller with all rights enabled for everybody
	 * 
	 * @param ureq
	 * @param wControl
	 * @param subsContext
	 * @return Copnfigured FolderRunController
	 */
	public FolderRunController createFolderController(UserRequest ureq, WindowControl wControl,
			BusinessGroup businessGroup, boolean isAdmin, final SubscriptionContext subsContext) {
		// do not use a global translator since in the fututre a collaborationtools
		// may be shared among users
		Translator trans = Util.createPackageTranslator(this.getClass(), ureq.getLocale());
		OlatRootFolderImpl rootContainer = getSecuredFolder(businessGroup, subsContext, ureq.getIdentity(), isAdmin);
		OlatNamedContainerImpl namedContainer = new OlatNamedContainerImpl(trans.translate("folder"), rootContainer);
		
		FolderRunController frc = new FolderRunController(namedContainer, true, true, true, ureq, wControl);
		return frc;
	}
	
	/**
	 * Return the root VFS container with security callback set
	 * @return
	 */
	public OlatRootFolderImpl getSecuredFolder(BusinessGroup businessGroup, SubscriptionContext subsContext, Identity identity, boolean isAdmin) {
		if(!isToolEnabled(CollaborationTools.TOOL_FOLDER)) {
			return null;
		}

		//fxdiff VCRP-8: collaboration tools folder access control
		boolean writeAccess;
		boolean isOwner = BaseSecurityManager.getInstance().isIdentityInSecurityGroup(identity, businessGroup.getOwnerGroup());
		if (!(isAdmin || isOwner)) {
				// check if participants have read/write access
			int folderAccess = CollaborationTools.FOLDER_ACCESS_ALL;
			Long lFolderAccess = CollaborationToolsFactory.getInstance().getOrCreateCollaborationTools(businessGroup).lookupFolderAccess();
			if (lFolderAccess != null) {
				folderAccess = lFolderAccess.intValue();
			}
			writeAccess = (folderAccess == CollaborationTools.CALENDAR_ACCESS_ALL);
		} else {
			writeAccess = true;
		}

		String relPath = getFolderRelPath();
		VFSSecurityCallback secCallback = new CollabSecCallback(writeAccess, relPath, subsContext);
		OlatRootFolderImpl rootContainer = new OlatRootFolderImpl(relPath, null);
		rootContainer.setLocalSecurityCallback(secCallback);
		return rootContainer;
	}

	/**
	 * Creates a calendar controller
	 * @param ureq
	 * @param wControl
	 * @param resourceableId
	 * @return Configured WeeklyCalendarController
	 */
	public CalendarController createCalendarController(UserRequest ureq, WindowControl wControl, BusinessGroup businessGroup, boolean isAdmin) {
		CollaborationManager collaborationManager = CoreSpringFactory.getImpl(CollaborationManager.class);
		KalendarRenderWrapper calRenderWrapper = collaborationManager.getCalendar(businessGroup, ureq, isAdmin);
	
		// add linking
		List<OLATResource> resources = CoreSpringFactory.getImpl(BusinessGroupService.class).findResources(Collections.singleton(businessGroup), 0, -1);
		
		List<ICourse> courses = new ArrayList<ICourse>(resources.size());
		for (OLATResource resource:resources) {
			if (resource.getResourceableTypeName().equals(CourseModule.getCourseTypeName())) {
				ICourse course = CourseFactory.loadCourse(resource);
				courses.add(course);
			}
		}
		if(!courses.isEmpty()) {
			CourseLinkProviderController clp = new CourseLinkProviderController(null, courses, ureq, wControl);
			calRenderWrapper.setLinkProvider(clp);
		}

		List<KalendarRenderWrapper> calendars = new ArrayList<KalendarRenderWrapper>();
		calendars.add(calRenderWrapper);
		
		WeeklyCalendarController calendarController = new WeeklyCalendarController(
				ureq, wControl, calendars, WeeklyCalendarController.CALLER_COLLAB, true);

		return calendarController;
	}

	/**
	 * @param ureq
	 * @param wControl
	 * @return a contact form controller
	 */
	public ContactFormController createContactFormController(UserRequest ureq, WindowControl wControl, ContactMessage cmsg) {
		ContactFormController cfc = new ContactFormController(ureq, wControl, true, true, false, false, cmsg);
		return cfc;
	}

	
	/**
	 * @param ureq
	 * @param wControl
	 * @param chatName
	 * @return Controller
	 */
	public ChatToolController createChatController(UserRequest ureq, WindowControl wControl, BusinessGroup grp, boolean isAdmin) {
		InstantMessagingModule imModule = CoreSpringFactory.getImpl(InstantMessagingModule.class);
		if (imModule.isEnabled() && imModule.isGroupEnabled()) {
			return new ChatToolController(ureq, wControl, grp, isAdmin);
		}
		return null;
	}
	
	/**
	 * return an controller for the wiki tool
	 * @param ureq
	 * @param wControl
	 * @return
	 */
	public Controller createWikiController(UserRequest ureq, WindowControl wControl) {
		// Check for jumping to certain wiki page
		BusinessControl bc = wControl.getBusinessControl();
		ContextEntry ce = bc.popLauncherContextEntry();
		
		SubscriptionContext subContext = new SubscriptionContext(ores, WikiManager.WIKI_RESOURCE_FOLDER_NAME);
		boolean isOlatAdmin = ureq.getUserSession().getRoles().isOLATAdmin();
		boolean isGuestOnly = ureq.getUserSession().getRoles().isGuestOnly();
		boolean isResourceOwner = BaseSecurityManager.getInstance().isIdentityPermittedOnResourceable(ureq.getIdentity(), Constants.PERMISSION_ACCESS, ores);
		WikiSecurityCallback callback = new WikiSecurityCallbackImpl(null, isOlatAdmin, isGuestOnly, true, isResourceOwner, subContext);
		if ( ce != null ) { //jump to a certain context
			OLATResourceable ceOres = ce.getOLATResourceable();
			String typeName = ceOres.getResourceableTypeName();
			String page = typeName.substring("page=".length());
			if(page != null && page.endsWith(":0")) {
				page = page.substring(0, page.length() - 2);
			}
			return WikiManager.getInstance().createWikiMainController(ureq, wControl, ores, callback, page);
		} else {
			return WikiManager.getInstance().createWikiMainController(ureq, wControl, ores, callback, null);
		}
	}
	
	/**
	 * return an controller for the wiki tool
	 * @param ureq
	 * @param wControl
	 * @return
	 */
	public Controller createPortfolioController(final UserRequest ureq, WindowControl wControl, final BusinessGroup group) {
		final EPFrontendManager ePFMgr = (EPFrontendManager)CoreSpringFactory.getBean("epFrontendManager");
		final NarrowedPropertyManager npm = NarrowedPropertyManager.getInstance(ores);
	//TODO gsync
		PortfolioStructureMap map = coordinatorManager.getCoordinator().getSyncer().doInSync(ores, new SyncerCallback<PortfolioStructureMap>(){
			public PortfolioStructureMap execute() {
				PortfolioStructureMap aMap;
				Long mapKey;
				Property mapKeyProperty = npm.findProperty(null, null, PROP_CAT_BG_COLLABTOOLS, KEY_PORTFOLIO);
				if (mapKeyProperty == null) {
					// First call of portfolio-tool, create new map and save
					aMap = ePFMgr.createAndPersistPortfolioDefaultMap(group, group.getName(), group.getDescription());					
					Translator pT = Util.createPackageTranslator(EPCreateMapController.class, ureq.getLocale());					
					// add a page, as each map should have at least one per default!
					final String title = pT.translate("new.page.title");
					final String description = pT.translate("new.page.desc");
					ePFMgr.createAndPersistPortfolioPage(aMap, title, description);
					mapKey = aMap.getKey();
					if (log.isDebug()) {
						log.debug("created new portfolio map in collab tools: mapid::" + mapKey + " for ores::" + ores.getResourceableTypeName() + "/"
								+ ores.getResourceableId());
					}
					mapKeyProperty = npm.createPropertyInstance(null, null, PROP_CAT_BG_COLLABTOOLS, KEY_PORTFOLIO, null, mapKey, null, null);
					npm.saveProperty(mapKeyProperty);
				} else {
					// map does already exist, load map with key from properties
					mapKey = mapKeyProperty.getLongValue();
					aMap = (PortfolioStructureMap) ePFMgr.loadPortfolioStructureByKey(mapKey);
					if (aMap == null) { throw new AssertException("Unable to load portfolio map with key " + mapKey + " for ores "
							+ ores.getResourceableTypeName() + " with key " + ores.getResourceableId()); }
					if (log.isDebug()) {
						log.debug("loading portfolio map in collab tools from properties: foid::" + mapKey + " for ores::"
								+ ores.getResourceableTypeName() + "/" + ores.getResourceableId());
					}
				}
				return aMap;
			}});

		EPSecurityCallback secCallback = new EPSecurityCallbackImpl(true, true);
		return EPUIFactory.createMapViewController(ureq, wControl, map, secCallback);
	}
	
	public Controller createOpenMeetingsController(final UserRequest ureq, WindowControl wControl, final BusinessGroup group, boolean admin) {
		OpenMeetingsRunController runController = new OpenMeetingsRunController(ureq, wControl, group, null, null, admin, admin);
		return runController;
	}

	/**
	 * @param toolToChange
	 * @param enable
	 */
	public void setToolEnabled(String toolToChange, boolean enable) {
		createOrUpdateProperty(toolToChange, enable);
	}

	/**
	 * reads from the internal cache. <b>Precondition </b> cache must be filled at
	 * CollaborationTools creation time.
	 * 
	 * @param enabledTool
	 * @return boolean
	 */
	public boolean isToolEnabled(String enabledTool) {
		//o_clusterOK as whole object gets invalidated if tool is added or deleted
		if (!cacheToolStates.containsKey(enabledTool)) {
			// not in cache yet, read property first (see getPropertyOf(..))
			getPropertyOf(enabledTool);
		}
		// POSTCONDITION: cacheToolStates.get(enabledTool) != null
		Boolean cachedValue = cacheToolStates.get(enabledTool);
		return cachedValue.booleanValue();
	}

	/**
	 * delete all CollaborationTools stuff from the database, which is related to
	 * the calling OLATResourceable.
	 */
	public void deleteTools(BusinessGroup businessGroupTodelete) {
		NarrowedPropertyManager npm = NarrowedPropertyManager.getInstance(ores);
		/*
		 * delete the forum, if existing
		 */
		ForumManager fom = ForumManager.getInstance();
		Property forumKeyProperty = npm.findProperty(null, null, PROP_CAT_BG_COLLABTOOLS, KEY_FORUM);
		if (forumKeyProperty != null) {
			// if there was a forum, delete it
			Long forumKey = forumKeyProperty.getLongValue();
			if (forumKey == null) throw new AssertException("property had no longValue, prop:" + forumKeyProperty);
			fom.deleteForum(forumKey);
		}
		/*
		 * delete the folder, if existing
		 */
		OlatRootFolderImpl vfsContainer = new OlatRootFolderImpl(getFolderRelPath(), null);
		File fFolderRoot = vfsContainer.getBasefile();
		if (fFolderRoot.exists()) {
			FileUtils.deleteDirsAndFiles(fFolderRoot, true, true);
		}
		
		/*
		 * delete the wiki if existing
		 */
		VFSContainer rootContainer = WikiManager.getInstance().getWikiRootContainer(ores);
		if(rootContainer != null) rootContainer.delete();
		
		/*
		 * Delete calendar if exists
		 */
		if (businessGroupTodelete != null) {
			CalendarManager calManager = CalendarManagerFactory.getInstance().getCalendarManager();
			calManager.deleteGroupCalendar(businessGroupTodelete);
		}
		
		/*
		 * delete chatRoom
		 */
		// no cleanup needed, automatically done when last user exits the room
		/*
		 * delete all Properties defining enabled/disabled CollabTool XY and the
		 * news content
		 */
		npm.deleteProperties(null, null, PROP_CAT_BG_COLLABTOOLS, null);
		
		/*
		 * Delete OpenMeetings room
		 */
		OpenMeetingsModule omModule = CoreSpringFactory.getImpl(OpenMeetingsModule.class);
		if(omModule.isEnabled()) {
			OpenMeetingsManager omManager = CoreSpringFactory.getImpl(OpenMeetingsManager.class);
			try {
				omManager.deleteAll(ores, null, null);
			} catch (OpenMeetingsException e) {
				log.error("A room could not be deleted for group: " + ores, e);
			}
		}

		/*
		 * and last but not least the cache is reseted
		 */
		cacheToolStates.clear();
		this.dirty = true;
	}
	
	
	private void openOpenMeetingsRoom() {
		OpenMeetingsModule omModule = CoreSpringFactory.getImpl(OpenMeetingsModule.class);
		if(!omModule.isEnabled()) return;
		
		OpenMeetingsManager omm = CoreSpringFactory.getImpl(OpenMeetingsManager.class);
		Long roomId = omm.getRoomId(ores, null, null);
		if(roomId == null) {
			//create the room
			OpenMeetingsRoom room = new OpenMeetingsRoom();
			room.setComment(ores.getDescription());
			room.setModerated(true);
			room.setName(ores.getName());
			room.setRecordingAllowed(true);
			room.setResourceName(ores.getName());
			room.setSize(25);
			room.setType(RoomType.conference.type());
			omm.addRoom(ores, null, null, room);
		}
	}

	/**
	 * creates the property if non-existing, or updates the existing property to
	 * the supplied values. Real changes are made persistent immediately.
	 * 
	 * @param selectedTool
	 * @param toolValue
	 */
	private void createOrUpdateProperty(final String selectedTool, final boolean toolValue) {

		Boolean cv = cacheToolStates.get(selectedTool);
		if (cv != null && cv.booleanValue() == toolValue) {
			return; // nice, cache saved a needless update
		}

		// handle Boolean Values via String Field in Property DB Table
		final String toolValueStr = toolValue ? TRUE : FALSE;
		final PropertyManager pm = PropertyManager.getInstance();
		//TODO gsync
		coordinatorManager.getCoordinator().getSyncer().doInSync(ores, new SyncerExecutor() {
			public void execute() {				
				//was: synchronized (CollaborationTools.class) {
				Property property = getPropertyOf(selectedTool);
				if (property == null) {
					// not existing -> create it
					property = pm.createPropertyInstance(null, null, ores, PROP_CAT_BG_COLLABTOOLS, selectedTool, null, null, toolValueStr, null);
				} else {
					// if existing -> update to desired value
					property.setStringValue(toolValueStr);
				}
				
				//create a room if needed
				if(toolValue && TOOL_OPENMEETINGS.equals(selectedTool)) {
					openOpenMeetingsRoom();
				}
				
				// property becomes persistent
				pm.saveProperty(property);
			}});
		this.dirty = true;
		cacheToolStates.put(selectedTool, Boolean.valueOf(toolValue));
	}

	Property getPropertyOf(String selectedTool) {
		PropertyManager pm = PropertyManager.getInstance();
		Property property = pm.findProperty(null, null, ores, PROP_CAT_BG_COLLABTOOLS, selectedTool);
		Boolean res;
		if (property == null) { // meaning false
			res = Boolean.FALSE;
		} else {
			String val = property.getStringValue();
			res = val.equals(TRUE) ? Boolean.TRUE : Boolean.FALSE;
		}
		cacheToolStates.put(selectedTool, res);
		return property;
	}

	/**
	 * create the Collaboration Tools Suite. This Controller handles the
	 * enabling/disabling of Collab Tools.
	 * 
	 * @param ureq
	 * @return a collaboration tools settings controller
	 */
	public CollaborationToolsSettingsController createCollaborationToolsSettingsController(UserRequest ureq, WindowControl wControl) {
		return new CollaborationToolsSettingsController(ureq, wControl, ores);
	}

	/**
	 * @return the news; if there is no news yet: return null;
	 */
	public String lookupNews() {
		NarrowedPropertyManager npm = NarrowedPropertyManager.getInstance(ores);
		Property property = npm.findProperty(null, null, PROP_CAT_BG_COLLABTOOLS, KEY_NEWS);
		if (property == null) { // no entry
			return null;
		}
		// read the text value of the existing property
		String text = property.getTextValue();
		return text;
	}

	/**
	 * @param news
	 */
	public void saveNews(String news) {
		NarrowedPropertyManager npm = NarrowedPropertyManager.getInstance(ores);
		Property property = npm.findProperty(null, null, PROP_CAT_BG_COLLABTOOLS, KEY_NEWS);
		if (property == null) { // create a new one
			Property nP = npm.createPropertyInstance(null, null, PROP_CAT_BG_COLLABTOOLS, KEY_NEWS, null, null, null, news);
			npm.saveProperty(nP);
		} else { // modify the existing one
			property.setTextValue(news);
			npm.updateProperty(property);
		}
	}
	
	public Long lookupCalendarAccess() {
		NarrowedPropertyManager npm = NarrowedPropertyManager.getInstance(ores);
		Property property = npm.findProperty(null, null, PROP_CAT_BG_COLLABTOOLS, KEY_CALENDAR_ACCESS);
		if (property == null) { // no entry
			return null;
		}
		// read the long value of the existing property
		return property.getLongValue();
	}

	
	public void saveCalendarAccess(Long calendarAccess) {
		NarrowedPropertyManager npm = NarrowedPropertyManager.getInstance(ores);
		Property property = npm.findProperty(null, null, PROP_CAT_BG_COLLABTOOLS, KEY_CALENDAR_ACCESS);
		if (property == null) { // create a new one
			Property nP = npm.createPropertyInstance(null, null, PROP_CAT_BG_COLLABTOOLS, KEY_CALENDAR_ACCESS, null, calendarAccess, null, null);
			npm.saveProperty(nP);
		} else { // modify the existing one
			property.setLongValue(calendarAccess);
			npm.updateProperty(property);
		}
	}
	
	//fxdiff VCRP-8: collaboration tools folder access control
	public Long lookupFolderAccess() {
		NarrowedPropertyManager npm = NarrowedPropertyManager.getInstance(ores);
		Property property = npm.findProperty(null, null, PROP_CAT_BG_COLLABTOOLS, KEY_FOLDER_ACCESS);
		if (property == null) { // no entry
			return null;
		}
		// read the long value of the existing property
		return property.getLongValue();
	}
	
	//fxdiff VCRP-8: collaboration tools folder access control
	public void saveFolderAccess(Long folderrAccess) {
		NarrowedPropertyManager npm = NarrowedPropertyManager.getInstance(ores);
		Property property = npm.findProperty(null, null, PROP_CAT_BG_COLLABTOOLS, KEY_FOLDER_ACCESS);
		if (property == null) { // create a new one
			Property nP = npm.createPropertyInstance(null, null, PROP_CAT_BG_COLLABTOOLS, KEY_FOLDER_ACCESS, null, folderrAccess, null, null);
			npm.saveProperty(nP);
		} else { // modify the existing one
			property.setLongValue(folderrAccess);
			npm.updateProperty(property);
		}
	}
	
	public class CollabSecCallback implements VFSSecurityCallback {
		
		//fxdiff VCRP-8: collaboration tools folder access control
		private final boolean write;
		private Quota folderQuota = null;
		private SubscriptionContext subsContext;

		public CollabSecCallback(boolean write, String relPath, SubscriptionContext subsContext) {
			this.subsContext = subsContext;
			initFolderQuota(relPath);
			this.write = write;
		}

		private void initFolderQuota(String relPath) {
			QuotaManager qm = QuotaManager.getInstance();
			folderQuota = qm.getCustomQuota(relPath);
			if (folderQuota == null) {
				Quota defQuota = qm.getDefaultQuota(QuotaConstants.IDENTIFIER_DEFAULT_GROUPS);
				folderQuota = QuotaManager.getInstance().createQuota(relPath, defQuota.getQuotaKB(), defQuota.getUlLimitKB());
			}
		}

		public boolean canRead() {
			return true;
		}

		public boolean canWrite() {
			return write;
		}

		@Override
		public boolean canCreateFolder() {
			return write;
		}

		public boolean canDelete() {
			return write;
		}

		public boolean canList() {
			return true;
		}

		public boolean canCopy() {
			return true;
		}
		
		public boolean canDeleteRevisionsPermanently() {
			return write;
		}

		public Quota getQuota() {
			return folderQuota;
		}

		public void setQuota(Quota quota) {
			this.folderQuota = quota;
		}

		public SubscriptionContext getSubscriptionContext() {
			return subsContext;
		}
	}

	/**
	 * It is assumed that this is only called by an administrator
	 * (e.g. at deleteGroup) 
	 * @param archivFilePath
	 */
	public void archive(String archivFilePath) {
		if (isToolEnabled(CollaborationTools.TOOL_FORUM)) {
			archiveForum(this.ores, archivFilePath);
		}
		if (isToolEnabled(CollaborationTools.TOOL_WIKI)) {
			archiveWiki(this.ores, archivFilePath);
		}
		if (isToolEnabled(CollaborationTools.TOOL_FOLDER)) {
			archiveFolder(this.ores, archivFilePath);
		}
	}

	private void archiveForum(OLATResourceable ores, String archivFilePath) {
		Property forumKeyProperty = NarrowedPropertyManager.getInstance(ores).findProperty(null, null, PROP_CAT_BG_COLLABTOOLS, KEY_FORUM);
		if (forumKeyProperty != null) {
			VFSContainer archiveContainer = new LocalFolderImpl(new File(archivFilePath));
			String archiveForumName = "del_forum_" + forumKeyProperty.getLongValue();
			VFSContainer archiveForumContainer = archiveContainer.createChildContainer(archiveForumName);
			ForumFormatter ff = new ForumRTFFormatter(archiveForumContainer, false);
			ForumArchiveManager.getInstance().applyFormatter(ff, forumKeyProperty.getLongValue(), null);
		}
	}

	private void archiveWiki(OLATResourceable ores, String archivFilePath) { 
		VFSContainer wikiContainer = WikiManager.getInstance().getWikiRootContainer(ores);
		VFSLeaf wikiZip = WikiToZipUtils.getWikiAsZip(wikiContainer);
		String exportFileName = "del_wiki_" + ores.getResourceableId() + ".zip";
		File archiveDir = new File(archivFilePath);
		if (!archiveDir.exists()) {
			archiveDir.mkdir();
		}
		String fullFilePath = archivFilePath + File.separator + exportFileName;
		
		try {
			FileUtils.bcopy(wikiZip.getInputStream(), new File(fullFilePath), "archive wiki");
		} catch (FileNotFoundException e) {
			log.warn("Can not archive wiki repoEntry=" + ores.getResourceableId());
		} catch (IOException ioe) {
			log.warn("Can not archive wiki repoEntry=" + ores.getResourceableId());
		}		
	}

	private void archiveFolder(OLATResourceable ores, String archiveFilePath) {
		OlatRootFolderImpl folderContainer = new OlatRootFolderImpl(getFolderRelPath(), null);
		File fFolderRoot = folderContainer.getBasefile();
		if (fFolderRoot.exists()) {
			String zipFileName = "del_folder_" + ores.getResourceableId() + ".zip";
			String fullZipFilePath = archiveFilePath + File.separator + zipFileName;
			ZipUtil.zipAll(fFolderRoot, new File(fullZipFilePath), true);
		}
	}

	/**
	 * whole object gets cached, if tool gets added or deleted the object becomes dirty and will be removed from cache.
	 * @return
	 */
	protected boolean isDirty() {
		return dirty;
	}
}