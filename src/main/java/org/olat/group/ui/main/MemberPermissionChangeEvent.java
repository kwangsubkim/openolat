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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.olat.core.id.Identity;
import org.olat.group.BusinessGroupShort;
import org.olat.group.model.BusinessGroupMembershipChange;
import org.olat.repository.model.RepositoryEntryPermissionChangeEvent;

/**
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class MemberPermissionChangeEvent extends RepositoryEntryPermissionChangeEvent {
	private static final long serialVersionUID = 8499004967313689825L;

	private List<BusinessGroupMembershipChange> groupChanges;
	
	public MemberPermissionChangeEvent(Identity member) {
		super(member);
	}
	
	public List<BusinessGroupShort> getGroups() {
		List<BusinessGroupShort> groups = new ArrayList<BusinessGroupShort>();
		if(groupChanges != null && !groupChanges.isEmpty()) {
			for(BusinessGroupMembershipChange change:groupChanges) {
				BusinessGroupShort group = change.getGroup();
				if(!groups.contains(group)) {
					groups.add(group);
				}
			}
		}
		return groups;
	}
	
	public List<BusinessGroupMembershipChange> getGroupChanges() {
		return groupChanges;
	}

	public void setGroupChanges(List<BusinessGroupMembershipChange> changes) {
		this.groupChanges = changes;
	}
	
	@Override
	public int size() {
		return (groupChanges == null ? 0 : groupChanges.size()) + super.size();
	}
	
	public List<RepositoryEntryPermissionChangeEvent> generateRepositoryChanges(List<Identity> members) {
		if(members == null || members.isEmpty()) {
			return Collections.emptyList();
		}
		
		List<RepositoryEntryPermissionChangeEvent> repoChanges = new ArrayList<RepositoryEntryPermissionChangeEvent>();
		for(Identity member:members) {
			repoChanges.add(new RepositoryEntryPermissionChangeEvent(member, this));
		}
		return repoChanges;
	}
	
	public List<BusinessGroupMembershipChange> generateBusinessGroupMembershipChange(List<Identity> members) {
		if(members == null || members.isEmpty()) {
			return Collections.emptyList();
		}
		List<BusinessGroupMembershipChange> groupChanges = getGroupChanges();
		if(groupChanges == null || groupChanges.isEmpty()) {
			return Collections.emptyList();
		}
		
		List<BusinessGroupMembershipChange> allModifications = new ArrayList<BusinessGroupMembershipChange>();
		for(BusinessGroupMembershipChange groupChange:groupChanges) {
			for(Identity member:members) {
				allModifications.add(new BusinessGroupMembershipChange(member, groupChange));
			}
		}
		return allModifications;
	}
}