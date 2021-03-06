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
package org.olat.core.util.filter.impl;

import java.io.ByteArrayInputStream;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.olat.core.util.filter.impl.NekoHTMLFilter.NekoContent;

/**
 * Description:<br>
 * This test case tests the NEko HTML tags filter
 * 
 * <P>
 * Initial Date:  14.07.2009 <br>
 * @author gnaegi
 */
@RunWith(JUnit4.class)
public class NekoHTMLFilterTest{

	protected NekoHTMLFilter filter;

	@Before public void setup() {
		filter = new NekoHTMLFilter();
	}

	@After public void tearDown() {
		filter = null;
	}

	private void t(String input, String result) {
		Assert.assertEquals(result, filter.filter(input));
	}
	
	@Test public void testPlainText() {
		t(null, null);
		t("", "");
		t("hello world", "hello world");
		t("hello \n \t \r world", "hello \n \t \n world"); // \r are converted to \n by filter
		t("1+2=3", "1+2=3");
	}
	
	@Test public void testSimpleTags() {
		t("<b>hello</b> world", "hello world");
		t("<b><i>hello</i></b> world", "hello world");
		t("<b>h<i>ell</i>o</b> world", "hello world");
		t("<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\">", "");
		t("<a ref='#bla' \n title='gugus'>hello</b> world", "hello world");
	}
	
	@Test public void testBRAndPReplacement() {
		t("<br>", "");
		t("<p>", "");
		t("<br >", "");
		t("<p >", "");
		t("<br/>", "");
		t("<p/>", "");
		t("<br />", "");
		t("<p />", "");
		t("bla<br>bla", "bla bla");
		t("bla<p>bla", "bla bla");
		t("bla<br >bla", "bla bla");
		t("bla<p >bla", "bla bla");
		t("bla<br/>bla", "bla bla");
		t("bla<p/>bla", "bla bla");
		t("bla<br />bla", "bla bla");
		t("bla<p />bla", "bla bla");
		t("hello<br /> world", "hello world");
	}

	@Test public void testTagsWithAttributes() {
		t("<font color='red'>hello</font> world", "hello world");
		t("<font color=\"red\">hello</font> world", "hello world");
		t("<a href=\"#top\" color='=>top'>go up</a>", "go up");
		t("<a href=\"#top\" title=\"a > b < c <=x\">blu blu</a>", "blu blu");
		t("<a href=\"#top\" title=\"a > b < c <=x\">blu <font color='red'>hello</font> world blu</a>", "blu hello world blu");
	}

	@Test public void testTagsWithEntities() {
		t("Gn&auml;gi", "Gnägi");
		t("This is &copy; by frentix", "This is © by frentix");
	}
	
	@Test public void testHtmlText() {
		String htmlText = "<html><head><meta name=\"generator\" content=\"olat-tinymce-1\"><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"></head><body>"
            + "<H1>Test HTML Seite fuer JUnit Test</H1>"
            + "Dies ist<br />der Test&nbsp;Text"
            + "</body></html>"; // Text = 'Dies ist der Test Text'
		String text = "Test HTML Seite fuer JUnit Test Dies ist der Test\u00A0Text"; // must include '\u00A0' !!! 19.5.2010/cg
		t(htmlText,text);
	}
	
	@Test
	public void testHtmlTextAndTitle() {
		String htmlText = "<html><head><title>Hello Neko</title><meta name=\"generator\" content=\"olat-tinymce-1\"><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"></head><body>"
            + "<H1>Test HTML Seite fuer JUnit Test</H1>"
            + "Dies ist<br />der Test&nbsp;Text"
            + "</body></html>"; // Text = 'Dies ist der Test Text'
		String text = "Hello Neko Test HTML Seite fuer JUnit Test Dies ist der Test\u00A0Text"; // must include '\u00A0' !!! 19.5.2010/cg
		
		NekoContent content = filter.filter(new ByteArrayInputStream(htmlText.getBytes()));
		Assert.assertNotNull(content);
		Assert.assertEquals("Hello Neko", content.getTitle());
		Assert.assertEquals(text, content.getContent());
	}
}
