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
package org.olat.portfolio.manager;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.olat.core.commons.modules.bc.vfs.OlatRootFolderImpl;
import org.olat.core.commons.persistence.DB;
import org.olat.core.commons.persistence.DBQuery;
import org.olat.core.commons.services.tagging.manager.TaggingManager;
import org.olat.core.commons.services.tagging.model.Tag;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.logging.AssertException;
import org.olat.core.manager.BasicManager;
import org.olat.core.util.FileUtils;
import org.olat.core.util.StringHelper;
import org.olat.core.util.resource.OresHelper;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSItem;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.core.util.vfs.VFSManager;
import org.olat.portfolio.EPArtefactHandler;
import org.olat.portfolio.PortfolioModule;
import org.olat.portfolio.model.EPFilterSettings;
import org.olat.portfolio.model.artefacts.AbstractArtefact;
import org.olat.portfolio.model.structel.EPStructureToArtefactLink;

/**
 * 
 * Description:<br>
 * EPArtefactManager manage the artefacts
 * 
 * <P>
 * Initial Date: 11.06.2010 <br>
 * 
 * @author Roman Haag, roman.haag@frentix.com, http://www.frentix.com
 */
public class EPArtefactManager extends BasicManager {

	private static final String ARTEFACT_FULLTEXT_ON_FS = "ARTEFACT_FULLTEXT_ON_FS";
	// those are here as instance variable, as mocking the tests won't be possible
	// without!
	// it also helps to find failures in loading a manager, as already spring
	// would warn and not
	// only later on, when a click happens.
	private DB dbInstance;
	private PortfolioModule portfolioModule;
	private TaggingManager taggingManager;
	// end.

	private static final int ARTEFACT_FULLTEXT_DB_FIELD_LENGTH = 16384;
	public static final String ARTEFACT_CONTENT_FILENAME = "artefactContent.html";
	private static final String ARTEFACT_INTERNALDATA_FOLDER = "data";
	
	private VFSContainer artefactsRoot;

	/**
	 * 
	 */
	public EPArtefactManager() {
		//
	}

	/**
	 * [used by Spring]
	 * 
	 * @param dbInstance
	 */
	public void setDbInstance(DB dbInstance) {
		this.dbInstance = dbInstance;
	}

	/**
	 * [used by Spring]
	 * 
	 * @param portfolioModule
	 */
	public void setPortfolioModule(PortfolioModule portfolioModule) {
		this.portfolioModule = portfolioModule;
	}

	/**
	 * [used by Spring]
	 * 
	 * @param taggingManager
	 */
	public void setTaggingManager(TaggingManager taggingManager) {
		this.taggingManager = taggingManager;
	}

	/**
	 * load the persisted artefact from FS
	 */
	void loadFile() {
		//
	}

	/**
	 * convert html/text to PDF and save in Filesystem
	 */
	void persistAsPDF() {
		//
	}
	
	/**
	 * Used by the indexer to retrieve all the artefacts
	 * @param artefactIds List of ids to seek (optional)
	 * @param firstResult First position
	 * @param maxResults Max number of returned artefacts (0 or below for all)
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected List<AbstractArtefact> getArtefacts(Identity author, List<Long> artefactIds, int firstResult, int maxResults) {
		StringBuilder sb = new StringBuilder();
		sb.append("select artefact from ").append(AbstractArtefact.class.getName()).append(" artefact");
		boolean where = false;
		if(author != null) {
			where = true;
			sb.append(" where artefact.author=:author");
		}
		if(artefactIds != null && !artefactIds.isEmpty()) {
			if(where) sb.append(" and ");
			else sb.append(" where ");
			sb.append(" artefact.id in (:artefactIds)");
		}
		DBQuery query = dbInstance.createQuery(sb.toString());
		if(maxResults > 0) {
			query.setMaxResults(maxResults);
		}
		if(firstResult >= 0) {
			query.setFirstResult(firstResult);
		}
		if(author != null) {
			query.setEntity("author", author);
		}
		if(artefactIds != null && !artefactIds.isEmpty()) {
			query.setParameterList("artefactIds", artefactIds);
		}
		
		List<AbstractArtefact> artefacts = query.list();
		return artefacts;
	}
	
	protected boolean isArtefactClosed(AbstractArtefact artefact) {
		StringBuilder sb = new StringBuilder();
		sb.append("select count(link) from ").append(EPStructureToArtefactLink.class.getName()).append(" link ")
			.append(" inner join link.structureElement structure ")
			.append(" inner join structure.root rootStructure")
			.append(" where link.artefact=:artefact and rootStructure.status='closed'");

		DBQuery query = dbInstance.createQuery(sb.toString());
		query.setEntity("artefact", artefact);
		Number count = (Number)query.uniqueResult();
		return count.intValue() > 0;
	}

	protected List<AbstractArtefact> getArtefactPoolForUser(Identity ident) {
		long start = System.currentTimeMillis();
		StringBuilder sb = new StringBuilder();
		sb.append("select artefact from ").append(AbstractArtefact.class.getName()).append(" artefact").append(" where author=:author");
		DBQuery query = dbInstance.createQuery(sb.toString());
		query.setEntity("author", ident);
		@SuppressWarnings("unchecked")
		List<AbstractArtefact> artefacts = query.list();
		if (artefacts.isEmpty()) return null;
		long duration = System.currentTimeMillis() - start;
		if (isLogDebugEnabled()) logDebug("loading the full artefact pool took " + duration + "ms");
		return artefacts;
	}

	protected VFSContainer getArtefactsRoot() {
		if (artefactsRoot == null) {
			VFSContainer root = portfolioModule.getPortfolioRoot();
			VFSItem artefactsItem = root.resolve("artefacts");
			if (artefactsItem == null) {
				artefactsRoot = root.createChildContainer("artefacts");
			} else if (artefactsItem instanceof VFSContainer) {
				artefactsRoot = (VFSContainer) artefactsItem;
			} else {
				logError("The root folder for artefact is a file and not a folder", null);
			}
		}
		return artefactsRoot;
	}
	
	protected VFSContainer getArtefactsTempContainer(Identity ident){
		VFSContainer artRoot = new OlatRootFolderImpl(File.separator + "tmp", null);
		VFSItem tmpI = artRoot.resolve("portfolio");
		if (tmpI == null) {
			tmpI = artRoot.createChildContainer("portfolio");
		}	
		VFSItem userTmp = tmpI.resolve(ident.getName());
		if (userTmp == null){
			userTmp = ((VFSContainer) tmpI).createChildContainer(ident.getName());
		}
		String idFolder = UUID.randomUUID().toString();
		VFSContainer thisTmp = ((VFSContainer) userTmp).createChildContainer(idFolder);
		return thisTmp;
	}

	protected List<String> getArtefactTags(AbstractArtefact artefact) {
		// wrap concrete artefact as abstract-artefact to get the correct resName for the tag
		if (artefact.getKey() == null ) return null;
		OLATResourceable artefactOres = OresHelper.createOLATResourceableInstance(AbstractArtefact.class, artefact.getKey());
		List<String> tags = taggingManager.getTagsAsString(null, artefactOres, null, null);
		return tags;
	}

	protected void setArtefactTag(Identity identity, AbstractArtefact artefact, String tag) {
		// wrap concrete artefact as abstract-artefact to get the correct resName for the tag
		OLATResourceable artefactOres = OresHelper.createOLATResourceableInstance(AbstractArtefact.class, artefact.getKey());
		taggingManager.createAndPersistTag(identity, tag, artefactOres, null, null);
	}

	protected void setArtefactTags(Identity identity, AbstractArtefact artefact, List<String> tags) {
		if (tags==null) return;
		// wrap concrete artefact as abstract-artefact to get the correct resName for the tag
		OLATResourceable artefactOres = OresHelper.createOLATResourceableInstance(AbstractArtefact.class, artefact.getKey());
		List<Tag> oldTags = taggingManager.loadTagsForResource(artefactOres, null, null);
		List<String> oldTagStrings = new ArrayList<String>();
		List<String> tagsToAdd = new ArrayList<String>(tags.size());
		tagsToAdd.addAll(tags);
		if (oldTags != null) { // there might be no tags yet
			for (Tag oTag : oldTags) {
				if (tags.contains(oTag.getTag())){
					// still existing, nothing to do
					oldTagStrings.add(oTag.getTag());
					tagsToAdd.remove(oTag.getTag());
				} else {
					// tag was deleted, remove it
					taggingManager.deleteTag(oTag);
				}
			}
		}
		// look for all given tags, add the ones yet missing
		for (String tag : tagsToAdd) {
			if (StringHelper.containsNonWhitespace(tag)) {
				taggingManager.createAndPersistTag(identity, tag, artefactOres, null, null);
			}
		}
	}

	/**
	 * Create and persist an artefact of the given type
	 * 
	 * @param type
	 * @return The persisted artefact
	 */
	protected AbstractArtefact createAndPersistArtefact(Identity identity, String type) {
		EPArtefactHandler<?> handler = portfolioModule.getArtefactHandler(type);
		if(handler != null && handler.isEnabled()){
			AbstractArtefact artefact = handler.createArtefact();
			artefact.setAuthor(identity);
			
			dbInstance.saveObject(artefact);
			saveArtefactFulltextContent(artefact);
			return artefact;
		} else {
			return null;
		}
	}

	protected AbstractArtefact updateArtefact(AbstractArtefact artefact) {
		if (artefact == null) return null;
		
		String tmpFulltext = artefact.getFulltextContent();
		if (StringHelper.containsNonWhitespace(tmpFulltext) && artefact.getFulltextContent().equals(ARTEFACT_FULLTEXT_ON_FS)){
			tmpFulltext = getArtefactFullTextContent(artefact);
		}
		artefact.setFulltextContent("");
		if (artefact.getKey() == null) {
			dbInstance.saveObject(artefact);
		} else {
			dbInstance.updateObject(artefact);
		}
		artefact.setFulltextContent(tmpFulltext);
		saveArtefactFulltextContent(artefact);
		
		return artefact;
	}

	// decides itself if fulltext fits into db or will be written on fs
	protected boolean saveArtefactFulltextContent(AbstractArtefact artefact){
		String fullText = artefact.getFulltextContent();
		if (StringHelper.containsNonWhitespace(fullText)) {
			if (fullText.length() > ARTEFACT_FULLTEXT_DB_FIELD_LENGTH){
				// save the real content on FS
				try {
					VFSContainer container = getArtefactContainer(artefact);
					VFSLeaf artData = (VFSLeaf) container.resolve(ARTEFACT_CONTENT_FILENAME);
					if (artData == null) {
						artData = container.createChildLeaf(ARTEFACT_CONTENT_FILENAME);
					} 
					VFSManager.copyContent(new ByteArrayInputStream(fullText.getBytes()), artData, true);
					artefact.setFulltextContent(ARTEFACT_FULLTEXT_ON_FS);
					dbInstance.updateObject(artefact);
				} catch (Exception e) {
					logError("could not really save the fulltext content of an artefact", e);
					return false;
				}
			}	else {
				// if length is shorter, but still a file there -> delete it (but only if loading included the long version from fs before, else its overwritten!)
				VFSLeaf artData = (VFSLeaf) getArtefactContainer(artefact).resolve(ARTEFACT_INTERNALDATA_FOLDER + "/" + ARTEFACT_CONTENT_FILENAME); // v.1 had /data/ in path
				if (artData!=null) artData.delete();
				artData = (VFSLeaf) getArtefactContainer(artefact).resolve(ARTEFACT_CONTENT_FILENAME);
				if (artData!=null) artData.delete();
				dbInstance.updateObject(artefact); // persist fulltext in db
			}
		}
		return true;
	}
	
	protected String getArtefactFullTextContent(AbstractArtefact artefact){
		VFSLeaf artData = (VFSLeaf) getArtefactContainer(artefact).resolve(ARTEFACT_CONTENT_FILENAME);
		if (artData== null) artData = (VFSLeaf) getArtefactContainer(artefact).resolve(ARTEFACT_INTERNALDATA_FOLDER + "/" + ARTEFACT_CONTENT_FILENAME); // fallback to v.1
		if (artData!=null) {
			return FileUtils.load(artData.getInputStream(), "utf-8");
		} else return artefact.getFulltextContent();
	}
	
	/**
	 * This is an optimized method to filter a list of artefact by tags and return
	 * the tags of this list of artefacts. This prevent to search two times or more the list
	 * of tags of an artefact.
	 * @param identity
	 * @param tags
	 * @return the filtered artefacts and their tags
	 */
	protected EPArtefactTagCloud getArtefactsAndTagCloud(Identity identity, List<String> tags) {
		List<AbstractArtefact> artefacts = getArtefactPoolForUser(identity);
		EPFilterSettings filterSettings = new EPFilterSettings();
		filterSettings.setTagFilter(tags);
		
		Set<String> newTags = new HashSet<String>();
		filterArtefactsByTags(artefacts, filterSettings, newTags);

		return new EPArtefactTagCloud(artefacts, newTags);
	}

	protected List<AbstractArtefact> filterArtefactsByFilterSettings(List<AbstractArtefact> allArtefacts, EPFilterSettings filterSettings) {
		long start = System.currentTimeMillis();
		if (allArtefacts == null) return null;
		List<AbstractArtefact> filteredArtefactList = new ArrayList<AbstractArtefact>(allArtefacts.size());
		filteredArtefactList.addAll(allArtefacts);
		if (filterSettings != null && !filterSettings.isFilterEmpty()) {
			if (filteredArtefactList.size() != 0) {
				filterArtefactsByTags(filteredArtefactList, filterSettings, null);
			}
			if (filteredArtefactList.size() != 0) {
				filterArtefactsByType(filteredArtefactList, filterSettings.getTypeFilter());
			}
			if (filteredArtefactList.size() != 0) {
				filterArtefactsByString(filteredArtefactList, filterSettings.getTextFilter());
			}
			if (filteredArtefactList.size() != 0) {
				filterArtefactsByDate(filteredArtefactList, filterSettings.getDateFilter());
			}
		}
		long duration = System.currentTimeMillis() - start;
		if (isLogDebugEnabled()) logDebug("filtering took " + duration + "ms");
		return filteredArtefactList;
	}

	/**
	 * @param allArtefacts
	 * @param filterSettings (containing tags to filter for or boolean if filter should keep only artefacts without a tag)
	 * @param collect the tags found in the filtered artefacts
	 * @return filtered artefact list
	 */
	private void filterArtefactsByTags(List<AbstractArtefact> artefacts, EPFilterSettings filterSettings, Set<String> cloud) {
		List<String> tags = filterSettings.getTagFilter();
		// either search for artefacts with given tags, or such with no one!
		List<AbstractArtefact> toRemove = new ArrayList<AbstractArtefact>();
		if (tags != null && tags.size() != 0) {
			// TODO: epf: RH: fix needed, as long as tags with uppercase initial are
			// allowed!
			for (AbstractArtefact artefact : artefacts) {
				List<String> artefactTags = getArtefactTags(artefact);
				if (!artefactTags.containsAll(tags)) {
					toRemove.add(artefact);
				} else if(cloud != null) {
					cloud.addAll(artefactTags);
				}
			}
			artefacts.removeAll(toRemove);
		} else if (filterSettings.isNoTagFilterSet()) {
			for (AbstractArtefact artefact : artefacts) {
				if (!getArtefactTags(artefact).isEmpty()) {
					toRemove.add(artefact);
				}
			}
			artefacts.removeAll(toRemove);
		}
	}

	private void filterArtefactsByType(List<AbstractArtefact> artefacts, List<String> type) {
		if (type != null && type.size() != 0) {
			List<AbstractArtefact> toRemove = new ArrayList<AbstractArtefact>();
			for (AbstractArtefact artefact : artefacts) {
				if (!type.contains(artefact.getResourceableTypeName())) {
					toRemove.add(artefact);
				}
			}
			artefacts.removeAll(toRemove);
		}
	}

	/**
	 * date comparison will first set startDate to 00:00:00 and set endDate to
	 * 23:59:59 else there might be no results if start = end date. dateList must
	 * be set according to: dateList(0) = startDate dateList(1) = endDate
	 */
	private void filterArtefactsByDate(List<AbstractArtefact> artefacts, List<Date> dateList) {
		if (dateList != null && dateList.size() != 0) {
			if (dateList.size() == 2) {
				Date startDate = dateList.get(0);
				Date endDate = dateList.get(1);
				Calendar cal = Calendar.getInstance();
				if (startDate == null) {
					cal.set(1970, 1, 1);
				} else {
					cal.setTime(startDate);
				}
				cal.set(Calendar.HOUR_OF_DAY, 0);
				cal.set(Calendar.MINUTE, 0);
				cal.set(Calendar.SECOND, 0);
				startDate = cal.getTime();
				cal.setTime(endDate);
				cal.set(Calendar.HOUR_OF_DAY, 23);
				cal.set(Calendar.MINUTE, 59);
				cal.set(Calendar.SECOND, 59);
				endDate = cal.getTime();
				List<AbstractArtefact> toRemove = new ArrayList<AbstractArtefact>();
				for (AbstractArtefact artefact : artefacts) {
					Date creationDate = artefact.getCreationDate();
					if (!(creationDate.before(endDate) && creationDate.after(startDate))) {
						toRemove.add(artefact);
					}
				}
				artefacts.removeAll(toRemove);
			} else throw new AssertException("provided DateList must contain exactly two Date-objects");
		}
	}

	private void filterArtefactsByString(List<AbstractArtefact> artefacts, String textFilter) {
		if (StringHelper.containsNonWhitespace(textFilter)) {
			List<AbstractArtefact> toRemove = new ArrayList<AbstractArtefact>();
			for (AbstractArtefact artefact : artefacts) {
				String textCompare = artefact.getTitle() + artefact.getDescription() + artefact.getFulltextContent();
				if (!textCompare.toLowerCase().contains(textFilter.toLowerCase())) {
					toRemove.add(artefact);
				}
			}
			artefacts.removeAll(toRemove);
		}
	}

	/**
	 * Load the artefact by its primary key
	 * 
	 * @param key The primary key
	 * @return The artefact or null if nothing found
	 */
	protected AbstractArtefact loadArtefactByKey(Long key) {
		if (key == null) throw new NullPointerException();

		StringBuilder sb = new StringBuilder();
		sb.append("select artefact from ").append(AbstractArtefact.class.getName()).append(" artefact").append(" where artefact=:key");

		DBQuery query = dbInstance.createQuery(sb.toString());
		query.setLong("key", key);

		@SuppressWarnings("unchecked")
		List<AbstractArtefact> artefacts = query.list();
		// if not found, it is an empty list
		if (artefacts.isEmpty()) return null;
		return artefacts.get(0);
	}
	
	protected List<AbstractArtefact> loadArtefactsByBusinessPath(String businessPath, Identity author){
		if (!StringHelper.containsNonWhitespace(businessPath)) return null;
		StringBuilder sb = new StringBuilder();
		sb.append("select artefact from ").append(AbstractArtefact.class.getName()).append(" artefact")
		.append(" where artefact.businessPath=:bpath");
		
		if (author != null) {
			 sb.append(" and artefact.author=:ident");
		}

		DBQuery query = dbInstance.createQuery(sb.toString());
		query.setString("bpath", businessPath);
		if (author != null) {
			query.setEntity("ident", author);
		}

		@SuppressWarnings("unchecked")
		List<AbstractArtefact> artefacts = query.list();
		// if not found, it is an empty list
		if (artefacts.isEmpty()) return null;
		return artefacts;		
	}

	protected void deleteArtefact(AbstractArtefact artefact) {
		getArtefactContainer(artefact).delete();
	  // wrap concrete artefact as abstract-artefact to get the correct resName for the tag
		OLATResourceable artefactOres = OresHelper.createOLATResourceableInstance(AbstractArtefact.class, artefact.getKey());
		taggingManager.deleteTags(artefactOres, null, null);

		dbInstance.deleteObject(artefact);
		logInfo("Deleted artefact " + artefact.getTitle() + " with key: " + artefact.getKey());
	}

	protected VFSContainer getArtefactContainer(AbstractArtefact artefact) {
		Long key = artefact.getKey();
		if (key == null) throw new AssertException("artefact not yet persisted -> no key available!");
		VFSContainer container = null;
		VFSItem item = getArtefactsRoot().resolve(key.toString());
		if (item == null) {
			container = getArtefactsRoot().createChildContainer(key.toString());
		} else if (item instanceof VFSContainer) {
			container = (VFSContainer) item;
		} else {
			logError("Cannot create a container for artefact: " + artefact, null);
		}
		return container;
	}

}
