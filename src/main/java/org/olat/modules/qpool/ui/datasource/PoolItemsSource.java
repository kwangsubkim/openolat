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
package org.olat.modules.qpool.ui.datasource;

import java.util.Collections;
import java.util.List;

import org.olat.core.id.Identity;
import org.olat.core.id.Roles;
import org.olat.modules.qpool.Pool;
import org.olat.modules.qpool.QuestionItem;
import org.olat.modules.qpool.QuestionItemShort;

/**
 * 
 * Initial date: 26.06.2013<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class PoolItemsSource extends DefaultItemsSource {
	
	private final Pool pool;
	
	public PoolItemsSource(Identity me, Roles roles, Pool pool) {
		super(me, roles, pool.getName());
		this.pool = pool;
		getDefaultParams().setPoolKey(pool.getKey());
	}
	
	@Override
	public void removeFromSource(List<QuestionItemShort> items) {
		qpoolService.removeItemsInPool(items, pool);
	}

	@Override
	public int postImport(List<QuestionItem> items) {
		if(items == null || items.isEmpty()) return 0;
		qpoolService.addItemsInPools(items, Collections.singletonList(pool), false);
		return items.size();
	}

	@Override
	public boolean isDeleteEnabled() {
		return false;
	}
}
