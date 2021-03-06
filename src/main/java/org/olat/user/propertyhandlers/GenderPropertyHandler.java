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
package org.olat.user.propertyhandlers;

import java.util.Locale;
import java.util.Map;

import org.olat.core.gui.components.form.ValidationError;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.FormUIFactory;
import org.olat.core.gui.translator.Translator;
import org.olat.core.id.User;
import org.olat.core.util.Util;
import org.olat.core.util.i18n.I18nModule;
import org.olat.user.AbstractUserPropertyHandler;
import org.olat.user.UserManager;

/**
 * <h3>Description:</h3>
 * The gender user property implemente a user attribute to specify the users gender
 * in a drop down. 
 * <p>
 * Initial Date: 23.07.2007 <br>
 * 
 * @author Florian Gnaegi, frentix GmbH, http://www.frentix.com
 */

public class GenderPropertyHandler extends AbstractUserPropertyHandler {
	
	private static final String[] keys = new String[] { "male", "female", "-" };
	
	/**
	 * Helper method to create translated values that correspond with the static keys
	 * @param locale
	 * @return
	 */
	
	private String[] getTranslatedValues(Locale locale) {
		Translator trans = Util.createPackageTranslator(this.getClass(), locale);
		String[] values = new String[] { 
				trans.translate(i18nFormElementLabelKey() + "." +keys[0]), 
				trans.translate(i18nFormElementLabelKey() + "." +keys[1]),
				trans.translate(i18nFormElementLabelKey() + "." +keys[2])
		};
		return values;
	}


	/**
	 * @see org.olat.user.AbstractUserPropertyHandler#getUserProperty(org.olat.core.id.User, java.util.Locale)
	 */
	@Override
	public String getUserProperty(User user, Locale locale) {
		Translator myTrans;
		if (locale == null) {
			myTrans = Util.createPackageTranslator(this.getClass(), I18nModule.getDefaultLocale());			
		} else {
			myTrans = Util.createPackageTranslator(this.getClass(), locale);
		}
		String internalValue = getInternalValue(user);
		String displayValue = myTrans.translate("form.name.gender." + internalValue);
		return displayValue;
	}

	/**
	 * @see org.olat.user.propertyhandlers.UserPropertyHandler#updateUserFromFormItem(org.olat.core.id.User, org.olat.core.gui.components.form.flexible.FormItem)
	 */
	@Override
	public void updateUserFromFormItem(User user, FormItem formItem) {
		String internalValue = getStringValue(formItem);
		setInternalValue(user, internalValue);
	}

	/**
	 * @see org.olat.user.propertyhandlers.UserPropertyHandler#getStringValue(org.olat.core.gui.components.form.flexible.FormItem)
	 */
	@Override
	public String getStringValue(FormItem formItem) {
		return ((org.olat.core.gui.components.form.flexible.elements.SingleSelection) formItem).getSelectedKey();
	}

	/**
	 * @see org.olat.user.propertyhandlers.UserPropertyHandler#getStringValue(java.lang.String, java.util.Locale)
	 */
	@Override
	public String getStringValue(String displayValue, Locale locale) {
		// This should be refactored, but currently the bulk change does not work
		// otherwhise. When changing this here, the isValidValue must also to
		// changed to work with the real display value

		// use default: use key as value
		return displayValue;
	}


	/**
	 *  
	 * @see org.olat.user.propertyhandlers.UserPropertyHandler#addFormItem(java.util.Locale, org.olat.core.id.User, java.lang.String, boolean, org.olat.core.gui.components.form.flexible.FormItemContainer)
	 */
	@Override
	public FormItem addFormItem(Locale locale, User user, String usageIdentifyer, boolean isAdministrativeUser,	FormItemContainer formItemContainer) {
		org.olat.core.gui.components.form.flexible.elements.SingleSelection	genderElem = null;
		//genderElem = FormUIFactory.getInstance().addDropdownSingleselect(getName(), i18nFormElementLabelKey(), formItemContainer, keys, getTranslatedValues(locale), null);
		genderElem = FormUIFactory.getInstance().addRadiosVertical(getName(), i18nFormElementLabelKey(), formItemContainer, keys, getTranslatedValues(locale));
		
		genderElem.select(user == null ? "-" : this.getInternalValue(user), true);
		
		UserManager um = UserManager.getInstance();
		if ( um.isUserViewReadOnly(usageIdentifyer, this) && ! isAdministrativeUser) {
			genderElem.setEnabled(false);
		}
		if (um.isMandatoryUserProperty(usageIdentifyer, this)) {
			genderElem.setMandatory(true);
		}
		return genderElem;
	}
	
	/**
	 * @see org.olat.user.AbstractUserPropertyHandler#getInternalValue(org.olat.core.id.User)
	 */
	@Override
	public String getInternalValue(User user) {
		String value = super.getInternalValue(user);
		return (value == null ? "-" : value); // default		
	}
	
	/**
	 * @see org.olat.user.propertyhandlers.UserPropertyHandler#isValid(org.olat.core.gui.components.form.flexible.FormItem, java.util.Map)
	 */
	@Override
	public boolean isValid(User user, FormItem formItem, Map<String,String> formContext) {
		if (formItem.isMandatory()) {
			org.olat.core.gui.components.form.flexible.elements.SingleSelection sse = (org.olat.core.gui.components.form.flexible.elements.SingleSelection) formItem;
			// when mandatory, the - must not be selected
			if (sse.getSelectedKey().equals("-")) {
				sse.setErrorKey("gender.error", null);
				return false;
			}
		}
		return true;
	}


	/**
	 * @see org.olat.user.propertyhandlers.UserPropertyHandler#isValidValue(java.lang.String, org.olat.core.gui.components.form.ValidationError, java.util.Locale)
	 */
	@Override
	public boolean isValidValue(User user, String value, ValidationError validationError, Locale locale) {
		if (value != null) {
			for (int i = 0; i < keys.length; i++) {
				String key = keys[i];
				if (key.equals(value)) {
					return true;
				}
			}
			validationError.setErrorKey("gender.error");
			return false;
		}
		// null values are ok
		return true;
	}

}
