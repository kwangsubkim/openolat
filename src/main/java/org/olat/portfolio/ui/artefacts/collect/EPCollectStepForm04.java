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
package org.olat.portfolio.ui.artefacts.collect;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.impl.Form;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.ajax.tree.AjaxTreeModel;
import org.olat.core.gui.control.generic.ajax.tree.AjaxTreeNode;
import org.olat.core.gui.control.generic.ajax.tree.TreeController;
import org.olat.core.gui.control.generic.ajax.tree.TreeNodeClickedEvent;
import org.olat.core.gui.control.generic.wizard.StepFormBasicController;
import org.olat.core.gui.control.generic.wizard.StepsEvent;
import org.olat.core.gui.control.generic.wizard.StepsRunContext;
import org.olat.core.logging.OLATRuntimeException;
import org.olat.portfolio.manager.EPFrontendManager;
import org.olat.portfolio.model.artefacts.AbstractArtefact;
import org.olat.portfolio.model.structel.EPAbstractMap;
import org.olat.portfolio.model.structel.EPStructuredMap;
import org.olat.portfolio.model.structel.ElementType;
import org.olat.portfolio.model.structel.PortfolioStructure;
import org.olat.portfolio.model.structel.StructureStatusEnum;
import org.olat.portfolio.ui.structel.EPStructureChangeEvent;

/**
 * Description:<br>
 * controller to select a map as target for an artefact 
 * 
 * <P>
 * Initial Date: 28.07.2010 <br>
 * 
 * @author Roman Haag, roman.haag@frentix.com, http://www.frentix.com
 */
public class EPCollectStepForm04 extends StepFormBasicController {

	private static final String NO_MAP_CHOOSEN = "noMapChoosen";
	private static final String ROOT_NODE_IDENTIFIER = "root";
	private TreeController treeCtr;
	EPFrontendManager ePFMgr;
	private PortfolioStructure selStructure;
	private AbstractArtefact artefact;
	private PortfolioStructure oldStructure;
	
	@SuppressWarnings("unused")
	public EPCollectStepForm04(UserRequest ureq, WindowControl wControl, Form rootForm, StepsRunContext runContext, int layout,
			String customLayoutPageName, AbstractArtefact artefact) {
		super(ureq, wControl, rootForm, runContext, layout, "step04selectmap");
		ePFMgr = (EPFrontendManager) CoreSpringFactory.getBean("epFrontendManager");
		initForm(this.flc, this, ureq);
	}

	public EPCollectStepForm04(UserRequest ureq, WindowControl wControl, AbstractArtefact artefact, PortfolioStructure oldStructure) {
		super(ureq, wControl, "step04selectmap");
		ePFMgr = (EPFrontendManager) CoreSpringFactory.getBean("epFrontendManager");
		this.artefact = artefact;
		this.oldStructure = oldStructure;
		initForm(this.flc, this, ureq);	
	}
	
	/**
	 * @see org.olat.core.gui.control.generic.wizard.StepFormBasicController#initForm(org.olat.core.gui.components.form.flexible.FormItemContainer,
	 *      org.olat.core.gui.control.Controller, org.olat.core.gui.UserRequest)
	 */
	@SuppressWarnings("unused")
	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {

		List<PortfolioStructure> structs = ePFMgr.getStructureElementsForUser(getIdentity());
		if (structs != null && structs.size() != 0) { 
			AjaxTreeModel treeModel = buildTreeModel();
			treeCtr = new TreeController(ureq, getWindowControl(), translate("step4.my.maps"), treeModel, null);
			treeCtr.setTreeSorting(false, false, false);
			listenTo(treeCtr);
		
			// find last used structure and preselect
			PortfolioStructure lastStruct = ePFMgr.getUsersLastUsedPortfolioStructure(getIdentity());
			if (lastStruct != null) {
				treeCtr.selectPath("/" + ROOT_NODE_IDENTIFIER + getPath(lastStruct));
				selStructure = lastStruct;
			}
			flc.put("treeCtr", treeCtr.getInitialComponent());
		}

		if (!isUsedInStepWizzard()) {
			// add form buttons
			uifactory.addFormSubmitButton("stepform.submit", formLayout);
		}		
	}

	private AjaxTreeModel buildTreeModel() {
		AjaxTreeModel model = new AjaxTreeModel(ROOT_NODE_IDENTIFIER) {

			private boolean firstLevelDone = false;

			@SuppressWarnings("synthetic-access")
			@Override
			public List<AjaxTreeNode> getChildrenFor(String nodeId) {
				List<AjaxTreeNode> children = new ArrayList<AjaxTreeNode>();
				AjaxTreeNode child;
				try {
					List<PortfolioStructure> structs = null;
					if (nodeId.equals(ROOT_NODE_IDENTIFIER)) {
						structs = ePFMgr.getStructureElementsForUser(getIdentity(), ElementType.STRUCTURED_MAP, ElementType.DEFAULT_MAP);
						firstLevelDone = false;
					} else {
						PortfolioStructure selStruct = ePFMgr.loadPortfolioStructureByKey(new Long(nodeId));
						structs = ePFMgr.loadStructureChildren(selStruct);
					}
					if (structs == null || structs.size() == 0) { return null; }
					// add a fake map to choose if no target should be set
					if (!firstLevelDone){
						child = new AjaxTreeNode(NO_MAP_CHOOSEN, translate("no.map.as.target"));
						child.put(AjaxTreeNode.CONF_LEAF, true);
						child.put(AjaxTreeNode.CONF_IS_TYPE_LEAF, true);
						child.put(AjaxTreeNode.CONF_ALLOWDRAG, false);
						child.put(AjaxTreeNode.CONF_ALLOWDROP, false);
						child.put(AjaxTreeNode.CONF_ICON_CSS_CLASS, "b_ep_collection_icon");
						child.put(AjaxTreeNode.CONF_QTIP, translate("no.map.as.target.desc"));
						if (isUsedInStepWizzard()) children.add(child);
						firstLevelDone = true;
					}
					for (PortfolioStructure portfolioStructure : structs) {
						// FXOLAT-436 : skip templateMaps that are closed
						if (portfolioStructure instanceof EPStructuredMap) {
							if( ((EPStructuredMap) portfolioStructure).getStatus().equals(StructureStatusEnum.CLOSED)){
								continue;
							}
						}
						
						String title = portfolioStructure.getTitle();
						if (!isUsedInStepWizzard() && oldStructure.getKey().equals(portfolioStructure.getKey())) {
							title = portfolioStructure.getTitle() + "&nbsp; &nbsp; <-- " + translate("move.artefact.actual.node");
						}						
						child = new AjaxTreeNode(String.valueOf(portfolioStructure.getKey()), title);
						boolean hasChilds = ePFMgr.countStructureChildren(portfolioStructure) > 0;	
						child.put(AjaxTreeNode.CONF_LEAF, !hasChilds);
						child.put(AjaxTreeNode.CONF_IS_TYPE_LEAF, !hasChilds);
						child.put(AjaxTreeNode.CONF_ALLOWDRAG, false);
						child.put(AjaxTreeNode.CONF_ALLOWDROP, false);
						child.put(AjaxTreeNode.CONF_EXPANDED, true);
						child.put(AjaxTreeNode.CONF_DISABLED, portfolioStructure instanceof EPAbstractMap);
						child.put(AjaxTreeNode.CONF_ICON_CSS_CLASS, portfolioStructure.getIcon());
						child.put(AjaxTreeNode.CONF_QTIP, portfolioStructure.getDescription());
						
						children.add(child);
					}
				} catch (JSONException e) {
					throw new OLATRuntimeException("Error while creating tree model for map/page/structure selection", e);
				}
				return children;
			}
		};
		model.setCustomRootIconCssClass("o_st_icon");
		return model;
	}

	/**
	 * save clicked node to selStructure
	 * 
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	@Override
	protected void event(@SuppressWarnings("unused") UserRequest ureq, Controller source, Event event) {
		if (source == treeCtr) {
			if (event instanceof TreeNodeClickedEvent) {
				TreeNodeClickedEvent clickedEvent = (TreeNodeClickedEvent) event;
				String selNode = clickedEvent.getNodeId();
				if (!selNode.equals(ROOT_NODE_IDENTIFIER) && !selNode.equals(NO_MAP_CHOOSEN)) {
					selStructure = ePFMgr.loadPortfolioStructureByKey(new Long(selNode));
				} else if (selNode.equals(NO_MAP_CHOOSEN)) {
					selStructure = null;
				} else {
					treeCtr.selectPath(null);
					selStructure = null;
					this.flc.setDirty(true);
				}
				if (selStructure!=null && selStructure instanceof EPAbstractMap) {
					showWarning("map.not.choosable");
					treeCtr.selectPath(null);
					selStructure = null;
					this.flc.setDirty(true);
				}
			}
		}
	}

	private String getPath(PortfolioStructure pStruct) {
		StringBuffer path = new StringBuffer();
		PortfolioStructure ps = pStruct;
		while (ePFMgr.loadStructureParent(ps) != null) {
			path.insert(0, "/" + ps.getKey().toString());
			ps = ePFMgr.loadStructureParent(ps);
		}
		path.insert(0, "/" + ps.getKey().toString());
		return path.toString();
	}

	/**
	 * @see org.olat.core.gui.control.generic.wizard.StepFormBasicController#formOK(org.olat.core.gui.UserRequest)
	 */
	@Override
	protected void formOK(UserRequest ureq) {
		if (selStructure != null) {
			ePFMgr.setUsersLastUsedPortfolioStructure(getIdentity(), selStructure);
		}
		if (isUsedInStepWizzard()) {
			addToRunContext("selectedStructure", selStructure);
			fireEvent(ureq, StepsEvent.ACTIVATE_NEXT);
		} else {
			if (!selStructure.getKey().equals(oldStructure.getKey())){
				ePFMgr.moveArtefactFromStructToStruct(artefact, oldStructure, selStructure);
				fireEvent(ureq, new EPStructureChangeEvent(EPStructureChangeEvent.CHANGED, selStructure)); // refresh ui
			}
		}
	}

	/**
	 * @see org.olat.core.gui.control.generic.wizard.StepFormBasicController#doDispose()
	 */
	@Override
	protected void doDispose() {
		// nothing
	}

}
