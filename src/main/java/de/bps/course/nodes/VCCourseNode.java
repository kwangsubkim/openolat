// <OLATCE-103>
/**
 * 
 * BPS Bildungsportal Sachsen GmbH<br>
 * Bahnhofstrasse 6<br>
 * 09111 Chemnitz<br>
 * Germany<br>
 * 
 * Copyright (c) 2005-2010 by BPS Bildungsportal Sachsen GmbH<br>
 * http://www.bps-system.de<br>
 * 
 * All rights reserved.
 */
package de.bps.course.nodes;

import java.util.List;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.tabbable.TabbableController;
import org.olat.core.id.Roles;
import org.olat.core.util.Util;
import org.olat.core.util.ValidationStatus;
import org.olat.course.CourseFactory;
import org.olat.course.ICourse;
import org.olat.course.condition.ConditionEditController;
import org.olat.course.editor.CourseEditorEnv;
import org.olat.course.editor.NodeEditController;
import org.olat.course.editor.StatusDescription;
import org.olat.course.nodes.AbstractAccessableCourseNode;
import org.olat.course.nodes.CourseNode;
import org.olat.course.nodes.StatusDescriptionHelper;
import org.olat.course.nodes.TitledWrapperHelper;
import org.olat.course.run.navigation.NodeRunConstructionResult;
import org.olat.course.run.userview.NodeEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;

import de.bps.course.nodes.vc.VCConfiguration;
import de.bps.course.nodes.vc.VCEditController;
import de.bps.course.nodes.vc.VCRunController;
import de.bps.course.nodes.vc.provider.VCProvider;
import de.bps.course.nodes.vc.provider.VCProviderFactory;

/**
 * Description:<br>
 * date list course node.
 * 
 * <P>
 * Initial Date: 19.07.2010 <br>
 * 
 * @author Jens Lindner (jlindne4@hs-mittweida.de)
 * @author skoeber
 */
public class VCCourseNode extends AbstractAccessableCourseNode {

	private static final String TYPE = "vc";

	// configuration
	public static final String CONF_VC_CONFIGURATION = "vc_configuration";
	public final static String CONF_PROVIDER_ID = "vc_provider_id";

	public VCCourseNode() {
		super(TYPE);
	}

	/**
	 * To support different virtual classroom implementations it's necessary to
	 * check whether the persisted configuration suits to the actual virtual
	 * classroom implementation or not. If not a new one will be created and
	 * persisted.
	 * 
	 * @param provider
	 * @return the persisted configuration or a fresh one
	 */
	private VCConfiguration handleConfig(final VCProvider provider) {
		getModuleConfiguration().setStringValue(CONF_PROVIDER_ID, provider.getProviderId());
		VCConfiguration config = (VCConfiguration) getModuleConfiguration().get(CONF_VC_CONFIGURATION);
		if (config == null || config.getProviderId() == null || !config.getProviderId().equals(provider.getProviderId())) {
			config = provider.createNewConfiguration();
		}
		getModuleConfiguration().set(CONF_VC_CONFIGURATION, config);
		return config;
	}

	@Override
	public void updateModuleConfigDefaults(boolean isNewNode) {
		// no update to default config necessary
	}

	@Override
	public TabbableController createEditController(UserRequest ureq, WindowControl wControl, ICourse course,
			UserCourseEnvironment userCourseEnv) {
		updateModuleConfigDefaults(false);
		CourseNode chosenNode = course.getEditorTreeModel().getCourseNode(userCourseEnv.getCourseEditorEnv().getCurrentCourseNodeId());
		// load and check configuration
		String providerId = getModuleConfiguration().getStringValue(CONF_PROVIDER_ID);
		VCProvider provider = providerId == null ? VCProviderFactory.createDefaultProvider() : VCProviderFactory.createProvider(providerId);
		VCConfiguration config = handleConfig(provider);
		// create room if configured to do it immediately
		if(config.isCreateMeetingImmediately()) {
			Long key = course.getResourceableId();
			// here, the config is empty in any case, thus there are no start and end dates
			provider.createClassroom(key + "_" + this.getIdent(), this.getShortName(), this.getLongTitle(), null, null, config);
		}
		// create edit controller
		VCEditController childTabCntrllr = new VCEditController(ureq, wControl, this, course, userCourseEnv, provider, config);
		NodeEditController nodeEditCtr = new NodeEditController(ureq, wControl, course.getEditorTreeModel(), course, chosenNode, course.getCourseEnvironment()
				.getCourseGroupManager(), userCourseEnv, childTabCntrllr);
		nodeEditCtr.addControllerListener(childTabCntrllr);
		return nodeEditCtr;
	}

	@Override
	public NodeRunConstructionResult createNodeRunConstructionResult(UserRequest ureq, WindowControl wControl,
			UserCourseEnvironment userCourseEnv, NodeEvaluation ne, String nodecmd) {
		updateModuleConfigDefaults(false);
		// check if user is moderator of the virtual classroom
		Roles roles = ureq.getUserSession().getRoles();
		boolean moderator = roles.isOLATAdmin();
		Long key = userCourseEnv.getCourseEnvironment().getCourseResourceableId();
		if (!moderator) {
			if(roles.isInstitutionalResourceManager() | roles.isAuthor()) {
				RepositoryManager rm = RepositoryManager.getInstance();
				ICourse course = CourseFactory.loadCourse(key);
				RepositoryEntry re = rm.lookupRepositoryEntry(course, false);
				if (re != null) {
					moderator = rm.isOwnerOfRepositoryEntry(ureq.getIdentity(), re);
					if(!moderator) {
						moderator = rm.isInstitutionalRessourceManagerFor(re, ureq.getIdentity());
					}
				}
			}
		}
		// load configuration
		final String providerId = getModuleConfiguration().getStringValue(CONF_PROVIDER_ID);
		VCProvider provider = providerId == null ? VCProviderFactory.createDefaultProvider() : VCProviderFactory.createProvider(providerId);
		VCConfiguration config = handleConfig(provider);
		// create run controller
		Controller runCtr = new VCRunController(ureq, wControl, key + "_" + this.getIdent(), this.getShortName(), this.getLongTitle(), config, provider, moderator);
		Controller controller = TitledWrapperHelper.getWrapper(ureq, wControl, runCtr, this, "o_vc_icon");
		return new NodeRunConstructionResult(controller);
	}

	@Override
	public Controller createPeekViewRunController(UserRequest ureq, WindowControl wControl, UserCourseEnvironment userCourseEnv,
			NodeEvaluation ne) {
		return null;
	}

	@Override
	public StatusDescription[] isConfigValid(CourseEditorEnv cev) {
		String translatorStr = Util.getPackageName(ConditionEditController.class);
		List<StatusDescription> statusDescs = isConfigValidWithTranslator(cev, translatorStr, getConditionExpressions());
		return StatusDescriptionHelper.sort(statusDescs);
	}

	public RepositoryEntry getReferencedRepositoryEntry() {
		return null;
	}

	public StatusDescription isConfigValid() {
		if (oneClickStatusCache != null) { return oneClickStatusCache[0]; }
		StatusDescription status = StatusDescription.NOERROR;
		
		// load configuration
		final String providerId = getModuleConfiguration().getStringValue(CONF_PROVIDER_ID);
		VCProvider provider = providerId == null ? VCProviderFactory.createDefaultProvider() : VCProviderFactory.createProvider(providerId);
		VCConfiguration config = handleConfig(provider);
		boolean invalid = !config.isConfigValid();
		if (invalid) {
			String[] params = new String[] { this.getShortTitle() };
			String shortKey = "error.config.short";
			String longKey = "error.config.long";
			String translationPackage = VCEditController.class.getPackage().getName();
			status = new StatusDescription(ValidationStatus.ERROR, shortKey, longKey, params, translationPackage);
			status.setDescriptionForUnit(getIdent());
			status.setActivateableViewIdentifier(VCEditController.PANE_TAB_VCCONFIG);
		}
		
		return status;
	}

	public boolean needsReferenceToARepositoryEntry() {
		return false;
	}
	
	@Override
	public void cleanupOnDelete(ICourse course) {
		// load configuration
		final String providerId = getModuleConfiguration().getStringValue(CONF_PROVIDER_ID);
		VCProvider provider = providerId == null ? VCProviderFactory.createDefaultProvider() : VCProviderFactory.createProvider(providerId);
		VCConfiguration config = handleConfig(provider);
		// remove meeting
		provider.removeClassroom(course.getResourceableId() + "_" + this.getIdent(), config);
	}

}
// </OLATCE-103>