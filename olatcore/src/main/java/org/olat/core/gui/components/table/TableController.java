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

package org.olat.core.gui.components.table;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.olat.core.gui.ShortName;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.choice.Choice;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.ajax.autocompletion.AutoCompleterController;
import org.olat.core.gui.control.generic.ajax.autocompletion.EntriesChosenEvent;
import org.olat.core.gui.control.generic.ajax.autocompletion.ListProvider;
import org.olat.core.gui.control.generic.ajax.autocompletion.ListReceiver;
import org.olat.core.gui.control.generic.closablewrapper.CloseableModalController;
import org.olat.core.gui.media.MediaResource;
import org.olat.core.gui.render.StringOutput;
import org.olat.core.gui.translator.PackageTranslator;
import org.olat.core.gui.translator.Translator;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.OLog;
import org.olat.core.logging.Tracing;
import org.olat.core.util.StringHelper;
import org.olat.core.util.Util;
import org.olat.core.util.filter.Filter;
import org.olat.core.util.filter.FilterFactory;

/**
 * <!--**************-->
 * <h3>Responsability:</h3>
 * This controller wraps a table component and offers additional features like
 * column selection. Two constructors are supported: regular table and table
 * with a table filter. Use the TableGuiConfiguration object to configure the
 * various rendering options.
 * <p>
 * <!--**************-->
 * <h3>Events fired:</h3>
 * <ul>
 * <li><i>{@link #EVENT_FILTER_SELECTED}</i>:<br>
 * After succesfully activation of the selected filter. </li>
 * <li><i>{@link #EVENT_NOFILTER_SELECTED}</i>:<br>
 * After deactivation of the last filter.</li>
 * <li><i>{@link org.olat.core.gui.components.table.Table Table component events}</i>:<br>
 * Forwards all events from the table component.</li>
 * </ul>
 * <p>
 * <!--**************-->
 * <h3>Workflow:</h3>
 * <ul>
 * <li><i>Change table columns:</i><br>
 * Show a modal dialog for choosing visible/invisible table columns.<br>
 * Save table columns settings in the users preferences.</li>
 * <li><i>Download table content:</i><br>
 * Formats table content as CSV.<br>
 * Creates an asynchronously delivered
 * {@link org.olat.core.gui.media.ExcelMediaResource excel media resource}.</li>
 * <li><i>Apply table filter:</i><br>
 * Activates a defined table filter.<br>
 * Deactivates a table filter. </li>
 * </ul>
 * <p>
 * <!--**************-->
 * <h3>Special translators:</h3>
 * Uses a translator provided in the constructor as <i>fallback</i>.
 * <p>
 * <!--**************-->
 * <h3>Hints:</h3>
 * Opens a modal dialog for choosing which columns to hide or show.
 * <p>
 * 
 * @author Felix Jost, Florian Gnägi
 */
public class TableController extends BasicController {
	
	private static final String VC_VAR_USE_NO_FILTER_OPTION = "useNoFilterOption";

	private static final String COMPONENT_TABLE_NAME = "table";

	private static final String VC_VAR_SELECTED_FILTER_VALUE = "selectedFilterValue";

	private static final String LINK_NUMBER_OF_ELEMENTS = "link.numberOfElements";

	private static final String VC_VAR_IS_FILTERED = "isFiltered";

	private static final String VC_VAR_HAS_TABLE_SEARCH = "hasTableSearch";

	private static final String LOG_DEBUG_DURATION = "  duration=";

	private OLog log = Tracing.createLoggerFor(this.getClass());
	
	private static final String CMD_FILTER = "cmd.filter.";
	private static final String CMD_FILTER_NOFILTER = "cmd.filter.nofilter";

	/** Event is fired when the 'apply no filter' is selected * */
	public static final Event EVENT_NOFILTER_SELECTED = new Event("nofilter.selected");
	/**
	 * Event is fired when a specific filter is selected. Use getActiveFilter to
	 * retrieve the selected filter
	 */
	public static final Event EVENT_FILTER_SELECTED = new Event("filter.selected");

	/**
	 * Limit the number of search-suggestions in table-search-popup
	 */
	private static final int MAX_TABLE_SEARCH_RESULT_ENTRIES = 20;

	private VelocityContainer contentVc;

	private Table table;

	private Choice colsChoice;
	private TablePrefs prefs;
	private TableGuiConfiguration tableConfig;

	private List filters;
	private ShortName activeFilter;

	private boolean tablePrefsInitialized = false;
	private CloseableModalController cmc;
	private Controller tableSearchController;

	private Link resetLink;

	/**
	 * Constructor for the table controller using the table filter.
	 * 
	 * @param tableConfig The table GUI configuration determines the tables
	 *          behavior, may be <code>null</code> to use default table configuration.
	 * @param ureq The user request
	 * @param wControl The window control
	 * @param filters A list of filter objects ({@link ShortName})
	 * @param activeFilter The initially activated filter object
	 * @param filterTitle The translated title of the filter
	 * @param noFilterOption The translated key for the no-filter filter or
	 *          <code>null</code> if not used
	 * @param tableTrans The translator that is used to translate the table
	 */
	public TableController(final TableGuiConfiguration tableConfig, final UserRequest ureq, final WindowControl wControl, final List filters, final ShortName activeFilter,
			final String filterTitle, final String noFilterOption, final Translator tableTrans) {
		// init using regular constructor
		this(tableConfig, ureq, wControl, tableTrans);

		// push filter to velocity page
		setFilters(filters, activeFilter);
		this.contentVc.contextPut("filterTitle", filterTitle);
		if (noFilterOption != null) {
			this.contentVc.contextPut("noFilterOption", noFilterOption);
			this.contentVc.contextPut(VC_VAR_USE_NO_FILTER_OPTION, Boolean.TRUE);
		} else {
			this.contentVc.contextPut(VC_VAR_USE_NO_FILTER_OPTION, Boolean.FALSE);
		}
	}

	/**
	 * Constructor for the table controller
	 * 
	 * @param tableConfig The table gui configuration determines the tables
	 *          behaviour, may be <code>null</code> to use default table config.
	 * @param ureq The user request
	 * @param wControl The window control
	 * @param tableTrans The translator that is used to translate the table
	 */
	public TableController(final TableGuiConfiguration tableConfigP, final UserRequest ureq, final WindowControl wControl, final Translator tableTrans) {
		super(ureq, wControl);
		if (tableConfigP == null){
			tableConfig = new TableGuiConfiguration();
		}else{
			tableConfig = tableConfigP;
		}
		
		if (tableTrans != null) {
			setTranslator(new PackageTranslator(Util.getPackageName(TableController.class), ureq.getLocale(), tableTrans));
		}
		
		this.table = new Table(COMPONENT_TABLE_NAME, getTranslator());
		this.table.addListener(this);

		// propagate table specific configuration to table,
		// rest of configuration is handled by this controller
		this.table.setColumnMovingOffered(tableConfig.isColumnMovingOffered());
		this.table.setDisplayTableHeader(tableConfig.isDisplayTableHeader());
		this.table.setSelectedRowUnselectable(tableConfig.isSelectedRowUnselectable());
		this.table.setSortingEnabled(tableConfig.isSortingEnabled());
		this.table.setPageingEnabled(tableConfig.isPageingEnabled());
		this.table.setResultsPerPage(tableConfig.getResultsPerPage());
		this.table.setMultiSelect(tableConfig.isMultiSelect());
		this.table.enableShowAllLink(tableConfig.isShowAllLinkEnabled());


		// table is embedded in a velocity page that renders the surrounding layout
		contentVc = createVelocityContainer("tablelayout");
		contentVc.put(COMPONENT_TABLE_NAME, table);

		// fetch prefs (which were loaded at login time
		String preferencesKey = tableConfig.getPreferencesKey();
		if (tableConfig.isPreferencesOffered() && preferencesKey != null) {
			this.prefs = (TablePrefs) ureq.getUserSession().getGuiPreferences().get(TableController.class, preferencesKey);
		}

		// empty table message
		String tableEmptyMessage = tableConfig.getTableEmptyMessage();
		if (tableEmptyMessage == null){
			tableEmptyMessage = translate("default.tableEmptyMessage");
		}
		contentVc.contextPut("tableEmptyMessage", tableEmptyMessage);

		contentVc.contextPut("tableConfig", tableConfig);
		contentVc.contextPut(VC_VAR_HAS_TABLE_SEARCH, Boolean.FALSE);
		
		putInitialPanel(contentVc);
	}

	public TableController(final TableGuiConfiguration tableConfig, final UserRequest ureq, final WindowControl wControl, final Translator tableTrans,
			final boolean enableTableSearch ) {
		this(tableConfig, ureq, wControl, tableTrans);
		if (enableTableSearch) {
			tableSearchController = createTableSearchController(ureq, wControl);
			contentVc.put("tableSearch", tableSearchController.getInitialComponent());
			contentVc.contextPut(VC_VAR_HAS_TABLE_SEARCH, Boolean.TRUE);
		} else {
			contentVc.contextPut(VC_VAR_HAS_TABLE_SEARCH, Boolean.FALSE);
		}
	}

	private Controller createTableSearchController(final UserRequest ureq, final WindowControl wControl) {
		ListProvider genericProvider = new ListProvider() {
			public void getResult(final String searchValue, final ListReceiver receiver) {
				Filter htmlFilter = FilterFactory.getHtmlTagsFilter();
				log.debug("getResult start");
				long startTime = System.currentTimeMillis();
				Set<String> searchEntries = new TreeSet<String>();
				int entryCounter = 1;
				// loop over whole data-model
				for (int rowIndex=0; rowIndex < table.getUnfilteredTableDataModel().getRowCount(); rowIndex++) {
					for (int colIndex=0; colIndex < table.getUnfilteredTableDataModel().getColumnCount(); colIndex++) {
						Object obj = table.getUnfilteredTableDataModel().getValueAt(rowIndex, colIndex);
						// When a CustomCellRenderer exist, use this to render cell-value to String
						ColumnDescriptor cd = table.getColumnDescriptorFromAllCDs(colIndex);
						if (table.isColumnDescriptorVisible(cd)) {
							
							if (cd instanceof CustomRenderColumnDescriptor) {
								CustomCellRenderer customCellRenderer = ((CustomRenderColumnDescriptor)cd).getCustomCellRenderer();
								if (customCellRenderer instanceof CustomCssCellRenderer) {
									// For css renderers only use the hover
									// text, not the CSS class name and other
									// markup
									CustomCssCellRenderer cssRenderer = (CustomCssCellRenderer) customCellRenderer;
									obj = cssRenderer.getHoverText(obj);									
									if (!StringHelper.containsNonWhitespace((String) obj)) {
										continue;
									}
								} else {
									StringOutput sb = new StringOutput();
									customCellRenderer.render(sb, null, obj, ((CustomRenderColumnDescriptor) cd).getLocale(), cd.getAlignment(), null);
									obj = sb.toString();																		
								}
							} 
	
							if (obj instanceof String) {
								String valueString = (String)obj;
								// Remove any HTML markup from the value
								valueString = htmlFilter.filter(valueString);
								// Finally compare with search value based on a simple lowercase match
								if (valueString.toLowerCase().indexOf(searchValue.toLowerCase()) != -1) {
									if (searchEntries.add(valueString) ) {
										// Add to receiver list same entries only once
										if (searchEntries.size() == 1) {
											// before first entry, add searchValue. But add only when one search match
											receiver.addEntry( searchValue, searchValue );
										}
										// limit the number of entries
										if (entryCounter++ > MAX_TABLE_SEARCH_RESULT_ENTRIES) {
											receiver.addEntry("...", "...");
											long duration = System.currentTimeMillis() - startTime;	
											log.debug("getResult reach MAX_TABLE_SEARCH_RESULT_ENTRIES, entryCounter=" + entryCounter + LOG_DEBUG_DURATION + duration);
											return;
										}
										receiver.addEntry(valueString, valueString);
									}								
								}
							}
						}
					}
				}
				long duration = System.currentTimeMillis() - startTime;	
				log.debug("getResult finished entryCounter=" + entryCounter + LOG_DEBUG_DURATION + duration);
			}
		};
		removeAsListenerAndDispose(tableSearchController);
		tableSearchController = new AutoCompleterController(ureq, wControl, genericProvider, null, false, 60, 3, translate("table.filter.label"));
		listenTo(tableSearchController); // TODO:CG 02.09.2010 Test Tablesearch Performance, remove
		return tableSearchController;
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest,
	 *      org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	public void event(final UserRequest ureq, final Component source, final Event event) {
		if (source == table) {
			boolean aPageingCommand = event.getCommand().equalsIgnoreCase(Table.COMMAND_SHOW_PAGES);
			aPageingCommand = aPageingCommand || event.getCommand().equalsIgnoreCase(Table.COMMAND_PAGEACTION_SHOWALL);
			
			if (!aPageingCommand) {
				// forward to table controller listener
				fireEvent(ureq, event);
			}
		} else if (source == contentVc) {
			handleCommandsOfTableVcContainer(ureq, event); 
		} else if (source == colsChoice) {
			if (event == Choice.EVNT_VALIDATION_OK) {
				//sideeffect on table and prefs
				applyAndcheckChangedColumnsChoice(ureq);
			} else { // cancelled
				cmc.deactivate();
			}
		} else if (source == resetLink) {
			this.table.setSearchString(null);
			this.modelChanged();
		}
	}

	private void handleCommandsOfTableVcContainer(final UserRequest ureq,	final Event event) {
		// links of this vc container coming in
		String cmd = event.getCommand();
		if (cmd.equals("cmd.changecols") && tableConfig.getPreferencesKey() != null) {
			colsChoice = getColumnListAndTheirVisibility();
			removeAsListenerAndDispose(cmc);
			cmc = new CloseableModalController(getWindowControl(), "close", colsChoice,true,translate("title.changecols"));
			listenTo(cmc);
			cmc.activate();
		} else if (cmd.equals("cmd.download") && tableConfig.isDownloadOffered()) {
			TableExporter tableExporter = tableConfig.getDownloadOffered();
			MediaResource mr = tableExporter.export(table);
			ureq.getDispatchResult().setResultingMediaResource(mr);
		} else if (cmd.equals(CMD_FILTER_NOFILTER)) {
			// update new filter value
			setActiveFilter(null);
			fireEvent(ureq, EVENT_NOFILTER_SELECTED);
		} else if (cmd.indexOf(CMD_FILTER) == 0) {
			String areafilter = cmd.substring(CMD_FILTER.length());
			int filterPosition = Integer.parseInt(areafilter);
			// security check
			if (filters.size() < (filterPosition + 1)){
				throw new AssertException("Filter size was ::" + filters.size() + " but requested filter was ::" + filterPosition);
			}
			// update new filter value
			setActiveFilter((ShortName) filters.get(filterPosition));
			fireEvent(ureq, EVENT_FILTER_SELECTED);
		}
	}

	private void applyAndcheckChangedColumnsChoice(final UserRequest ureq) {
		List selRows = colsChoice.getSelectedRows();
		if (selRows.size() == 0) {
			showError("error.selectatleastonecolumn");
		} else {
			// check that there is at least one data column (because of sorting
			// (technical) and information (usability))
			if (table.isSortableColumnIn(selRows)) {
				// ok
				table.updateConfiguredRows(selRows);
				// update user preferences, use the given preferences key
				if (prefs == null){
					prefs = new TablePrefs();
				}
				prefs.setActiveColumnsRef(selRows);
				ureq.getUserSession().getGuiPreferences().putAndSave(TableController.class, tableConfig.getPreferencesKey(), prefs);
				// pop configuration dialog
				cmc.deactivate();
			} else {
				showError("error.atleastonedatacolumn");
			}
		}
	}

	private Choice getColumnListAndTheirVisibility() {
		Choice choice = new Choice("colchoice", getTranslator());
		choice.setTableDataModel(table.createChoiceTableDataModel());
		choice.addListener(this);
		choice.setCancelKey("cancel");
		choice.setSubmitKey("save");
		return choice;
	}

	public void event(final UserRequest ureq, final Controller source, final Event event) {
		log.debug("dispatchEvent event=" + event + "  source=" + source);
		if (event instanceof EntriesChosenEvent) {
			EntriesChosenEvent ece = (EntriesChosenEvent)event;				
			List filterList = ece.getEntries();
			if (!filterList.isEmpty()) {
				this.table.setSearchString((String)filterList.get(0));
				this.modelChanged(false);
			}	else {
			  // reset filter search filter in modelChanged
				this.modelChanged();
			}
		} 
	}
	/**
	 * @return The currently active filter object or <code>null</code> if no
	 *         filter is applied
	 */
	public ShortName getActiveFilter() {
		return this.activeFilter;
	}

	/**
	 * @param activeFilter The currently applied filter or <code>null</code> if
	 *          no filter is applied
	 */
	public void setActiveFilter(final ShortName activeFilter) {
		this.activeFilter = activeFilter;
		if (this.activeFilter == null) {
			this.contentVc.contextPut(VC_VAR_SELECTED_FILTER_VALUE, CMD_FILTER_NOFILTER);
		} else {
			this.contentVc.contextPut(VC_VAR_SELECTED_FILTER_VALUE, this.activeFilter.getShortName());
		}
	}

	/**
	 * Sets the list of filters and the currently active filter
	 * 
	 * @param filters List of TableFilter
	 * @param activeFilter active TableFilter
	 */
	public void setFilters(final List filters, final ShortName activeFilter) {
		this.filters = filters;
		this.contentVc.contextPut("hasFilters", filters == null ? Boolean.FALSE : Boolean.TRUE);
		this.contentVc.contextPut("filters", filters);
		setActiveFilter(activeFilter);
	}

	public void modelChanged() {
		modelChanged(true);
	}

	/**
	 * Notifies the controller about a changed table data model. This will check
	 * if the table data model has any values and show a message instead of the
	 * table when the model has no rows.
	 */
	public void modelChanged(final boolean resetSearchString) {
		if (resetSearchString) {
			table.setSearchString(null);
		}
		table.modelChanged();
		TableDataModel tableModel = table.getTableDataModel();
		if (tableModel != null) {
			this.contentVc.contextPut("tableEmpty", tableModel.getRowCount() == 0 ? Boolean.TRUE : Boolean.FALSE);
			this.contentVc.contextPut("numberOfElements", String.valueOf(table.getUnfilteredRowCount()));
			if (table.isTableFiltered()) {
				this.contentVc.contextPut("numberFilteredElements", String.valueOf(table.getRowCount()));
				this.contentVc.contextPut(VC_VAR_IS_FILTERED, Boolean.TRUE); 
				this.contentVc.contextPut("filter", table.getSearchString());
				resetLink = LinkFactory.createCustomLink(LINK_NUMBER_OF_ELEMENTS, LINK_NUMBER_OF_ELEMENTS, String.valueOf(table.getUnfilteredRowCount()), Link.NONTRANSLATED, contentVc, this);
			} else {
				this.contentVc.contextPut(VC_VAR_IS_FILTERED, Boolean.FALSE); 
			}
		}
		// else do nothing. The table might have no table data model during
		// constructing time of
		// this controller.
	}

	/**
	 * Sets the tableDataModel. IMPORTANT: Once a tableDataModel is set, it is
	 * assumed to remain constant in its data & row & colcount. Otherwise a
	 * modelChanged has to be called
	 * 
	 * @param tableDataModel The tableDataModel to set
	 */
	public void setTableDataModel(final TableDataModel tableDataModel) {
		table.setTableDataModel(tableDataModel);
		if (!tablePrefsInitialized) { // first time
			if (prefs != null) {
				try {
					List acolRefs = prefs.getActiveColumnsRef();
					table.updateConfiguredRows(acolRefs);
				} catch(IndexOutOfBoundsException ex) {
					// GUI prefs match not to table data model => reset prefs
					prefs = null;
				}
			}
			tablePrefsInitialized = true;
		}
		modelChanged();
	}

	/**
	 * Add a table column descriptor
	 * 
	 * @param visible true: is visible; false: is not visible
	 * @param cd column descriptor
	 */
	public void addColumnDescriptor(final boolean visible, final ColumnDescriptor cd) {
		table.addColumnDescriptor(cd, -1, visible);
	}

	/**
	 * Add a visible table column descriptor
	 * 
	 * @param cd column descriptor
	 */
	public void addColumnDescriptor(final ColumnDescriptor cd) {
		table.addColumnDescriptor(cd, -1, true);
	}
	
	/**
	 * Get the table column descriptor.
	 * @param row
	 * @return ColumnDescriptor
	 */
	public ColumnDescriptor getColumnDescriptor(final int row) {
		return table.getColumnDescriptor(row);
	}

	/**
	 * Get the current table data model from the table
	 * 
	 * @return TableDataModel
	 */
	public TableDataModel getTableDataModel() {
		return table.getTableDataModel();
	}
	
	/**
	 * Sorts the selected table row indexes according with the table Comparator,
	 * and then retrieves the rows from the input defaultTableDataModel.
	 * It is assumed that the defaultTableDataModel IS THE MODEL for the table.
	 * @param objectMarkers
	 * @return the List with the sorted selected objects in this table.
	 */
	public List getSelectedSortedObjects(final BitSet objectMarkers, final DefaultTableDataModel defaultTableDataModel) {		
		List results = new ArrayList();
		List<Integer> sortedIndexes = new ArrayList<Integer>();
		if(objectMarkers.isEmpty()) {
			sortedIndexes.clear();
		}
		for (int i = objectMarkers.nextSetBit(0); i >= 0; i = objectMarkers.nextSetBit(i + 1)) {
			sortedIndexes.add(i);
		}
		Collections.sort(sortedIndexes, table);
		Iterator<Integer> indexesIterator = sortedIndexes.iterator();
		while (indexesIterator.hasNext()) {
			results.add(defaultTableDataModel.getObject(indexesIterator.next()));
		}
		return results;
	}

	/**
	 * Sets the selectedRowId to a specific row id. Make sure that this is valid,
	 * the table does not check for out of bound exception.
	 * 
	 * @param selectedRowId The selectedRowId to set
	 */
	public void setSelectedRowId(final int selectedRowId) {
		table.setSelectedRowId(selectedRowId);
	}

	/**
	 * Sets the sortColumn to a specific colun id. Check if the column can be accessed 
	 * and if it is sortable.
	 * 
	 * @param sortColumn The sortColumn to set
	 * @param isSortAscending true: sorting is ascending
	 */
	public void setSortColumn(final int sortColumn, final boolean isSortAscending) {
		if ((table.getColumnCount() > sortColumn)
				&& table.getColumnDescriptor(sortColumn).isSortingAllowed()) {
			table.setSortColumn(sortColumn, isSortAscending);
			table.resort();
		}
	}

	/**
	 * Sets whether user is able to select multiple rows via checkboxes.
	 * 
	 * @param isMultiSelect
	 */
	public void setMultiSelect(final boolean isMultiSelect) {
		table.setMultiSelect(isMultiSelect);
	}
	
	public void setMultiSelectSelectedAt(final int row, final boolean selected) {
		table.setMultiSelectSelectedAt(row, selected);
	}
	
	public void setMultiSelectReadonlyAt(final int row, final boolean readonly) {
		table.setMultiSelectReadonlyAt(row, readonly);
	}
	
	/**
	 * Add a multiselect action.
	 * 
	 * @param actionKeyi18n
	 * @param actionIdentifier
	 */
	public void addMultiSelectAction(final String actionKeyi18n, final String actionIdentifier) {
		table.addMultiSelectAction(actionKeyi18n, actionIdentifier);
	}
	
	public int getTableSortCol() {
		return table.getSortColumn();
	}
	public boolean getTableSortAsc() {
		return table.getSortAscending();
	}
	
	/**
	 * @see org.olat.core.gui.control.DefaultController#doDispose(boolean)
	 */
	protected void doDispose() {
		//
	}
	
}
