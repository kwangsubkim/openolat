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

/**
 * 
 * Initial date: 15.02.2013<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class StaticFlexiColumnModel extends AbstractFlexiColumnModel {

	private final String action;
	
	/**
	 * Used the standard renderer
	 * @param headerKey
	 * @param label
	 * @param action
	 */
	public StaticFlexiColumnModel(String headerKey, String label, String action) {
		super(headerKey, -1, FlexiColumnModel.ALIGNMENT_LEFT, new StaticFlexiCellRenderer(label, action));
		this.action = action;
	}
	
	/**
	 * Use a custom renderer
	 * @param headerKey
	 * @param columnIndex
	 * @param action
	 * @param renderer
	 */
	public StaticFlexiColumnModel(String headerKey, int columnIndex, String action, FlexiCellRenderer renderer) {
		super(headerKey, columnIndex, FlexiColumnModel.ALIGNMENT_LEFT, renderer);
		this.action = action;
	}
	
	public StaticFlexiColumnModel(String headerKey, int columnIndex, String action, boolean sortable, String sortedKey, FlexiCellRenderer renderer) {
		super(headerKey, columnIndex, FlexiColumnModel.ALIGNMENT_LEFT, sortable, sortedKey, renderer);
		this.action = action;
	}

	@Override
	public String getAction() {
		return action;
	}
}
