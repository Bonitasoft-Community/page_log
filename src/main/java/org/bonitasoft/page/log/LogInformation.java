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

    public final static Set<String> listWarnings = new HashSet<String>(Arrays.asList("WARNING", "WARN", "GRAVE", "AVERTISSEMENT"));
    public final static Set<String> listErrors = new HashSet<String>(Arrays.asList("SEVERE", "ERROR", "FATAL"));
    public final static Set<String> listInfos = new HashSet<String>(Arrays.asList("INFO", "INFOS", "CONFIG"));
    public final static Set<String> listDebugs = new HashSet<String>(Arrays.asList("DEBUG", "TRACE", "TRACE_INT", "X_TRACE_INT", "FINE", "PRÉCIS"));

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

    public List<Object> listLogs = new ArrayList<>();
    /** total line we detect in the addLogItem */

    /** we like to collect only aftr the pageNumber - start at 1 */
    // bob public long pageNumber;

    /** please collect only nbLines */
    // bob public long numberPerPage;

    /**
     * we collect this number of line
     */
    public long currentLine = 0;
    /**
     * Keep the line number filtered : example, we want to keep only SEVERE line, or GREP line
     */
    public long lineNumberFiltered=0;
    public long nbTotalLines = -1;

    // if the produceJson is true, produce directly in Json
    public List<BEvent> listEvents = new ArrayList<>();
    public boolean addAsJson = true;

    public long startTime;

    public LogInformation(LogParameter logParameter) {
        this.logParameter = logParameter;
        this.logAnalyseError = new LogAnalyseError(logParameter);

        this.startTime = System.currentTimeMillis();
    }

    /**
     * the previous LogItem is explode at the analysis time. We may have for one error, multiple log : so, we have to detect this group, to keep only one error
     * in fact.
     */
    private LogItem previousLogItem = null;

    public void start() {
        logAnalyseError.startAnalyse();
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
    private boolean allowSave = false;

    public void allowSave(boolean allowSave) {
        this.allowSave = allowSave;
    }

    public void addLogItem(final LogItem logItem) {
        // do the log analysis everytime

        logAnalyseError.analyse(logItem);
      
        // first, determine if we want to keep this line

        if (!keepTheLine(logItem)) {
            // logger.info("Not keep the line " + logItem.lineNumber);
            return;
        }
        // then, let upgrade the lineNumber, this list is part of the filter area
        lineNumberFiltered++;
        
        // then, not allow to save, we don't keep it (due to pagination)
        if (!allowSave)
            return;
      
       
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
            if (lineNumberFiltered <= (logParameter.pageNumber - 1) * logParameter.numberPerPage || lineNumberFiltered > logParameter.pageNumber * logParameter.numberPerPage) {
                return;
            }
        }

        if (addAsJson) {
            listLogs.add(logItem.toJson());
        } else {
            listLogs.add(logItem);
        }
        previousLogItem = logItem;
    }

    /**
     * @param nbTotalLines
     */
    public void end(long nbTotalLines) {
        this.nbTotalLines = nbTotalLines;
        logAnalyseError.endAnalysis(nbTotalLines);
        logAnalyseError.setTimeInMs(System.currentTimeMillis() - startTime);
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
            doKeepTheLine = logItem.getContent().indexOf(logParameter.filterText) != -1 || logItem.getLocalisation().indexOf(logParameter.filterText) != -1;

        }
        return doKeepTheLine;

    }

}
