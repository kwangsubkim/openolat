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
package org.olat.group.ui.main;

import org.olat.NewControllerFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.EscapeMode;
import org.olat.core.gui.components.table.ColumnDescriptor;
import org.olat.core.gui.components.table.CustomCellRenderer;
import org.olat.core.gui.components.table.CustomRenderColumnDescriptor;
import org.olat.core.gui.components.table.DefaultColumnDescriptor;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.id.OLATResourceable;
import org.olat.core.logging.activity.ThreadLocalUserActivityLogger;
import org.olat.core.util.resource.OresHelper;
import org.olat.group.BusinessGroup;
import org.olat.group.model.SearchBusinessGroupParams;
import org.olat.group.ui.main.BusinessGroupTableModelWithType.Cols;
import org.olat.util.logging.activity.LoggingResourceable;

/**
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class OpenBusinessGroupListController extends AbstractBusinessGroupListController {
	
	public OpenBusinessGroupListController(UserRequest ureq, WindowControl wControl) {
		super(ureq, wControl, "open_group_list");

		updateOpenGroupModel(ureq);
	}
	
	@Override
	protected void initButtons(UserRequest ureq) {
		//
	}

	@Override
	protected int initColumns() {
		groupListCtr.addColumnDescriptor(new BusinessGroupNameColumnDescriptor(TABLE_ACTION_LAUNCH, getLocale()));
		groupListCtr.addColumnDescriptor(false, new DefaultColumnDescriptor(Cols.key.i18n(), Cols.key.ordinal(), null, getLocale()));
		if(groupModule.isManagedBusinessGroups()) {
			groupListCtr.addColumnDescriptor(false, new DefaultColumnDescriptor(Cols.externalId.i18n(), Cols.externalId.ordinal(), null, getLocale()));
		}
		DefaultColumnDescriptor descCol = new DefaultColumnDescriptor(Cols.description.i18n(), Cols.description.ordinal(), null, getLocale());
		descCol.setEscapeHtml(EscapeMode.antisamy);
		groupListCtr.addColumnDescriptor(descCol);
		groupListCtr.addColumnDescriptor(new ResourcesColumnDescriptor(this, mainVC, getTranslator()));
		DefaultColumnDescriptor freePlacesCol = new DefaultColumnDescriptor(Cols.freePlaces.i18n(), Cols.freePlaces.ordinal(), TABLE_ACTION_LAUNCH, getLocale());
		freePlacesCol.setEscapeHtml(EscapeMode.none);
		groupListCtr.addColumnDescriptor(freePlacesCol);
		CustomCellRenderer acRenderer = new BGAccessControlledCellRenderer();
		groupListCtr.addColumnDescriptor(new CustomRenderColumnDescriptor(Cols.accessTypes.i18n(), Cols.accessTypes.ordinal(), null, getLocale(), ColumnDescriptor.ALIGNMENT_LEFT, acRenderer));
		groupListCtr.addColumnDescriptor(new RoleColumnDescriptor(getLocale()));
		groupListCtr.addColumnDescriptor(new AccessActionColumnDescriptor(Cols.accessControlLaunch.i18n(), Cols.accessControlLaunch.ordinal(), getTranslator()));
		return 8;
	}
	
	@Override
	protected void doLaunch(UserRequest ureq, BusinessGroup group) {	
		if(businessGroupService.isIdentityInBusinessGroup(getIdentity(), group)) {
			super.doLaunch(ureq, group);
		} else {
			String businessPath = "[GroupCard:" + group.getKey() + "]";
			NewControllerFactory.getInstance().launch(businessPath, ureq, getWindowControl());
		}
	}

	private void updateOpenGroupModel(UserRequest ureq) {
		//find all accessible business groups
		SearchBusinessGroupParams params = new SearchBusinessGroupParams();
		params.setPublicGroups(Boolean.TRUE);
		updateTableModel(params, false);
		
		OLATResourceable ores = OresHelper.createOLATResourceableInstance("All", 0l);
		ThreadLocalUserActivityLogger.addLoggingResourceInfo(LoggingResourceable.wrapBusinessPath(ores));
		addToHistory(ureq, ores, null);
	}
}