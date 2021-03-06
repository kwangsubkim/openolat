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
package org.olat.core.gui.components.form.flexible.impl.elements.table;


import org.olat.core.gui.components.form.flexible.impl.Form;
import org.olat.core.gui.components.form.flexible.impl.FormJSHelper;
import org.olat.core.gui.components.form.flexible.impl.NameValuePair;
import org.olat.core.gui.render.StringOutput;
import org.olat.core.gui.render.URLBuilder;
import org.olat.core.gui.translator.Translator;
import org.olat.core.util.StringHelper;

/**
 * 
 * Initial date: 15.02.2013<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class StaticFlexiCellRenderer implements FlexiCellRenderer {

	private String label;
	private String action;
	private String cssClass;
	private FlexiCellRenderer labelDelegate;
	
	public StaticFlexiCellRenderer(String label, String action) {
		this(label, action, null);
	}
	
	public StaticFlexiCellRenderer(String label, String action, String cssClass) {
		this.label = label;
		this.action = action;
		this.cssClass = cssClass;
	}
	
	public StaticFlexiCellRenderer(String action, FlexiCellRenderer labelDelegate) {
		this.labelDelegate = labelDelegate;
		this.action = action;
	}

  /**
   * 
   * @param target
   * @param cellValue
   * @param translator
   */	
	@Override
	public void render(StringOutput target, Object cellValue, int row, FlexiTableComponent source,
			URLBuilder ubu, Translator translator) {
		
		String action = getAction();
		if(StringHelper.containsNonWhitespace(action)) {
			FlexiTableElementImpl ftE = source.getFlexiTableElement();
			String id = source.getFormDispatchId();
			Form rootForm = ftE.getRootForm();
			NameValuePair pair = new NameValuePair(action, Integer.toString(row));
			String jsCode = FormJSHelper.getXHRFnCallFor(rootForm, id, 1, pair);
			target.append("<a href=\"javascript:").append(jsCode).append("\"");
			if(StringHelper.containsNonWhitespace(cssClass)) {
				target.append(" class=\"").append(cssClass).append("\"");
			}
			target.append(">");
			if(labelDelegate == null) {
				target.append(getLabel());
			} else {
				labelDelegate.render(target, cellValue, row, source, ubu, translator);
			}
			target.append("</a>");
		}	else if(labelDelegate == null) {
			target.append(getLabel());
		} else {
			labelDelegate.render(target, cellValue, row, source, ubu, translator);
		}
	}
	
	protected String getAction() {
		return action;
	}
	
	protected String getLabel() {
		return label;
	}
}
