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

package org.olat.resource.accesscontrol.manager;

import java.util.List;

import org.olat.core.id.Identity;
import org.olat.resource.accesscontrol.model.AccessMethod;
import org.olat.resource.accesscontrol.model.Offer;
import org.olat.resource.accesscontrol.model.OfferAccess;

/**
 * 
 * Description:<br>
 * Manage the access methods to the resources
 * 
 * <P>
 * Initial Date:  18 avr. 2011 <br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public interface ACMethodManager {
	
	/**
	 * Get the list of access methods which a user /author can use
	 * @param identity
	 * @return List of access methods
	 */
	public List<AccessMethod> getAvailableMethods(Identity identity);
	
	/**
	 * Return the list of access methods of a specific type.
	 * @param type
	 * @return List of access methods
	 */
	public List<AccessMethod> getAvailableMethodsByType(Class<? extends AccessMethod> type);
	
	/**
	 * Return a list of links offer to method for the specified offer.
	 * @param offer
	 * @param valid
	 * @return List of link offer to method
	 */
	public List<OfferAccess> getOfferAccess(Offer offer, boolean valid);
	
	/**
	 * Return a list of links offer to access method for the specified offers.
	 * @param offer
	 * @param valid
	 * @return List of link offer to access method
	 */
	public List<OfferAccess> getOfferAccess(List<Offer> offers, boolean valid);
	
	/**
	 * Create a link between offer and access method. The link is not persisted
	 * on the database with this method.
	 * @param offer
	 * @param method
	 * @return
	 */
	public OfferAccess createOfferAccess(Offer offer, AccessMethod method);
	
	/**
	 * The link is not really deleted on the database but set as invalid.
	 * @param link
	 */
	public void delete(OfferAccess link);
	
	/**
	 * Persist/update the link offer to access method.
	 * @param link
	 */
	public void save(OfferAccess link);

}