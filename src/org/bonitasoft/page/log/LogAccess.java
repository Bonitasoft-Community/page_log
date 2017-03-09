package org.bonitasoft.page.log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.logging.Logger;

import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.json.simple.JSONValue;

// System.getProperty("catalina.base") + "/logs"

public class LogAccess {

    private static String sdfFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    private static Logger logger = Logger.getLogger(LogAccess.class.getName());

    private static BEvent EventInvalidLogFormat = new BEvent(LogAccess.class.getName(), 1, Level.APPLICATIONERROR,
            "Invalid Log Format", "The first line of the log should start by the date and a space, like '2017-02-27 '");
    private static BEvent EventErrorDuringDecodage = new BEvent(LogAccess.class.getName(), 2, Level.APPLICATIONERROR,
            "Error during decodate", "An error arrived during the decodage", "The decodage will be wrong on one line", "Check the Log file");
    private static BEvent EventReadLogFile = new BEvent(LogAccess.class.getName(), 2, Level.APPLICATIONERROR,
            "Error during the log file access", "An error arrived during the access to a log file", "No result", "Check the Execption");

    public static Set<String> listWarnings = new HashSet<String>(Arrays.asList("WARNING", "WARN", "GRAVE", "AVERTISSEMENT"));
    public static Set<String> listErrors = new HashSet<String>(Arrays.asList("SEVERE", "ERROR"));
    /**
     * get the list of files
     *
     * @return
     */
    public static Map<String, Object> getFilesLog()
    {
        logger.info("LogAccess: getFilesLog 1.1");

        final String logPath = getLogPath();
        if (logPath == null) {
            return new HashMap<String, Object>();
        }
        try
        {
            final File folder = new File(logPath);
            logger.info("LogAccess: listFiles=" + folder.getCanonicalPath());
            final File[] listOfFiles = folder.listFiles();
            if (listOfFiles == null) {
                logger.info("LogAccess: no file under =" + folder.getCanonicalPath());

                return new HashMap<String, Object>();
            }
            final Map<String, Object> mapLogs = new HashMap<String, Object>();

            for (int i = 0; i < listOfFiles.length; i++) {
                if (listOfFiles[i].isFile()) {
                    final String name = listOfFiles[i].getName();

                    final StringTokenizer st = new StringTokenizer(name, ".");
                    final String filename = st.hasMoreTokens() ? st.nextToken() : "";
                    final String filedate = st.hasMoreTokens() ? st.nextToken() : "";
                    List<String> listFiles = (List<String>) mapLogs.get(filedate);
                    if (listFiles == null) {
                        listFiles = new ArrayList<String>();
                    }
                    listFiles.add(name);
                    mapLogs.put(filedate, listFiles);
                    // logger.info("Add in [" + filedate + "] :" + listFiles);
                }
            }
            // now, set the map to a list
            // logger.info("Before sort " + mapLogs);
            final Map<String, Object> sortMapLogs = new TreeMap<String, Object>(mapLogs);
            // logger.info("After sort " + sortMapLogs);
            return sortMapLogs;

        } catch (final Exception e)
        {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            final String exceptionDetails = sw.toString();
            logger.severe("LogAccess :" + exceptionDetails);
            return new HashMap<String, Object>();

        }

    }

    public static class LogItem
    {

        private static String blanckString = "                                                    ";

        // keep the date as a String to go fast
        public long lineNumber;
        public String dateSt;
        private String logLevel;

        private final StringBuffer content = new StringBuffer();
        // this is an internal mark to detect the first line : on the second line, there are some interresing information
        public boolean firstLine;
        public String localisation;
        public String line;
        public LogParameter logParameter;
        public LogItem( final LogParameter logParameter)
        {
            this.logParameter = logParameter;
        }

        public int maxLengthContent = 400;

        public void setDate(final String date)
        {
            if (logParameter.filterShortDate)
            {
                final StringTokenizer st = new StringTokenizer(date, " ");
                if (st.hasMoreTokens()) {
                    st.nextToken();
                }
                dateSt=st.hasMoreTokens() ? st.nextToken() : date;
            } else {
                dateSt = date;
            }

        }
        public void setLevel(final String level)
        {
            logLevel = level == null ? "" : level.trim();
            if (level.endsWith(":")) {
                logLevel = logLevel.substring(0, logLevel.length() - 1);
            }
        }
        public void addContent(final String contentToAdd)
        {
            if (content.length() > maxLengthContent)
            {
                return; // already too big
            }
            if (content.length() + contentToAdd.length() > maxLengthContent)
            {
                content.append(contentToAdd.substring(0, maxLengthContent - content.length()) + "...");
            } else {
                content.append(contentToAdd);
            }
        }
        public Map<String, Object> toJson()
        {
            // SimpleDateFormat sdf = new SimpleDateFormat(sdfFormat);

            final Map<String, Object> map = new HashMap<String, Object>();
            if (line == null)
            {
                map.put("lin", lineNumber);
                map.put("dte", dateSt);
                map.put("lvl", logLevel);
                map.put("cnt", content.toString());
                map.put("loc", localisation);
                if (listWarnings.contains(logLevel)) {
                    map.put("stl", "background:#fcf8e3");
                }
                if (listErrors.contains(logLevel)) {
                    map.put("stl", "background:#f2dede");
                }

            } else {
                map.put("l", line);
            }
            return map;

        }

        public void addBrut(final String line)
        {
            this.line = line;
        }

        @Override
        public String toString()
        {
            return "#" + lineNumber + " " + dateSt + "/" + (logLevel + blanckString).substring(0, 8) + "/"
                    + (localisation + blanckString).substring(0, 20) + "/(" + content.length() + ") "
                    + (content + blanckString).substring(0, 40);
        }

    }

    /**
     * @author Firstname Lastname
     */
    public static class LogInformation
    {

        public String logFileName;
        public String completeLogFileName;

        public List<Object> listLogs = new ArrayList<Object>();
        /** total line we detect in the addLogItem */

        /** we like to collect only aftr the pageNumber - start at 1 */
        public long pageNumber;

        /** please collect only nbLines */
        public long numberPerPage;

        /**
         * keep this information to use the filter
         */
        public LogParameter logParameter;
        /**
         * we collect this number of line
         */
        public long totalLines = -1;

        // if the produceJson is true, produce directly in Json
        public List<BEvent> listEvents = new ArrayList<BEvent>();
        public boolean addAsJson = true;

        public void addLogItem(final LogItem logItem)
        {
            /*
             * if (listLogs.size() > 0) {
             * logger.info(listLogs.size() + " nb, First line is [" + listLogs.get(0).toString() + "] **** Last is*****["
             * + listLogs.get(listLogs.size() - 1).toString() + "] *****Current***** " + logItem.toString());
             * }
             */
            if (!keepTheLine(logItem)) {
                // logger.info("Not keep the line " + logItem.lineNumber);
                return;
            }

            totalLines++;
            if (logParameter.filterTail)
            {
                // we keep in the list the last numberPerPage
                if (listLogs.size() >= numberPerPage) {
                    //logger.info("Remove " + listLogs.get(0).toString());
                    listLogs.remove(0);
                    //logger.info("After Remove FirstLine=" + listLogs.get(0).toString() + "/ Last is " + listLogs.get(listLogs.size() - 1).toString());

                }
            }
            else
            {
                if (totalLines <= (pageNumber - 1) * numberPerPage || totalLines > pageNumber * numberPerPage) {
                    return;
                }
            }

            if (addAsJson) {
                listLogs.add(logItem.toJson());
            } else {
                listLogs.add(logItem);
            }
        }

        /** check if we keep the line. We play a AND if multiple filter are done. */
        private boolean keepTheLine(final LogItem logItem)
        {
            boolean doKeepTheLine = true;
            if (logParameter == null) {
                return doKeepTheLine;
            }
            if (doKeepTheLine && logParameter.filterError) {
                doKeepTheLine = listWarnings.contains(logItem.logLevel) || listErrors.contains(logItem.logLevel);
            }
            if (doKeepTheLine && logParameter.filterText != null)
            {
                doKeepTheLine = logItem.content.indexOf(logParameter.filterText) != -1
                        || logItem.localisation.indexOf(logParameter.filterText) != -1;

            }
            return doKeepTheLine;

        }

    }

    /**
     * logparametres
     */
    public static class LogParameter
    {

        public boolean logInformation = false;
        public String fileName;
        public String completeFileName;
        public boolean produceJson = false;
        public boolean brutResult = false;

        public long pageNumber = 0;
        public long numberPerPage = 500;
        public boolean filterError;
        public String filterText;
        public boolean filterShortDate;
        public boolean filterTail;

        public String getFileName()
        {
            if (fileName != null) {
                return fileName;
            }

            // get the current file
            final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

            return "bonita." + sdf.format(new Date()) + ".log";
        }

        public String getCompleteFileName()
        {
            if (completeFileName != null) {
                return completeFileName;
            } else {
                return getLogPath() + "/" + getFileName();
            }
        }

        /**
         * @param jsonSt
         * @return
         */
        public static LogParameter getInstanceFromJsonSt(final String jsonSt) {
            if (jsonSt == null) {
                return new LogParameter();
            }
            final HashMap<String, Object> jsonHash = (HashMap<String, Object>) JSONValue.parse(jsonSt);
            if (jsonHash == null) {
                return new LogParameter();
            }
            final LogParameter logParameter = new LogParameter();
            logParameter.logInformation = Toolbox.getBoolean(jsonHash.get("logInformation"), false);
            logParameter.pageNumber = Toolbox.getLong(jsonHash.get("pagenumber"), null);
            logParameter.numberPerPage = Toolbox.getLong(jsonHash.get("numberperpage"), null);
            logParameter.brutResult = Toolbox.getBoolean(jsonHash.get("brutResult"), false);
            logParameter.fileName = (String) jsonHash.get("fileName");
            logParameter.filterError = Toolbox.getBoolean(jsonHash.get("filterError"), false);
            logParameter.filterText = (String) jsonHash.get("filterText");
            if (logParameter.filterText != null && logParameter.filterText.trim().length() == 0) {
                logParameter.filterText = null;
            }
            logParameter.filterShortDate= Toolbox.getBoolean(jsonHash.get("filterShortDate"), false);
            logParameter.filterTail = Toolbox.getBoolean(jsonHash.get("filterTail"), false);

            return logParameter;
        }
    }

    // TWOLINE : 2017-02-27 09:03:47.992 -0800 org.bonitasoft.tomcat.H2Listener org.bonitasoft.tomcat.H2Listener lifecycleEvent
    // ONELINE : 2017-02-24 00:21:16.292 -0500 SEVERE: org.bonitasoft.engine.execution.work.FailureHandlingBonitaWork THREAD_ID=7472 | HOSTNAME=ip-10-74-58-84.ebiz.verizon.com | TENANT_ID=1 | The work [ExecuteConnectorOfActivity: flowNodeInstanceId = 11040113, connectorDefinitionName = Post NF job event] failed. The failure will be handled.

    public enum FormatLog {
        ONELINE, TWOLINES
    };

    /**
     * get the log file
     *
     * @param fileName
     * @return
     */

    public static LogInformation getLog(final LogParameter logParameter)
    {
        final LogInformation logInformation = new LogInformation();
        logInformation.logParameter = logParameter;
        logInformation.addAsJson = logParameter.produceJson;
        logInformation.pageNumber = logParameter.pageNumber;
        logInformation.numberPerPage = logParameter.numberPerPage;

        BufferedReader br = null;
        long lineNumber = 0;
        try {
            if (logParameter.logInformation) {
                logger.info("LogAccess.getLog : Start reading [" + logParameter.fileName + "=>" + logParameter.getCompleteFileName() + "] from Page["
                    + logParameter.pageNumber + "("
                    + logParameter.numberPerPage + ") Lines[" + logParameter.pageNumber * logParameter.numberPerPage + "-"
                    + (logParameter.pageNumber * logParameter.numberPerPage + logParameter.numberPerPage) + "]"
                    + " filterError=" + logParameter.filterError + " fiterText[" + logParameter.filterText + "]"
                    );
            }

            logInformation.logFileName = logParameter.getFileName();
            logInformation.completeLogFileName = logParameter.getCompleteFileName();

            br = new BufferedReader(new FileReader(logParameter.getCompleteFileName()));

            String detectDate = null;
            String line = br.readLine();
            lineNumber++;

            if (line != null)
            {
                final int posBlank = line.indexOf(" ");
                if (posBlank == -1)
                {
                    logInformation.listEvents.add(EventInvalidLogFormat);
                } else {
                    detectDate = line.substring(0, posBlank);
                }

            }

            BEvent errorDuringDecodage = null;
            // all lines
            // Attention : add the currentlogitem at the last moment
            LogItem currentLogItem = null;
            FormatLog formatLog = null;
            while (line != null)
            {
                // manage
                if (true) // lineNumber >= logParameter.firstLine && lineNumber < logParameter.firstLine + logParameter.nbLines)
                {

                    if (logParameter.brutResult)
                    {
                        currentLogItem = new LogItem(logParameter);
                        currentLogItem.lineNumber = lineNumber;
                        currentLogItem.addBrut(line);
                        logInformation.addLogItem(currentLogItem);
                    }
                    else
                    {
                        try
                        {
                            // manage the line
                            if (detectDate == null)
                            {
                                if (currentLogItem != null) {
                                    logInformation.addLogItem(currentLogItem);
                                }

                                currentLogItem = new LogItem(logParameter);
                                currentLogItem.lineNumber = lineNumber;
                                currentLogItem.addContent(line);

                            }
                            else
                            { // we can get the structure
                                if (line.startsWith(detectDate))
                                {
                                    // Two kind of format :
                                    // 2017-02-27 09:03:47.992 -0800 org.bonitasoft.tomcat.H2Listener org.bonitasoft.tomcat.H2Listener lifecycleEvent
                                    // 2017-02-24 00:21:16.292 -0500 SEVERE: org.bonitasoft.engine.execution.work.FailureHandlingBonitaWork THREAD_ID=7472 | HOSTNAME=ip-10-74-58-84.ebiz.verizon.com | TENANT_ID=1 | The work [ExecuteConnectorOfActivity: flowNodeInstanceId = 11040113, connectorDefinitionName = Post NF job event] failed. The failure will be handled.
                                    if (currentLogItem != null) {
                                        logInformation.addLogItem(currentLogItem);
                                    }

                                    currentLogItem = new LogItem(logParameter);
                                    currentLogItem.lineNumber = lineNumber;
                                    currentLogItem.setDate(line.substring(0, 29));

                                    // which kind ?
                                    if (formatLog == null) {
                                        formatLog = detectFormat(logParameter, line);
                                    }
                                    if (formatLog == FormatLog.TWOLINES)
                                    {
                                        currentLogItem.firstLine = true;
                                        final int posFirstBlanck = line.indexOf(" ", 30);
                                        currentLogItem.localisation = posFirstBlanck == -1 ? line.substring(30) : line.substring(posFirstBlanck + 1);
                                    }
                                    else
                                    {
                                        currentLogItem.firstLine = false;
                                        decodeLine(formatLog, currentLogItem, line);
                                    }
                                }
                                else if (currentLogItem != null && currentLogItem.firstLine)
                                {
                                    currentLogItem.firstLine = false;
                                    decodeLine(formatLog, currentLogItem, line);

                                }
                                else if (currentLogItem != null)
                                {
                                    currentLogItem.addContent("<br>" + line);
                                }
                                else // not very normal : that's mean the first line does not have a date
                                {
                                    if (currentLogItem != null) {
                                        logInformation.addLogItem(currentLogItem);
                                    }

                                    currentLogItem = new LogItem(logParameter);
                                    currentLogItem.lineNumber = lineNumber;
                                    currentLogItem.addContent(line);

                                }
                            }
                        } catch (final Exception e)
                        {
                            if (errorDuringDecodage == null) {
                                errorDuringDecodage = new BEvent(EventErrorDuringDecodage, "Line " + lineNumber);
                            }
                        }
                    }
                }
                line = br.readLine();
                lineNumber++;
                if (logParameter.logInformation && lineNumber % 500 == 0) {
                    logger.info("Read line " + lineNumber);
                }
            } // end loop
            if (logParameter.logInformation) {
                logger.info("End loop nbLines=" + logInformation.listLogs.size());
            }

            if (currentLogItem != null) {
                logInformation.addLogItem(currentLogItem);
            }

            if (errorDuringDecodage != null) {
                logInformation.listEvents.add(errorDuringDecodage);
            }
        } catch (final Exception e)
        {
            logInformation.listEvents.add(new BEvent(EventReadLogFile, e, "FileName=[" + logParameter.getFileName() + "]"));
        } finally
        {
            if (br != null) {
                try {
                    br.close();
                } catch (final Exception e) {
                };
            }
        }

        if (logParameter.logInformation) {
            logger.info("LogAccess.getLog : End reading [" + lineNumber + "]return [" + logInformation.listLogs.size() + "] lines");
        }

        return logInformation;
    }

    /**
     * return the path where all logs are
     *
     * @return
     */
    protected static String getLogPath()
    {
        final String logPath = System.getProperty("CATALINA_HOME");
        // logger.info("LogAccess: getFilesLog : logpath=" + logPath);
        if (logPath == null) {
            return null;
        }
        try
        {
            final File folder = new File(logPath + "/logs/");

            return folder.getCanonicalPath();
        } catch (final Exception e)
        {
            final StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            final String exceptionDetails = sw.toString();
            logger.severe("LogAccess.getLogPath :" + exceptionDetails);
        }
        return "";
    }

    /**
     * detectFormat
     *
     * @param line
     * @return
     */
    private static FormatLog detectFormat(final LogParameter logParameter, final String line)
    {
        if (logParameter.logInformation) {
            logger.info("LogAccess.detectFormat on the line [" + line + "]");
        }
        final int posLevel = line.indexOf(" ", 29 + 1);
        if (posLevel == -1) {
            return FormatLog.TWOLINES;
        }
        final String level = line.substring(29, posLevel);
        if (level.endsWith(":")) {
            return FormatLog.ONELINE;
        }
        return FormatLog.TWOLINES;
    }

    /**
     *
     */
    private static void decodeLine(final FormatLog formatLog, final LogItem logItem, final String line)
    {
        // TWOLINES:
        //  2017-02-27 13:27:11.260 -0800 org.bonitasoft.engine.api.impl.transaction.SetServiceState org.bonitasoft.engine.log.technical.TechnicalLoggerSLF4JImpl log
        //  INFO: THREAD_ID=76 | HOSTNAME=pc-pierre-yves | TENANT_ID=1 | resume tenant-level service org.bonitasoft.engine.tracking.TimeTracker on tenant with ID 1
        // ONELINE:
        //  2017-02-24 00:21:16.292 -0500 SEVERE: org.bonitasoft.engine.execution.work.FailureHandlingBonitaWork THREAD_ID=7472 | HOSTNAME=ip-10-74-58-84.ebiz.verizon.com | TENANT_ID=1 | The work [ExecuteConnectorOfActivity: flowNodeInstanceId = 11040113, connectorDefinitionName = Post NF job event] failed. The failure will be handled.
        int beginContent = 0;
        if (FormatLog.ONELINE.equals(formatLog))
        {
            logItem.setDate(line.substring(0, 29));
            final int pos = line.indexOf(" ", 29 + 1);
            if (pos != -1)
            {
                logItem.setLevel(line.substring(29 + 1, pos));

                final int posLoca = line.indexOf(" ", pos + 1);
                if (posLoca != -1) {
                    logItem.localisation = line.substring(pos + 1, posLoca);
                }
                beginContent = posLoca == -1 ? pos + 1 : posLoca + 1;
            } else {
                beginContent = 0;
            }
        }
        else if (FormatLog.TWOLINES.equals(formatLog))
        {
            // we are on the second line
            // INFO: THREAD_ID=76 | HOSTNAME=pc-pierre-yves | TENANT_ID=1 | TimeTracker started.
            logItem.firstLine = true;
            final int pos = line.indexOf(" ");
            if (pos != -1)
            {
                logItem.setLevel(line.substring(0, pos));
                logItem.addContent(line.substring(pos));
                beginContent = pos;
            } else {
                beginContent = 0;
            }

        } else {
            beginContent = 0;

        }
        // soon decode the complete information
        // INFO: THREAD_ID=76 | HOSTNAME=pc-pierre-yves | TENANT_ID=1 | TimeTracker started.

        logItem.addContent(line.substring(beginContent));

    }

}
