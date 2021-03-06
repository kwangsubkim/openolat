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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.table.ColumnDescriptor;
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
import org.olat.group.BusinessGroupManagedFlag;
import org.olat.group.BusinessGroupService;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryEntryManagedFlag;
import org.olat.repository.RepositoryTableModel;
import org.olat.repository.controllers.ReferencableEntriesSearchController;
import org.olat.repository.controllers.RepositoryEntryFilter;
import org.olat.repository.controllers.RepositorySearchController.Can;

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
	private ReferencableEntriesSearchController repoSearchCtr;
	private CloseableModalController cmc;
	private DialogBoxController confirmRemoveResource;
	private Link addTabResourcesButton;

	private BusinessGroup group;
	private final boolean managed;
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
		managed = BusinessGroupManagedFlag.isManaged(group, BusinessGroupManagedFlag.resources);
		
		Translator resourceTrans = Util.createPackageTranslator(RepositoryTableModel.class, getLocale(), getTranslator());
		TableGuiConfiguration tableConfig = new TableGuiConfiguration();
		tableConfig.setTableEmptyMessage(translate("resources.noresources"));
		resourcesCtr = new TableController(tableConfig, ureq, getWindowControl(), resourceTrans);
		listenTo(resourcesCtr);

		repoTableModel = new RepositoryTableModel(resourceTrans);
		List<RepositoryEntry> repoTableModelEntries = businessGroupService.findRepositoryEntries(Collections.singletonList(group), 0, -1);
		repoTableModel.setObjects(repoTableModelEntries);
		
		ColumnDescriptor sortCol = repoTableModel.addColumnDescriptors(resourcesCtr, null, false);	
		if(!managed) {
			resourcesCtr.addColumnDescriptor(new RemoveResourceActionColumnDescriptor("resources.remove", 1, getTranslator()));
		}
		resourcesCtr.setTableDataModel(repoTableModel);
		resourcesCtr.setSortColumn(sortCol, true);
		
		mainVC = createVelocityContainer("tab_bgResources");
		addTabResourcesButton = LinkFactory.createButtonSmall("cmd.addresource", mainVC, this);
		addTabResourcesButton.setVisible(!managed);
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
			
			RepositoryEntryFilter filter = new ManagedEntryfilter();
			repoSearchCtr = new ReferencableEntriesSearchController(getWindowControl(), ureq,
					new String[]{ CourseModule.getCourseTypeName() }, filter,
					translate("resources.add"), true, true, true, true, true, Can.referenceable);
			listenTo(repoSearchCtr);
			cmc = new CloseableModalController(getWindowControl(), translate("close"), repoSearchCtr.getInitialComponent(), true, translate("resources.add.title"));
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
				if (re != null) {
					doAddRepositoryEntry(Collections.singletonList(re));
					fireEvent(ureq, Event.CHANGED_EVENT);
				}
			} else if(event == ReferencableEntriesSearchController.EVENT_REPOSITORY_ENTRIES_SELECTED) {
				// repository search controller done
				List<RepositoryEntry> res = repoSearchCtr.getSelectedEntries();
				removeAsListenerAndDispose(repoSearchCtr);
				cmc.deactivate();
				if (res != null && !res.isEmpty()) {
					doAddRepositoryEntry(res);
					fireEvent(ureq, Event.CHANGED_EVENT);
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
		businessGroupService.removeResourceFrom(Collections.singletonList(group), entry);
		repoTableModel.getObjects().remove(entry);
		resourcesCtr.modelChanged();
	}

	private void doAddRepositoryEntry(List<RepositoryEntry> entries) {
		List<RepositoryEntry> repoEntries = new ArrayList<RepositoryEntry>();
		for(RepositoryEntry entry:entries) {
			if(!repoTableModel.getObjects().contains(entry)) {
				repoEntries.add(entry);
			}
		}
		businessGroupService.addResourcesTo(Collections.singletonList(group), repoEntries);
		repoTableModel.addObjects(repoEntries);
		resourcesCtr.modelChanged();
	}
	
	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean asynchronous)
	 */
	@Override
	protected void doDispose() {
		//
	}
	
	private static class ManagedEntryfilter implements RepositoryEntryFilter {

		@Override
		public boolean accept(RepositoryEntry re) {
			return !RepositoryEntryManagedFlag.isManaged(re, RepositoryEntryManagedFlag.groups);
		}
	}
}