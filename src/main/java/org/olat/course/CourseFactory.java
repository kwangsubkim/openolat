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

package org.olat.course;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.zip.ZipOutputStream;

import org.apache.poi.util.IOUtils;
import org.olat.admin.quota.QuotaConstants;
import org.olat.basesecurity.BaseSecurity;
import org.olat.basesecurity.BaseSecurityManager;
import org.olat.basesecurity.Constants;
import org.olat.basesecurity.SecurityGroup;
import org.olat.commons.calendar.CalendarManager;
import org.olat.commons.calendar.CalendarManagerFactory;
import org.olat.commons.calendar.notification.CalendarNotificationManager;
import org.olat.commons.calendar.ui.components.KalendarRenderWrapper;
import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.fullWebApp.LayoutMain3ColsController;
import org.olat.core.commons.modules.bc.FolderConfig;
import org.olat.core.commons.modules.bc.vfs.OlatRootFolderImpl;
import org.olat.core.commons.persistence.DBFactory;
import org.olat.core.commons.services.notifications.NotificationsManager;
import org.olat.core.commons.services.notifications.Publisher;
import org.olat.core.commons.services.notifications.SubscriptionContext;
import org.olat.core.commons.services.taskexecutor.TaskExecutorManager;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.htmlheader.jscss.CustomCSS;
import org.olat.core.gui.components.stack.StackedController;
import org.olat.core.gui.components.tree.TreeNode;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.layout.MainLayoutController;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.context.BusinessControlFactory;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.OLATRuntimeException;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.manager.BasicManager;
import org.olat.core.util.CodeHelper;
import org.olat.core.util.ExportUtil;
import org.olat.core.util.FileUtils;
import org.olat.core.util.Formatter;
import org.olat.core.util.ObjectCloner;
import org.olat.core.util.StringHelper;
import org.olat.core.util.UserSession;
import org.olat.core.util.Util;
import org.olat.core.util.WebappHelper;
import org.olat.core.util.ZipUtil;
import org.olat.core.util.cache.CacheWrapper;
import org.olat.core.util.coordinate.CoordinatorManager;
import org.olat.core.util.coordinate.SyncerCallback;
import org.olat.core.util.coordinate.SyncerExecutor;
import org.olat.core.util.event.MultiUserEvent;
import org.olat.core.util.nodes.INode;
import org.olat.core.util.resource.OresHelper;
import org.olat.core.util.tree.TreeVisitor;
import org.olat.core.util.tree.Visitor;
import org.olat.core.util.vfs.Quota;
import org.olat.core.util.vfs.QuotaManager;
import org.olat.core.util.vfs.VFSConstants;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSManager;
import org.olat.core.util.vfs.VFSStatus;
import org.olat.core.util.xml.XStreamHelper;
import org.olat.course.archiver.ScoreAccountingHelper;
import org.olat.course.assessment.manager.UserCourseInformationsManager;
import org.olat.course.config.CourseConfig;
import org.olat.course.config.CourseConfigManagerImpl;
import org.olat.course.config.ui.courselayout.CourseLayoutHelper;
import org.olat.course.editor.EditorMainController;
import org.olat.course.editor.PublishProcess;
import org.olat.course.editor.StatusDescription;
import org.olat.course.editor.PublishSetInformations;
import org.olat.course.groupsandrights.CourseGroupManager;
import org.olat.course.groupsandrights.PersistingCourseGroupManager;
import org.olat.course.nodes.AssessableCourseNode;
import org.olat.course.nodes.BCCourseNode;
import org.olat.course.nodes.CourseNode;
import org.olat.course.nodes.STCourseNode;
import org.olat.course.nodes.TACourseNode;
import org.olat.course.properties.CoursePropertyManager;
import org.olat.course.properties.PersistingCoursePropertyManager;
import org.olat.course.repository.ImportGlossaryReferencesController;
import org.olat.course.repository.ImportSharedfolderReferencesController;
import org.olat.course.run.RunMainController;
import org.olat.course.run.environment.CourseEnvironment;
import org.olat.course.statistic.AsyncExportManager;
import org.olat.course.tree.CourseEditorTreeModel;
import org.olat.course.tree.CourseEditorTreeNode;
import org.olat.course.tree.PublishTreeModel;
import org.olat.group.BusinessGroup;
import org.olat.instantMessaging.InstantMessagingService;
import org.olat.instantMessaging.manager.ChatLogHelper;
import org.olat.modules.glossary.GlossaryManager;
import org.olat.modules.sharedfolder.SharedFolderManager;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryImportExport;
import org.olat.repository.RepositoryManager;
import org.olat.resource.OLATResource;
import org.olat.resource.OLATResourceManager;
import org.olat.resource.references.ReferenceImpl;
import org.olat.resource.references.ReferenceManager;
import org.olat.testutils.codepoints.server.Codepoint;
import org.olat.user.UserManager;
import org.olat.util.logging.activity.LoggingResourceable;


/**
 * Description: <BR>
 * Use the course factory to create course run and edit controllers or to load a
 * course from disk
 * 
 * Initial Date: Oct 12, 2004
 * @author Felix Jost
 * @author guido
 */
public class CourseFactory extends BasicManager {
		
	private static CacheWrapper<Long,PersistingCourseImpl> loadedCourses;
	private static ConcurrentHashMap<Long, ModifyCourseEvent> modifyCourseEvents = new ConcurrentHashMap<Long, ModifyCourseEvent>();

	public static final String COURSE_EDITOR_LOCK = "courseEditLock";
  //this is the lock that must be aquired at course editing, copy course, export course, configure course.
	private static Map<Long,PersistingCourseImpl> courseEditSessionMap = new ConcurrentHashMap<Long,PersistingCourseImpl>();
	private static OLog log = Tracing.createLoggerFor(CourseFactory.class);
	private static RepositoryManager repositoryManager;
	private static OLATResourceManager olatResourceManager;
	private static BaseSecurity securityManager;
	private static ReferenceManager referenceManager;
	private static GlossaryManager glossaryManager;
	
	
	/**
	 * [used by spring]
	 */
	private CourseFactory(CoordinatorManager coordinatorManager, RepositoryManager repositoryManager, OLATResourceManager olatResourceManager, 
			BaseSecurity securityManager, ReferenceManager referenceManager, GlossaryManager glossaryManager) {
		loadedCourses = coordinatorManager.getCoordinator().getCacher().getCache(CourseFactory.class.getSimpleName(), "courses");
		CourseFactory.repositoryManager = repositoryManager;
		CourseFactory.olatResourceManager = olatResourceManager;
		CourseFactory.securityManager = securityManager;
		CourseFactory.referenceManager = referenceManager;
		CourseFactory.glossaryManager = glossaryManager;
	}
	
	/**
	 * Create a run controller for the given course resourceable
	 * 
	 * @param ureq
	 * @param wControl
	 * @param re
	 * @param initialViewIdentifier if null the default view will be started,
	 *          otherwise a controllerfactory type dependant view will be
	 *          activated (subscription subtype)
	 * @return run controller for the given course resourceable
	 */
	public static MainLayoutController createLaunchController(UserRequest ureq, WindowControl wControl, final RepositoryEntry re) {
		ICourse course = loadCourse(re.getOlatResource());
		boolean isDebug = log.isDebug();
		long startT = 0;
		if(isDebug){
			startT = System.currentTimeMillis();
		}
		MainLayoutController launchC = new RunMainController(ureq, wControl, course, re, true, true);
		if(isDebug){
			log.debug("Runview for [["+course.getCourseTitle()+"]] took [ms]"+(System.currentTimeMillis() - startT));
		}
		
		return launchC;
	}

	/**
	 * Create an editor controller for the given course resourceable
	 * 
	 * @param ureq
	 * @param wControl
	 * @param olatResource
	 * @return editor controller for the given course resourceable; if the editor
	 *         is already locked, it returns a controller with a lock message
	 */
	public static Controller createEditorController(UserRequest ureq, WindowControl wControl, StackedController stack,
			OLATResourceable olatResource, CourseNode selectedNode) {
		ICourse course = loadCourse(olatResource);
		EditorMainController emc = new EditorMainController(ureq, wControl, course, stack, selectedNode);
		if (!emc.getLockEntry().isSuccess()) {
			// get i18n from the course runmaincontroller to say that this editor is
			// already locked by another person

			Translator translator = Util.createPackageTranslator(RunMainController.class, ureq.getLocale());
			String lockerName = CoreSpringFactory.getImpl(UserManager.class).getUserDisplayName(emc.getLockEntry().getOwner());
			wControl.setWarning(translator.translate("error.editoralreadylocked", new String[] { lockerName }));
			return null;
		}
		//set the logger if editor is started
		//since 5.2 groups / areas can be created from the editor -> should be logged.
		emc.addLoggingResourceable(LoggingResourceable.wrap(course));
		return emc;
	}

	/**
	 * Creates an empty course with a single root node. The course is linked to
	 * the resourceable ores.
	 * 
	 * @param ores
	 * @param shortTitle Short title of root node
	 * @param longTitle Long title of root node
	 * @param learningObjectives Learning objectives of root node
	 * @return an empty course with a single root node.
	 */
	public static ICourse createEmptyCourse(OLATResourceable ores, String shortTitle, String longTitle, String learningObjectives) {
		PersistingCourseImpl newCourse = new PersistingCourseImpl(ores.getResourceableId());
		// Put new course in course cache    
		putCourseInCache(newCourse.getResourceableId() ,newCourse);
		
		Structure initialStructure = new Structure();
		CourseNode runRootNode = new STCourseNode();
		runRootNode.setShortTitle(shortTitle);
		runRootNode.setLongTitle(longTitle);
		runRootNode.setLearningObjectives(learningObjectives);
		initialStructure.setRootNode(runRootNode);
		newCourse.setRunStructure(initialStructure);
		newCourse.saveRunStructure();

		CourseEditorTreeModel editorTreeModel = new CourseEditorTreeModel();
		CourseEditorTreeNode editorRootNode = new CourseEditorTreeNode((CourseNode) ObjectCloner.deepCopy(runRootNode));
		editorTreeModel.setRootNode(editorRootNode);
		newCourse.setEditorTreeModel(editorTreeModel);
		newCourse.saveEditorTreeModel();

		return newCourse;
	}

	/**
	 * Gets the course from cache if already there, or loads the course and puts it into cache.
	 * To be called for the "CourseRun" model.
	 * @param resourceableId
	 * @return the course with the given id (the type is always
	 *         CourseModule.class.toString())
	 */
	public static ICourse loadCourse(final Long resourceableId) {
		if (resourceableId == null) throw new AssertException("No resourceable ID found.");
		PersistingCourseImpl course = getCourseFromCache(resourceableId);
		if (course == null) {
			// o_clusterOK by:ld - load and put in cache in doInSync block to ensure
			// that no invalidate cache event was missed
			if (log.isDebug()) log.debug("try to load course with resourceableId=" + resourceableId);
			OLATResourceable courseResourceable = OresHelper.createOLATResourceableInstance(PersistingCourseImpl.class, resourceableId);
			course = CoordinatorManager.getInstance().getCoordinator().getSyncer().doInSync(courseResourceable, new SyncerCallback<PersistingCourseImpl>() {
				public PersistingCourseImpl execute() {
					PersistingCourseImpl theCourse = null;
					theCourse = getCourseFromCache(resourceableId);
					if (theCourse == null) {
						long startTime = 0;
						long endTime = 0;
						if (log.isDebug()) startTime = System.currentTimeMillis();
						theCourse = new PersistingCourseImpl(resourceableId);
						theCourse.load();
						if (log.isDebug()) endTime = System.currentTimeMillis();
						putCourseInCache(resourceableId, theCourse);
						long diff = 0;
						if (log.isDebug()) diff = Long.valueOf(endTime - startTime);
						if (log.isDebug()) 	log.debug("[[" + resourceableId + "[[" + diff + "[[" + theCourse.getCourseTitle());
					}
					return theCourse;
				}
			});
		}

		return course;
	}

	/**
	 * Load the course for the given course resourceable
	 * 
	 * @param olatResource
	 * @return the course for the given course resourceable
	 */
	public static ICourse loadCourse(OLATResourceable olatResource) {
		Long resourceableId = olatResource.getResourceableId();
		return loadCourse(resourceableId);
	}

	/**
	 * 
	 * @param resourceableId
	 * @return the PersistingCourseImpl instance for the input key.
	 */
	static PersistingCourseImpl getCourseFromCache(Long resourceableId) {	//o_clusterOK by:ld    
		return loadedCourses.get(resourceableId);
	}

	/**
	 * Puts silent.
	 * @param resourceableId
	 * @param course
	 */
	static void putCourseInCache(Long resourceableId, PersistingCourseImpl course) { //o_clusterOK by:ld    
		loadedCourses.put(resourceableId, course);
		log.debug("putCourseInCache ");
	}
			
	/**
	 * 
	 * @param resourceableId
	 */
	private static void removeFromCache(Long resourceableId) { //o_clusterOK by: ld
		loadedCourses.remove(resourceableId);	
		log.debug("removeFromCache");
	}
	
	/**
	 * Puts the current course in the local cache and removes it from other caches (other cluster nodes).
	 * @param resourceableId
	 * @param course
	 */
	private static void updateCourseInCache(Long resourceableId, PersistingCourseImpl course) { //o_clusterOK by:ld    
		loadedCourses.update(resourceableId, course);				
		log.debug("updateCourseInCache");
	}

	/**
	 * Delete a course including its course folder and all references to resources
	 * this course holds.
	 * 
	 * @param res
	 */
	public static void deleteCourse(OLATResourceable res) {
		final long start = System.currentTimeMillis();
		log.info("deleteCourse: starting to delete course. res="+res);

		// find all references to course
		List<ReferenceImpl> refs = referenceManager.getReferences(res);
		for (Iterator<ReferenceImpl> iter = refs.iterator(); iter.hasNext();) {
			ReferenceImpl ref = (ReferenceImpl) iter.next();
			referenceManager.delete(ref);
		}
		
		PersistingCourseImpl course = null;
		try {
			course = (PersistingCourseImpl)loadCourse(res);
		} catch (CorruptedCourseException e) {
			log.error("Try to delete a corrupted course, I make want I can.");
		}
		
		// call cleanupOnDelete for nodes
		if(course != null) {
			Visitor visitor = new NodeDeletionVisitor(course);
			TreeVisitor tv = new TreeVisitor(visitor, course.getRunStructure().getRootNode(), true);
			tv.visitAll();
		}

		// delete assessment notifications
		OLATResourceable assessmentOres = OresHelper.createOLATResourceableInstance(CourseModule.ORES_COURSE_ASSESSMENT, res.getResourceableId());
		NotificationsManager.getInstance().deletePublishersOf(assessmentOres);
		// delete all course notifications
		NotificationsManager.getInstance().deletePublishersOf(res);
		//delete calendar subscription
		clearCalenderSubscriptions(res);
		// delete course configuration (not really usefull, the config is in
		// the course folder which is deleted right after)
		if(course != null) {
			CourseConfigManagerImpl.getInstance().deleteConfigOf(course);
		}
		
		//clean up tasks
		OLATResource resource = course.getCourseEnvironment().getCourseGroupManager().getCourseResource();
		CoreSpringFactory.getImpl(TaskExecutorManager.class).delete(resource);
		
		// delete course group- and rightmanagement
		CourseGroupManager courseGroupManager = PersistingCourseGroupManager.getInstance(res);
		courseGroupManager.deleteCourseGroupmanagement();
		// delete all remaining course properties
		CoursePropertyManager propertyManager = PersistingCoursePropertyManager.getInstance(res);
		propertyManager.deleteAllCourseProperties();
		// delete course calendar
		CalendarManager calManager = CalendarManagerFactory.getInstance().getCalendarManager();
		calManager.deleteCourseCalendar(res);
		// delete IM messages
		CoreSpringFactory.getImpl(InstantMessagingService.class).deleteMessages(res);

		// cleanup cache
		removeFromCache(res.getResourceableId());
		//TODO: ld: broadcast event: DeleteCourseEvent

		// Everything is deleted, so we could get rid of course logging
		// with the change in user audit logging - which now all goes into a DB
		// we no longer do this though!

		// delete course directory
		VFSContainer fCourseBasePath = getCourseBaseContainer(res.getResourceableId());
		VFSStatus status = fCourseBasePath.delete();
		boolean deletionSuccessful = (status == VFSConstants.YES || status == VFSConstants.SUCCESS);
		log.info("deleteCourse: finished deletion. res="+res+", deletion successful: "+deletionSuccessful+", duration: "+(System.currentTimeMillis()-start)+" ms.");
	}

	/**
	 * Checks all learning group calendars and the course calendar for publishers (of subscriptions)
	 * and sets their state to "1" which indicates that the ressource is deleted.
	 */
	private static void clearCalenderSubscriptions(OLATResourceable res) {
		//set Publisher state to 1 (= ressource is deleted) for all calendars of the course
		CalendarManager calMan = CalendarManagerFactory.getInstance().getCalendarManager();
		CalendarNotificationManager notificationManager = CoreSpringFactory.getImpl(CalendarNotificationManager.class);
		NotificationsManager nfm = NotificationsManager.getInstance();
		CourseGroupManager courseGroupManager = PersistingCourseGroupManager.getInstance(res);
		List<BusinessGroup> learningGroups = courseGroupManager.getAllBusinessGroups();
		//all learning and right group calendars
		for (BusinessGroup bg : learningGroups) {
			KalendarRenderWrapper calRenderWrapper = calMan.getGroupCalendar(bg);
			SubscriptionContext subsContext = notificationManager.getSubscriptionContext(calRenderWrapper);
			Publisher pub = nfm.getPublisher(subsContext);
			if (pub != null) {
				pub.setState(1); //int 0 is OK -> all other is not OK
			}
		}
		//the course calendar
		try {
			/**
			 * TODO:gs 2010-01-26
			 * OLAT-4947: if we do not have an repo entry we get an exception here. 
			 * This is normal in the case of courseimport and click canceling.
			 */
			KalendarRenderWrapper courseCalendar = calMan.getCalendarForDeletion(res);
			if(courseCalendar != null) {
				SubscriptionContext subContext = notificationManager.getSubscriptionContext(courseCalendar, res);
				OLATResourceable oresToDelete = OresHelper.createOLATResourceableInstance(subContext.getResName(), subContext.getResId());
				nfm.deletePublishersOf(oresToDelete);
			}
		} catch (AssertException e) {
			//if we have a broken course (e.g. canceled import or no repo entry somehow) skip calendar deletion...
		}
	}
	
	/**
	 * Copies a course. More specifically, the run and editor structures and the
	 * course folder will be copied to create a new course.
	 *  
	 * 
	 * @param sourceRes
	 * @param ureq
	 * @return copy of the course.
	 */
	public static OLATResourceable copyCourse(OLATResourceable sourceRes, UserRequest ureq) {
		
		PersistingCourseImpl sourceCourse = (PersistingCourseImpl) loadCourse(sourceRes);

		OLATResourceable targetRes = OLATResourceManager.getInstance().createOLATResourceInstance(CourseModule.class);
		PersistingCourseImpl targetCourse = new PersistingCourseImpl(targetRes.getResourceableId());
		File fTargetCourseBasePath = targetCourse.getCourseBaseContainer().getBasefile();
		
		synchronized (sourceCourse) { // o_clusterNOK - cannot be solved with doInSync since could take too long (leads to error: "Lock wait timeout exceeded")
			// copy configuration
			CourseConfig courseConf = CourseConfigManagerImpl.getInstance().copyConfigOf(sourceCourse);
			targetCourse.setCourseConfig(courseConf);
			// save structures
			targetCourse.setRunStructure((Structure) XStreamHelper.xstreamClone(sourceCourse.getRunStructure()));
			targetCourse.saveRunStructure();
			targetCourse.setEditorTreeModel((CourseEditorTreeModel) XStreamHelper.xstreamClone(sourceCourse.getEditorTreeModel()));
			targetCourse.saveEditorTreeModel();
			
			Codepoint.codepoint(CourseFactory.class, "copyCourseAfterSaveTreeModel");

			// copy course folder
			File fSourceCourseFolder = sourceCourse.getIsolatedCourseFolder().getBasefile();
			if (fSourceCourseFolder.exists()) FileUtils.copyDirToDir(fSourceCourseFolder, fTargetCourseBasePath, false, "copy course folder");
			
			// copy folder nodes directories
			File fSourceFoldernodesFolder = new File(FolderConfig.getCanonicalRoot()
					+ BCCourseNode.getFoldernodesPathRelToFolderBase(sourceCourse.getCourseEnvironment()));
			if (fSourceFoldernodesFolder.exists()) FileUtils.copyDirToDir(fSourceFoldernodesFolder, fTargetCourseBasePath, false, "copy folder nodes directories");

			// copy task folder directories
			File fSourceTaskfoldernodesFolder = new File(FolderConfig.getCanonicalRoot()
					+ TACourseNode.getTaskFoldersPathRelToFolderRoot(sourceCourse.getCourseEnvironment()));
			if (fSourceTaskfoldernodesFolder.exists()) FileUtils.copyDirToDir(fSourceTaskfoldernodesFolder, fTargetCourseBasePath, false, "copy task folder directories");

			//make sure the DB connection is available after this point
			DBFactory.getInstance(false).commitAndCloseSession();
			
			// update references
			List<ReferenceImpl> refs = referenceManager.getReferences(sourceCourse);
			int count = 0;
			for (ReferenceImpl ref: refs) {
				referenceManager.addReference(targetCourse, ref.getTarget(), ref.getUserdata());
				if(count % 20 == 0) {
					DBFactory.getInstance(false).intermediateCommit();
				}
			}
			
			// set quotas
			Quota sourceQuota = VFSManager.isTopLevelQuotaContainer(sourceCourse.getCourseFolderContainer());
			Quota targetQuota = VFSManager.isTopLevelQuotaContainer(targetCourse.getCourseFolderContainer());
			if (sourceQuota != null && targetQuota != null) {
				QuotaManager qm = QuotaManager.getInstance();
				if (sourceQuota.getQuotaKB() != qm.getDefaultQuota(QuotaConstants.IDENTIFIER_DEFAULT_COURSE).getQuotaKB()) {
					targetQuota = qm.createQuota(targetQuota.getPath(), sourceQuota.getQuotaKB(), sourceQuota.getUlLimitKB());
					qm.setCustomQuotaKB(targetQuota);
				}
			}
		}
		return targetRes;			
	}

	/**
	 * Exports an entire course to a zip file. 
	 * 
	 * @param sourceRes
	 * @param fTargetZIP
	 * @return true if successfully exported, false otherwise.
	 */
	public static void exportCourseToZIP(OLATResourceable sourceRes, File fTargetZIP, boolean runtimeDatas, boolean backwardsCompatible) {
		PersistingCourseImpl sourceCourse = (PersistingCourseImpl) loadCourse(sourceRes);

		// add files to ZIP
		File fExportDir = new File(WebappHelper.getTmpDir(), CodeHelper.getUniqueID());
		fExportDir.mkdirs();
		log.info("Export folder: " + fExportDir);
		synchronized (sourceCourse) { //o_clusterNOK - cannot be solved with doInSync since could take too long (leads to error: "Lock wait timeout exceeded")
			OLATResource courseResource = sourceCourse.getCourseEnvironment().getCourseGroupManager().getCourseResource();
			sourceCourse.exportToFilesystem(courseResource, fExportDir, runtimeDatas, backwardsCompatible);
			Set<String> fileSet = new HashSet<String>();
			String[] files = fExportDir.list();
			for (int i = 0; i < files.length; i++) {
				fileSet.add(files[i]);
			}
			ZipUtil.zip(fileSet, fExportDir, fTargetZIP, false);
			log.info("Delete export folder: " + fExportDir);
			FileUtils.deleteDirsAndFiles(fExportDir, true, true);
		}
	}

	/**
	 * Import a course from a ZIP file.
	 * 
	 * @param ores
	 * @param zipFile
	 * @return New Course.
	 */
	public static ICourse importCourseFromZip(OLATResourceable ores, File zipFile) {
		// Generate course with filesystem
		PersistingCourseImpl newCourse = new PersistingCourseImpl(ores.getResourceableId());
		CourseConfigManagerImpl.getInstance().deleteConfigOf(newCourse);
		
		// Unzip course strucure in new course
		File fCanonicalCourseBasePath = newCourse.getCourseBaseContainer().getBasefile();
		if (ZipUtil.unzip(zipFile, fCanonicalCourseBasePath)) {
			// Load course strucure now
			try {
				newCourse.load();
				CourseConfig cc = CourseConfigManagerImpl.getInstance().loadConfigFor(newCourse);								
				//newCourse is not in cache yet, so we cannot call setCourseConfig()
				newCourse.setCourseConfig(cc);
				putCourseInCache(newCourse.getResourceableId(), newCourse);						
				return newCourse;
			} catch (AssertException ae) {
				// ok failed, cleanup below
				// better logging to search error
				log.error("rollback importCourseFromZip",ae);
			}
		}
		// cleanup if not successfull
		FileUtils.deleteDirsAndFiles(fCanonicalCourseBasePath, true, true);
		return null;
	}

	/**
	 * Deploys a course from an exported course ZIP file. This process is unatended and
	 * therefore relies on some default assumptions on how to setup the entry and add
	 * any referenced resources to the repository.
	 * 
	 * @param exportedCourseZIPFile
	 */
	public static RepositoryEntry deployCourseFromZIP(File exportedCourseZIPFile, int access) {
		return deployCourseFromZIP(exportedCourseZIPFile, "administrator", null, access);
	}
	
	public static RepositoryEntry deployCourseFromZIP(File exportedCourseZIPFile, String initialAuthor, String softKey, int access) {
		// create the course instance
		OLATResource newCourseResource = olatResourceManager.createOLATResourceInstance(CourseModule.class);
		ICourse course = CourseFactory.importCourseFromZip(newCourseResource, exportedCourseZIPFile);
		// course is now also in course cache!
		if (course == null) {
			log.error("Error deploying course from ZIP: " + exportedCourseZIPFile.getAbsolutePath());
			return null;
		}
		File courseExportData = course.getCourseExportDataDir().getBasefile();
		// get the export data directory
		// create the repository entry
		RepositoryEntry re = repositoryManager.createRepositoryEntryInstance("administrator");
		RepositoryEntryImportExport importExport = new RepositoryEntryImportExport(courseExportData);
		if(!StringHelper.containsNonWhitespace(softKey)) {
			softKey = importExport.getSoftkey();
		}
		RepositoryEntry existingEntry = repositoryManager.lookupRepositoryEntryBySoftkey(softKey, false);
		if (existingEntry != null) {
			log.info("RepositoryEntry with softkey " + softKey + " already exists. Course will not be deployed.");
			//seem to be a problem
			UserCourseInformationsManager userCourseInformationsManager = CoreSpringFactory.getImpl(UserCourseInformationsManager.class);
			userCourseInformationsManager.deleteUserCourseInformations(existingEntry);
			CourseFactory.deleteCourse(newCourseResource);
			return existingEntry;
		}
		// ok, continue import
		newCourseResource = olatResourceManager.findOrPersistResourceable(newCourseResource);
		re.setOlatResource(newCourseResource);
		re.setSoftkey(softKey);
		re.setInitialAuthor(importExport.getInitialAuthor());
		re.setDisplayname(importExport.getDisplayName());
		re.setResourcename(importExport.getResourceName());
		re.setDescription(importExport.getDescription());
		re.setCanLaunch(true);
		
		// set access configuration
		re.setAccess(access);

		// create security group
		SecurityGroup ownerGroup = securityManager.createAndPersistSecurityGroup();
		// member of this group may modify member's membership
		securityManager.createAndPersistPolicy(ownerGroup, Constants.PERMISSION_ACCESS, ownerGroup);
		// members of this group are always authors also
		securityManager.createAndPersistPolicy(ownerGroup, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_AUTHOR);
		securityManager.addIdentityToSecurityGroup(securityManager.findIdentityByName("administrator"), ownerGroup);
		re.setOwnerGroup(ownerGroup);
		// save the repository entry
		repositoryManager.saveRepositoryEntry(re);
		// Create course admin policy for owner group of repository entry
		// -> All owners of repository entries are course admins
		securityManager.createAndPersistPolicy(re.getOwnerGroup(), Constants.PERMISSION_ADMIN, re.getOlatResource());
		
		//fxdiff VCRP-1,2: access control of resources
		// create security group for tutors / coaches
		SecurityGroup tutorGroup = securityManager.createAndPersistSecurityGroup();
		// member of this group may modify member's membership
		securityManager.createAndPersistPolicy(tutorGroup, Constants.PERMISSION_ACCESS, re.getOlatResource());
		securityManager.createAndPersistPolicy(tutorGroup, Constants.PERMISSION_COACH, re.getOlatResource());
		// members of this group are always tutors also
		securityManager.createAndPersistPolicy(tutorGroup, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_TUTOR);
		re.setTutorGroup(tutorGroup);
	
		// create security group for participants
		SecurityGroup participantGroup = securityManager.createAndPersistSecurityGroup();
		// member of this group may modify member's membership
		securityManager.createAndPersistPolicy(participantGroup, Constants.PERMISSION_ACCESS, re.getOlatResource());
		securityManager.createAndPersistPolicy(participantGroup, Constants.PERMISSION_PARTI, re.getOlatResource());
		// members of this group are always participants also
		securityManager.createAndPersistPolicy(participantGroup, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_PARTICIPANT);
		re.setParticipantGroup(participantGroup);
		
		//import groups
		course = openCourseEditSession(course.getResourceableId());
		// create group management
		CourseGroupManager cgm = course.getCourseEnvironment().getCourseGroupManager();
		// import groups
		cgm.importCourseBusinessGroups(courseExportData);
		
		// deploy any referenced repository entries of the editor structure. This will also
		// include any references in the run structure, since any node in the runstructure is also
		// present in the editor structure.
		deployReferencedRepositoryEntries(courseExportData, course,
				(CourseEditorTreeNode)course.getEditorTreeModel().getRootNode());
		// register any references in the run structure. The referenced entries have been 
		// previousely deplyed (as part of the editor structure deployment process - see above method call)
		registerReferences(course, course.getRunStructure().getRootNode());
		// import shared folder references
		deployReferencedSharedFolders(courseExportData, course);
		// import glossary references
		deployReferencedGlossary(courseExportData, course);
		closeCourseEditSession(course.getResourceableId(), true);
		// cleanup export data
		FileUtils.deleteDirsAndFiles(courseExportData, true, true);
		log.info("Successfully deployed course " + re.getDisplayname() + " from ZIP: " + exportedCourseZIPFile.getAbsolutePath());
		return re;
	}
	
	
	/**
	 * Unattended deploy any referenced repository entries.
	 * 
	 * @param importDirectory
	 * @param course
	 * @param currentNode
	 */
	private static void deployReferencedRepositoryEntries(File importDirectory, ICourse course, CourseEditorTreeNode currentNode) {
		for (int i = 0; i < currentNode.getChildCount(); i++) {
			CourseEditorTreeNode childNode = (CourseEditorTreeNode)currentNode.getChildAt(i);
			childNode.getCourseNode().importNode(importDirectory, course, true, null, null);
			deployReferencedRepositoryEntries(importDirectory, course, childNode);
		}
	}
	
	/**
	 * Register any referenced repository entries.
	 * @param course
	 * @param currentNode
	 */
	private static void registerReferences(ICourse course, CourseNode currentNode) {
		for (int i = 0; i < currentNode.getChildCount(); i++) {
			CourseNode childNode = (CourseNode)currentNode.getChildAt(i);
			if (childNode.needsReferenceToARepositoryEntry()) {
				referenceManager.addReference(course,
					childNode.getReferencedRepositoryEntry().getOlatResource(), childNode.getIdent());
			}
			registerReferences(course, childNode);
		}
	}
	
	private static void deployReferencedSharedFolders(File importDirectory, ICourse course) {
		CourseConfig cc = course.getCourseEnvironment().getCourseConfig();
		if (!cc.hasCustomSharedFolder()) return;
		RepositoryEntryImportExport importExport = SharedFolderManager.getInstance()
			.getRepositoryImportExport(importDirectory);
		Identity owner = BaseSecurityManager.getInstance().findIdentityByName("administrator");
		ImportSharedfolderReferencesController.doImport(importExport, course, false, owner);
	}

	/**
	 * Deploy referenced glossaries using the administrator account as owner
	 * @param importDirectory
	 * @param course
	 */
	private static void deployReferencedGlossary(File importDirectory, ICourse course) {
		CourseConfig cc = course.getCourseEnvironment().getCourseConfig();
		if (!cc.hasGlossary()) return;
		RepositoryEntryImportExport importExport = glossaryManager.getRepositoryImportExport(importDirectory);
		Identity owner = securityManager.findIdentityByName("administrator");
		ImportGlossaryReferencesController.doImport(importExport, course, false, owner);
	}
	
	/**
	 * Publish the course with some standard options
	 * @param course
	 * @param locale
	 * @param identity
	 */
	public static void publishCourse(ICourse course, int access, boolean membersOnly, Identity identity, Locale locale) {
		 CourseEditorTreeModel cetm = course.getEditorTreeModel();
		 PublishProcess publishProcess = PublishProcess.getInstance(course, cetm, locale);
		 PublishTreeModel publishTreeModel = publishProcess.getPublishTreeModel();

		 int newAccess = (access < RepositoryEntry.ACC_OWNERS || access > RepositoryEntry.ACC_USERS_GUESTS)
				 ? RepositoryEntry.ACC_USERS : access;
		 //access rule -> all users can the see course
		 //RepositoryEntry.ACC_OWNERS
		 //only owners can the see course
		 //RepositoryEntry.ACC_OWNERS_AUTHORS //only owners and authors can the see course
		 //RepositoryEntry.ACC_USERS_GUESTS // users and guests can see the course
		 //fxdiff VCRP-1,2: access control of resources
		 publishProcess.changeGeneralAccess(null, newAccess, membersOnly);
		 
		 if (publishTreeModel.hasPublishableChanges()) {
			 List<String>nodeToPublish = new ArrayList<String>();
			 visitPublishModel(publishTreeModel.getRootNode(), publishTreeModel, nodeToPublish);

			 publishProcess.createPublishSetFor(nodeToPublish);
			 PublishSetInformations set = publishProcess.testPublishSet(locale);
			 StatusDescription[] status = set.getWarnings();
			 //publish not possible when there are errors
			 for(int i = 0; i < status.length; i++) {
				 if(status[i].isError()) {
					 log.error("Status error by publish: " + status[i].getLongDescription(locale));
					 return;
				 }
			 }
			 
			 try {
				 course = CourseFactory.openCourseEditSession(course.getResourceableId());
				 publishProcess.applyPublishSet(identity, locale);
			 } catch(Exception e) {
				 log.error("",  e);
			 } finally {
				 closeCourseEditSession(course.getResourceableId(), true);
			 }
		 }
	}
	
	/**
	 * Get a details form for a given course resourceable
	 * 
	 * @param res
	 * @param ureq
	 * @return details component displaying details of the course.
	 */
	public static Controller getDetailsForm(UserRequest ureq, WindowControl wControl, OLATResourceable res) {
		// course does not provide a details component, this is somehow hardcoded in the
		// RepositoryDetailsController
		return null;
	}

	/**
	 * Create a user locale dependent help-course run controller
	 * 
	 * @param ureq The user request
	 * @param wControl The current window controller
	 * @return The help-course run controller
	 */
	public static Controller createHelpCourseLaunchController(UserRequest ureq, WindowControl wControl) {
		// Find repository entry for this course
		String helpCourseSoftKey = CourseModule.getHelpCourseSoftKey();
		RepositoryManager rm = RepositoryManager.getInstance();
		RepositoryEntry entry = null;
		if (StringHelper.containsNonWhitespace(helpCourseSoftKey)) {
			entry = rm.lookupRepositoryEntryBySoftkey(helpCourseSoftKey, false);
		}
		if (entry == null) {
			Translator translator = Util.createPackageTranslator(CourseFactory.class, ureq.getLocale());
			wControl.setError(translator.translate("error.helpcourse.not.configured"));
			// create empty main controller
			LayoutMain3ColsController emptyCtr = new LayoutMain3ColsController(ureq, wControl, null, null, null, null);
			return emptyCtr;
		} else {
			// Increment launch counter
			rm.incrementLaunchCounter(entry);
			OLATResource ores = entry.getOlatResource();
			ICourse course = loadCourse(ores);
			
			ContextEntry ce = BusinessControlFactory.getInstance().createContextEntry(entry);
			WindowControl bwControl = BusinessControlFactory.getInstance().createBusinessWindowControl(ce, wControl);	
			
			RunMainController launchC = new RunMainController(ureq, bwControl, course, entry, false, false);
			return launchC;			
		}		
	}

	/**
	 * visit all nodes in the specified course and make them archiving any data
	 * into the identity's export directory.
	 * 
	 * @param res
	 * @param charset
	 * @param locale
	 * @param identity
	 */
	public static void archiveCourse(OLATResourceable res, String charset, Locale locale, Identity identity) {
		RepositoryEntry courseRe = RepositoryManager.getInstance().lookupRepositoryEntry(res, false);
		PersistingCourseImpl course = (PersistingCourseImpl) loadCourse(res);
		File exportDirectory = CourseFactory.getOrCreateDataExportDirectory(identity, course.getCourseTitle());
		boolean isOLATAdmin = BaseSecurityManager.getInstance().isIdentityPermittedOnResourceable(identity, Constants.PERMISSION_HASROLE, Constants.ORESOURCE_ADMIN);
		boolean isOresOwner = RepositoryManager.getInstance().isOwnerOfRepositoryEntry(identity, courseRe);
		boolean isOresInstitutionalManager = RepositoryManager.getInstance().isInstitutionalRessourceManagerFor(courseRe, identity);
		archiveCourse(identity, course, charset, locale, exportDirectory, isOLATAdmin, isOresOwner, isOresInstitutionalManager);
	}
		
	/**
	 * visit all nodes in the specified course and make them archiving any data
	 * into the identity's export directory.
	 * 
	 * @param res
	 * @param charset
	 * @param locale
	 * @param identity
	 */
	public static void archiveCourse(Identity archiveOnBehalfOf, ICourse course, String charset, Locale locale, File exportDirectory, boolean isOLATAdmin, boolean... oresRights) {
		// archive course results overview
		List<Identity> users = ScoreAccountingHelper.loadUsers(course.getCourseEnvironment());
		List<AssessableCourseNode> nodes = ScoreAccountingHelper.loadAssessableNodes(course.getCourseEnvironment());
		
		String result = ScoreAccountingHelper.createCourseResultsOverviewTable(users, nodes, course, locale);
		String fileName = ExportUtil.createFileNameWithTimeStamp(course.getCourseTitle(), "xls");
		ExportUtil.writeContentToFile(fileName, result, exportDirectory, charset);
		
		// archive all nodes content
		Visitor archiveV = new NodeArchiveVisitor(locale, course, exportDirectory, charset);
		TreeVisitor tv = new TreeVisitor(archiveV, course.getRunStructure().getRootNode(), true);
		tv.visitAll();
		// archive all course log files
		//OLATadmin gets all logfiles independent of the visibility configuration		
		boolean isOresOwner = (oresRights.length > 0)?oresRights[0]:false;
		boolean isOresInstitutionalManager = (oresRights.length > 1)?oresRights[1]:false;
		
		boolean aLogV = isOresOwner || isOresInstitutionalManager || isOLATAdmin;
		boolean uLogV = isOLATAdmin;
		boolean sLogV = isOresOwner || isOresInstitutionalManager || isOLATAdmin;
		
		// make an intermediate commit here to make sure long running course log export doesn't 
		// cause db connection timeout to be triggered
		//@TODO transactions/backgroundjob:
		// rework when backgroundjob infrastructure exists
		DBFactory.getInstance(false).intermediateCommit();
		AsyncExportManager.getInstance().asyncArchiveCourseLogFiles(archiveOnBehalfOf, new Runnable() {
			public void run() {
				// that's fine, I dont need to do anything here
			};
		}, course.getResourceableId(), exportDirectory.getPath(), null, null, aLogV, uLogV, sLogV, charset, null, null);

		PersistingCourseGroupManager.getInstance(course).archiveCourseGroups(exportDirectory);
		
		CoreSpringFactory.getImpl(ChatLogHelper.class).archive(course, exportDirectory);
		
	}

	/**
	 * Returns the data export directory. If the directory does not yet exist the
	 * directory will be created
	 * 
	 * @param ureq The user request
	 * @param courseName The course name or title. Will be used as directory name
	 * @return The file representing the dat export directory
	 */
	public static File getOrCreateDataExportDirectory(Identity identity, String courseName) {
		String courseFolder = StringHelper.transformDisplayNameToFileSystemName(courseName);
		// folder where exported user data should be put
		File exportFolder = new File(FolderConfig.getCanonicalRoot() + FolderConfig.getUserHomes() + "/" + identity.getName() + "/private/archive/"
						+ courseFolder);
		if (exportFolder.exists()) {
			if (!exportFolder.isDirectory()) { throw new OLATRuntimeException(ExportUtil.class, "File " + exportFolder.getAbsolutePath()
					+ " already exists but it is not a folder!", null); }
		} else {
			exportFolder.mkdirs();
		}
		return exportFolder;
	}
	
	
	/**
	 * Returns the data export directory. 
	 * 
	 * @param ureq The user request
	 * @param courseName The course name or title. Will be used as directory name
	 * @return The file representing the dat export directory
	 */
	public static File getDataExportDirectory(Identity identity, String courseName) {
		File exportFolder = new File( // folder where exported user data should be
				// put
				FolderConfig.getCanonicalRoot() + FolderConfig.getUserHomes() + "/" + identity.getName() + "/private/archive/"
						+ Formatter.makeStringFilesystemSave(courseName));
		return exportFolder;
	}
	
	/**
	 * Returns the personal folder of the given identity.
	 * <p>
	 * The idea of this method is to match the first part of what
	 * getOrCreateDataExportDirectory() returns.
	 * <p>
	 * @param identity
	 * @return
	 */
	public static File getPersonalDirectory(Identity identity) {
		if (identity==null) {
			return null;
		}
		return new File(FolderConfig.getCanonicalRoot() + FolderConfig.getUserHomes() + "/" + identity.getName());		
	}
	
	/**
	 * Returns the data export directory. If the directory does not yet exist the
	 * directory will be created
	 * 
	 * @param ureq The user request
	 * @param courseName The course name or title. Will be used as directory name
	 * @return The file representing the dat export directory
	 */
	public static File getOrCreateStatisticDirectory(Identity identity, String courseName) {
		File exportFolder = new File( // folder where exported user data should be
				// put
				FolderConfig.getCanonicalRoot() + FolderConfig.getUserHomes() + "/" + identity.getName() + "/private/statistics/"
						+ Formatter.makeStringFilesystemSave(courseName));
		if (exportFolder.exists()) {
			if (!exportFolder.isDirectory()) { throw new OLATRuntimeException(ExportUtil.class, "File " + exportFolder.getAbsolutePath()
					+ " already exists but it is not a folder!", null); }
		} else {
			exportFolder.mkdirs();
		}
		return exportFolder;
	}
	
	/**
	 * Stores the editor tree model AND the run structure (both xml files). Called at publish.
	 * @param resourceableId
	 */
	public static void saveCourse(final Long resourceableId) {
		if (resourceableId == null) throw new AssertException("No resourceable ID found.");
				
		PersistingCourseImpl theCourse = getCourseEditSession(resourceableId);
		if(theCourse!=null) {
			//o_clusterOK by: ld (although the course is locked for editing, we still have to insure that load course is synchronized)
			CoordinatorManager.getInstance().getCoordinator().getSyncer().doInSync(theCourse, new SyncerExecutor(){
				public void execute() {
					final PersistingCourseImpl course = getCourseEditSession(resourceableId);
					if(course!=null && course.isReadAndWrite()) {
		        course.initHasAssessableNodes();
		        course.saveRunStructure();
		        course.saveEditorTreeModel();
		        
		        //clear modifyCourseEvents at publish, since the updateCourseInCache is called anyway
		        modifyCourseEvents.remove(resourceableId);
		        updateCourseInCache(resourceableId, course);		        
					} else if(!course.isReadAndWrite()) {
						throw new AssertException("Cannot saveCourse because theCourse is readOnly! You have to open an courseEditSession first!");
					}
				}
			});
		} else {
			throw new AssertException("Cannot saveCourse because theCourse is null! Have you opened a courseEditSession yet?");
		}
	}
	
	/**
	 * Stores ONLY the editor tree model (e.g. at course tree editing - add/remove/move course nodes).
	 * @param resourceableId
	 */
	public static void saveCourseEditorTreeModel(Long resourceableId) {
		if (resourceableId == null) throw new AssertException("No resourceable ID found.");
				
		PersistingCourseImpl course = getCourseEditSession(resourceableId);		
		if(course!=null && course.isReadAndWrite()) {	
			synchronized(loadedCourses) { //o_clusterOK by: ld (clusterOK since the course is locked for editing)
		    course.saveEditorTreeModel();
		   
		    modifyCourseEvents.putIfAbsent(resourceableId, new ModifyCourseEvent(resourceableId));	
			}
		} else if(course==null) {
			throw new AssertException("Cannot saveCourseEditorTreeModel because course is null! Have you opened a courseEditSession yet?");
		} else if(!course.isReadAndWrite()) {
			throw new AssertException("Cannot saveCourse because theCourse is readOnly! You have to open an courseEditSession first!");
		}
	}
	
	/**
	 * Updates the course cache forcing other cluster nodes to reload this course. <br/>
	 * This is triggered after the course editor is closed. <br/>
	 * It also removes the courseEditSession for this course.
	 * 
	 * @param resourceableId
	 */
	public static void fireModifyCourseEvent(Long resourceableId) {
		ModifyCourseEvent modifyCourseEvent = modifyCourseEvents.get(resourceableId); 
		if(modifyCourseEvent!=null){
			synchronized(modifyCourseEvents) { //o_clusterOK by: ld
				modifyCourseEvent = modifyCourseEvents.remove(resourceableId);
				if(modifyCourseEvent != null) {					
					PersistingCourseImpl course = getCourseEditSession(resourceableId);
			    if(course!=null) {
			    	updateCourseInCache(resourceableId, course);			    	
			    }
				}				
			}
		}
		//close courseEditSession if not already closed
		closeCourseEditSession(resourceableId, false);
	}
	
	public static Controller createDisposedCourseRestartController(UserRequest ureq, WindowControl wControl, RepositoryEntry re) {
		return new DisposedCourseRestartController(ureq, wControl, re);
	}

	/**
	 * Create a custom css object for the course layout. This can then be set on a
	 * MainLayoutController to activate the course layout
	 * 
	 * @param usess The user session
	 * @param courseEnvironment the course environment
	 * @return The custom course css or NULL if no course css is available
	 */
	public static CustomCSS getCustomCourseCss(UserSession usess, CourseEnvironment courseEnvironment) {
		CustomCSS customCSS = null;
		CourseConfig courseConfig = courseEnvironment.getCourseConfig();
		if (courseConfig.hasCustomCourseCSS()) {
			// Notify the current tab that it should load a custom CSS
			return CourseLayoutHelper.getCustomCSS(usess, courseEnvironment);
		}
		return customCSS;
	}


	/**
	 * the provided resourceableID must belong to a ICourse.getResourceableId(), otherwise you
	 * risk to use a wrong course base container.
	 * @param resourceableId
	 * @return
	 */
	public static VFSContainer getCourseBaseContainer(Long resourceableId) {
		String relPath = "/course/" + resourceableId.longValue();
		OlatRootFolderImpl courseRootContainer = new OlatRootFolderImpl(relPath, null);
		File fBasePath = courseRootContainer.getBasefile();
		if (!fBasePath.exists())
			throw new OLATRuntimeException(PersistingCourseImpl.class, "Could not resolve course base path:" + courseRootContainer, null);
		return courseRootContainer;
	}
	
	/**
	 * Save courseConfig and update cache.
	 * @param resourceableId
	 * @param cc
	 */
	public static void setCourseConfig(final Long resourceableId, final CourseConfig cc) {
		if (resourceableId == null) throw new AssertException("No resourceable ID found.");
		
		PersistingCourseImpl theCourse = getCourseEditSession(resourceableId);
		if(theCourse!=null) {
			//o_clusterOK by: ld (although the course is locked for editing, we still have to insure that load course is synchronized)
			CoordinatorManager.getInstance().getCoordinator().getSyncer().doInSync(theCourse, new SyncerExecutor(){
				public void execute() {
					PersistingCourseImpl course = getCourseEditSession(resourceableId);
					if(course!=null) {
					  course.setCourseConfig(cc);
		    	    
		        updateCourseInCache(resourceableId, course);
					}
				}
			});
		} else {
			throw new AssertException("Cannot setCourseConfig because theCourse is null! Have you opened a courseEditSession yet?");
		}	
	}
	
	/**
	 * Loads the course or gets it from cache, and adds it to the courseEditSessionMap. <br/>
	 * It guarantees that the returned value is never null. <br/>
	 * The courseEditSession object should live between acquire course lock and release course lock.
	 * 
	 * TODO: remove course from courseEditSessionMap at close course editor
	 * @param resourceableId
	 * @return
	 */
	public static PersistingCourseImpl openCourseEditSession(Long resourceableId) {
		PersistingCourseImpl course = courseEditSessionMap.get(resourceableId);
		if(course!=null) {
			//TODO :LD: check this out! - here we might have a valid session if the course was just created/imported/copied
			throw new AssertException("There is already an edit session open for this course: " + resourceableId);
		} else if(course==null) {
			course = (PersistingCourseImpl)loadCourse(resourceableId);
			course.setReadAndWrite(true);
			courseEditSessionMap.put(resourceableId, course);
			log.debug("getCourseEditSession - put course in courseEditSessionMap: " + resourceableId);
		}	
		return course;
	}
	
	/**
	 * Provides the currently edited course object with this id. <br/>
	 * It guarantees that the returned value is never null if the openCourseEditSession was called first. <br/>
	 * The CourseEditSession object should live between acquire course lock and release course lock.
	 * 
	 * TODO: remove course from courseEditSessionMap at close course editor
	 * @param resourceableId
	 * @return
	 */
	public static PersistingCourseImpl getCourseEditSession(Long resourceableId) {
		//TODO: this must be called after the course lock is acquired, else must by synchronized (doInSync)
		PersistingCourseImpl course = courseEditSessionMap.get(resourceableId);
		if(course==null) {
			throw new AssertException("No edit session open for this course: " + resourceableId + " - Open a session first!");
		}	
		return course;
	}
	
	/**
	 * TODO: remove course from courseEditSessionMap at releaseLock
	 * @param resourceableId
	 */
	public static void closeCourseEditSession(Long resourceableId, boolean checkIfAnyAvailable) {
		PersistingCourseImpl course = courseEditSessionMap.get(resourceableId);
		if(course==null && checkIfAnyAvailable) {
			throw new AssertException("No edit session open for this course: " + resourceableId + " - There is nothing to be closed!");
		}	else if (course!=null) {
		  course.setReadAndWrite(false);
		  courseEditSessionMap.remove(resourceableId);
		  log.debug("removeCourseEditSession for course: " + resourceableId);
		}
	}
	
	private static void visitPublishModel(TreeNode node, PublishTreeModel publishTreeModel, Collection<String> nodeToPublish) {
		int numOfChildren = node.getChildCount();
		for (int i = 0; i < numOfChildren; i++) {
			INode child = node.getChildAt(i);
			if (child instanceof TreeNode && publishTreeModel.isVisible(child)) {
				nodeToPublish.add(child.getIdent());
				visitPublishModel((TreeNode)child, publishTreeModel, nodeToPublish);
			}
		}
	}
	
	private static class NodeArchiveVisitor implements Visitor {
		private File exportPath;
		private Locale locale;
		private ICourse course;
		private String charset;

		/**
		 * @param locale
		 * @param course
		 * @param exportPath
		 * @param charset
		 */
		public NodeArchiveVisitor(Locale locale, ICourse course, File exportPath, String charset) {
			this.locale = locale;
			this.exportPath = exportPath;
			//o_clusterOk by guido: save to hold reference to course inside editor
			this.course = course;
			this.charset = charset;
		}

		/**
		 * @see org.olat.core.util.tree.Visitor#visit(org.olat.core.util.nodes.INode)
		 */
		public void visit(INode node) {
			CourseNode cn = (CourseNode) node;

			String archiveName = cn.getType() + "_"
					+ StringHelper.transformDisplayNameToFileSystemName(cn.getShortName())
					+ "_" + Formatter.formatDatetimeFilesystemSave(new Date(System.currentTimeMillis()));
			
			FileOutputStream fileStream = null;
			ZipOutputStream exportStream = null;
			try {
				File exportFile = new File(exportPath, archiveName);
				fileStream = new FileOutputStream(exportFile);
				exportStream = new ZipOutputStream(fileStream);
				cn.archiveNodeData(locale, course, null, exportStream, charset);
			} catch (FileNotFoundException e) {
				log.error("", e);
			} finally {
				IOUtils.closeQuietly(exportStream);
				IOUtils.closeQuietly(fileStream);
			}
		}
	}
	
	private static class NodeDeletionVisitor implements Visitor {

		private ICourse course;

		/**
		 * Constructor of the node deletion visitor
		 * 
		 * @param course
		 */
		public NodeDeletionVisitor(ICourse course) {
			this.course = course;
		}

		/**
		 * Visitor pattern to delete the course nodes
		 * 
		 * @see org.olat.core.util.tree.Visitor#visit(org.olat.core.util.nodes.INode)
		 */
		public void visit(INode node) {
			CourseNode cNode = (CourseNode) node;
			cNode.cleanupOnDelete(course);
		}
	}
}

/**
 * 
 * Description:<br>
 * Event triggered if a course was edited - namely the course tree model have changed 
 * (e.g. nodes added, deleted)
 * 
 * <P>
 * Initial Date:  22.07.2008 <br>
 * @author Lavinia Dumitrescu
 */
class ModifyCourseEvent extends MultiUserEvent {
	private static final long serialVersionUID = -2940724437608086461L;
	private final Long courseId;
	/**
	 * @param command
	 */
	public ModifyCourseEvent(Long resourceableId) {
		super("modify_course");
		courseId = resourceableId;		
	}
	
	public Long getCourseId() {
		return courseId;
	}
}