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
package org.olat.modules.qpool.manager;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipOutputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.IOUtils;
import org.olat.core.gui.media.MediaResource;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.StringHelper;
import org.olat.modules.qpool.QuestionItemShort;

/**
 * 
 * Initial date: 17.04.2013<br>
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public abstract class AbstractExportTestResource implements MediaResource {

	private static final OLog log = Tracing.createLoggerFor(AbstractExportTestResource.class);
	
	private String encoding;
	private final List<QuestionItemShort> items;
	
	public AbstractExportTestResource(String encoding, List<QuestionItemShort> items) {
		this.encoding = encoding;
		this.items = items;
	}

	@Override
	public String getContentType() {
		return "application/octet-stream";
	}

	@Override
	public Long getSize() {
		return null;
	}

	@Override
	public InputStream getInputStream() {
		return null;
	}

	@Override
	public Long getLastModified() {
		return null;
	}

	public List<QuestionItemShort> getItems() {
		return items;
	}

	@Override
	public void prepare(HttpServletResponse hres) {
		try {
			hres.setCharacterEncoding(encoding);
		} catch (Exception e) {
			log.error("", e);
		}
		
		String label = "Test";
		String file = StringHelper.transformDisplayNameToFileSystemName(label) + ".zip";
		hres.setHeader("Content-Disposition","attachment; filename=\"" + StringHelper.urlEncodeISO88591(file) + "\"");			
		hres.setHeader("Content-Description",StringHelper.urlEncodeISO88591(label));
		
		ZipOutputStream zout = null;
		try {
			zout = new ZipOutputStream(hres.getOutputStream());
			zout.setLevel(9);
			exportTest(items, zout);
		} catch (IOException e) {
			log.error("", e);
		}  catch (Exception e) {
			log.error("", e);
		} finally {
			IOUtils.closeQuietly(zout);
		}
	}
	
	protected abstract void exportTest(List<QuestionItemShort> items, ZipOutputStream zout);

	@Override
	public void release() {
		//
	}
}
