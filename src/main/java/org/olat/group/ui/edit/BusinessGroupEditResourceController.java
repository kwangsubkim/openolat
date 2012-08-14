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

package org.olat.group.ui.edit;

import java.util.Collections;
import java.util.List;

import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.persistence.PersistenceHelper;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.table.Table;
import org.olat.core.gui.components.table.TableController;
import org.olat.core.gui.components.table.TableEvent;
import org.olat.core.gui.components.table.TableGuiConfiguration;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.ControllerEventListener;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.gui.control.generic.modal.DialogBoxController;
import org.olat.core.gui.control.generic.modal.DialogBoxUIFactory;
import org.olat.core.gui.translator.Translator;
import org.olat.core.util.Util;
import org.olat.course.CourseModule;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupService;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryTableModel;
import org.olat.repository.controllers.ReferencableEntriesSearchController;

/**
 * Description:<BR>
 * Controller to edit a business group context. The editor proviedes a tabbed
 * pane with the following tabs: - details / metadata - owner management (who
 * can edit this group context) - resource management (where to use this group
 * context) This controller however does no functionality to create groups,
 * learning areas etc. See BGManagementController for this functionality
 * <P>
 * Initial Date: Jan 31, 2005
 * 
 * @author gnaegi
 */
public class BusinessGroupEditResourceController extends BasicController implements ControllerEventListener {

	private final VelocityContainer mainVC;

	private TableController resourcesCtr;
	private RepositoryTableModel repoTableModel;
	private List<RepositoryEntry> repoTableModelEntries;
	private ReferencableEntriesSearchController repoSearchCtr;
	private CloseableModalController cmc;
	private DialogBoxController confirmRemoveResource;
	private Link addTabResourcesButton;

	private BusinessGroup group;
	private final BusinessGroupService businessGroupService;

	/**
	 * Constructor for a business group edit controller
	 * 
	 * @param ureq The user request
	 * @param wControl The window control
	 * @param groupContext The business group context to be edited
	 */
	public BusinessGroupEditResourceController(UserRequest ureq, WindowControl wControl, BusinessGroup group) {
		super(ureq, wControl);
		businessGroupService = CoreSpringFactory.getImpl(BusinessGroupService.class);
		this.group = group;
		
		Translator resourceTrans = Util.createPackageTranslator(RepositoryTableModel.class, getLocale(), getTranslator());
		TableGuiConfiguration tableConfig = new TableGuiConfiguration();
		tableConfig.setTableEmptyMessage(translate("resources.noresources"));
		resourcesCtr = new TableController(tableConfig, ureq, getWindowControl(), resourceTrans);
		listenTo(resourcesCtr);

		repoTableModel = new RepositoryTableModel(resourceTrans);
		repoTableModelEntries = businessGroupService.findRepositoryEntries(Collections.singletonList(group), 0, -1);
		repoTableModel.setObjects(repoTableModelEntries);
		repoTableModel.addColumnDescriptors(resourcesCtr, translate("resources.remove"), false);
		resourcesCtr.setTableDataModel(repoTableModel);
		
		mainVC = createVelocityContainer("tab_bgResources");
		addTabResourcesButton = LinkFactory.createButtonSmall("cmd.addresource", mainVC, this);
		mainVC.put("resources", resourcesCtr.getInitialComponent());
		putInitialPanel(mainVC);
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(UserRequest ureq, Component source, Event event) {
		if (source == addTabResourcesButton) {
			removeAsListenerAndDispose(repoSearchCtr);
			removeAsListenerAndDispose(cmc);
			
			repoSearchCtr = new ReferencableEntriesSearchController(getWindowControl(), ureq, CourseModule.getCourseTypeName(), translate("resources.add"));
			listenTo(repoSearchCtr);
			cmc = new CloseableModalController(getWindowControl(), translate("close"), this.repoSearchCtr.getInitialComponent(), true, translate("resources.add.title"));
			listenTo(cmc);
			cmc.activate();
		}
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	public void event(UserRequest ureq, Controller source, Event event) {
		if (source == repoSearchCtr) {
			if (event == ReferencableEntriesSearchController.EVENT_REPOSITORY_ENTRY_SELECTED) {
				// repository search controller done
				RepositoryEntry re = repoSearchCtr.getSelectedEntry();
				removeAsListenerAndDispose(repoSearchCtr);
				cmc.deactivate();
				if (re != null && !repoTableModelEntries.contains(re)) {
					// check if already in model
					boolean alreadyAssociated = PersistenceHelper.listContainsObjectByKey(this.repoTableModelEntries, re);
					if (!alreadyAssociated) {
						doAddRepositoryEntry(re);
						fireEvent(ureq, Event.CHANGED_EVENT);
					}
				}
			}
		} else if (source == resourcesCtr) {
			if (event.getCommand().equals(Table.COMMANDLINK_ROWACTION_CLICKED)) {
				TableEvent te = (TableEvent) event;
				String actionid = te.getActionId();
				RepositoryEntry re = repoTableModel.getObject(te.getRowId());
				if (actionid.equals(RepositoryTableModel.TABLE_ACTION_SELECT_LINK)) {
					//present dialog box if resource should be removed
					String text = getTranslator().translate("resource.remove", new String[] { group.getName(), re.getDisplayname() });
					confirmRemoveResource = activateYesNoDialog(ureq, null, text, this.confirmRemoveResource);
					confirmRemoveResource.setUserObject(re);
				}
			}
		} else if (source == confirmRemoveResource) {
			if (DialogBoxUIFactory.isYesEvent(event)) { // yes case
				RepositoryEntry re = (RepositoryEntry)confirmRemoveResource.getUserObject();
				doRemoveResource(re);
				fireEvent(ureq, Event.CHANGED_EVENT);
			}
		}
	}
	
	private void doRemoveResource(RepositoryEntry entry) {
		businessGroupService.removeResourceFrom(group, entry.getOlatResource());
		repoTableModelEntries.remove(entry);
		resourcesCtr.modelChanged();
	}

	private void doAddRepositoryEntry(RepositoryEntry entry) {
		businessGroupService.addResourceTo(group, entry.getOlatResource());
		repoTableModelEntries.add(entry);
		resourcesCtr.modelChanged();
	}
	
	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean asynchronous)
	 */
	@Override
	protected void doDispose() {
		//
	}
}