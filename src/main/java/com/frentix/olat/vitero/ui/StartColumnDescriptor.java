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
 * Copyright (c) frentix GmbH<br>
 * http://www.frentix.com<br>
 * <p>
 */
package com.frentix.olat.vitero.ui;

import java.util.Locale;

import org.olat.core.gui.components.table.DefaultColumnDescriptor;
import org.olat.core.gui.render.Renderer;
import org.olat.core.gui.render.StringOutput;
import org.olat.core.gui.translator.Translator;

import com.frentix.olat.vitero.manager.ViteroManager;
import com.frentix.olat.vitero.model.ViteroBooking;

/**
 * 
 * Description:<br>
 * 
 * <P>
 * Initial Date:  11 oct. 2011 <br>
 *
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 */
public class StartColumnDescriptor extends DefaultColumnDescriptor {
	
	private final ViteroManager viteroManager;
	
	private final Translator translator;
	
	public StartColumnDescriptor(final String headerKey, String action, final Locale locale, ViteroManager viteroManager, Translator translator) {
		super(headerKey, 1, action, locale);
		this.viteroManager = viteroManager;
		this.translator = translator;
	}

	@Override
	public String getAction(int row) {
		int sortedRow = table.getSortedRow(row);
		ViteroBooking booking = (ViteroBooking)getTable().getTableDataModel().getObject(sortedRow);
		if(viteroManager.canGoBooking(booking)) {
			Object state = getTable().getTableDataModel().getValueAt(sortedRow, ViteroBookingDataModel.Column.sign.ordinal());
			if(Sign.signout.equals(state)) {
				return super.getAction(row);
			}
		}
		return null;
	}

	@Override
	public void renderValue(StringOutput sb, int row, Renderer renderer) {
		int sortedRow = table.getSortedRow(row);
		ViteroBooking booking = (ViteroBooking)getTable().getTableDataModel().getObject(sortedRow);
		if(viteroManager.canGoBooking(booking)) {
			Object state = getTable().getTableDataModel().getValueAt(sortedRow, ViteroBookingDataModel.Column.sign.ordinal());
			if(Sign.signout.equals(state)) {
				sb.append(translator.translate(getHeaderKey()));
			}
		}
	}
}