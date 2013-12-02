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
package org.olat.core.commons.services.webdav.manager;

import java.io.InputStream;

import org.olat.core.commons.services.webdav.servlets.WebResource;

/**
 * An empty resource
 * 
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class EmptyWebResource implements WebResource {
	
	private final String path;
	
	public EmptyWebResource(String path) {
		this.path = path;
	}

	@Override
	public String getPath() {
		return path;
	}

	@Override
	public long getLastModified() {
		return -1;
	}

	@Override
	public String getLastModifiedHttp() {
		return null;
	}

	@Override
	public boolean exists() {
		return false;
	}

	@Override
	public boolean isDirectory() {
		return false;
	}

	@Override
	public boolean isFile() {
		return false;
	}

	@Override
	public String getName() {
		return null;
	}

	@Override
	public long getContentLength() {
		return 0;
	}

	@Override
	public String getETag() {
	        return null;
	}

	@Override
	public void setMimeType(String mimeType) {
		//
	}

	@Override
	public String getMimeType() {
		return null;
	}

	@Override
	public InputStream getInputStream() {
		return null;
	}

	@Override
	public byte[] getContent() {
		return null;//use the input stream instead
	}
}