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

package org.olat.ims.qti.editor.tree;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.tabbedpane.TabbedPane;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.tabbable.TabbableController;
import org.olat.core.gui.translator.Translator;
import org.olat.core.util.memento.Memento;
import org.olat.ims.qti.editor.QTIEditorMainController;
import org.olat.ims.qti.editor.QTIEditorPackage;
import org.olat.ims.qti.editor.SectionController;
import org.olat.ims.qti.editor.beecom.objects.QTIObject;
import org.olat.ims.qti.editor.beecom.objects.Section;

/**
 * Initial Date: Nov 21, 2004 <br>
 * @author patrick
 */
public class SectionNode extends GenericQtiNode {

	private Section section;
	private QTIEditorPackage qtiPackage;
	private TabbedPane myTabbedPane;

	/**
	 * @param theSection
	 * @param qtiPackage
	 */
	public SectionNode(Section theSection, QTIEditorPackage qtiPackage) {
		section = theSection;
		this.qtiPackage = qtiPackage;
		setMenuTitleAndAlt(section.getTitle());
		setUserObject(section.getIdent());
		setIconCssClass("o_mi_qtisection");
	}

	/**
	 * Set's the node's title and alt text (truncates title)
	 * @param title
	 */
	public void setMenuTitleAndAlt(String title) {
		super.setMenuTitleAndAlt(title);
		section.setTitle(title);
	}
	
	/**
	 * @see org.olat.ims.qti.editor.tree.IQtiNode#createRunController(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.control.WindowControl)
	 */
	public Controller createRunController(UserRequest ureq, WindowControl wControl) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * @see org.olat.ims.qti.editor.tree.GenericQtiNode#createEditTabbedPane(org.olat.core.gui.UserRequest, org.olat.core.gui.control.WindowControl, org.olat.core.gui.translator.Translator, QTIEditorMainController)
	 */
	public TabbedPane createEditTabbedPane(UserRequest ureq, WindowControl wControl, Translator trnsltr, QTIEditorMainController editorMainController) {
		if (myTabbedPane == null) {
			myTabbedPane = new TabbedPane("tabbedPane", ureq.getLocale());
			TabbableController tabbCntrllr = new SectionController(section, qtiPackage, ureq, wControl, editorMainController.isRestrictedEdit());
			tabbCntrllr.addTabs(myTabbedPane);
			tabbCntrllr.addControllerListener(editorMainController);
		}
		return myTabbedPane;
	}

	/**
	 * @see org.olat.ims.qti.editor.tree.IQtiNode#insertQTIObjectAt(org.olat.ims.qti.editor.beecom.objects.QTIObject, int)
	 */
	public void insertQTIObjectAt(QTIObject object, int position) {
		List items = section.getItems();
		items.add(position, object);
	}

	/**
	 * @see org.olat.ims.qti.editor.tree.IQtiNode#removeQTIObjectAt(int)
	 */
	public QTIObject removeQTIObjectAt(int position) {
		List items = section.getItems();
		return (QTIObject)items.remove(position);
	}

	/**
	 * @see org.olat.ims.qti.editor.tree.IQtiNode#getQTIObjectAt(int)
	 */
	public QTIObject getQTIObjectAt(int position) {
		List items = section.getItems();
		return (QTIObject)items.get(position);
	}

	/**
	 * @see org.olat.ims.qti.editor.tree.IQtiNode#getUnderlyingQTIObject()
	 */
	public QTIObject getUnderlyingQTIObject() {
		return section;
	}

	public Memento createMemento() {
		//so far only TITLE and OBJECTIVES are stored in the memento
		QtiNodeMemento qnm = new  QtiNodeMemento();
		Map qtiState = new HashMap();
		qtiState.put("ID",section.getIdent());
		qtiState.put("TITLE",section.getTitle());
		qtiState.put("OBJECTIVES",section.getObjectives());
		qnm.setQtiState(qtiState);
		return qnm;
	}

	public void setMemento(Memento state) {
		// TODO Auto-generated method stub
		
	}

	public String createChangeMessage(Memento mem) {
		String retVal = null;
		if(mem instanceof QtiNodeMemento){
			QtiNodeMemento qnm = (QtiNodeMemento)mem;
			Map qtiState = qnm.getQtiState();
			String oldTitle = (String)qtiState.get("TITLE");
			String newTitle = section.getTitle();
			String titleChange = null;
			String oldObjectives  = (String)qtiState.get("OBJECTIVES");
			String newObjectives = section.getObjectives();
			String objectChange = null;
			retVal = "\nSection metadata changed:";
			if((oldTitle!=null && !oldTitle.equals(newTitle))||(newTitle!=null && !newTitle.equals(oldTitle))){
				titleChange ="\n\nold title: \n\t"+ formatVariable(oldTitle)+"\n\nnew title: \n\t"+formatVariable(newTitle);
				retVal += titleChange;
			}
			if((oldObjectives!=null && !oldObjectives.equals(newObjectives))||(newObjectives!=null && !newObjectives.equals(oldObjectives))){
				objectChange ="\n\nold objectives: \n\t"+formatVariable(oldObjectives)+"\n\nnew objectives: \n\t"+formatVariable(newObjectives);
				retVal += objectChange;
			}
			return retVal;
		}
		return "undefined";
	}

}