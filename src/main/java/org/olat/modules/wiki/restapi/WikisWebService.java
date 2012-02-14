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
package org.olat.modules.wiki.restapi;

import static org.olat.restapi.security.RestSecurityHelper.getIdentity;
import static org.olat.restapi.security.RestSecurityHelper.getRoles;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.ArrayUtils;
import org.olat.collaboration.CollaborationTools;
import org.olat.collaboration.CollaborationToolsFactory;
import org.olat.core.id.Identity;
import org.olat.core.id.Roles;
import org.olat.fileresource.types.WikiResource;
import org.olat.group.BusinessGroup;
import org.olat.group.BusinessGroupManager;
import org.olat.group.BusinessGroupManagerImpl;
import org.olat.modules.wiki.restapi.vo.WikiVO;
import org.olat.modules.wiki.restapi.vo.WikiVOes;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.repository.SearchRepositoryEntryParameters;
import org.olat.restapi.security.RestSecurityHelper;

/**
 * 
 * The Wikis Webservice.<br />
 * OO-112
 * 
 * @author strentini, sergio.trentini@frentix.com, http://www.frentix.com
 * 
 */
@Path("repo/wikis")
public class WikisWebService {

	/**
	 * export a specific wiki
	 * 
	 * @param wikiKey
	 * @return
	 */
	@Path("{wikiKey}")
	public WikiWebService getWiki(@PathParam("wikiKey") String wikiKey) {
		return new WikiWebService();
	}

	/**
	 * get list of wikis
	 * 
	 * @param start
	 * @param limit
	 * @param httpRequest
	 * @return
	 */
	@GET
	@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML })
	public Response getWikis(@Context HttpServletRequest httpRequest) {

		try {
			// the repo-Wikis (the "normal" wikis)
			Roles roles = getRoles(httpRequest);
			Identity identity = getIdentity(httpRequest);
			RepositoryManager rm = RepositoryManager.getInstance();
			SearchRepositoryEntryParameters params = new SearchRepositoryEntryParameters(identity, roles, new String[] { WikiResource.TYPE_NAME });
			List<RepositoryEntry> res = rm.genericANDQueryWithRolesRestriction(params, 0, -1, true);

			// the group wikis
			List<BusinessGroup> groups = findAccessibleGroupsWithWikis(RestSecurityHelper.getIdentity(httpRequest));

			// patch 'em together
			WikiVOes voes = new WikiVOes();
			WikiVO[] wks_repo = toArrayOfVOes_repoentry(res);
			WikiVO[] wks_grp = toArrayOfVOes_group(groups);

			voes.setWikis((WikiVO[]) ArrayUtils.addAll(wks_repo, wks_grp));
			return Response.ok(voes).build();
		} catch (Exception e) {
			throw new WebApplicationException(e);
		}

	}

	/**
	 * gets a list of all groups that have the wiki enabled AND where current
	 * REST user is attendee or owner
	 * 
	 * @param identity
	 * @return
	 */
	private List<BusinessGroup> findAccessibleGroupsWithWikis(Identity identity) {
		BusinessGroupManager bgm = BusinessGroupManagerImpl.getInstance();
		List<BusinessGroup> groups = bgm.findBusinessGroupsAttendedBy(null, identity, null);
		groups.addAll(bgm.findBusinessGroupsOwnedBy(null, identity, null));
		List<BusinessGroup> filteredGroups = new ArrayList<BusinessGroup>();

		// check for every group, if it has a wiki
		for (BusinessGroup businessGroup : groups) {
			CollaborationTools ctsm = CollaborationToolsFactory.getInstance().getOrCreateCollaborationTools(businessGroup);
			if (ctsm.isToolEnabled(CollaborationTools.TOOL_WIKI)) {
				filteredGroups.add(businessGroup);
			}
		}
		return filteredGroups;
	}

	private static WikiVO[] toArrayOfVOes_repoentry(List<RepositoryEntry> entries) {
		int i = 0;
		WikiVO[] wikiVOs = new WikiVO[entries.size()];
		for (RepositoryEntry entry : entries) {
			wikiVOs[i++] = wikivoFromRepoEntry(entry);
		}
		return wikiVOs;
	}

	private static WikiVO[] toArrayOfVOes_group(List<BusinessGroup> groups) {
		int i = 0;
		WikiVO[] wikiVOs = new WikiVO[groups.size()];
		for (BusinessGroup group : groups) {
			wikiVOs[i++] = wikivoFromGroup(group);
		}
		return wikiVOs;
	}

	private static WikiVO wikivoFromRepoEntry(RepositoryEntry entry) {
		WikiVO wiki = new WikiVO();
		wiki.setGroupWiki(false);
		wiki.setTitle(entry.getDisplayname());
		wiki.setKey(entry.getResourceableId());
		wiki.setSoftkey(entry.getSoftkey());
		return wiki;
	}

	private static WikiVO wikivoFromGroup(BusinessGroup group) {
		WikiVO wiki = new WikiVO();
		wiki.setGroupWiki(true);
		wiki.setTitle(group.getName());
		wiki.setKey(group.getResourceableId());
		wiki.setSoftkey("n/a");
		return wiki;
	}
}