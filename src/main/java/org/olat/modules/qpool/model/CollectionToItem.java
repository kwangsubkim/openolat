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
package org.olat.modules.qpool.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

import org.hibernate.annotations.GenericGenerator;
import org.olat.core.id.CreateInfo;
import org.olat.core.id.Persistable;
import org.olat.modules.qpool.QuestionItemCollection;

/**
 * 
 * Initial date: 22.02.2013<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
@Entity(name="qcollection2item")
@Table(name="o_qp_collection_2_item")
public class CollectionToItem implements CreateInfo, Persistable  {
	
	private static final long serialVersionUID = -666092954369833281L;

	@Id
  @GeneratedValue(generator = "system-uuid")
  @GenericGenerator(name = "system-uuid", strategy = "hilo")
	@Column(name="id", nullable=false, unique=true, insertable=true, updatable=false)
	private Long key;
	
	@Temporal(TemporalType.TIMESTAMP)
	@Column(name="creationdate", nullable=false, insertable=true, updatable=false)
	private Date creationDate;
	
	@ManyToOne(targetEntity=ItemCollectionImpl.class,fetch=FetchType.LAZY,optional=false)
	@JoinColumn(name="fk_collection_id", nullable=false, updatable=false)
	private QuestionItemCollection collection;
	
	@ManyToOne(targetEntity=QuestionItemImpl.class,fetch=FetchType.LAZY,optional=false)
	@JoinColumn(name="fk_item_id", nullable=false, updatable=false)
	private QuestionItemImpl item;

	@Override
	public Long getKey() {
		return key;
	}
	
	public void setKey(Long key) {
		this.key = key;
	}
	
	@Override
	public Date getCreationDate() {
		return creationDate;
	}
	
	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}
	
	public QuestionItemCollection getCollection() {
		return collection;
	}

	public void setCollection(QuestionItemCollection collection) {
		this.collection = collection;
	}

	public QuestionItemImpl getItem() {
		return item;
	}

	public void setItem(QuestionItemImpl item) {
		this.item = item;
	}

	@Override
	public int hashCode() {
		return key == null ? 19456 : key.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if(this == obj) {
			return true;
		}
		if(obj instanceof CollectionToItem) {
			CollectionToItem q = (CollectionToItem)obj;
			return key != null && key.equals(q.key);
		}
		return false;
	}

	@Override
	public boolean equalsByPersistableKey(Persistable persistable) {
		return equals(persistable);
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("coll2item[key=").append(this.key)
			.append("]").append(super.toString());
		return sb.toString();
	}

}
