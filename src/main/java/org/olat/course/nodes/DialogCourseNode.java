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

package org.olat.course.nodes;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.olat.core.commons.modules.bc.vfs.OlatRootFolderImpl;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.tabbable.TabbableController;
import org.olat.core.util.Util;
import org.olat.core.util.notifications.NotificationsManager;
import org.olat.core.util.notifications.SubscriptionContext;
import org.olat.core.util.vfs.LocalFolderImpl;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSItem;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.core.util.vfs.callbacks.FullAccessCallback;
import org.olat.core.util.vfs.filters.VFSLeafFilter;
import org.olat.course.CourseModule;
import org.olat.course.ICourse;
import org.olat.course.condition.Condition;
import org.olat.course.condition.interpreter.ConditionInterpreter;
import org.olat.course.editor.CourseEditorEnv;
import org.olat.course.editor.NodeEditController;
import org.olat.course.editor.StatusDescription;
import org.olat.course.nodes.dialog.DialogConfigForm;
import org.olat.course.nodes.dialog.DialogCourseNodeEditController;
import org.olat.course.nodes.dialog.DialogCourseNodeRunController;
import org.olat.course.run.navigation.NodeRunConstructionResult;
import org.olat.course.run.userview.NodeEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.modules.ModuleConfiguration;
import org.olat.modules.dialog.DialogElement;
import org.olat.modules.dialog.DialogElementsPropertyManager;
import org.olat.modules.dialog.DialogPropertyElements;
import org.olat.modules.fo.ForumManager;
import org.olat.modules.fo.archiver.ForumArchiveManager;
import org.olat.modules.fo.archiver.formatters.ForumFormatter;
import org.olat.modules.fo.archiver.formatters.ForumRTFFormatter;
import org.olat.repository.RepositoryEntry;

/**
 * Description:<br>
 * TODO: guido Class Description for DialogCourseNode
 * <P>
 * Initial Date: 02.11.2005 <br>
 * 
 * @author Guido Schnider
 */
public class DialogCourseNode extends AbstractAccessableCourseNode {

	public static final String TYPE = "dialog";
	private Condition preConditionReader, preConditionPoster, preConditionModerator;

	public DialogCourseNode() {
		super(TYPE);
		updateModuleConfigDefaults(true);
	}

	/**
	 * @see org.olat.course.nodes.GenericCourseNode#createEditController(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl, org.olat.course.ICourse,
	 *      org.olat.course.run.userview.UserCourseEnvironment)
	 */
	public TabbableController createEditController(UserRequest ureq, WindowControl wControl, ICourse course, UserCourseEnvironment euce) {
		updateModuleConfigDefaults(false);
		DialogCourseNodeEditController childTabCntrllr = new DialogCourseNodeEditController(ureq, wControl, this,
				course, euce);
		CourseNode chosenNode = course.getEditorTreeModel().getCourseNode(euce.getCourseEditorEnv().getCurrentCourseNodeId());
		return new NodeEditController(ureq, wControl, course.getEditorTreeModel(), course, chosenNode, course.getCourseEnvironment()
				.getCourseGroupManager(), euce, childTabCntrllr);
	}

	/**
	 * @see org.olat.course.nodes.GenericCourseNode#createNodeRunConstructionResult(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl,
	 *      org.olat.course.run.userview.UserCourseEnvironment,
	 *      org.olat.course.run.userview.NodeEvaluation, java.lang.String)
	 */
	public NodeRunConstructionResult createNodeRunConstructionResult(UserRequest ureq, WindowControl wControl,
			UserCourseEnvironment userCourseEnv, NodeEvaluation ne, String nodecmd) {
		//FIXME:gs:a nodecmd has now the subsubId in it -> pass to DialogCourseNodeRunController below
		DialogCourseNodeRunController ctrl = new DialogCourseNodeRunController(ureq, userCourseEnv, wControl, this, ne);
		Controller wrappedCtrl = TitledWrapperHelper.getWrapper(ureq, wControl, ctrl, this, "o_dialog_icon");
		return new NodeRunConstructionResult(wrappedCtrl);
	}

	/**
	 * @see org.olat.course.nodes.GenericCourseNode#isConfigValid(org.olat.course.editor.CourseEditorEnv)
	 */
	public StatusDescription[] isConfigValid(CourseEditorEnv cev) {
		oneClickStatusCache = null;
		// only here we know which translator to take for translating condition
		// error messages
		String translatorStr = Util.getPackageName(DialogCourseNodeEditController.class);
		List sds = isConfigValidWithTranslator(cev, translatorStr, getConditionExpressions());
		oneClickStatusCache = StatusDescriptionHelper.sort(sds);
		return oneClickStatusCache;
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#getReferencedRepositoryEntry()
	 */
	public RepositoryEntry getReferencedRepositoryEntry() {
		return null;
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#needsReferenceToARepositoryEntry()
	 */
	public boolean needsReferenceToARepositoryEntry() {
		return false;
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#isConfigValid()
	 */
	public StatusDescription isConfigValid() {
		/*
		 * first check the one click cache
		 */
		if (oneClickStatusCache != null) { return oneClickStatusCache[0]; }

		return StatusDescription.NOERROR;
	}

	/**
	 * Update the module configuration to have all mandatory configuration flags
	 * set to usefull default values
	 * 
	 * @param isNewNode true: an initial configuration is set; false: upgrading
	 *          from previous node configuration version, set default to maintain
	 *          previous behaviour
	 */
	public void updateModuleConfigDefaults(boolean isNewNode) {
		ModuleConfiguration config = getModuleConfiguration();
		if (isNewNode) {
			// use defaults for new course building blocks
			//REVIEW:pb version should go to 2 now and the handling for 1er should be to remove 
			config.setConfigurationVersion(1);
			config.set(DialogConfigForm.DIALOG_CONFIG_INTEGRATION, DialogConfigForm.CONFIG_INTEGRATION_VALUE_INLINE);
		}
	}

	public String informOnDelete(Locale locale, ICourse course) {
		return null;
	}

	/**
	 * life cycle of node data e.g properties stuff should be deleted if node gets
	 * deleted life cycle: create - delete - migrate
	 */
	public void cleanupOnDelete(ICourse course) {
		DialogElementsPropertyManager depm = DialogElementsPropertyManager.getInstance();
		
		//remove all possible forum subscriptions
		DialogPropertyElements findDialogElements = depm.findDialogElements(course.getResourceableId(), getIdent());
		if(findDialogElements != null){
			List<DialogElement> dialogElments = findDialogElements.getDialogPropertyElements();
			for (DialogElement dialogElement : dialogElments) {
				Long forumKey = dialogElement.getForumKey();
				SubscriptionContext subsContext = CourseModule.createSubscriptionContext(course.getCourseEnvironment(), this, forumKey.toString());
				NotificationsManager.getInstance().delete(subsContext);
				//also delete forum -> was archived in archiveNodeData step
				ForumManager.getInstance().deleteForum(forumKey);
			}
		}
		
		//delete property
		depm.deleteProperty(course.getResourceableId(), this.getIdent());
		
		
		
	}

	/**
	 * Generic interface implementation. May be overriden by specific node's
	 * implementation.
	 * 
	 * @see org.olat.course.nodes.CourseNode#archiveNodeData(java.util.Locale,
	 *      org.olat.course.ICourse, java.io.File)
	 */
	public boolean archiveNodeData(Locale locale, ICourse course, File exportDirectory, String charset) {
		boolean dataFound = false;
		DialogElementsPropertyManager depm = DialogElementsPropertyManager.getInstance();
		DialogPropertyElements elements = depm.findDialogElements(course.getCourseEnvironment().getCoursePropertyManager(), this);
		List list = new ArrayList();
		if (elements != null) list = elements.getDialogPropertyElements();

		for (Iterator iter = list.iterator(); iter.hasNext();) {
			DialogElement element = (DialogElement) iter.next();
			doArchiveElement(element, exportDirectory);
			//at least one element found
			dataFound = true;
		}
		return dataFound;
	}

	/**
	 * Archive a single dialog element with files and forum
	 * @param element
	 * @param exportDirectory
	 */
	public void doArchiveElement(DialogElement element, File exportDirectory) {
		VFSContainer forumContainer = getForumContainer(element.getForumKey());
		//there is only one file (leave) in the top forum container 
		VFSItem dialogFile = (VFSLeaf)forumContainer.getItems(new VFSLeafFilter()).get(0);
		VFSContainer exportContainer = new LocalFolderImpl(exportDirectory);
		
		// append export timestamp to avoid overwriting previous export 
		java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH_mm_ss_SSS");
		String exportDirName = getShortTitle()+"_"+element.getForumKey()+"_"+formatter.format(new Date(System.currentTimeMillis()));
		VFSContainer diaNodeElemExportContainer = exportContainer.createChildContainer(exportDirName);
		// don't check quota
		diaNodeElemExportContainer.setLocalSecurityCallback(new FullAccessCallback());
		diaNodeElemExportContainer.copyFrom(dialogFile);

		ForumArchiveManager fam = ForumArchiveManager.getInstance();
		ForumFormatter ff = new ForumRTFFormatter(diaNodeElemExportContainer, false);
		fam.applyFormatter(ff, element.getForumKey().longValue(), null);
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#exportNode(java.io.File,
	 *      org.olat.course.ICourse)
	 */
	public void exportNode(File exportDirectory, ICourse course) {
	// nothing to do in default implementation
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#importNode(java.io.File,
	 *      org.olat.course.ICourse, org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl)
	 */
	public Controller importNode(File importDirectory, ICourse course, boolean unattendedImport, UserRequest ureq, WindowControl wControl) {
		// nothing to do in default implementation
		return null;
	}

	protected void calcAccessAndVisibility(ConditionInterpreter ci, NodeEvaluation nodeEval) {
		// evaluate the preconditions
		boolean reader = (getPreConditionReader().getConditionExpression() == null ? true : ci.evaluateCondition(getPreConditionReader()));
		nodeEval.putAccessStatus("reader", reader);
		boolean poster = (getPreConditionPoster().getConditionExpression() == null ? true : ci.evaluateCondition(getPreConditionPoster()));
		nodeEval.putAccessStatus("poster", poster);
		boolean moderator = (getPreConditionModerator().getConditionExpression() == null ? true : ci
				.evaluateCondition(getPreConditionModerator()));
		nodeEval.putAccessStatus("moderator", moderator);

		boolean visible = (getPreConditionVisibility().getConditionExpression() == null ? true : ci
				.evaluateCondition(getPreConditionVisibility()));
		nodeEval.setVisible(visible);
	}

	/**
	 * @return Returns the preConditionModerator.
	 */
	public Condition getPreConditionModerator() {
		if (this.preConditionModerator == null) {
			this.preConditionModerator = new Condition();
			//learner should not be able to delete files by default
			this.preConditionModerator.setEasyModeCoachesAndAdmins(true);
			this.preConditionModerator.setEasyModeAlwaysAllowCoachesAndAdmins(true);
			this.preConditionModerator.setConditionExpression("(  ( isCourseCoach(0) | isCourseAdministrator(0) ) )");
		}
		this.preConditionModerator.setConditionId("moderator");
		return this.preConditionModerator;
	}

	/**
	 * @param preConditionModerator The preConditionModerator to set.
	 */
	public void setPreConditionModerator(Condition preConditionMod) {
		if (preConditionMod == null) {
			preConditionMod = getPreConditionModerator();
		}
		preConditionMod.setConditionId("moderator");
		this.preConditionModerator = preConditionMod;
	}

	/**
	 * @return Returns the preConditionPoster.
	 */
	public Condition getPreConditionPoster() {
		if (preConditionPoster == null) {
			preConditionPoster = new Condition();
		}
		preConditionPoster.setConditionId("poster");
		return preConditionPoster;
	}

	/**
	 * @param preConditionPoster The preConditionPoster to set.
	 */
	public void setPreConditionPoster(Condition preConditionPoster) {
		if (preConditionPoster == null) {
			preConditionPoster = getPreConditionPoster();
		}
		preConditionPoster.setConditionId("poster");
		this.preConditionPoster = preConditionPoster;
	}

	/**
	 * @return Returns the preConditionReader.
	 */
	public Condition getPreConditionReader() {
		if (preConditionReader == null) {
			preConditionReader = new Condition();
		}
		preConditionReader.setConditionId("reader");
		return preConditionReader;
	}

	/**
	 * @param preConditionReader The preConditionReader to set.
	 */
	public void setPreConditionReader(Condition preConditionReader) {
		if (preConditionReader == null) {
			preConditionReader = getPreConditionReader();
		}
		preConditionReader.setConditionId("reader");
		this.preConditionReader = preConditionReader;
	}

	/**
	 * to save content
	 * 
	 * @param forumKey
	 * @return
	 */
	private OlatRootFolderImpl getForumContainer(Long forumKey) {
		StringBuilder sb = new StringBuilder();
		sb.append("/forum/");
		sb.append(forumKey);
		sb.append("/");
		String pathToForumDir = sb.toString();
		OlatRootFolderImpl forumContainer = new OlatRootFolderImpl(pathToForumDir, null);
		File baseFile = forumContainer.getBasefile();
		baseFile.mkdirs();
		return forumContainer;
	}

}