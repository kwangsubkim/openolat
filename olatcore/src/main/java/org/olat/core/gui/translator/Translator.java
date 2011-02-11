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
* Copyright (c) 1999-2006 at Multimedia- & E-Learning Services (MELS),<br>
* University of Zurich, Switzerland.
* <p>
*/ 

package org.olat.core.gui.translator;

import java.util.Locale;

import org.apache.log4j.Level;
/**
 * @author Felix Jost
 */
public interface Translator {
	/**
	 * A key that can't be translated should start with this error
	 */
	public static final String NO_TRANSLATION_ERROR_PREFIX = "no translation::::";

	/**
	 * @param key
	 * @return
	 */
	public String translate(String key);

	/**
	 * @param key
	 * @param args
	 * @return
	 */
	public String translate(String key, String[] args);

	/**
	 * Same as translate(String,String[]) but allows to specify the level with
     * which a possible missing translation is issued.
	 * @param key
	 * @param args
     * @param missingTranslationLogLevel one of Level.OFF,Level.INFO,Level.WARN,Level.ERROR
	 * @return
	 */
	public String translate(String key, String[] args, Level missingTranslationLogLevel);

	/**
	 * @param key
	 * @param args the args to translate, may be null
	 * @param fallBackToDefaultLocale  if true fall back to configurated default language. 
	 * @return
	 */
	public String translate(String key, String[] args, boolean fallBackToDefaultLocale);

	/**
	 * @return
	 */
	public Locale getLocale();

	/**
	 * @param locale
	 */
	public void setLocale(Locale locale);


}