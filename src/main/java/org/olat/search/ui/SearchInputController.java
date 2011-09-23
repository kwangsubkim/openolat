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

package org.olat.search.ui;

import static org.olat.search.ui.ResultsController.RESULT_PER_PAGE;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.TermAttribute;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.util.Version;
import org.olat.NewControllerFactory;
import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.services.search.AbstractOlatDocument;
import org.olat.core.commons.services.search.QueryException;
import org.olat.core.commons.services.search.ResultDocument;
import org.olat.core.commons.services.search.SearchResults;
import org.olat.core.commons.services.search.ServiceNotAvailableException;
import org.olat.core.commons.services.search.ui.SearchController;
import org.olat.core.commons.services.search.ui.SearchEvent;
import org.olat.core.commons.services.search.ui.SearchServiceUIFactory;
import org.olat.core.commons.services.search.ui.SearchServiceUIFactory.DisplayOption;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.form.flexible.FormItem;
import org.olat.core.gui.components.form.flexible.FormItemContainer;
import org.olat.core.gui.components.form.flexible.elements.FormLink;
import org.olat.core.gui.components.form.flexible.elements.TextElement;
import org.olat.core.gui.components.form.flexible.impl.Form;
import org.olat.core.gui.components.form.flexible.impl.FormBasicController;
import org.olat.core.gui.components.form.flexible.impl.FormEvent;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalWindowController;
import org.olat.core.gui.media.RedirectMediaResource;
import org.olat.core.id.context.BusinessControl;
import org.olat.core.id.context.BusinessControlFactory;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.StringHelper;
import org.olat.search.service.searcher.SearchClientProxy;

/**
 * Description:<br>
 * Controller with a simple input for the full text search. The display option
 * select how the input is shown: only a button, button with text, input field and
 * button.
 * <P>
 * Initial Date:  3 dec. 2009 <br>
 * @author srosse, stephane.rosse@frentix.com
 */
public class SearchInputController extends FormBasicController implements SearchController {
	private static final OLog log = Tracing.createLoggerFor(SearchInputController.class);
	
	private static final String FUZZY_SEARCH = "~0.7";
	private static final String CMD_DID_YOU_MEAN_LINK = "didYouMeanLink-";
	private static final String SEARCH_STORE_KEY = "search-store-key";
	private static final String SEARCH_CACHE_KEY = "search-cache-key";
	
	private String parentContext;
	private String documentType;
	private String resourceUrl;
	private boolean resourceContextEnable = true;
	
	private DisplayOption displayOption; 
	
	protected FormLink searchButton;
	protected TextElement searchInput;
	private ResultsSearchController resultCtlr;
	private Controller searchDialogBox;

	protected List<FormLink> didYouMeanLinks;
	
	private Map<String,Properties> prefs;
	private SearchLRUCache searchCache;
	private SearchClientProxy searchClient;
	
	public SearchInputController(UserRequest ureq, WindowControl wControl, String resourceUrl, DisplayOption displayOption) {
		super(ureq, wControl, LAYOUT_HORIZONTAL);
		this.resourceUrl = resourceUrl;
		this.displayOption = displayOption;
		setSearchStore(ureq);
		initForm(ureq);
		loadPersistedSearch();
		loadContext();
	}
	
	public SearchInputController(UserRequest ureq, WindowControl wControl, String resourceUrl, String customPage) {
		super(ureq, wControl, customPage);
		this.displayOption = DisplayOption.STANDARD_TEXT;
		this.resourceUrl = resourceUrl;
		setSearchStore(ureq);
		initForm(ureq);
		loadPersistedSearch();
		loadContext();
	}
	
	public SearchInputController(UserRequest ureq, WindowControl wControl, String resourceUrl, DisplayOption displayOption, Form mainForm) {
		super(ureq, wControl, LAYOUT_HORIZONTAL, null, mainForm);
		this.displayOption = displayOption;
		this.resourceUrl = resourceUrl;
		setSearchStore(ureq);
		initForm(ureq);
		loadPersistedSearch();
		loadContext();
	}

	public String getParentContext() {
		return parentContext;
	}

	public void setParentContext(String parentContext) {
		this.parentContext = parentContext;
	}

	public String getDocumentType() {
		return documentType;
	}

	public void setDocumentType(String documentType) {
		this.documentType = documentType;
	}

	public String getResourceUrl() {
		return resourceUrl;
	}
	
	public void setResourceUrl(String resourceUrl) {
		this.resourceUrl = resourceUrl;
	}
	
	public boolean isResourceContextEnable() {
		return resourceContextEnable;
	}

	public void setResourceContextEnable(boolean resourceContextEnable) {
		this.resourceContextEnable = resourceContextEnable;
	}

	public String getSearchString() {
		return searchInput.getValue();
	}
	
	public void setSearchString(String searchString) {
		if (StringHelper.containsNonWhitespace(searchString)) {
			if(searchInput != null) {
				searchInput.setValue(searchString);
			}
		}
	}

	@Override
	protected void initForm(FormItemContainer formLayout, Controller listener, UserRequest ureq) {
		searchClient = (SearchClientProxy)CoreSpringFactory.getBean("searchClient");
		
		if (displayOption.equals(DisplayOption.STANDARD) || displayOption.equals(DisplayOption.STANDARD_TEXT)) {
			searchInput = uifactory.addTextElement("search_input", "search.title", 255, "", formLayout);
			searchInput.setLabel(null, null);
		}
		
		if (displayOption.equals(DisplayOption.STANDARD) || displayOption.equals(DisplayOption.BUTTON)) {
			searchButton = uifactory.addFormLink("search", "", "", formLayout, Link.NONTRANSLATED + Link.LINK_CUSTOM_CSS);
			searchButton.setCustomEnabledLinkCSS("o_fulltext_search_button b_small_icon");
		} else if (displayOption.equals(DisplayOption.BUTTON_WITH_LABEL)) {
			searchButton = uifactory.addFormLink("search", formLayout, Link.BUTTON_SMALL);
		} else if (displayOption.equals(DisplayOption.STANDARD_TEXT)) {
			String searchLabel = getTranslator().translate("search");
			searchButton = uifactory.addFormLink("search", searchLabel, "", formLayout, Link.NONTRANSLATED + Link.BUTTON_SMALL);
		}
		searchButton.setEnabled(true);
	}
	
	private void loadContext() {
		if(resourceUrl != null) {
			ContextTokens context = getContextTokens(resourceUrl);
			setContext(context);
		}
	}
	
	protected void setContext(ContextTokens context) {
		if(!context.isEmpty()) {
			String scope = context.getValueAt(context.getSize() - 1);
			String tooltip = getTranslator().translate("form.search.label.tooltip", new String[]{scope});
			((Link)searchButton.getComponent()).setTooltip(tooltip, false);
		}
	}
	
	private void setSearchStore(UserRequest ureq) {
		prefs = (Map<String,Properties>)ureq.getUserSession().getEntry(SEARCH_STORE_KEY);
		if(prefs == null) {
			prefs = new HashMap<String,Properties>();
			ureq.getUserSession().putEntry(SEARCH_STORE_KEY, prefs);
		}
		
		searchCache = (SearchLRUCache)ureq.getUserSession().getEntry(SEARCH_CACHE_KEY);
		if(searchCache == null) {
			searchCache = new SearchLRUCache();
			ureq.getUserSession().putEntry(SEARCH_CACHE_KEY, searchCache);
		}
	}
	
	@Override
	protected void doDispose() {
		//
	}

	@Override
	protected void formOK(UserRequest ureq) {
		doSearch(ureq);
	}
	
	@Override
	protected void formNOK(UserRequest ureq) {
		doSearch(ureq);
	}

	@Override
	protected void formInnerEvent(UserRequest ureq, FormItem source, FormEvent event) {
		if (source == searchButton) {
			doSearch(ureq);
		} else if (didYouMeanLinks != null && didYouMeanLinks.contains(source)) {
			String didYouMeanWord = (String)source.getUserObject();
			searchInput.setValue(didYouMeanWord);
			doSearch(ureq, didYouMeanWord, null, parentContext, documentType, resourceUrl, 0, RESULT_PER_PAGE, false);
		}
	}
	
	protected void doSearch(UserRequest ureq) {
		if (resultCtlr != null) {
			removeAsListenerAndDispose(resultCtlr);
			resultCtlr = null;
		}
		
		String oldSearchString = null;
		Properties props = getPersistedSearch();
		if(props != null) {
			oldSearchString = props.getProperty("s");
		}
		
		persistSearch(ureq);
		
		if (DisplayOption.BUTTON.equals(displayOption) || DisplayOption.BUTTON_WITH_LABEL.equals(displayOption)) {
			//no search, only popup
			createResultsSearchController(ureq);
			popupResultsSearchController(ureq);
			if(resultCtlr.getPersistedSearch() != null && !resultCtlr.getPersistedSearch().isEmpty()) {
				resultCtlr.doSearch(ureq);
			}
		} else {
			String searchString = getSearchString();
			if(StringHelper.containsNonWhitespace(searchString)) {
				if(oldSearchString != null && !oldSearchString.equals(searchString)) {
					resetSearch();
				}

				createResultsSearchController(ureq);
				resultCtlr.setSearchString(searchString);
				popupResultsSearchController(ureq);
				resultCtlr.doSearch(ureq);
			}
		}
	}
	
	protected Properties getPersistedSearch() {
		if(getResourceUrl() != null) {
			String uri = getResourceUrl();
			Properties props = prefs.get(uri);
			if(props == null) {
				props = new Properties();
				prefs.put(uri, props);
			}
			return props;
		}
		//not possible but i don't want to trigger a red screen for this if i'm wrong
		return new Properties();
	}
	
	protected void resetSearch() {
		if(getResourceUrl() != null) {
			String uri = getResourceUrl();
			Properties props = prefs.get(uri);
			if(props != null) {
				prefs.remove(uri);
			}
		}
	}
	
	protected void persistSearch(UserRequest ureq) {
		if(getResourceUrl() != null) {
			String uri = getResourceUrl();
			Properties props = prefs.get(uri);
			if(props == null) {
				props = new Properties();
			}
			getSearchProperties(props);
			
			if(props.isEmpty()) {
				prefs.remove(uri);
			} else {
				prefs.put(uri, props);
			}
		}
	}
	
	protected void loadPersistedSearch() {
		if(getResourceUrl() != null) {
			String uri = getResourceUrl();
			Properties props = prefs.get(uri);
			if(props != null) {
				setSearchProperties(props);
			}
		}
	}
	
	private void createResultsSearchController(UserRequest ureq) {
		resultCtlr = new ResultsSearchController(ureq, getWindowControl(), getResourceUrl());
		resultCtlr.setDocumentType(getDocumentType());
		resultCtlr.setParentContext(getParentContext());
		resultCtlr.setResourceContextEnable(isResourceContextEnable());
		listenTo(resultCtlr);
	}
	
	protected void getSearchProperties(Properties props) {
		if (displayOption.equals(DisplayOption.STANDARD) || displayOption.equals(DisplayOption.STANDARD_TEXT)) {
			String searchString = getSearchString();
			props.setProperty("s", searchString == null ? "" : searchString);
		}
	}
	
	protected void setSearchProperties(Properties props) {
		if (displayOption.equals(DisplayOption.STANDARD) || displayOption.equals(DisplayOption.STANDARD_TEXT)) {
			String searchString = props.getProperty("s");
			if(StringHelper.containsNonWhitespace(searchString)) {
				setSearchString(searchString);
			} else {
				setSearchString("");
			}
		}
	}
	
	private void popupResultsSearchController(UserRequest ureq) {
		String title = translate("search.title");
		boolean ajaxOn = getWindowControl().getWindowBackOffice().getWindowManager().isAjaxEnabled();
		if (ajaxOn) {
			searchDialogBox = new CloseableModalWindowController(ureq, getWindowControl(), title, resultCtlr.getInitialComponent(), "ofulltextsearch");
			((CloseableModalWindowController)searchDialogBox).activate();
			resultCtlr.listenTo(searchDialogBox);
		} else {
			searchDialogBox = new CloseableModalController(getWindowControl(), title, resultCtlr.getInitialComponent());
			((CloseableModalController)searchDialogBox).activate();
		}
	}

	@Override
	protected void event(UserRequest ureq, Controller source, Event event) {
		if (source == resultCtlr) {
			if (event instanceof SearchEvent) {
				SearchEvent goEvent = (SearchEvent)event;
				ResultDocument doc = goEvent.getDocument();
				gotoSearchResult(ureq, doc);
			} else if (event == Event.DONE_EVENT) {
				setSearchString(resultCtlr.getSearchString());
			}
		} else if (CloseableModalWindowController.CLOSE_WINDOW_EVENT.equals(event)) {
			fireEvent(ureq, Event.DONE_EVENT);
		}
	}

	public void closeSearchDialogBox() {
		if (searchDialogBox instanceof CloseableModalController) {
			((CloseableModalController)searchDialogBox).deactivate();
		} else if(searchDialogBox instanceof CloseableModalWindowController) {
			((CloseableModalWindowController)searchDialogBox).deactivate();
		}
		searchDialogBox = null;
	}
	
	/**
	 * 
	 * @param ureq
	 * @param command
	 */
	public void gotoSearchResult(UserRequest ureq, ResultDocument document) {
		try {
		// attach the launcher data
			closeSearchDialogBox();
			String url = document.getResourceUrl();
			if(!StringHelper.containsNonWhitespace(url)) {
				//no url, no document
				getWindowControl().setWarning(getTranslator().translate("error.resource.could.not.found"));
			} else if(url != null && url.startsWith("[ContextHelpModule:")) {
				//do something special for ContextHelp
				int pathIndex = url.indexOf("path=");
				String uri = url.substring(pathIndex + 5, url.length() - 1);
				RedirectMediaResource rsrc = new RedirectMediaResource(uri);
				ureq.getDispatchResult().setResultingMediaResource(rsrc);
			} else {
				BusinessControl bc = BusinessControlFactory.getInstance().createFromString(url);
			  WindowControl bwControl = BusinessControlFactory.getInstance().createBusinessWindowControl(bc, getWindowControl());
			  NewControllerFactory.getInstance().launch(ureq, bwControl);
			}
		} catch (Exception ex) {
			log.debug("Document not found");
			getWindowControl().setWarning(getTranslator().translate("error.resource.could.not.found"));
		}		
	}
	
	protected SearchResults doSearch(UserRequest ureq, String searchString, List<String> condSearchStrings, String parentCtxt, String docType, String rsrcUrl,
			int firstResult, int maxReturns, boolean doSpellCheck) {
		
		String query = null;
		List<String> condQueries = null;
		try {
			if(doSpellCheck) {
				//remove first old "did you mean words"
				hideDidYouMeanWords();
			}
			
			getHighlightWords(searchString);
			
			query = getQueryString(searchString, false);
			condQueries = getCondQueryStrings(condSearchStrings, parentCtxt, docType, rsrcUrl);
			SearchResults searchResults = searchCache.get(getQueryCacheKey(firstResult, query, condQueries));
			if(searchResults == null || true) {
				searchResults = searchClient.doSearch(query, condQueries, ureq.getIdentity(), ureq.getUserSession().getRoles(), firstResult, maxReturns, true);
				searchCache.put(getQueryCacheKey(firstResult, query, condQueries), searchResults);
			}	
			if ((firstResult == 0 && searchResults.getList().isEmpty())
					&& !query.endsWith(FUZZY_SEARCH)) {
				// result-list was empty => first try to find word via spell-checker
	    	if (doSpellCheck) {
	    		Set<String> didYouMeansWords = searchClient.spellCheck(searchString);
		    	if (didYouMeansWords != null && !didYouMeansWords.isEmpty()) {
		    		setDidYouMeanWords(didYouMeansWords);
		    	} else {
		    		searchResults = doFuzzySearch(ureq, searchString, null, parentCtxt, docType, rsrcUrl, firstResult, maxReturns);
		    	}
	    	} else {
	    		searchResults = doFuzzySearch(ureq, searchString, null, parentCtxt, docType, rsrcUrl, firstResult, maxReturns);
	    	}
			}
			
			if(firstResult == 0 && searchResults.getList().isEmpty()) {
				showInfo("found.no.result.try.fuzzy.search");
			}
			return searchResults;
		} catch (ParseException e) {
			if(log.isDebug()) log.debug("Query cannot be parsed: " + query);
			getWindowControl().setWarning(translate("invalid.search.query"));
		} catch (QueryException e) {
			getWindowControl().setWarning(translate("invalid.search.query.with.wildcard"));
		} catch(ServiceNotAvailableException e) {
			getWindowControl().setWarning(translate("search.service.not.available"));
		} catch (Exception e) {
			log.error("Unexpected exception while searching", e);
			getWindowControl().setWarning(translate("search.service.unexpected.error"));
		}
		return SearchResults.EMPTY_SEARCH_RESULTS;
	}
	
	protected Set<String> getHighlightWords(String searchString) {
		try {
			Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_CURRENT);
			TokenStream stream = analyzer.tokenStream("content", new StringReader(searchString));
			TermAttribute termAtt = (TermAttribute) stream.addAttribute(TermAttribute.class);
			for (boolean next = stream.incrementToken(); next; next = stream.incrementToken()) {
				String term = termAtt.term();
				if(log.isDebug()) log.debug(term);
			}
		} catch (IOException e) {
			log.error("", e);
		}
		return null;
	}
	
	protected SearchResults doFuzzySearch(UserRequest ureq, String searchString, List<String> condSearchStrings, String parentCtxt, String docType, String rsrcUrl,
			int firstResult, int maxReturns) throws QueryException, ParseException, ServiceNotAvailableException  {
		hideDidYouMeanWords();
		String query = getQueryString(searchString, true);
		List<String> condQueries = getCondQueryStrings(condSearchStrings, parentCtxt, docType, rsrcUrl);
		SearchResults searchResults = searchCache.get(getQueryCacheKey(firstResult, query, condQueries));
		if(searchResults == null) {
			searchResults = searchClient.doSearch(query, condQueries, ureq.getIdentity(), ureq.getUserSession().getRoles(), firstResult, maxReturns, true);
			searchCache.put(getQueryCacheKey(firstResult, query, condQueries), searchResults);
		}
		return searchResults;
	}
	
	private Object getQueryCacheKey(int firstResult, String query, List<String> condQueries) {
		StringBuilder sb = new StringBuilder();
		sb.append('[').append(firstResult).append(']').append(query).append(' ');
		for(String condQuery:condQueries) {
			sb.append(condQuery).append(' ');
		}
		return sb.toString();
	}
	
	public Set<String> getDidYouMeanWords() {
		if (didYouMeanLinks != null && !didYouMeanLinks.isEmpty()) {
			Set<String> didYouMeanWords = new HashSet<String>();
			for(FormLink link:didYouMeanLinks) {
				String word = (String)link.getUserObject();
				didYouMeanWords.add(word);
			}
			return didYouMeanWords;
		}
		return Collections.emptySet();
	}
	
	/**
	 * Unregister existing did-you-mean-links from content and add new links.
	 * @param didYouMeansWords  List of 'did you mean' words
	 */
	public void setDidYouMeanWords(Set<String> didYouMeansWords) {
		// unregister existing did-you-mean links
		hideDidYouMeanWords();
		
		didYouMeanLinks = new ArrayList<FormLink>(didYouMeansWords.size());
		int wordNumber = 0;
		for (String word : didYouMeansWords) {
			FormLink l = uifactory.addFormLink(CMD_DID_YOU_MEAN_LINK + wordNumber++, word, null, flc, Link.NONTRANSLATED);
			l.setUserObject(word);
			didYouMeanLinks.add(l);
		}
		flc.contextPut("didYouMeanLinks", didYouMeanLinks);
		flc.contextPut("hasDidYouMean", Boolean.TRUE);
	}
	
	protected void hideDidYouMeanWords() {
		// unregister existing did-you-mean links
		if (didYouMeanLinks != null) {
			for (int i = 0; i < didYouMeanLinks.size(); i++) {
				flc.remove(CMD_DID_YOU_MEAN_LINK + i);
			}
			didYouMeanLinks = null;
		}
		flc.contextPut("didYouMeanLinks", didYouMeanLinks);
		flc.contextPut("hasDidYouMean", Boolean.FALSE);
	}
	
	private String getQueryString(String searchString, boolean fuzzy) {
		StringBuilder query = new StringBuilder(searchString);
		if(fuzzy) {
			query.append(FUZZY_SEARCH);
		}
		return query.toString();
	}
	
	private List<String> getCondQueryStrings(List<String> condSearchStrings, String parentCtxt, String docType, String rsrcUrl) {
		List<String> queries = new ArrayList<String>();
		if(condSearchStrings != null && !condSearchStrings.isEmpty()) {
			queries.addAll(condSearchStrings);
		}
		
		if (StringHelper.containsNonWhitespace(parentCtxt)) {
			appendAnd(queries, AbstractOlatDocument.PARENT_CONTEXT_TYPE_FIELD_NAME, ":\"", parentCtxt, "\"");
		}
		if (StringHelper.containsNonWhitespace(docType)) {
			appendAnd(queries, "(", AbstractOlatDocument.DOCUMENTTYPE_FIELD_NAME, ":(", docType, "))");
		}
		if (StringHelper.containsNonWhitespace(rsrcUrl)) {
			appendAnd(queries, AbstractOlatDocument.RESOURCEURL_FIELD_NAME, ":", escapeResourceUrl(rsrcUrl), "*");
		}
		return queries;
	}
	
	private void appendAnd(List<String> queries, String... strings) {
		StringBuilder query = new StringBuilder();
		for(String string:strings) {
			query.append(string);
		}
		
		if(query.length() > 0) {
			queries.add(query.toString());
		}
	}
	
	/**
	 * Remove the ROOT keyword, duplicate entry in the business path
	 * and escape the keywords used by lucene.
	 * @param url
	 * @return
	 */
	protected String escapeResourceUrl(String url) {
		List<String> tokens = getResourceUrlTokenized(url);
		StringBuilder sb = new StringBuilder();
		for(String token:tokens) {
			sb.append("\\[").append(token.replace(":", "\\:")).append("\\]");
		}
		return sb.toString();
	}
	
	protected List<String> getResourceUrlTokenized(String url) {
		if (url.startsWith("ROOT")) {
			url = url.substring(4, url.length());
		}
		List<String> tokens = new ArrayList<String>();
		for(StringTokenizer tokenizer = new StringTokenizer(url, "[]"); tokenizer.hasMoreTokens(); ) {
			String token = tokenizer.nextToken();
			if(!tokens.contains(token)) {
				tokens.add(token);
			}
		}
		return tokens;
	}
	
	protected ContextTokens getContextTokens(String resourceURL) {
		SearchServiceUIFactory searchUIFactory = (SearchServiceUIFactory)CoreSpringFactory.getBean(SearchServiceUIFactory.class);
		List<String> tokens = getResourceUrlTokenized(resourceURL);
		String[] keys = new String[tokens.size() + 1];
		String[] values = new String[tokens.size() + 1];
		keys[0] = "";
		values[0] = translate("search.context.all");
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<tokens.size(); i++) {
			String token = tokens.get(i);
			keys[i+1] = sb.append('[').append(token).append(']').toString();
			values[i+1] = searchUIFactory.getBusinessPathLabel(token, tokens, getLocale());
		}
		return new ContextTokens(keys, values);
	}
	
	public FormItem getFormItem() {
		return flc;
	}
	
	public class ContextTokens {
		private final String[] keys;
		private final String[] values;
		
		public ContextTokens(String[] keys, String[] values) {
			this.keys = keys == null ? new String[0] : keys;
			this.values = values == null ? new String[0] : values;
		}

		public String[] getKeys() {
			return keys;
		}

		public String[] getValues() {
			return values;
		}
		
		public boolean isEmpty() {
			return values.length == 0;
		}
		
		public int getSize() {
			return values.length;
		}
		
		public String getKeyAt(int index) {
			if(keys != null && index < keys.length && index >= 0) {
				return keys[index];
			}
			return "";
		}
		
		public String getValueAt(int index) {
			if(values != null && index < values.length && index >= 0) {
				return values[index];
			}
			return "";
		}
	}
}