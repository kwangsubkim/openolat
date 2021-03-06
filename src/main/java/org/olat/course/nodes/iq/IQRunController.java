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
* Copyright (c) since 2004 at Multimedia- & E-Learning Services (MELS),<br>
* University of Zurich, Switzerland.
* <hr>
* <a href="http://www.openolat.org">
* OpenOLAT - Online Learning and Training</a><br>
* This file has been modified by the OpenOLAT community. Changes are licensed
* under the Apache 2.0 license as the original file.
*/

package org.olat.course.nodes.iq;

import java.text.DateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import org.dom4j.Document;
import org.olat.core.CoreSpringFactory;
import org.olat.core.commons.fullWebApp.LayoutMain3ColsController;
import org.olat.core.gui.UserRequest;
import org.olat.core.gui.components.Component;
import org.olat.core.gui.components.htmlsite.HtmlStaticPageComponent;
import org.olat.core.gui.components.link.Link;
import org.olat.core.gui.components.link.LinkFactory;
import org.olat.core.gui.components.panel.Panel;
import org.olat.core.gui.components.velocity.VelocityContainer;
import org.olat.core.gui.control.Controller;
import org.olat.core.gui.control.Event;
import org.olat.core.gui.control.WindowControl;
import org.olat.core.gui.control.controller.BasicController;
import org.olat.core.gui.control.generic.dtabs.Activateable2;
import org.olat.core.gui.control.generic.iframe.IFrameDisplayController;
import org.olat.core.id.Identity;
import org.olat.core.id.OLATResourceable;
import org.olat.core.id.Roles;
import org.olat.core.id.context.ContextEntry;
import org.olat.core.id.context.StateEntry;
import org.olat.core.logging.AssertException;
import org.olat.core.logging.OLATRuntimeException;
import org.olat.core.logging.activity.ThreadLocalUserActivityLogger;
import org.olat.core.util.Formatter;
import org.olat.core.util.StringHelper;
import org.olat.core.util.UserSession;
import org.olat.core.util.event.EventBus;
import org.olat.core.util.event.GenericEventListener;
import org.olat.core.util.resource.OresHelper;
import org.olat.core.util.vfs.VFSContainer;
import org.olat.course.CourseFactory;
import org.olat.course.ICourse;
import org.olat.course.assessment.AssessmentHelper;
import org.olat.course.assessment.AssessmentManager;
import org.olat.course.assessment.AssessmentNotificationsHandler;
import org.olat.course.auditing.UserNodeAuditManager;
import org.olat.course.nodes.AssessableCourseNode;
import org.olat.course.nodes.CourseNode;
import org.olat.course.nodes.IQSELFCourseNode;
import org.olat.course.nodes.IQSURVCourseNode;
import org.olat.course.nodes.IQTESTCourseNode;
import org.olat.course.nodes.ObjectivesHelper;
import org.olat.course.nodes.SelfAssessableCourseNode;
import org.olat.course.run.scoring.ScoreEvaluation;
import org.olat.course.run.userview.UserCourseEnvironment;
import org.olat.ims.qti.QTIChangeLogMessage;
import org.olat.ims.qti.container.AssessmentContext;
import org.olat.ims.qti.navigator.NavigatorDelegate;
import org.olat.ims.qti.process.AssessmentInstance;
import org.olat.ims.qti.process.ImsRepositoryResolver;
import org.olat.instantMessaging.InstantMessagingService;
import org.olat.modules.ModuleConfiguration;
import org.olat.modules.iq.IQDisplayController;
import org.olat.modules.iq.IQManager;
import org.olat.modules.iq.IQSecurityCallback;
import org.olat.modules.iq.IQSubmittedEvent;
import org.olat.repository.RepositoryEntry;
import org.olat.repository.RepositoryManager;
import org.olat.util.logging.activity.LoggingResourceable;

/**
 * Description:<BR>
 * Run controller for the qti test, selftest and survey course node.
 * Call assessmentStopped if test is finished, closed or at dispose (e.g. course tab gets closed).
 * 
 * Initial Date:  Oct 13, 2004
 * @author Felix Jost
 */
public class IQRunController extends BasicController implements GenericEventListener, Activateable2, NavigatorDelegate {

	private VelocityContainer myContent;
	
	private IQSecurityCallback secCallback;
	private ModuleConfiguration modConfig;
	
	private LayoutMain3ColsController displayContainerController;
	private IQDisplayController displayController;
	private CourseNode courseNode;
	private String type;
	private UserCourseEnvironment userCourseEnv;
	private Link startButton;
	private Link showResultsButton;
	private Link hideResultsButton;

	private IFrameDisplayController iFrameCtr;

	private Panel mainPanel;
	
	private boolean assessmentStopped = true; //default: true
	private EventBus singleUserEventCenter;
	private OLATResourceable assessmentEventOres;	
	private UserSession userSession;
	
	private OLATResourceable assessmentInstanceOres;
	
	private final IQManager iqManager;
	
	/**
	 * Constructor for a test run controller
	 * @param userCourseEnv
	 * @param moduleConfiguration
	 * @param secCallback
	 * @param ureq
	 * @param wControl
	 * @param testCourseNode
	 */
	IQRunController(UserCourseEnvironment userCourseEnv, ModuleConfiguration moduleConfiguration, IQSecurityCallback secCallback, UserRequest ureq, WindowControl wControl, IQTESTCourseNode testCourseNode) {
		super(ureq, wControl);
		
		this.modConfig = moduleConfiguration;
		this.secCallback = secCallback;
		this.userCourseEnv = userCourseEnv;
		this.courseNode = testCourseNode;
		this.type = AssessmentInstance.QMD_ENTRY_TYPE_ASSESS;
		this.singleUserEventCenter = ureq.getUserSession().getSingleUserEventCenter();
		this.assessmentEventOres = OresHelper.createOLATResourceableType(AssessmentEvent.class);
		this.assessmentInstanceOres = OresHelper.createOLATResourceableType(AssessmentInstance.class);
		
		this.userSession = ureq.getUserSession();
		iqManager = CoreSpringFactory.getImpl(IQManager.class);
		
		addLoggingResourceable(LoggingResourceable.wrap(courseNode));
		
		myContent = createVelocityContainer("testrun");
		
		mainPanel = putInitialPanel(myContent);

		if (!modConfig.get(IQEditController.CONFIG_KEY_TYPE).equals(AssessmentInstance.QMD_ENTRY_TYPE_ASSESS)) {
			throw new OLATRuntimeException("IQRunController launched with Test constructor but module configuration not configured as test" ,null);
		}
		init(ureq);
		exposeUserTestDataToVC(ureq);
		
		StringBuilder qtiChangelog = createChangelogMsg(ureq);
		// decide about changelog in VC
		if(qtiChangelog.length()>0){
			//there is some message
			myContent.contextPut("changeLog", qtiChangelog);
		}
		
	  //if show results on test home page configured - show log
		Boolean showResultOnHomePage = (Boolean) testCourseNode.getModuleConfiguration().get(IQEditController.CONFIG_KEY_RESULT_ON_HOME_PAGE);		
		myContent.contextPut("showChangelog", showResultOnHomePage);
	}

	/**
	 * @param ureq
	 * @return
	 */
	private StringBuilder createChangelogMsg(UserRequest ureq) {
		/*
		 * TODO:pb:is ImsRepositoryResolver the right place for getting the change log?
		 */
		RepositoryEntry re = courseNode.getReferencedRepositoryEntry();
		//re could be null, but if we are here it should not be null!
		Roles userRoles = ureq.getUserSession().getRoles();
		boolean showAll = userRoles.isAuthor() || userRoles.isOLATAdmin();
		//get changelog
		Formatter formatter = Formatter.getInstance(ureq.getLocale());
		ImsRepositoryResolver resolver = new ImsRepositoryResolver(re);
		QTIChangeLogMessage[] qtiChangeLog = resolver.getDocumentChangeLog();
		StringBuilder qtiChangelog = new StringBuilder();

		if(qtiChangeLog.length>0){
			//there are resource changes
			Arrays.sort(qtiChangeLog);
			for (int i = qtiChangeLog.length-1; i >= 0 ; i--) {
				//show latest change first
				if(!showAll && qtiChangeLog[i].isPublic()){
					//logged in person is a normal user, hence public messages only
					Date msgDate = new Date(qtiChangeLog[i].getTimestmp());
					qtiChangelog.append("\nChange date: ").append(formatter.formatDateAndTime(msgDate)).append("\n");
					String msg = StringHelper.escapeHtml(qtiChangeLog[i].getLogMessage());
					qtiChangelog.append(msg);
					qtiChangelog.append("\n********************************\n");
				}else if (showAll){
					//logged in person is an author, olat admin, owner, show all messages
					Date msgDate = new Date(qtiChangeLog[i].getTimestmp());
					qtiChangelog.append("\nChange date: ").append(formatter.formatDateAndTime(msgDate)).append("\n");
					String msg = StringHelper.escapeHtml(qtiChangeLog[i].getLogMessage());
					qtiChangelog.append(msg);
					qtiChangelog.append("\n********************************\n");
				}//else non public messages are not shown to normal user
			}
		}
		return qtiChangelog;
	}

	/**
	 * Constructor for a self-test run controller
	 * @param userCourseEnv
	 * @param moduleConfiguration
	 * @param secCallback
	 * @param ureq
	 * @param wControl
	 * @param selftestCourseNode
	 */
	IQRunController(UserCourseEnvironment userCourseEnv, ModuleConfiguration moduleConfiguration, IQSecurityCallback secCallback, UserRequest ureq, WindowControl wControl, IQSELFCourseNode selftestCourseNode) {
		super(ureq, wControl);
		
		this.modConfig = moduleConfiguration;
		this.secCallback = secCallback;
		this.userCourseEnv = userCourseEnv;
		this.courseNode = selftestCourseNode;
		this.type = AssessmentInstance.QMD_ENTRY_TYPE_SELF;
		iqManager = CoreSpringFactory.getImpl(IQManager.class);

		addLoggingResourceable(LoggingResourceable.wrap(courseNode));

		myContent = createVelocityContainer("selftestrun");

		mainPanel = putInitialPanel(myContent);		

		if (!modConfig.get(IQEditController.CONFIG_KEY_TYPE).equals(AssessmentInstance.QMD_ENTRY_TYPE_SELF)) {
			throw new OLATRuntimeException("IQRunController launched with Selftest constructor but module configuration not configured as selftest" ,null);
		}
		init(ureq);
		exposeUserSelfTestDataToVC(ureq);
				
		StringBuilder qtiChangelog = createChangelogMsg(ureq);
		// decide about changelog in VC
		if(qtiChangelog.length()>0){
			//there is some message
			myContent.contextPut("changeLog", qtiChangelog);
		}
		//per default change log is not open
		myContent.contextPut("showChangelog", Boolean.FALSE);
	}

	/**
	 * Constructor for a survey run controller
	 * @param userCourseEnv
	 * @param moduleConfiguration
	 * @param secCallback
	 * @param ureq
	 * @param wControl
	 * @param surveyCourseNode
	 */
	IQRunController(UserCourseEnvironment userCourseEnv, ModuleConfiguration moduleConfiguration, IQSecurityCallback secCallback, UserRequest ureq, WindowControl wControl, IQSURVCourseNode surveyCourseNode) {
		super(ureq, wControl);
		
		this.modConfig = moduleConfiguration;
		this.secCallback = secCallback;
		this.userCourseEnv = userCourseEnv;
		this.courseNode = surveyCourseNode;
		this.type = AssessmentInstance.QMD_ENTRY_TYPE_SURVEY;
		iqManager = CoreSpringFactory.getImpl(IQManager.class);
		
		addLoggingResourceable(LoggingResourceable.wrap(courseNode));

		myContent = createVelocityContainer("surveyrun");
		
		mainPanel = putInitialPanel(myContent);		

		if (!modConfig.get(IQEditController.CONFIG_KEY_TYPE).equals(AssessmentInstance.QMD_ENTRY_TYPE_SURVEY)) {
			throw new OLATRuntimeException("IQRunController launched with Survey constructor but module configuration not configured as survey" ,null);
		}
		init(ureq);
		exposeUserQuestionnaireDataToVC();
		
		StringBuilder qtiChangelog = createChangelogMsg(ureq);
		// decide about changelog in VC
		if(qtiChangelog.length()>0){
			//there is some message
			myContent.contextPut("changeLog", qtiChangelog);
		}
		//per default change log is not open
		myContent.contextPut("showChangelog", Boolean.FALSE);
	}

	
	
	
	private void init(UserRequest ureq) {
		startButton = LinkFactory.createButton("start", myContent, this);
		// fetch disclaimer file
		String sDisclaimer = (String)modConfig.get(IQEditController.CONFIG_KEY_DISCLAIMER);
		if (sDisclaimer != null) {
			VFSContainer baseContainer = userCourseEnv.getCourseEnvironment().getCourseFolderContainer();
			int lastSlash = sDisclaimer.lastIndexOf('/');
			if (lastSlash != -1) {
				baseContainer = (VFSContainer)baseContainer.resolve(sDisclaimer.substring(0, lastSlash));
				sDisclaimer = sDisclaimer.substring(lastSlash);
				// first check if disclaimer exists on filesystem
				if (baseContainer == null || baseContainer.resolve(sDisclaimer) == null) {
					showWarning("disclaimer.file.invalid", sDisclaimer);
				} else {
					//screenreader do not like iframes, display inline
					if (getWindowControl().getWindowBackOffice().getWindowManager().isForScreenReader()) {
						HtmlStaticPageComponent disclaimerComp = new HtmlStaticPageComponent("disc", baseContainer);
						myContent.put("disc", disclaimerComp);
						disclaimerComp.setCurrentURI(sDisclaimer);
						myContent.contextPut("hasDisc", Boolean.TRUE);
					} else {
						iFrameCtr = new IFrameDisplayController(ureq, getWindowControl(), baseContainer);
						listenTo(iFrameCtr);//dispose automatically
						myContent.put("disc", iFrameCtr.getInitialComponent());
						iFrameCtr.setCurrentURI(sDisclaimer);
						myContent.contextPut("hasDisc", Boolean.TRUE);
					}

				}
			}
		}

		// push title and learning objectives, only visible on intro page
		myContent.contextPut("menuTitle", courseNode.getShortTitle());
		myContent.contextPut("displayTitle", courseNode.getLongTitle());
		
		// Adding learning objectives
		String learningObj = courseNode.getLearningObjectives();
		if (learningObj != null) {
			Component learningObjectives = ObjectivesHelper.createLearningObjectivesComponent(learningObj, ureq); 
			myContent.put("learningObjectives", learningObjectives);
			myContent.contextPut("hasObjectives", learningObj); // dummy value, just an exists operator					
		}
		
		if (type.equals(AssessmentInstance.QMD_ENTRY_TYPE_ASSESS)) {
			checkChats(ureq);
			singleUserEventCenter.registerFor(this, getIdentity(), InstantMessagingService.TOWER_EVENT_ORES);
		}
	}
	
	private void checkChats (UserRequest ureq) {
		List<?> allChats = null;
		if (ureq != null) {
			allChats = ureq.getUserSession().getChats();
		}
		if (allChats == null || allChats.size() == 0) {
			startButton.setEnabled (true);
			myContent.contextPut("hasChatWindowOpen", false);
		} else {
			startButton.setEnabled (false);
			myContent.contextPut("hasChatWindowOpen", true);
		}
	}
	
	@Override
	public void event(Event event) {
		if (type == AssessmentInstance.QMD_ENTRY_TYPE_ASSESS) {
			if (event.getCommand().startsWith("ChatWindow")) {
				checkChats(null);
			}
		}
	}
	
	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.components.Component, org.olat.core.gui.control.Event)
	 */
	public void event(UserRequest ureq, Component source, Event event) {
		if (source == startButton && startButton.isEnabled()){
			long callingResId = userCourseEnv.getCourseEnvironment().getCourseResourceableId().longValue();
			String callingResDetail = courseNode.getIdent();
			removeAsListenerAndDispose(displayController);

			//fxdiff BAKS-7 Resume function
			OLATResourceable ores = OresHelper.createOLATResourceableTypeWithoutCheck("test");
			ThreadLocalUserActivityLogger.addLoggingResourceInfo(LoggingResourceable.wrapBusinessPath(ores));
			WindowControl bwControl = addToHistory(ureq, ores, null);
			Controller returnController = iqManager.createIQDisplayController(modConfig, secCallback, ureq, bwControl, callingResId, callingResDetail, this);
			/*
			 * either returnController is a MessageController or it is a IQDisplayController
			 * this should not serve as pattern to be copy&pasted.
			 * FIXME:2008-11-21:pb INTRODUCED because of read/write QTI Lock solution for scalability II, 6.1.x Release 
			 */
			if(returnController instanceof IQDisplayController){
				displayController = (IQDisplayController)returnController;
				listenTo(displayController);
				if(displayController.isClosed()) {
					//do nothing
				} else  if (displayController.isReady()) {
					// in case displayController was unable to initialize, a message was set by displayController
					// this is the case if no more attempts or security check was unsuccessfull
					displayContainerController = new LayoutMain3ColsController(ureq, getWindowControl(), null, null, displayController.getInitialComponent(), null);
					listenTo(displayContainerController); // autodispose

					
					//need to wrap a course restart controller again, because IQDisplay
					//runs on top of GUIStack
					ICourse course = CourseFactory.loadCourse(callingResId);
					RepositoryEntry courseRepositoryEntry = RepositoryManager.getInstance().lookupRepositoryEntry(course, true);
					Panel empty = new Panel("empty");//empty panel set as "menu" and "tool"
					Controller courseCloser = CourseFactory.createDisposedCourseRestartController(ureq, getWindowControl(), courseRepositoryEntry);
					Controller disposedRestartController = new LayoutMain3ColsController(ureq, getWindowControl(), empty, empty, courseCloser.getInitialComponent(), "disposed course whily in iqRun" + callingResId);
					displayContainerController.setDisposedMessageController(disposedRestartController);
					
					final Boolean fullWindow = (Boolean)modConfig.getBooleanSafe(IQEditController.CONFIG_FULLWINDOW, true);
					if(fullWindow.booleanValue()) {
						displayContainerController.setAsFullscreen(ureq);
					}
					displayContainerController.activate();
					
					if (modConfig.get(IQEditController.CONFIG_KEY_TYPE).equals(AssessmentInstance.QMD_ENTRY_TYPE_ASSESS)) {
						assessmentStopped = false;		
						singleUserEventCenter.registerFor(this, getIdentity(), assessmentInstanceOres);
						singleUserEventCenter.fireEventToListenersOf(new AssessmentEvent(AssessmentEvent.TYPE.STARTED, ureq.getUserSession()), assessmentEventOres);						
					}
				}//endif isReady
			
				
			}else{
				// -> qti file was locked -> show info message
				// user must click again on course node to activate
				mainPanel.pushContent(returnController.getInitialComponent());
			}
		} else if(source == showResultsButton) {			
			AssessmentManager am = userCourseEnv.getCourseEnvironment().getAssessmentManager();
			Long assessmentID = am.getAssessmentID(courseNode, ureq.getIdentity());			
			if(assessmentID==null) {
        //fallback solution: if the assessmentID is not available via AssessmentManager than try to get it via IQManager
				long callingResId = userCourseEnv.getCourseEnvironment().getCourseResourceableId().longValue();
				String callingResDetail = courseNode.getIdent();
				assessmentID = iqManager.getLastAssessmentID(ureq.getIdentity(), callingResId, callingResDetail);
			}
			if(assessmentID!=null && !assessmentID.equals("")) {
				Document doc = iqManager.getResultsReportingFromFile(ureq.getIdentity(), type, assessmentID);
				//StringBuilder resultsHTML = LocalizedXSLTransformer.getInstance(ureq.getLocale()).renderResults(doc);
				String summaryConfig = (String)modConfig.get(IQEditController.CONFIG_KEY_SUMMARY);
				int summaryType = AssessmentInstance.getSummaryType(summaryConfig);
				String resultsHTML = iqManager.transformResultsReporting(doc, ureq.getLocale(), summaryType);
				myContent.contextPut("displayreporting", resultsHTML);
				myContent.contextPut("resreporting", resultsHTML);
				myContent.contextPut("showResults", Boolean.TRUE);
			} 
		} else if (source == hideResultsButton) {
			myContent.contextPut("showResults", Boolean.FALSE);
		}
	}

	@Override
	public void submitAssessment(AssessmentInstance ai) {
		if (type.equals(AssessmentInstance.QMD_ENTRY_TYPE_ASSESS)) {
			AssessmentContext ac = ai.getAssessmentContext();
			AssessmentManager am = userCourseEnv.getCourseEnvironment().getAssessmentManager();
			Float score = new Float(ac.getScore());
			Boolean passed = new Boolean(ac.isPassed());
			ScoreEvaluation sceval = new ScoreEvaluation(score, passed, am.getNodeFullyAssessed(courseNode,
					getIdentity()), new Long(ai.getAssessID()));
			AssessableCourseNode acn = (AssessableCourseNode)courseNode; // assessment nodes are assesable			
			boolean incrementUserAttempts = true;
			acn.updateUserScoreEvaluation(sceval, userCourseEnv, getIdentity(), incrementUserAttempts);
				
			// Mark publisher for notifications
			AssessmentNotificationsHandler anh = AssessmentNotificationsHandler.getInstance();
			Long courseId = userCourseEnv.getCourseEnvironment().getCourseResourceableId();
			anh.markPublisherNews(getIdentity(), courseId);
			if(!assessmentStopped) {
			  assessmentStopped = true;					  
			  AssessmentEvent assessmentStoppedEvent = new AssessmentEvent(AssessmentEvent.TYPE.STOPPED, userSession);
			  singleUserEventCenter.deregisterFor(this, assessmentInstanceOres);
				singleUserEventCenter.fireEventToListenersOf(assessmentStoppedEvent, assessmentEventOres);
			}
		} else if (type.equals(AssessmentInstance.QMD_ENTRY_TYPE_SURVEY)) {
			// save number of attempts
			// although this is not an assessable node we still use the assessment
			// manager since this one uses caching
			AssessmentManager am = userCourseEnv.getCourseEnvironment().getAssessmentManager();
			am.incrementNodeAttempts(courseNode, getIdentity(), userCourseEnv);
		} else if(type.equals(AssessmentInstance.QMD_ENTRY_TYPE_SELF)){
			AssessmentManager am = userCourseEnv.getCourseEnvironment().getAssessmentManager();
			am.incrementNodeAttempts(courseNode, getIdentity(), userCourseEnv);
		}
	}

	@Override
	public void cancelAssessment(AssessmentInstance ai) {
		//
	}

	/**
	 * @see org.olat.core.gui.control.DefaultController#event(org.olat.core.gui.UserRequest, org.olat.core.gui.control.Controller, org.olat.core.gui.control.Event)
	 */
	public void event(UserRequest urequest, Controller source, Event event) {
		if (source == displayController) {
			if (event instanceof IQSubmittedEvent) {
				// Save results in case of test
				if (type.equals(AssessmentInstance.QMD_ENTRY_TYPE_ASSESS)) {		
					exposeUserTestDataToVC(urequest);
				} 
				// Save results in case of questionnaire
				else if (type.equals(AssessmentInstance.QMD_ENTRY_TYPE_SURVEY)) {
					exposeUserQuestionnaireDataToVC();
					
					if(displayContainerController != null) {
						displayContainerController.deactivate(urequest);
					} else {
						getWindowControl().pop();
					}
					OLATResourceable ores = OresHelper.createOLATResourceableInstance("test", -1l);
					addToHistory(urequest, ores, null);
				}
				// Don't save results in case of self-test
				// but do safe attempts !
				else if(type.equals(AssessmentInstance.QMD_ENTRY_TYPE_SELF)){
					//am.incrementNodeAttempts(courseNode, urequest.getIdentity(), userCourseEnv);
				}
			} else if (event.equals(Event.DONE_EVENT)) {
				stopAssessment(urequest, event);
			} else if ("test_stopped".equals(event.getCommand())) {
				stopAssessment(urequest, event);
				showWarning("error.assessment.stopped");
			}
		}
	}
	
	private void stopAssessment(UserRequest ureq, Event event) {
		if(displayContainerController != null) {
			displayContainerController.deactivate(ureq);
		} else {
			getWindowControl().pop();
		}	
		removeHistory(ureq);
		OLATResourceable ores = OresHelper.createOLATResourceableInstance("test", -1l);
		addToHistory(ureq, ores, null);
		if (type.equals(AssessmentInstance.QMD_ENTRY_TYPE_ASSESS) && !assessmentStopped ) {
			assessmentStopped = true;					
			AssessmentEvent assessmentStoppedEvent = new AssessmentEvent(AssessmentEvent.TYPE.STOPPED, userSession);
			singleUserEventCenter.deregisterFor(this, assessmentInstanceOres);
			singleUserEventCenter.fireEventToListenersOf(assessmentStoppedEvent, assessmentEventOres);
		}
		fireEvent(ureq, event);
	}

	private void exposeUserTestDataToVC(UserRequest ureq) {
    // config : show score info
		Object enableScoreInfoObject = modConfig.get(IQEditController.CONFIG_KEY_ENABLESCOREINFO);
		if (enableScoreInfoObject != null) {
			myContent.contextPut("enableScoreInfo", enableScoreInfoObject );	
		} else {
			myContent.contextPut("enableScoreInfo", Boolean.TRUE );
		}
   
    // configuration data
    myContent.contextPut("attemptsConfig", modConfig.get(IQEditController.CONFIG_KEY_ATTEMPTS));
    // user data
    if ( !(courseNode instanceof AssessableCourseNode))
    	throw new AssertException("exposeUserTestDataToVC can only be called for test nodes, not for selftest or questionnaire");
		AssessableCourseNode acn = (AssessableCourseNode)courseNode; // assessment nodes are assesable
		ScoreEvaluation scoreEval = acn.getUserScoreEvaluation(userCourseEnv);
		
		//block if test passed (and config set to check it)
		Boolean blockAfterSuccess = (Boolean)modConfig.get(IQEditController.CONFIG_KEY_BLOCK_AFTER_SUCCESS);
    Boolean blocked = Boolean.FALSE;
    if(blockAfterSuccess != null && blockAfterSuccess.booleanValue()) {
    	Boolean passed = scoreEval.getPassed();
    	if(passed != null && passed.booleanValue()) {
    		blocked = Boolean.TRUE;
    	}
    }
    myContent.contextPut("blockAfterSuccess", blocked );
		
		Identity identity = userCourseEnv.getIdentityEnvironment().getIdentity();
		myContent.contextPut("score", AssessmentHelper.getRoundedScore(scoreEval.getScore()));
		myContent.contextPut("hasPassedValue", (scoreEval.getPassed() == null ? Boolean.FALSE : Boolean.TRUE));
		myContent.contextPut("passed", scoreEval.getPassed());
		StringBuilder comment = Formatter.stripTabsAndReturns(acn.getUserUserComment(userCourseEnv));
		myContent.contextPut("comment", StringHelper.xssScan(comment));
		myContent.contextPut("attempts", acn.getUserAttempts(userCourseEnv));
		
		UserNodeAuditManager am = userCourseEnv.getCourseEnvironment().getAuditManager();
		myContent.contextPut("log", am.getUserNodeLog(courseNode, identity));
						
		exposeResults(ureq);
	}
	
	/**
	 * Provides the self test score and results, if any, to the velocity container.
	 * @param ureq
	 */
	private void exposeUserSelfTestDataToVC(UserRequest ureq) {
    // config : show score info
		Object enableScoreInfoObject = modConfig.get(IQEditController.CONFIG_KEY_ENABLESCOREINFO);
		if (enableScoreInfoObject != null) {
			myContent.contextPut("enableScoreInfo", enableScoreInfoObject );	
		} else {
			myContent.contextPut("enableScoreInfo", Boolean.TRUE );
		}
      
    if ( !(courseNode instanceof SelfAssessableCourseNode))
    	throw new AssertException("exposeUserSelfTestDataToVC can only be called for selftest nodes, not for test or questionnaire");
    SelfAssessableCourseNode acn = (SelfAssessableCourseNode)courseNode; 
		ScoreEvaluation scoreEval = acn.getUserScoreEvaluation(userCourseEnv);
		if (scoreEval != null) {
			myContent.contextPut("hasResults", Boolean.TRUE);
			myContent.contextPut("score", AssessmentHelper.getRoundedScore(scoreEval.getScore()));
			myContent.contextPut("hasPassedValue", (scoreEval.getPassed() == null ? Boolean.FALSE : Boolean.TRUE));
			myContent.contextPut("passed", scoreEval.getPassed());
			myContent.contextPut("attempts", new Integer(1)); //at least one attempt
			
			exposeResults(ureq);
		}
	}
		
	/**
	 * Provides the show results button if results available or a message with the visibility period.
	 * @param ureq
	 */
	private void exposeResults(UserRequest ureq) {
    //migration: check if old tests have no summary configured
	  String configuredSummary = (String) modConfig.get(IQEditController.CONFIG_KEY_SUMMARY);
	  boolean noSummary = configuredSummary==null || (configuredSummary!=null && configuredSummary.equals(AssessmentInstance.QMD_ENTRY_SUMMARY_NONE));
		if(!noSummary) {
			Boolean showResultsObj = (Boolean)modConfig.get(IQEditController.CONFIG_KEY_RESULT_ON_HOME_PAGE);		
			boolean showResultsOnHomePage = (showResultsObj!=null && showResultsObj.booleanValue());
			myContent.contextPut("showResultsOnHomePage",new Boolean(showResultsOnHomePage));			
			boolean dateRelatedVisibility = AssessmentHelper.isResultVisible(modConfig);		
			if(showResultsOnHomePage && dateRelatedVisibility) {
				myContent.contextPut("showResultsVisible",Boolean.TRUE);
			  showResultsButton = LinkFactory.createButton("command.showResults", myContent, this);
			  hideResultsButton = LinkFactory.createButton("command.hideResults", myContent, this);
			} else if(showResultsOnHomePage) {
				Date startDate = (Date)modConfig.get(IQEditController.CONFIG_KEY_RESULTS_START_DATE);
			  Date endDate = (Date)modConfig.get(IQEditController.CONFIG_KEY_RESULTS_END_DATE);
			  String visibilityStartDate = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, ureq.getLocale()).format(startDate);
			  String visibilityEndDate = "-";
			  if(endDate!=null) {
			    visibilityEndDate = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT, ureq.getLocale()).format(endDate);
			  }
			  String visibilityPeriod = getTranslator().translate("showResults.visibility", new String[] { visibilityStartDate, visibilityEndDate});
				myContent.contextPut("visibilityPeriod",visibilityPeriod);
				myContent.contextPut("showResultsVisible",Boolean.FALSE);
			}
		}		
	}

	private void exposeUserQuestionnaireDataToVC() {
		AssessmentManager am = userCourseEnv.getCourseEnvironment().getAssessmentManager();
		Identity identity = userCourseEnv.getIdentityEnvironment().getIdentity();
		// although this is not an assessable node we still use the assessment
		// manager since this one uses caching
		myContent.contextPut("attempts", am.getNodeAttempts(courseNode, identity));
	}
	
	/**
	 * 
	 * @see org.olat.core.gui.control.DefaultController#doDisspose(boolean)
	 */
	protected void doDispose() {
		// child controllers disposed by basic controller
		if (!type.equals(AssessmentInstance.QMD_ENTRY_TYPE_ASSESS)) {
			return;
		}
		
		singleUserEventCenter.deregisterFor(this, assessmentInstanceOres);
		singleUserEventCenter.deregisterFor(this, InstantMessagingService.TOWER_EVENT_ORES);
		
		if (!assessmentStopped) {		 
				AssessmentEvent assessmentStoppedEvent = new AssessmentEvent(AssessmentEvent.TYPE.STOPPED, userSession);
				singleUserEventCenter.fireEventToListenersOf(assessmentStoppedEvent, assessmentEventOres);
		}
		
	}

	@Override
	//fxdiff BAKS-7 Resume function
	public void activate(UserRequest ureq, List<ContextEntry> entries, StateEntry state) {
		if(entries == null || entries.isEmpty()) return;
		
		ContextEntry ce = entries.remove(0);
		if("test".equals(ce.getOLATResourceable().getResourceableTypeName())) {
			Long resourceId = ce.getOLATResourceable().getResourceableId();
			if(resourceId != null && resourceId.longValue() >= 0) {
				event(ureq, startButton, Event.CHANGED_EVENT);
			}
		}
	}
}
