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
 * BPS Bildungsportal Sachsen GmbH, http://www.bps-system.de
 * <p>
 */
package org.olat.course.condition.additionalconditions;

import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormLayoutContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.util.StringHelper;
import org.olat.course.run.RunMainController;

import de.bps.course.nodes.CourseNodePasswordManager;
import de.bps.course.nodes.CourseNodePasswordManagerImpl;

/**
 * Initial Date:  17.09.2010 <br>
 * @author blaw
 * @author srosse, stephane.rosse@frentix.com
 */
public class PasswordVerificationController extends FormBasicController {
	private final PasswordCondition condition;
	
	private TextElement pwElement;

	protected PasswordVerificationController(UserRequest ureq, WindowControl wControl, PasswordCondition condition) {
		super(ureq, wControl);
		this.condition = condition;
		initForm(ureq);
	}
	
	@Override
	protected void doDispose() {
		// nothing to do
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		setFormTitle("password.title");
		setFormWarning("password.inputorder");

		pwElement = uifactory.addPasswordElement("password.field", "password.field", 255, "", formLayout);
		pwElement.setMandatory(true);
		pwElement.setDisplaySize(30);
		
		FormLayoutContainer buttonLayout = FormLayoutContainer.createButtonLayout("button_layout", getTranslator());
		formLayout.add(buttonLayout);
		uifactory.addFormSubmitButton("save", "password.submit", buttonLayout);
	}

	@Override
	protected void formOK(UserRequest ureq) {
		fireEvent(ureq, new Event(RunMainController.REBUILD));
	}

	@Override
	protected boolean validateFormLogic(UserRequest ureq) {
		boolean valid = false;
		pwElement.clearError();
		
		if(!StringHelper.containsNonWhitespace(pwElement.getValue())) {
			pwElement.setErrorKey("form.legende.mandatory", new String[] {});
		} else {
			condition.setAnswer(pwElement.getValue());
			valid = condition.evaluate();
			if (!valid) {
				pwElement.setErrorKey("password.incorrect", new String[] {});
			} else {
				CourseNodePasswordManager cnpm = CourseNodePasswordManagerImpl.getInstance();
				cnpm.updatePwd(ureq.getIdentity(), condition.getNodeIdentifier(), condition.getCourseId().toString(), pwElement.getValue());
			}
		}
		
		return valid;
	}
}