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
package org.olat.portfolio;

import org.olat.NewControllerFactory;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.id.context.DefaultContextEntryControllerCreator;
import org.olat.home.HomeSite;
import org.olat.portfolio.model.structel.EPDefaultMap;

/**
 * Description:<br>
 * Load a context entry creator
 * runtime
 * 
 * <P>
 * Initial Date: 03.08.2010 <br>
 * 
 * @author Roman Haag, roman.haag@frentix.com, http://www.frentix.com
 */
public class EPMyMapsExtension {

	public EPMyMapsExtension() {

		NewControllerFactory.getInstance().addContextEntryControllerCreator(EPDefaultMap.class.getSimpleName(), new DefaultContextEntryControllerCreator(){
			
			@Override
			public Controller createController(ContextEntry ce, UserRequest ureq, WindowControl wControl) {
				return null;
			}

			@Override
			public String getTabName(ContextEntry ce, UserRequest ureq) {
				// opens in home-tab
				return null;
			}

			@Override
			public String getSiteClassName(ContextEntry ce, UserRequest ureq) {
				return HomeSite.class.getName();
			}

			@Override
			public boolean validateContextEntryAndShowError(ContextEntry ce, UserRequest ureq, WindowControl wControl) {
				return true;
			}
			
		});	
	}
}
