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
* <p>
*/
package org.olat.core.gui.components.form.flexible.impl;

import java.util.Map;

import org.olat.core.gui.components.form.flexible.FormDecorator;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.SpacerElement;
import org.olat.core.util.StringHelper;

/**
 * Description:<br>
 * TODO: patrickb Class Description for FormDecorator
 * 
 * <P>
 * Initial Date: 06.12.2006 <br>
 * 
 * @author patrickb
 */
public class FormDecoratorImpl implements FormDecorator {

	private final FormItemContainer container;

	public FormDecoratorImpl(FormItemContainer container) {
		this.container = container;
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.FormDecorator#hasError(java.lang.String)
	 */
	public boolean hasError(String formItemName) {
		FormItem foco = getFormItem(formItemName);
		return foco == null ? false : foco.hasError();
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.FormDecorator#hasExample(java.lang.String)
	 */
	public boolean hasExample(String formItemName) {
		FormItem foco = getFormItem(formItemName);
		return foco == null ? false : foco.hasExample();
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.FormDecorator#hasLabel(java.lang.String)
	 */
	public boolean hasLabel(String formItemName) {
		FormItem foco = getFormItem(formItemName);
		return foco == null ? false : foco.hasLabel();
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.FormDecorator#isMandatory(java.lang.String)
	 */
	public boolean isMandatory(String formItemName) {
		FormItem foco = getFormItem(formItemName);
		return foco == null ? false : foco.isMandatory();
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.FormDecorator#isEnabled(java.lang.String)
	 */
	public boolean isEnabled(String formItemName) {
		FormItem foco = getFormItem(formItemName);
		return foco == null ? false : foco.isEnabled();
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.FormDecorator#isVisible(java.lang.String)
	 */
	public boolean isVisible(String formItemName) {
		FormItem foco = getFormItem(formItemName);
		return foco == null ? false : foco.isVisible();
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.FormDecorator#getItemId(java.lang.String)
	 */
	public String getItemId(String formItemName) {
		FormItem foco = getFormItem(formItemName);
		return foco == null ? "" : foco.getFormDispatchId();
	}

	/**
	 * @see org.olat.core.gui.components.form.flexible.FormDecorator#isSpacerElement(java.lang.String)
	 */
	public boolean isSpacerElement(String formItemName) {
		FormItem item = getFormItem(formItemName);
		if (item == null)
			return false;
		else
			return (item instanceof SpacerElement);
	}
	
	public String getContainerCssClass() {
		if (container != null && StringHelper.containsNonWhitespace(container.getElementCssClass())) {
			return " " + container.getElementCssClass();
		}
		return "";
	}
	
	public String getElementCssClass(String formItemName) {
		FormItem item = getFormItem(formItemName);
		if (item != null && StringHelper.containsNonWhitespace(item.getElementCssClass())) {
			return " " + item.getElementCssClass();
		}
		return "";
	}

	/**
	 * Internal helper to get a form item for the given name
	 * 
	 * @param formItemName
	 * @return
	 */
	private FormItem getFormItem(String formItemName) {
		Map<String, FormItem> comps = container.getFormComponents();
		FormItem foco = comps.get(formItemName);
		return foco;
	}

}
