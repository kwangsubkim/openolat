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
* Initial code contributed and copyrighted by<br>
* JGS goodsolutions GmbH, http://www.goodsolutions.ch
* <p>
*/
package org.olat.core.util.prefs.db;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.olat.core.CoreSpringFactory;
import org.olat.core.id.Identity;
import org.olat.core.util.prefs.Preferences;
import org.olat.core.util.prefs.PreferencesStorage;

/**
 * Description:<br>
 * 
 * <P>
 * Initial Date:  21.06.2006 <br>
 *
 * @author Felix Jost
 */
public class DbPrefs implements Preferences, Serializable {

	private static final long serialVersionUID = 3828851618949061953L;

	// keys: prefs-keys; values: any Prefs-Objects
	private Map<String,Object> prefstore = new HashMap<String,Object>();

	private transient Identity owner;
  
	// true: don't save to disk, only in ram
	private transient boolean isTransient = false; 
	
	public DbPrefs() {
		// must have a default constructor for serialization!
	}

	public boolean isTransient() {
		return isTransient;
	}

	public void setTransient(boolean isTransient) {
		this.isTransient = isTransient;
	}

	public void save() {
		if (!isTransient) {
			PreferencesStorage storage = (PreferencesStorage)CoreSpringFactory.getBean("core.preferences.PreferencesStorage");
			storage.updatePreferencesFor(this, owner);
		}
	}
		
	/**
	 * @param attributedClass
	 * @param key
	 * @return Object
	 */
	public Object get(Class<?> attributedClass, String key) {
		return prefstore.get(attributedClass.getName()+"::"+key);
	}

	/**
	 * @see org.olat.core.util.prefs.Preferences#get(java.lang.Class, java.lang.String, java.lang.Object)
	 */
	public Object get(Class<?> attributedClass, String key, Object defaultValue) {
		Object value = get(attributedClass, key);
		if (value == null) return defaultValue;
		return value;
	}

	/**
	 * @param attributedClass
	 * @param key
	 * @param value
	 * TODO: make value not object, but basetypemap or such?
	 */
	public void put(Class<?> attributedClass, String key, Object value) {
		prefstore.put(attributedClass.getName()+"::"+key, value);
	}

	/**
	 * @param identity
	 */
	void setIdentity(Identity identity) {
		this.owner = identity;
	}

	/**
	 * 
	 * @see org.olat.core.util.prefs.Preferences#putAndSave(java.lang.Class, java.lang.String, java.lang.Object)
	 */
	public void putAndSave(Class<?> attributedClass, String key, Object value) {
		put(attributedClass, key, value);
		save();
	}

	/**
	 * 
	 * @see org.olat.core.util.prefs.Preferences#findPrefByKey(java.lang.String)
	 */
	public Object findPrefByKey(String partOfKey) {
		for (Iterator<String> iterator = prefstore.keySet().iterator(); iterator.hasNext();) {
			String key = iterator.next();
			if (key.endsWith(partOfKey)) {
				return prefstore.get(key);
			}
		}
		return null;
	}

	
	public  String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("total of stored kv-pairs: ").append(prefstore.size());
		for (Entry<String,Object> entry : prefstore.entrySet()) {
			sb.append("----").append(entry.getKey()).append("=").append(entry.getValue());
		}
		return sb.toString();
	}
}
