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
 * Copyright (c) frentix GmbH<br>
 * http://www.frentix.com<br>
 * <p>
 */

package org.olat.group.ui.main;

import java.util.Locale;

import org.olat.core.gui.components.table.CustomCellRenderer;
import org.olat.core.gui.render.Renderer;
import org.olat.core.gui.render.StringOutput;

/**
 * 
 * Description:<br>
 * TODO: srosse Class Description for BGAccessControlledCellRenderer
 * 
 * <P>
 * Initial Date:  18 avr. 2011 <br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
//fxdiff VCRP-1,2: access control of resources
public class BGAccessControlledCellRenderer implements CustomCellRenderer {

	@Override
	public void render(StringOutput sb, Renderer renderer, Object val, Locale locale, int alignment, String action) {
		if(val instanceof Boolean) {
			boolean acessControlled = ((Boolean)val).booleanValue();
			if(acessControlled) {
				sb.append("<span class='b_with_small_icon_left b_group_accesscontrolled'>").append("&#160;&#160;").append("</span>");
			}
		}
	}
}