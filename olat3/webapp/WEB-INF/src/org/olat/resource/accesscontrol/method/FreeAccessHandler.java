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
* Copyright (c) 2008 frentix GmbH, Switzerland<br>
* <p>
*/

package org.olat.resource.accesscontrol.method;

import java.util.Locale;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.impl.Form;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.translator.Translator;
import org.olat.core.util.Util;
import org.olat.resource.accesscontrol.model.OfferAccess;
import org.olat.resource.accesscontrol.ui.AbstractConfigurationMethodController;
import org.olat.resource.accesscontrol.ui.FreeAccessController;
import org.olat.resource.accesscontrol.ui.TokenAccessController;

/**
 * 
 * Description:<br>
 * Implementation of the access handler for the free method
 * 
 * <P>
 * Initial Date:  18 avr. 2011 <br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class FreeAccessHandler implements AccessMethodHandler {
	
	public static final String METHOD_TYPE = "free.method";

	@Override
	public String getType() {
		return METHOD_TYPE;
	}

	@Override
	public String getMethodName(Locale locale) {
		Translator translator = Util.createPackageTranslator(TokenAccessController.class, locale);
		return translator.translate("free.method");
	}

	@Override
	public FreeAccessController createAccessController(UserRequest ureq, WindowControl wControl, OfferAccess link, Form form) {
		if(form == null) {
			return new FreeAccessController(ureq, wControl, link);
		} else {
			return new FreeAccessController(ureq, wControl, link, form);
		}
	}

	@Override
	public AbstractConfigurationMethodController createConfigurationController(UserRequest ureq, WindowControl wControl, OfferAccess link) {
		return null;
	}

	@Override
	public boolean checkArgument(OfferAccess link, Object argument) {
		return true;
	}
}