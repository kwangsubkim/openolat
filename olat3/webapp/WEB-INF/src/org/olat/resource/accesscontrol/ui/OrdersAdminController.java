package org.olat.resource.accesscontrol.ui;

import java.util.Collections;
import java.util.List;

import org.olat.core.CoreSpringFactory;
import org.olat.core.gui.ShortName;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.table.DefaultColumnDescriptor;
import org.olat.core.gui.components.table.StaticColumnDescriptor;
import org.olat.core.gui.components.table.Table;
import org.olat.core.gui.components.table.TableController;
import org.olat.core.gui.components.table.TableEvent;
import org.olat.core.gui.components.table.TableGuiConfiguration;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.resource.OLATResource;
import org.olat.resource.accesscontrol.manager.ACFrontendManager;
import org.olat.resource.accesscontrol.model.AccessTransaction;
import org.olat.resource.accesscontrol.model.Order;
import org.olat.resource.accesscontrol.ui.OrdersDataModel.Col;

public class OrdersAdminController extends BasicController  {
	
	private static final String CMD_SELECT = "sel";

	private final Panel mainPanel;
	private final VelocityContainer mainVC;
	private final TableController tableCtr;
	private OrderDetailController detailController;
	
	private final ACFrontendManager acFrontendManager;
	private final OLATResource resource;
	
	public OrdersAdminController(UserRequest ureq, WindowControl wControl, OLATResource resource) {
		super(ureq, wControl);
		
		acFrontendManager = (ACFrontendManager)CoreSpringFactory.getBean("acFrontendManager");
		this.resource = resource;

		TableGuiConfiguration tableConfig = new TableGuiConfiguration();
		tableConfig.setDownloadOffered(true);
		tableConfig.setPreferencesOffered(true, "Orders");		
		tableConfig.setTableEmptyMessage(translate("table.order.empty"));

		tableCtr = new TableController(tableConfig, ureq, wControl, Collections.<ShortName>emptyList(), null, null , null, false, getTranslator());
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("order.nr", Col.orderNr.ordinal(), CMD_SELECT, getLocale()));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("order.creationDate", Col.creationDate.ordinal(), null, getLocale()));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("order.summary", Col.summary.ordinal(), null, getLocale()));
		tableCtr.addColumnDescriptor(new DefaultColumnDescriptor("order.delivery", Col.delivery.ordinal(), null, getLocale()));
		tableCtr.addColumnDescriptor(new StaticColumnDescriptor(CMD_SELECT, "table.order.details", getTranslator().translate("select")));
		
		listenTo(tableCtr);
		
		loadModel();
		
		mainVC = createVelocityContainer("orders");
		mainVC.put("orderList", tableCtr.getInitialComponent());
		mainVC.contextPut("title", translate("orders.my"));
		mainVC.contextPut("description", translate("orders.my.desc"));

		mainPanel = putInitialPanel(mainVC);
	}
	
	private void loadModel() {
		List<Order> orders = acFrontendManager.findOrders(resource);
		List<AccessTransaction> transactions = acFrontendManager.findTransactions(orders);
		List<OrderTableItem> items = OrdersDataModel.create(orders, transactions);
		tableCtr.setTableDataModel(new OrdersDataModel(items, getLocale()));
	}
	
	protected void doDispose() {
		//
	}

	@Override
	protected void event(UserRequest ureq, Component source, Event event) {
		//
	}
	
	@Override
	protected void event(UserRequest ureq, Controller source, Event event) {
		if(source == tableCtr) {
			if (event.getCommand().equals(Table.COMMANDLINK_ROWACTION_CLICKED)) {
				TableEvent te = (TableEvent) event;
				String actionid = te.getActionId();
				int rowid = te.getRowId();
				OrderTableItem order = (OrderTableItem)tableCtr.getTableDataModel().getObject(rowid);
				if(CMD_SELECT.equals(actionid)) {
					selectOrder(ureq, order);
				}
			}
		} else if (source == detailController) {
			if(event == Event.BACK_EVENT) {
				mainPanel.setContent(mainVC);
				removeAsListenerAndDispose(detailController);
				detailController = null;
			}
		}
	}
	
	protected void selectOrder(UserRequest ureq, OrderTableItem order) {
		removeAsListenerAndDispose(detailController);
		
		detailController = new OrderDetailController(ureq, getWindowControl(), order.getOrder(), order.getTransactions());
		listenTo(detailController);
		mainPanel.setContent(detailController.getInitialComponent());
	}
}