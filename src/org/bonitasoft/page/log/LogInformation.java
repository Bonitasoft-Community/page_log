package org.bonitasoft.page.log;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.page.log.LogAccess.LogParameter;

/**
 * 
 */
public class LogInformation {
	public static Set<String> listWarnings = new HashSet<String>(Arrays.asList("WARNING", "WARN", "GRAVE", "AVERTISSEMENT"));
	public static Set<String> listErrors = new HashSet<String>(Arrays.asList("SEVERE", "ERROR"));

	/**
	 * keep this information to use the filter
	 */
	public LogParameter logParameter;
	/**
	 * create the analyseError
	 */
	public LogAnalyseError logAnalyseError;
	public String logFileName;
	public String completeLogFileName;

	public List<Object> listLogs = new ArrayList<Object>();
	/** total line we detect in the addLogItem */

	/** we like to collect only aftr the pageNumber - start at 1 */
	// bob public long pageNumber;

	/** please collect only nbLines */
	// bob public long numberPerPage;

	/**
	 * we collect this number of line
	 */
	public long currentLine=0;
	public long nbTotalLines = -1;

	// if the produceJson is true, produce directly in Json
	public List<BEvent> listEvents = new ArrayList<BEvent>();
	public boolean addAsJson = true;

	public long startTime;
	 
	public LogInformation(LogParameter logParameter) {
		this.logParameter = logParameter;
		this.logAnalyseError = new LogAnalyseError(logParameter);

		this.startTime = System.currentTimeMillis();
	}

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

	public void addLogItem(final LogItem logItem) {
		/*
		 * if (listLogs.size() > 0) { logger.info(listLogs.size() +
		 * " nb, First line is [" + listLogs.get(0).toString() +
		 * "] **** Last is*****[" + listLogs.get(listLogs.size() - 1).toString()
		 * + "] *****Current***** " + logItem.toString()); }
		 */
		logAnalyseError.analyse(logItem);
		if (!keepTheLine(logItem)) {
			// logger.info("Not keep the line " + logItem.lineNumber);
			return;
		}
		currentLine++;
		
		if (logParameter.filterTail) {
			// we keep in the list the last numberPerPage
			if (listLogs.size() >= logParameter.numberPerPage) {
				// logger.info("Remove " + listLogs.get(0).toString());
				listLogs.remove(0);
				// logger.info("After Remove FirstLine=" +
				// listLogs.get(0).toString() + "/ Last is " +
				// listLogs.get(listLogs.size() - 1).toString());

			}
		} else {
			if (currentLine <= (logParameter.pageNumber - 1) * logParameter.numberPerPage || currentLine > logParameter.pageNumber * logParameter.numberPerPage) {
				return;
			}
		}

		if (addAsJson) {
			listLogs.add(logItem.toJson());
		} else {
			listLogs.add(logItem);
		}
	}

	/**
	 * 
	 * @param nbTotalLines
	 */
	public void end( long nbTotalLines) {
		this.nbTotalLines= nbTotalLines;
		logAnalyseError.end(nbTotalLines );
		logAnalyseError.setTimeInMs( System.currentTimeMillis() - startTime);
	}
	/*
	 * *************************************************************************
	 * *******
	 */
	/*                                                                                  */
	/* Getter */
	/*                                                                                  */
	/*                                                                                  */
	/*
	 * *************************************************************************
	 * *******
	 */

	public List<Object> getListLogsJson() {
		return listLogs;
	}

	public Map<String, Object> getAnalysisSyntheseJson() {
		return logAnalyseError.getSynthese().toJson();
	}

	public Map<String, Object> getAnalysisErrorTimeLineJson() {
		return logAnalyseError.getTimeLine().toJson();
	}

	/*
	 * *************************************************************************
	 * *******
	 */
	/*                                                                                  */
	/* Private */
	/*                                                                                  */
	/*                                                                                  */
	/*
	 * *************************************************************************
	 * *******
	 */

	/** check if we keep the line. We play a AND if multiple filter are done. */
	private boolean keepTheLine(final LogItem logItem) {
		boolean doKeepTheLine = true;
		if (logParameter == null) {
			return doKeepTheLine;
		}
		if (doKeepTheLine && logParameter.filterError) {
			doKeepTheLine = logItem.isError();
		}
		if (doKeepTheLine && logParameter.filterText != null) {
			doKeepTheLine = logItem.content.indexOf(logParameter.filterText) != -1 || logItem.localisation.indexOf(logParameter.filterText) != -1;

		}
		return doKeepTheLine;

	}

}