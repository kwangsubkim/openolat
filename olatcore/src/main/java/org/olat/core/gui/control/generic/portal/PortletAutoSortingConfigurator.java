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
 * software distributed under the License is distributed on an "AS IS" BASIS,
 * <br>
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. <br>
 * See the License for the specific language governing permissions and <br>
 * limitations under the License.
 * <p>
 * Copyright (c) 1999-2007 at Multimedia- & E-Learning Services (MELS),<br>
 * University of Zurich, Switzerland.
 * <p>
 */

package org.olat.core.gui.control.generic.portal;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.IntegerElement;
import org.olat.core.gui.components.form.flexible.elements.Reset;
import org.olat.core.gui.components.form.flexible.elements.SingleSelection;
import org.olat.core.gui.components.form.flexible.elements.Submit;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.components.form.flexible.impl.elements.FormReset;
import org.olat.core.gui.components.form.flexible.impl.elements.FormSubmit;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;

/**
 * 
 * Description:<br>
 * Configuration Form to configure the automatic sorting criteria of Portlets.
 * 
 * <P>
 * Initial Date:  08.11.2007 <br>
 * @author Lavinia Dumitrescu
 */
public class PortletAutoSortingConfigurator extends FormBasicController{

	private IntegerElement entriesNum;
	private SingleSelection sortingCriteriaSelection;
	private SingleSelection sortingDirectionSelection;
	private Submit submit;
	private Reset reset;
		
	private SortingCriteria sortingCriteria;
	private static final String ASCENDING = "ascending";
	private static final String DESCENDING = "descending";
	
		
	/**
	 * The sorting terms list is configurable.
	 * @param ureq
	 * @param wControl
	 * @param sortingTerms
	 */
	public PortletAutoSortingConfigurator(UserRequest ureq, WindowControl wControl, SortingCriteria sortingCriteria) {
		super(ureq, wControl, "configAutoSorting");
	  this.sortingCriteria = sortingCriteria;			
				
		initForm(ureq);
		setSortingCriteria(sortingCriteria);
	}

	@Override
	protected void doDispose() {
		// TODO Auto-generated method stub
		
	}
	
	
	@Override
	protected void formOK(UserRequest ureq) {		
		sortingCriteria = new SortingCriteria(sortingCriteria.getSortingTermsList());
		try {
		  int maxEntries = entriesNum.getIntValue(); 
		  sortingCriteria.setMaxEntries(maxEntries);
		} catch(NumberFormatException ex) {	
			//nothing to do if wrong entry
		}
		
		String selectedSortingTermKey = sortingCriteriaSelection.getSelectedKey();
		sortingCriteria.setSortingTerm(Integer.valueOf(selectedSortingTermKey).intValue());
		
		sortingCriteria.setAscending(sortingDirectionSelection.getSelectedKey().equals(ASCENDING));
		
	  fireEvent(ureq, Event.DONE_EVENT);		
	}
	
	@Override
	protected void formResetted(UserRequest ureq) {
		fireEvent(ureq, Event.CANCELLED_EVENT);
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		// sorting form main layout container using default layout
		FormLayoutContainer sortingFormContainer = FormLayoutContainer.createDefaultFormLayout("sortingFormContainer", getTranslator());
		formLayout.add(sortingFormContainer);
		
		setFormTitle("portlet.auto.sorting.form.title");
		
		final int defaultDisplaySize = 2;
		//creates and adds the integer element
		entriesNum = uifactory.addIntegerElement("entriesNumTextField", "portlet.auto.sorting.num_entries", 6, sortingFormContainer);
		//configure the integer element
		entriesNum.setDisplaySize(defaultDisplaySize);
		entriesNum.setMaxValueCheck(99, "portlet.sorting.auto.notgreaterthan");
		entriesNum.setMinValueCheck(1, "portlet.sorting.auto.notsmallerthan");
		entriesNum.setNotEmptyCheck("portlet.sorting.auto.mustbefilled");
		entriesNum.setMandatory(true);
		entriesNum.setEnabled(true);

		// layout container to layout the criterias sorting type and sorting order horizontally
		FormLayoutContainer criteriasContainer = FormLayoutContainer.createHorizontalFormLayout("criteriasContainer", getTranslator());
		criteriasContainer.setLabel("portlet.auto.sorting.term", null);
		sortingFormContainer.add(criteriasContainer);
		
		String[] selectionKeys = null;
		String[] selectionValues = null;		
		if(sortingCriteria.getSortingTermsList().contains(SortingCriteria.TYPE_SORTING) 
				&& sortingCriteria.getSortingTermsList().contains(SortingCriteria.ALPHABETICAL_SORTING)
				&& sortingCriteria.getSortingTermsList().contains(SortingCriteria.DATE_SORTING)) {
			selectionKeys = new String[]{String.valueOf(SortingCriteria.TYPE_SORTING), String.valueOf(SortingCriteria.ALPHABETICAL_SORTING), String.valueOf(SortingCriteria.DATE_SORTING)};
			selectionValues = new String [] {getTranslator().translate("portlet.auto.sorting.type"),getTranslator().translate("portlet.auto.sorting.alphabetical"),getTranslator().translate("portlet.auto.sorting.date")};
		} else if(sortingCriteria.getSortingTermsList().contains(SortingCriteria.ALPHABETICAL_SORTING)
				&& sortingCriteria.getSortingTermsList().contains(SortingCriteria.DATE_SORTING)) {
			selectionKeys = new String[]{String.valueOf(SortingCriteria.ALPHABETICAL_SORTING), String.valueOf(SortingCriteria.DATE_SORTING)};
			selectionValues = new String [] {getTranslator().translate("portlet.auto.sorting.alphabetical"),getTranslator().translate("portlet.auto.sorting.date")};
		} else if (sortingCriteria.getSortingTermsList().contains(SortingCriteria.ALPHABETICAL_SORTING))	{
			selectionKeys = new String[]{String.valueOf(SortingCriteria.ALPHABETICAL_SORTING)};
			selectionValues = new String [] {getTranslator().translate("portlet.auto.sorting.alphabetical")};
		} else if (sortingCriteria.getSortingTermsList().contains(SortingCriteria.DATE_SORTING))	{
			selectionKeys = new String[]{String.valueOf(SortingCriteria.DATE_SORTING)};
			selectionValues = new String [] {getTranslator().translate("portlet.auto.sorting.date")};
		}
				
		final String[] keysIn = selectionKeys;		
		final String[] translatedKeys = selectionValues;
		sortingCriteriaSelection = uifactory.addRadiosVertical("criteriaSelection", null, formLayout, keysIn, translatedKeys);		
		if (sortingCriteria.getSortingTermsList().contains(SortingCriteria.ALPHABETICAL_SORTING)) {
			sortingCriteriaSelection.select(String.valueOf(SortingCriteria.ALPHABETICAL_SORTING), true);
		} else if (sortingCriteria.getSortingTermsList().contains(SortingCriteria.DATE_SORTING)) {
			sortingCriteriaSelection.select(String.valueOf(SortingCriteria.DATE_SORTING), true);
		}
		criteriasContainer.add(sortingCriteriaSelection);		
		
		//add sortingDirectionSelection
		final String[] sortingDirectionKeys = new String[]{ASCENDING, DESCENDING};
		final String[] sortingDirectionTranslatedKeys = new String [] {getTranslator().translate("portlet.auto.sorting.ascending"),getTranslator().translate("portlet.auto.sorting.descending")};
		sortingDirectionSelection = uifactory.addRadiosVertical("sortingDirectionSelection", null, formLayout, sortingDirectionKeys, sortingDirectionTranslatedKeys);
		//sortingDirectionSelection.setLabel("portlet.auto.sorting.direction", null);
		sortingDirectionSelection.select(ASCENDING, true);
		criteriasContainer.add(sortingDirectionSelection);		
		
		// button layout container horizontally. add this one to form layout to not have the indent
		FormLayoutContainer buttonContainer = FormLayoutContainer.createButtonLayout("buttonContainer", getTranslator());
		formLayout.add(buttonContainer);
		submit = new FormSubmit("submit","submit");
		buttonContainer.add(submit);
		reset = new FormReset("reset","reset");
		buttonContainer.add(reset);
		
	}
	
	public SortingCriteria getSortingCriteria() {
		return sortingCriteria;
	}

	void setSortingCriteria(SortingCriteria sortingCriteria) {
		if(sortingCriteria!=null) {
		  this.sortingCriteria = sortingCriteria;
		  entriesNum.setIntValue(sortingCriteria.getMaxEntries());
		  sortingCriteriaSelection.select(String.valueOf(sortingCriteria.getSortingTerm()), true);
		  sortingDirectionSelection.select(sortingCriteria.isAscending() ? ASCENDING : DESCENDING, true);
		}
	}
	

}
