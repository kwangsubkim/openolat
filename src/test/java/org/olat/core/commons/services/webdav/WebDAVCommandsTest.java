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
package org.olat.core.commons.services.webdav;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.UUID;

import javax.ws.rs.core.UriBuilder;

import junit.framework.Assert;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpOptions;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.apache.poi.util.IOUtils;
import org.junit.Test;
import org.olat.basesecurity.BaseSecurity;
import org.olat.core.commons.modules.bc.FolderConfig;
import org.olat.core.commons.modules.bc.vfs.OlatRootFolderImpl;
import org.olat.core.commons.persistence.DB;
import org.olat.core.id.Identity;
import org.olat.core.id.Roles;
import org.olat.core.util.FileUtils;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.core.util.vfs.VFSItem;
import org.olat.core.util.vfs.VFSLeaf;
import org.olat.core.util.vfs.VFSLockManager;
import org.olat.core.util.vfs.lock.LockInfo;
import org.olat.course.CourseFactory;
import org.olat.course.ICourse;
import org.olat.repository.RepositoryEntry;
import org.olat.restapi.CoursePublishTest;
import org.olat.test.JunitTestHelper;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 
 * Test the commands against the WedDAV implementation of OpenOLAT
 * 
 * @author srosse, stephane.rosse@frentix.com, http://www.frentix.com
 *
 */
public class WebDAVCommandsTest extends WebDAVTestCase {
	
	@Autowired
	private DB dbInstance;
	@Autowired
	private BaseSecurity securityManager;
	@Autowired
	private VFSLockManager lockManager;
	
	/**
	 * Check the DAV, Ms-Author and Allow header
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	@Test
	public void testOptions()
	throws IOException, URISyntaxException {
		//create a user
		Identity user = JunitTestHelper.createAndPersistIdentityAsUser("webdav-1-" + UUID.randomUUID().toString());
		
		//list root content of its webdav folder
		WebDAVConnection conn = new WebDAVConnection();
		conn.setCredentials(user.getName(), "A6B7C8");
		
		URI baseUri = conn.getBaseURI().build();
		HttpOptions optionsRoot = conn.createOptions(baseUri);
		HttpResponse optionsResponse = conn.execute(optionsRoot);
		Assert.assertEquals(200, optionsResponse.getStatusLine().getStatusCode());
		//check DAV header
		Header davHeader = optionsResponse.getFirstHeader("DAV");
		String davHeaderValue = davHeader.getValue();
		Assert.assertTrue(davHeaderValue.contains("1"));
		Assert.assertTrue(davHeaderValue.contains("2"));
		//check ms author
		Header msHeader = optionsResponse.getFirstHeader("MS-Author-Via");
		Assert.assertEquals("DAV", msHeader.getValue());
		//check methods
		Header allowHeader = optionsResponse.getFirstHeader("Allow");
		String allowValue = allowHeader.getValue();
		
		String[] allowedMethods = new String[] {
				"OPTIONS", "GET", "HEAD", "POST", "DELETE",
				"TRACE", "PROPPATCH", "COPY", "MOVE", "LOCK", "UNLOCK"
		};
		for(String allowedMethod:allowedMethods) {
			Assert.assertTrue(allowValue.contains(allowedMethod));
		}

		IOUtils.closeQuietly(conn);
	}
	
	@Test
	public void testPropFind()
	throws IOException, URISyntaxException {
		//create a user
		Identity user = JunitTestHelper.createAndPersistIdentityAsUser("webdav-2-" + UUID.randomUUID().toString());

		WebDAVConnection conn = new WebDAVConnection();
		conn.setCredentials(user.getName(), "A6B7C8");
		
		//list root content of its webdav folder
		URI uri = conn.getBaseURI().build();
		String xml = conn.propfind(uri, 1);
		Assert.assertTrue(xml.indexOf("<D:multistatus") > 0);//Windows need the D namespace
		Assert.assertTrue(xml.indexOf("<D:href>/</D:href>") > 0);//check the root
		Assert.assertTrue(xml.indexOf("<D:href>/webdav/</D:href>") > 0);//check the webdav folder

		//check public folder
		URI publicUri = conn.getBaseURI().path("webdav").path("home").path("public").build();
		String publicXml = conn.propfind(publicUri, 1);
		Assert.assertTrue(publicXml.indexOf("<D:multistatus") > 0);//Windows need the D namespace
		Assert.assertTrue(publicXml.indexOf("<D:href>/webdav/home/public/</D:href>") > 0);//check the root

		IOUtils.closeQuietly(conn);
	}
	
	@Test
	public void testMkcol_public()
	throws IOException, URISyntaxException {
		//create a user
		Identity user = JunitTestHelper.createAndPersistIdentityAsAuthor("webdav-2a-" + UUID.randomUUID().toString());

		//create a file
		String publicPath = FolderConfig.getUserHomes() + "/" + user.getName() + "/public";
		VFSContainer vfsPublic = new OlatRootFolderImpl(publicPath, null);
		Assert.assertTrue(vfsPublic.exists());

		
		WebDAVConnection conn = new WebDAVConnection();
		conn.setCredentials(user.getName(), "A6B7C8");

		//author check course folder
		URI publicUri = conn.getBaseURI().path("webdav").path("home").path("public").build();
		String publicXml = conn.propfind(publicUri, 2);
		Assert.assertTrue(publicXml.indexOf("<D:href>/webdav/home/public/</D:href>") > 0);

		//make a folder
		URI newUri = UriBuilder.fromUri(publicUri).path("newFolder").build();
		int returnMkcol = conn.mkcol(newUri);
		Assert.assertEquals(201, returnMkcol);
		
		//check if folder exists
		VFSItem newItem = vfsPublic.resolve("newFolder");
		Assert.assertNotNull(newItem);
		Assert.assertTrue(newItem instanceof VFSContainer);
		Assert.assertTrue(newItem.exists());
	
		IOUtils.closeQuietly(conn);
	}
	
	@Test
	public void testMove_public()
	throws IOException, URISyntaxException {
		//create a user
		Identity user = JunitTestHelper.createAndPersistIdentityAsAuthor("webdav-2b-" + UUID.randomUUID().toString());

		//create a file
		String publicPath = FolderConfig.getUserHomes() + "/" + user.getName() + "/public";
		VFSContainer vfsPublic = new OlatRootFolderImpl(publicPath, null);
		createFile(vfsPublic, "test.txt");
		VFSContainer subPublic = vfsPublic.createChildContainer("moveto");

		WebDAVConnection conn = new WebDAVConnection();
		conn.setCredentials(user.getName(), "A6B7C8");

		//author check course folder
		URI publicUri = conn.getBaseURI().path("webdav").path("home").path("public").build();
		URI fileUri = UriBuilder.fromUri(publicUri).path("test.txt").build();
		String destination = UriBuilder.fromUri(publicUri).path("moveto").path("test.txt").build().toString();
		int returnMove = conn.move(fileUri, destination);
		Assert.assertEquals(201, returnMove);

		//check move
		VFSItem movedItem = subPublic.resolve("test.txt");
		Assert.assertNotNull(movedItem);
		Assert.assertTrue(movedItem instanceof VFSLeaf);
		Assert.assertTrue(movedItem.exists());

		VFSItem sourceItem = vfsPublic.resolve("test.txt");
		Assert.assertNull(sourceItem);
	
		IOUtils.closeQuietly(conn);
	}
	
	@Test
	public void testCopy_public()
	throws IOException, URISyntaxException {
		//create a user
		Identity user = JunitTestHelper.createAndPersistIdentityAsAuthor("webdav-2b-" + UUID.randomUUID().toString());

		//create a file
		String publicPath = FolderConfig.getUserHomes() + "/" + user.getName() + "/public";
		VFSContainer vfsPublic = new OlatRootFolderImpl(publicPath, null);
		createFile(vfsPublic, "test.txt");
		VFSContainer subPublic = vfsPublic.createChildContainer("copyto");

		WebDAVConnection conn = new WebDAVConnection();
		conn.setCredentials(user.getName(), "A6B7C8");

		//author check course folder
		URI publicUri = conn.getBaseURI().path("webdav").path("home").path("public").build();
		URI fileUri = UriBuilder.fromUri(publicUri).path("test.txt").build();
		String destination = UriBuilder.fromUri(publicUri).path("copyto").path("copy.txt").build().toString();
		int returnMove = conn.copy(fileUri, destination);
		Assert.assertEquals(201, returnMove);

		//check move
		VFSItem movedItem = subPublic.resolve("copy.txt");
		Assert.assertNotNull(movedItem);
		Assert.assertTrue(movedItem instanceof VFSLeaf);
		Assert.assertTrue(movedItem.exists());

		VFSItem sourceItem = vfsPublic.resolve("test.txt");
		Assert.assertNotNull(sourceItem);
		Assert.assertTrue(sourceItem instanceof VFSLeaf);
		Assert.assertTrue(sourceItem.exists());
	
		IOUtils.closeQuietly(conn);
	}
	
	@Test
	public void testPut_course()
	throws IOException, URISyntaxException {
		//create a user
		Identity author = JunitTestHelper.createAndPersistIdentityAsAuthor("webdav-3-" + UUID.randomUUID().toString());
		deployTestCourse(author, null);

		WebDAVConnection conn = new WebDAVConnection();
		conn.setCredentials(author.getName(), "A6B7C8");

		//author check course folder
		URI courseUri = conn.getBaseURI().path("webdav").path("coursefolders").build();
		String publicXml = conn.propfind(courseUri, 2);
		Assert.assertTrue(publicXml.indexOf("<D:href>/webdav/coursefolders/Kurs/_courseelementdata/</D:href>") > 0);

		//PUT in the folder
		URI putUri = UriBuilder.fromUri(courseUri).path("Kurs").path("test.txt").build();
		HttpPut put = conn.createPut(putUri);
		InputStream dataStream = WebDAVCommandsTest.class.getResourceAsStream("text.txt");
		InputStreamEntity entity = new InputStreamEntity(dataStream, -1);
		put.setEntity(entity);
		HttpResponse putResponse = conn.execute(put);
		Assert.assertEquals(201, putResponse.getStatusLine().getStatusCode());
		
		//GET
		HttpGet get = conn.createGet(putUri);
		HttpResponse getResponse = conn.execute(get);
		Assert.assertEquals(200, getResponse.getStatusLine().getStatusCode());
		String text = EntityUtils.toString(getResponse.getEntity());
		Assert.assertEquals("Small text", text);
	
		IOUtils.closeQuietly(conn);
	}
	
	/**
	 * PROPPATCH is essential for Windows, the content of the response
	 * is not important but it must not return an error.
	 * 
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	@Test
	public void testPut_PropPatch_home()
	throws IOException, URISyntaxException {
		//create a user
		Identity author = JunitTestHelper.createAndPersistIdentityAsAuthor("webdav-3-" + UUID.randomUUID().toString());
		deployTestCourse(author, null);

		WebDAVConnection conn = new WebDAVConnection();
		conn.setCredentials(author.getName(), "A6B7C8");

		//author check course folder
		URI privateUri = conn.getBaseURI().path("webdav").path("home").path("private").build();
		conn.propfind(privateUri, 2);

		//PUT in the folder
		URI putUri = UriBuilder.fromUri(privateUri).path("test.txt").build();
		HttpPut put = conn.createPut(putUri);
		InputStream dataStream = WebDAVCommandsTest.class.getResourceAsStream("text.txt");
		InputStreamEntity entity = new InputStreamEntity(dataStream, -1);
		put.setEntity(entity);
		HttpResponse putResponse = conn.execute(put);
		Assert.assertEquals(201, putResponse.getStatusLine().getStatusCode());
		EntityUtils.consume(putResponse.getEntity());
		
		//PROPPATCH
		URI patchUri = UriBuilder.fromUri(privateUri).path("test.txt").build();
		HttpPropPatch patch = conn.createPropPatch(patchUri);
		
		StringBuilder sb = new StringBuilder();
		sb.append("<?xml version=\"1.0\" encoding=\"utf-8\" ?>")
		  .append("<D:propertyupdate xmlns:D=\"DAV:\"")
		  .append("  xmlns:Z=\"http://www.w3.com/standards/z39.50/\">")
		  .append("  <D:set>")
		  .append("      <D:prop>")
		  .append("           <Z:authors>")
		  .append("                <Z:Author>Jim Whitehead</Z:Author>")
		  .append("                <Z:Author>Roy Fielding</Z:Author>")
		  .append("           </Z:authors>")
		  .append("      </D:prop>")
		  .append("  </D:set>")
		  .append("  <D:remove>")
		  .append("      <D:prop><Z:Copyright-Owner/></D:prop>")
		  .append("   </D:remove>")
		  .append(" </D:propertyupdate>");
		
		patch.setEntity(new StringEntity(sb.toString()));
		
		HttpResponse patchResponse = conn.execute(patch);
		Assert.assertEquals(207, patchResponse.getStatusLine().getStatusCode());
	
		IOUtils.closeQuietly(conn);
	}
	
	/**
	 * In the this test, an author and its assistant try to concurrently
	 * lock a file.
	 * 
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	@Test
	public void testLock()
	throws IOException, URISyntaxException {
		//create a user
		Identity author = JunitTestHelper.createAndPersistIdentityAsAuthor("webdav-4-" + UUID.randomUUID().toString());
		Identity assistant = JunitTestHelper.createAndPersistIdentityAsAuthor("webdav-5-" + UUID.randomUUID().toString());
		deployTestCourse(author, assistant);

		WebDAVConnection authorConn = new WebDAVConnection();
		authorConn.setCredentials(author.getName(), "A6B7C8");
		
		WebDAVConnection assistantConn = new WebDAVConnection();
		assistantConn.setCredentials(assistant.getName(), "A6B7C8");
		
		//author check course folder
		URI courseUri = authorConn.getBaseURI().path("webdav").path("coursefolders").build();
		String publicXml = authorConn.propfind(courseUri, 2);
		Assert.assertTrue(publicXml.indexOf("<D:href>/webdav/coursefolders/Kurs/_courseelementdata/</D:href>") > 0);

		//coauthor check course folder
		String assistantPublicXml = assistantConn.propfind(courseUri, 2);
		Assert.assertTrue(assistantPublicXml.indexOf("<D:href>/webdav/coursefolders/Kurs/_courseelementdata/</D:href>") > 0);

		//PUT a file to lock
		URI putUri = UriBuilder.fromUri(courseUri).path("Kurs").path("test.txt").build();
		HttpPut put = authorConn.createPut(putUri);
		InputStream dataStream = WebDAVCommandsTest.class.getResourceAsStream("text.txt");
		InputStreamEntity entity = new InputStreamEntity(dataStream, -1);
		put.setEntity(entity);
		HttpResponse putResponse = authorConn.execute(put);
		Assert.assertEquals(201, putResponse.getStatusLine().getStatusCode());
		EntityUtils.consume(putResponse.getEntity());

		//author lock the file in the course folder
		String authorLockToken = UUID.randomUUID().toString().replace("-", "").toLowerCase();
		String authorResponseLockToken = authorConn.lock(putUri, authorLockToken);
		Assert.assertNotNull(authorResponseLockToken);
		
		//coauthor try to lock the same file
		String coauthorLockToken = UUID.randomUUID().toString().replace("-", "").toLowerCase();
		int coauthorLock = assistantConn.lockTry(putUri, coauthorLockToken);
		Assert.assertEquals(423, coauthorLock);// it's lock
		
		//author unlock the file
		int unlockCode = authorConn.unlock(putUri, authorResponseLockToken);
		Assert.assertEquals(204, unlockCode);
		
		//coauthor try a second time to lock the file
		String coauthorLockToken_2 = UUID.randomUUID().toString().replace("-", "").toLowerCase();
		int coauthorLock_2 = assistantConn.lockTry(putUri, coauthorLockToken_2);
		Assert.assertEquals(200, coauthorLock_2);// it's lock
		
		IOUtils.closeQuietly(authorConn);
		IOUtils.closeQuietly(assistantConn);
	}
	
	/**
	 * 
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	@Test
	public void testLock_propfind_lockedInOpenOLAT()
	throws IOException, URISyntaxException {
		//create a user
		Identity author = JunitTestHelper.createAndPersistIdentityAsAuthor("webdav-4-" + UUID.randomUUID().toString());
		Identity assistant = JunitTestHelper.createAndPersistIdentityAsAuthor("webdav-5-" + UUID.randomUUID().toString());
		RepositoryEntry re = deployTestCourse(author, assistant);
		ICourse course = CourseFactory.loadCourse(re.getOlatResource());
		Assert.assertNotNull(course);
		
		//the assistant lock the file as in OpenOLAT GUI
		VFSContainer folderContainer = course.getCourseFolderContainer();
		createFile(folderContainer, "tolock.txt");
		VFSItem itemToLock = folderContainer.resolve("tolock.txt");
		Assert.assertNotNull(itemToLock);
		boolean locked = lockManager.lock(itemToLock, assistant, new Roles(false, false, false, true, false, false, false));
		Assert.assertTrue(locked);
		
		//author make a propfind in the locked resource
		WebDAVConnection conn = new WebDAVConnection();
		conn.setCredentials(author.getName(), "A6B7C8");
		
		URI toLockUri = conn.getBaseURI().path("webdav").path("coursefolders").path("Kurs").path("tolock.txt").build();
		String propfindXml = conn.propfind(toLockUri, 2);

		Assert.assertTrue(propfindXml.indexOf("<D:lockscope><D:exclusive/></D:lockscope>") > 0);//not really a test
		Assert.assertTrue(propfindXml.indexOf("/Identity/" + assistant.getKey() + "</D:owner>") > 0);
		Assert.assertTrue(propfindXml.indexOf("<D:locktoken><D:href>opaquelocktoken:") > 0);
		
		LockInfo lock = lockManager.getLock(itemToLock);
		Assert.assertNotNull(lock);
		Assert.assertNotNull(lock.getScope());
		Assert.assertNotNull(lock.getType());
		Assert.assertNotNull(lock.getOwner());
		Assert.assertTrue(lock.getOwner().length() > 0);
		Assert.assertTrue(lock.isVfsLock());
		Assert.assertFalse(lock.isWebDAVLock());
		Assert.assertEquals(assistant.getKey(), lock.getLockedBy());
		Assert.assertEquals(1, lock.getTokensSize());

		IOUtils.closeQuietly(conn);
	}
	
	/**
	 * 
	 * @throws IOException
	 * @throws URISyntaxException
	 */
	@Test
	public void testLock_guilike_lockedWithWebdAV()
	throws IOException, URISyntaxException {
		//create a user
		Identity user = JunitTestHelper.createAndPersistIdentityAsAuthor("webdav-2c-" + UUID.randomUUID().toString());

		//create a file
		String publicPath = FolderConfig.getUserHomes() + "/" + user.getName() + "/public";
		VFSContainer vfsPublic = new OlatRootFolderImpl(publicPath, null);
		VFSItem item = createFile(vfsPublic, "test.txt");
		
		//lock the item with WebDAV
		WebDAVConnection conn = new WebDAVConnection();
		conn.setCredentials(user.getName(), "A6B7C8");

		//author check file 
		URI textUri = conn.getBaseURI().path("webdav").path("home").path("public").path("test.txt").build();
		String textPropfind = conn.propfind(textUri, 0);
		System.out.println(textPropfind);
		
		//author lock the file
		String lockToken = conn.lock(textUri, UUID.randomUUID().toString());
		Assert.assertNotNull(lockToken);

		//check vfs lock
		Roles adminRoles = new Roles(true, false, false, false, false, false, false);
		boolean lockedForMe = lockManager.isLockedForMe(item, user, adminRoles);
		Assert.assertTrue(lockedForMe);
		LockInfo lock = lockManager.getLock(item);
		Assert.assertNotNull(lock);
		Assert.assertNotNull(lock.getScope());
		Assert.assertNotNull(lock.getType());
		Assert.assertNotNull(lock.getOwner());
		Assert.assertTrue(lock.getOwner().length() > 0);
		Assert.assertFalse(lock.isVfsLock());
		Assert.assertTrue(lock.isWebDAVLock());
		Assert.assertEquals(user.getKey(), lock.getLockedBy());
		Assert.assertEquals(1, lock.getTokensSize());
		
		//try to unlock which should not be possible
		boolean unlocked = lockManager.unlock(item, user, adminRoles);
		Assert.assertFalse(unlocked);
		//check that nothing changed
		LockInfo lockAfterUnlock = lockManager.getLock(item);
		Assert.assertNotNull(lockAfterUnlock);
		Assert.assertNotNull(lockAfterUnlock.getScope());
		Assert.assertNotNull(lockAfterUnlock.getType());
		Assert.assertNotNull(lockAfterUnlock.getOwner());
		Assert.assertTrue(lockAfterUnlock.getOwner().length() > 0);
		Assert.assertFalse(lockAfterUnlock.isVfsLock());
		Assert.assertTrue(lockAfterUnlock.isWebDAVLock());
		Assert.assertEquals(user.getKey(), lockAfterUnlock.getLockedBy());
		Assert.assertEquals(1, lock.getTokensSize());
		
		IOUtils.closeQuietly(conn);
	}
	
	@Test
	public void testDelete()
	throws IOException, URISyntaxException {
		//create a user
		Identity user = JunitTestHelper.createAndPersistIdentityAsUser("webdav-6-" + UUID.randomUUID().toString());
		
		//create a file
		String publicPath = FolderConfig.getUserHomes() + "/" + user.getName() + "/public";
		VFSContainer vfsPublic = new OlatRootFolderImpl(publicPath, null);
		createFile(vfsPublic, "testDelete.txt");
		
		//check
		VFSItem item = vfsPublic.resolve("testDelete.txt");
		Assert.assertTrue(item instanceof VFSLeaf);
		Assert.assertTrue(item.exists());
		Assert.assertTrue(((VFSLeaf)item).getSize() > 0);

		//delete the file
		WebDAVConnection conn = new WebDAVConnection();
		conn.setCredentials(user.getName(), "A6B7C8");
	
		//check public folder
		URI checkUri = conn.getBaseURI().path("webdav").path("home").path("public").path("testDelete.txt").build();
		String publicXml = conn.propfind(checkUri, 1);
		Assert.assertTrue(publicXml.indexOf("<D:multistatus") > 0);//Windows need the D namespace
		Assert.assertTrue(publicXml.indexOf("<D:href>/webdav/home/public/testDelete.txt</D:href>") > 0);//check the root

		//delete the file
		HttpDelete delete = conn.createDelete(checkUri);
		HttpResponse deleteResponse = conn.execute(delete);
		Assert.assertEquals(204, deleteResponse.getStatusLine().getStatusCode());
		EntityUtils.consume(deleteResponse.getEntity());
		
		//check if really deleted
		VFSItem reloadTestLeaf = vfsPublic.resolve("testDelete.txt");
		Assert.assertNull(reloadTestLeaf);

		IOUtils.closeQuietly(conn);
	}
	
	private VFSItem createFile(VFSContainer container, String filename) throws IOException {
		VFSLeaf testLeaf = container.createChildLeaf(filename);
		InputStream in = WebDAVCommandsTest.class.getResourceAsStream("text.txt");
		OutputStream out = testLeaf.getOutputStream(false);
		FileUtils.copy(in, out);
		out.flush();
		IOUtils.closeQuietly(in);
		IOUtils.closeQuietly(out);
		return container.resolve(filename);
	}
	
	private RepositoryEntry deployTestCourse(Identity author, Identity coAuthor) throws URISyntaxException {
		URL courseWithForumsUrl = CoursePublishTest.class.getResource("myCourseWS.zip");
		Assert.assertNotNull(courseWithForumsUrl);
		File courseWithForums = new File(courseWithForumsUrl.toURI());
		String softKey = UUID.randomUUID().toString().replace("-", "").substring(0, 30);
		RepositoryEntry re = CourseFactory.deployCourseFromZIP(courseWithForums, author.getName(), softKey, 4);	
		securityManager.addIdentityToSecurityGroup(author, re.getOwnerGroup());
		if(coAuthor != null) {
			securityManager.addIdentityToSecurityGroup(coAuthor, re.getOwnerGroup());
		}
		
		dbInstance.commitAndCloseSession();
		return re;
	}
}