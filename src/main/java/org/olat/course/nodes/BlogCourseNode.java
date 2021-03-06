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
package org.olat.course.nodes;

import java.io.File;
import java.util.List;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.stack.StackedController;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.tabbable.TabbableController;
import org.olat.core.id.context.BusinessControlFactory;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.logging.activity.ThreadLocalUserActivityLogger;
import org.olat.core.util.Util;
import org.olat.core.util.ValidationStatus;
import org.olat.course.ICourse;
import org.olat.course.editor.CourseEditorEnv;
import org.olat.course.editor.NodeEditController;
import org.olat.course.editor.StatusDescription;
import org.olat.course.nodes.feed.FeedNodeEditController;
import org.olat.course.nodes.feed.FeedNodeSecurityCallback;
import org.olat.course.nodes.feed.FeedPeekviewController;
import org.olat.course.nodes.feed.blog.BlogNodeEditController;
import org.olat.course.repository.ImportReferencesController;
import org.olat.course.run.navigation.NodeRunConstructionResult;
import org.olat.course.run.userview.NodeEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.modules.webFeed.FeedSecurityCallback;
import org.olat.modules.webFeed.managers.FeedManager;
import org.olat.modules.webFeed.ui.FeedMainController;
import org.olat.modules.webFeed.ui.FeedUIFactory;
import org.olat.modules.webFeed.ui.blog.BlogUIFactory;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.util.logging.activity.LoggingResourceable;

/**
 * The blog course node.
 * 
 * <P>
 * Initial Date: Mar 30, 2009 <br>
 * 
 * @author gwassmann
 */
public class BlogCourseNode extends AbstractFeedCourseNode {
	public static final String TYPE = FeedManager.KIND_BLOG;

	/**
	 * @param type
	 */
	public BlogCourseNode() {
		super(TYPE);
		updateModuleConfigDefaults(true);
	}

	/**
	 * @see org.olat.course.nodes.AbstractAccessableCourseNode#createEditController(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl, org.olat.course.ICourse,
	 *      org.olat.course.run.userview.UserCourseEnvironment)
	 */
	@Override
	public TabbableController createEditController(UserRequest ureq, WindowControl wControl,  StackedController stackPanel, ICourse course, UserCourseEnvironment euce) {
		CourseNode chosenNode = course.getEditorTreeModel().getCourseNode(euce.getCourseEditorEnv().getCurrentCourseNodeId());
		TabbableController blogChildController = new BlogNodeEditController(this, course, euce, ureq, wControl);
		return new NodeEditController(ureq, wControl, course.getEditorTreeModel(), course, chosenNode, course.getCourseEnvironment()
				.getCourseGroupManager(), euce, blogChildController);
	}

	/**
	 * @see org.olat.course.nodes.AbstractAccessableCourseNode#createNodeRunConstructionResult(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl,
	 *      org.olat.course.run.userview.UserCourseEnvironment,
	 *      org.olat.course.run.userview.NodeEvaluation, java.lang.String)
	 */
	@Override
	public NodeRunConstructionResult createNodeRunConstructionResult(UserRequest ureq, WindowControl wControl,
			UserCourseEnvironment userCourseEnv, NodeEvaluation ne, String nodecmd) {
		RepositoryEntry entry = getReferencedRepositoryEntry();
		// create business path courseID:nodeID
		// userCourseEnv.getCourseEnvironment().getCourseResourceableId();
		// getIdent();
		Long courseId = userCourseEnv.getCourseEnvironment().getCourseResourceableId();
		String nodeId = this.getIdent();
		boolean isAdmin = ureq.getUserSession().getRoles().isOLATAdmin();
		boolean isGuest = ureq.getUserSession().getRoles().isGuestOnly();
		//fxdiff BAKS-18
		boolean isOwner = RepositoryManager.getInstance().isOwnerOfRepositoryEntry(ureq.getIdentity(), entry);
		FeedSecurityCallback callback = new FeedNodeSecurityCallback(ne, isAdmin, isOwner, isGuest);
		ThreadLocalUserActivityLogger.addLoggingResourceInfo(LoggingResourceable.wrap(this));
		FeedMainController blogCtr = BlogUIFactory.getInstance(ureq.getLocale()).createMainController(entry.getOlatResource(), ureq, wControl, callback,
				courseId, nodeId);
		List<ContextEntry> entries = BusinessControlFactory.getInstance().createCEListFromResourceType(nodecmd);
		blogCtr.activate(ureq, entries, null);
		Controller wrapperCtrl = TitledWrapperHelper.getWrapper(ureq, wControl, blogCtr, this, "o_blog_icon");
		NodeRunConstructionResult result = new NodeRunConstructionResult(wrapperCtrl);
		return result;

	}

	/**
	 * @see org.olat.course.nodes.GenericCourseNode#createPeekViewRunController(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl,
	 *      org.olat.course.run.userview.UserCourseEnvironment,
	 *      org.olat.course.run.userview.NodeEvaluation)
	 */
	@Override
	public Controller createPeekViewRunController(UserRequest ureq, WindowControl wControl, UserCourseEnvironment userCourseEnv,
			NodeEvaluation ne) {
		if (ne.isAtLeastOneAccessible()) {
			// Create a feed peekview controller that shows the latest two entries
			RepositoryEntry entry = getReferencedRepositoryEntry();
			Long courseId = userCourseEnv.getCourseEnvironment().getCourseResourceableId();
			String nodeId = this.getIdent();
			boolean isAdmin = ureq.getUserSession().getRoles().isOLATAdmin();
			boolean isGuest = ureq.getUserSession().getRoles().isGuestOnly();
			//fxdiff BAKS-18
			boolean isOwner = RepositoryManager.getInstance().isOwnerOfRepositoryEntry(ureq.getIdentity(), entry);
			FeedSecurityCallback callback = new FeedNodeSecurityCallback(ne, isAdmin, isOwner, isGuest);
			FeedUIFactory uiFactory = BlogUIFactory.getInstance(ureq.getLocale());
			Controller peekViewController = new FeedPeekviewController(entry.getOlatResource(), ureq, wControl, callback, courseId, nodeId, uiFactory, 2, "o_blog_peekview");
			return peekViewController;
		} else {
			// use standard peekview
			return super.createPeekViewRunController(ureq, wControl, userCourseEnv, ne);
		}
	}

	@Override
	protected String getDefaultTitleOption() {
		return CourseNode.DISPLAY_OPTS_CONTENT;
	}

	/**
	 * @see org.olat.course.nodes.GenericCourseNode#isConfigValid(org.olat.course.editor.CourseEditorEnv)
	 */
	@Override
	public StatusDescription[] isConfigValid(CourseEditorEnv cev) {
		oneClickStatusCache = null;
		String translatorStr = Util.getPackageName(BlogNodeEditController.class);
		List<StatusDescription> sds = isConfigValidWithTranslator(cev, translatorStr, getConditionExpressions());
		oneClickStatusCache = StatusDescriptionHelper.sort(sds);
		return oneClickStatusCache;
	}

	/**
	 * @see org.olat.course.nodes.CourseNode#isConfigValid()
	 */
	public StatusDescription isConfigValid() {
		if (oneClickStatusCache != null) { return oneClickStatusCache[0]; }

		StatusDescription status = StatusDescription.NOERROR;
		boolean invalid = config.get(CONFIG_KEY_REPOSITORY_SOFTKEY) == null;
		if (invalid) {
			String[] params = new String[] { this.getShortTitle() };
			String shortKey = "error.no.reference.short";
			String longKey = "error.no.reference.long";
			String translationPackage = Util.getPackageName(BlogNodeEditController.class);
			status = new StatusDescription(ValidationStatus.ERROR, shortKey, longKey, params, translationPackage);
			status.setDescriptionForUnit(getIdent());
			// Set which pane is affected by error
			status.setActivateableViewIdentifier(FeedNodeEditController.PANE_TAB_FEED);
		}
		return status;
	}

	/**
	 * @see org.olat.course.nodes.GenericCourseNode#importNode(java.io.File,
	 *      org.olat.course.ICourse, boolean, org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl)
	 */
	public Controller importNode(File importDirectory, ICourse course, boolean unattendedImport, UserRequest ureq, WindowControl wControl) {
		return super.importNode(importDirectory, course, unattendedImport, ureq, wControl, ImportReferencesController.IMPORT_BLOG);
	}
}
