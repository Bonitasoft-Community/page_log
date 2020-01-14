package org.bonitasoft.page.log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import org.bonitasoft.page.log.LogAccess.LogParameter;

/**
 * the LogInformation manage some LogItem
 *
 */
public class LogItem {

	private static String blanckString = "                                                    ";
	private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:SS");
	// keep the date as a String to go fast
	public long lineNumber;
	public String dateSt;
	private Date dateTime;
	String logLevel=null;
	public int nbLines;

	/** normal way to collect the information */
	final StringBuffer content = new StringBuffer();

	/**
	 * brut line : this string is then used
	 * 
	 */
	public String line;

	/**
	 * some LogItem are in fact the same error. So, to get a correct Analysis,
	 * the different logItem are linked
	 * 
	 */
	final private List<LogItem> listLinkedLogItem = new ArrayList<LogItem>();
	/**
	 * in general, the nbLinkedLog == listLinkedLogItem.size() but if the list
	 * is too big, then the new item is not recorded.
	 */
	int nbLinkedLog;
	final static int maxNbLinkedLog = 100;

	// this is an internal mark to detect the first line : on the second line,
	// there are some interresing information
	public boolean firstLine;
	public String localisation;
	public LogParameter logParameter;

	public LogItem(final LogParameter logParameter) {
		this.logParameter = logParameter;
	}

	public int maxLengthContent = 10000;

	/**
	 * a logItem is collected on multiple line. Then, the newLine explain how to
	 * separate the line during acquisition
	 */
	public static String newLine = "<br>";

	/*
	 * *************************************************************************
	 * *******
	 */
	/*                                                                                  */
	/* Add */
	/*                                                                                  */
	/*                                                                                  */
	/*
	 * *************************************************************************
	 * *******
	 */
	public void setDate(final String date) {
		try {
			dateTime = sdf.parse(date);
		} catch (Exception e) {
		}
		;
		if (logParameter.filterShortDate) {
			final StringTokenizer st = new StringTokenizer(date, " ");
			if (st.hasMoreTokens()) {
				st.nextToken();
			}
			dateSt = st.hasMoreTokens() ? st.nextToken() : date;
		} else {
			dateSt = date;
		}

	}

	public void setLevel(String level) {
		if (logLevel==null)
		{
	    level = level == null ? "" : level.trim();
	    if (level.endsWith(":")) {
	      level = level.substring(0, level.length() - 1);

	      // maybe a new level ?
		  if (LogInformation.listWarnings.contains(level) 
		      || LogInformation.listErrors.contains(level)
		      || LogInformation.listInfos.contains(level)
        || LogInformation.listDebugs.contains(level))
		    logLevel = level;
		  
		}
		}
		if (logLevel==null  || logLevel.trim().length()==0)
		{
			logLevel="INFO";
		}
	}

	public void addContent(final String contentToAdd) {
		if (content.length() > maxLengthContent) {
			return; // already too big
		}
		if (content.length() + contentToAdd.length() > maxLengthContent) {
			content.append(contentToAdd.substring(0, maxLengthContent - content.length()) + "...");
		} else {
			content.append(contentToAdd);
		}
	}

	public void addBrut(final String line) {
		this.line = line;
	}

	/*
	 * ***********************************************************************
	 */
	/*                                                                                  */
	/* DeeperAnalysis */
	/*                                                                                  */
	/*                                                                                  */
	/*
	 * /*
	 * ***********************************************************************
	 */
	/**
	 * one error may be on multiple LogItem. In order to perform a correct
	 * Analysis, the different logItem are chained in the first one
	 * 
	 * @param appendStr
	 */
	public void link(LogItem linkedLogItem) {

		// protect the server : if there are too much log linked, we do not
		// record it
		nbLinkedLog++;
		if (this.listLinkedLogItem.size() < maxNbLinkedLog)
			this.listLinkedLogItem.add(linkedLogItem);
	}

	public long getTime() {
		return dateTime == null ? 0 : dateTime.getTime();
	}

	/**
	 * as the header, return the 30 first characters
	 * 
	 */
	public String getHeader() {
		String header = content.toString().trim();
		// if the header is something like (default task-7)
		// org.bonitasoft.engine.bpm.flownode.UserTaskNotFoundException: USER,
		// then remove the (default task-7) which is the worker ID
		if (header.startsWith("(")) {
			int pos = header.indexOf(")");
			if (pos != -1)
				header = header.substring(pos + 1);
		}
		if (header.length() > 300)
			return header.substring(0, 300);
		return header;
	}

	public String getFirstWord(int numberOfWord) {
		String result = "";
		int count = 0;
		StringTokenizer st = new StringTokenizer(content.toString(), " ");
		while (st.hasMoreTokens()) {
			count++;
			result += st.nextToken() + " ";
			if (count == numberOfWord)
				return result;
		}
		return result;
	}

	protected Long processDefinitionId;
	protected String processDefinitionName;
	protected String processDefinitionVersion;

	protected Long flowNodeDefinitionId;
	protected String flowNodeDefinitionName;

	protected Long processInstanceId;
	protected Long rootProcessInstanceId;
	protected String connectorImplementationClassName;
	protected String causedBy;

	/*
	 * 2017-11-08 08:28:22,905 ERROR
	 * [org.bonitasoft.engine.execution.work.FailureHandlingBonitaWork]
	 * (Bonita-Worker-1-03) THREAD_ID=807 | HOSTNAME=op-boni-as3.ms.fcm |
	 * TENANT_ID=1 |
	 * org.bonitasoft.engine.core.connector.exception.SConnectorException :
	 * "PROCESS_DEFINITION_ID=5274809883096251272 | PROCESS_NAME=proc_livraison_physique | PROCESS_VERSION=3.3 "
	 * + "| PROCESS_INSTANCE_ID=130333 |" +
	 * " ROOT_PROCESS_INSTANCE_ID=130332 | FLOW_NODE_DEFINITION_ID=5304019830613407908 |"
	 * +
	 * " FLOW_NODE_INSTANCE_ID=1342443 | FLOW_NODE_NAME=act send welcome mail |"
	 * +
	 * " CONNECTOR_IMPLEMENTATION_CLASS_NAME=con_processStepSyncWithParameter_envoyer_mail1 |"
	 * + " CONNECTOR_INSTANCE_ID=1081410 | " +
	 * "org.bonitasoft.engine.connector.exception.SConnectorException: java.util.concurrent.ExecutionException: org.bonitasoft.engine.connector.exception.SConnectorException: org.bonitasoft.engine.connector.ConnectorException: Exception trying to call remote webservice"
	 * : org.bonitasoft.engine.core.connector.exception.SConnectorException:
	 * PROCESS_DEFINITION_ID=5274809883096251272 |
	 * PROCESS_NAME=proc_livraison_physique | PROCESS_VERSION=3.3 |
	 * PROCESS_INSTANCE_ID=130333 | ROOT_PROCESS_INSTANCE_ID=130332 |
	 * FLOW_NODE_DEFINITION_ID=5304019830613407908 |
	 * FLOW_NODE_INSTANCE_ID=1342443 | FLOW_NODE_NAME=act send welcome mail |
	 * CONNECTOR_IMPLEMENTATION_CLASS_NAME=
	 * con_processStepSyncWithParameter_envoyer_mail1 |
	 * CONNECTOR_INSTANCE_ID=1081410 |
	 * org.bonitasoft.engine.connector.exception.SConnectorException:
	 * java.util.concurrent.ExecutionException:
	 * org.bonitasoft.engine.connector.exception.SConnectorException:
	 * org.bonitasoft.engine.connector.ConnectorException: Exception trying to
	 * call remote webservice
	 */
	/**
	 * in order to save time, the deep analysis is done only on demande
	 */
	public void deepAnalysis() {
		StringBuffer completeContent = new StringBuffer(content);
		for (LogItem logItem : listLinkedLogItem)
			completeContent.append(logItem.content);
		String completeContentSt = completeContent.toString();
		processDefinitionId = searchLong("PROCESS_DEFINITION_ID", completeContentSt);
		flowNodeDefinitionId = searchLong("PROCESS_DEFINITION_ID", completeContentSt);
		processInstanceId = searchLong("PROCESS_INSTANCE_ID", completeContentSt);
		if (processInstanceId==null)
			processInstanceId = searchLong("processInstanceId", completeContentSt);
		rootProcessInstanceId = searchLong("ROOT_PROCESS_INSTANCE_ID", completeContentSt);
		connectorImplementationClassName = searchString("CONNECTOR_IMPLEMENTATION_CLASS_NAME", completeContentSt);
		processDefinitionName = searchString("PROCESS_NAME", completeContentSt);
		processDefinitionVersion = searchString("PROCESS_VERSION", completeContentSt);
		flowNodeDefinitionName = searchString("FLOW_NODE_NAME", completeContentSt);

		// search the LAST occurrence
		// Caused by: org.apache.cxf.binding.soap.SoapFault:
		// java.lang.RuntimeException: UnImplemented System:NONE
		int pos = completeContent.lastIndexOf("Caused by:");
		if (pos != -1) {
			// end of the line ?
			pos = pos + "Caused by:".length();
			int posEndLine = completeContent.indexOf(newLine, pos);
			if (posEndLine == -1)
				causedBy = completeContent.substring(pos);
			else
				causedBy = completeContent.substring(pos, posEndLine);
		}
		else
		{
			// format is 
		
			// 2018-02-01 10:55:05.291 -0500 SEVERE: org.bonitasoft.engine.execution.work.FailureHandlingBonitaWork THREAD_ID=446503 | HOSTNAME=fldlpdelba003 | TENANT_ID=1 | Unable to handle the failure.
			// 2018-02-01 09:13:44.715 -0500 SEVERE: org.bonitasoft.engine.execution.work.FailureHandlingBonitaWork THREAD_ID=445949 | HOSTNAME=fldlpdelba003 | TENANT_ID=1 | org.bonitasoft.engine.expression.exception.SExpressionEvaluationException : "PROCESS_DEFINITION_ID=6207137492058794890 | PROCESS_NAME=FDR_Ping_Pool | PROCESS_VERSION=1.0.0.7 | PROCESS_INSTANCE_ID=129915 | ROOT_PROCESS_INSTANCE_ID=129912 | FLOW_NODE_DEFINITION_ID=8021353421732438797 | FLOW_NODE_INSTANCE_ID=1032623 | FLOW_NODE_NAME=Ping BHR | CONNECTOR_IMPLEMENTATION_CLASS_NAME=RestConnector | CONNECTOR_INSTANCE_ID=562562 | org.bonitasoft.engine.data.instance.exception.SDataInstanceNotFoundException: org.bonitasoft.engine.commons.exceptions.SObjectNotFoundException: org.bonitasoft.engine.core.process.instance.api.exceptions.SFlowNodeNotFoundException: Flow node instance with id 1032616 not found"
			// 2018-02-01 23:59:20.960 -0500 SEVERE: org.bonitasoft.engine.scheduler.impl.BonitaJobStoreCMT Couldn't rollback jdbc connection. Closed Connection

			// in this situation, take the last causeBy
			StringTokenizer st = new StringTokenizer(completeContent.toString(), "|");
			while (st.hasMoreTokens())
				causedBy = st.nextToken();
			
			
		}
			
	}

	public boolean isError() {
		return LogInformation.listWarnings.contains(logLevel) || LogInformation.listErrors.contains(logLevel);
	}

	/* ********************************************************************* */
	/*                                                                                  */
	/* getter */
	/*                                                                                  */
	/*                                                                                  */
	/* ********************************************************************* */

	public Map<String, Object> toJson() {
		// SimpleDateFormat sdf = new SimpleDateFormat(sdfFormat);

		final Map<String, Object> map = new HashMap<String, Object>();
		if (line == null) {
			map.put("lin", lineNumber);
			map.put("dte", dateSt);
			map.put("lvl", logLevel);
			map.put("cnt", content.toString());
			map.put("loc", localisation);
			if (LogInformation.listWarnings.contains(logLevel)) {
				map.put("stl", "background:#fcf8e3");
			}
			if (LogInformation.listErrors.contains(logLevel)) {
				map.put("stl", "background:#f2dede");
			}

		} else {
			map.put("l", line);
		}
		return map;

	}

	@Override
	public String toString() {
		return "#" + lineNumber + " " + dateSt + "/" + (logLevel + blanckString).substring(0, 8) + "/" + (localisation + blanckString).substring(0, 20) + "/(" + content.length() + ") " + (content + blanckString).substring(0, 40);
	}

	/*
	 * ***********************************************************************
	 */
	/*                                                                                  */
	/* Private */
	/*                                                                                  */
	/*                                                                                  */
	/*
	 * ***********************************************************************
	 */

	// format is : "PROCESS_DEFINITION_ID=5274809883096251272 |
	// PROCESS_NAME=proc_livraison_physique
	private Long searchLong(String prefix, String completeContentSt) {
		String str = searchString(prefix, completeContentSt);
		try {
			return Long.valueOf(str);
		} catch (Exception e) {
		}
		;
		return null;
	}

	private String searchString(String prefix, String completeContentSt) {
		int pos = completeContentSt.indexOf(prefix + "=");
		if (pos == -1)
			return "";
		pos += prefix.length() + 1;
		int posNextBlanck = completeContentSt.indexOf(" ", pos);
		if (posNextBlanck == 1)
			return completeContentSt.substring(pos);
		return completeContentSt.substring(pos, posNextBlanck);
	}
}