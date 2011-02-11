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

package org.olat.core.id;

import java.util.Date;

/**
 * A <b>CreateInfo</b> is implemented by persistable objects
 * to provide a uniform way to present creation date
 * 
 * @author Andreas Ch. Kapp
 * @author guido
 *
 */
public interface CreateInfo {
	/**
	 * Get creation date. 
	 * @return Date. The creation date
	 */
	Date getCreationDate();

}
