package org.bonitasoft.page.log;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.bonitasoft.log.event.BEvent;
import org.bonitasoft.log.event.BEvent.Level;
import org.json.simple.JSONValue;

// System.getProperty("catalina.base") + "/logs"

public class LogAccess {

    // private static String sdfFormat = "yyyy-MM-dd'T'HH:mm:ss'Z'";

    private static Logger logger = Logger.getLogger(LogAccess.class.getName());

    private static BEvent EventInvalidLogFormat = new BEvent(LogAccess.class.getName(), 1, Level.APPLICATIONERROR, "Invalid Log Format", "The first line of the log should start by the date and a space, like '2017-02-27 '");
    private static BEvent EventErrorDuringDecodage = new BEvent(LogAccess.class.getName(), 2, Level.APPLICATIONERROR, "Error during decodate", "An error arrived during the decodage", "The decodage will be wrong on one line", "Check the Log file");
    private static BEvent EventReadLogFile = new BEvent(LogAccess.class.getName(), 2, Level.APPLICATIONERROR, "Error during the log file access", "An error arrived during the access to a log file", "No result", "Check the Execption");

    /*
     * *************************************************************************
     * *******
     */
    /*                                                                                  */
    /* LogParameter */
    /*                                                                                  */
    /*                                                                                  */
    /*
     * *************************************************************************
     * *******
     */

    public enum PERIMETER { ERROR, WARNING };
    public enum POLICY { TOP10, TOP100, ALL };
    
    /**
     * logparametres
     */
    public static class LogParameter {

        public boolean logInformation = false;
        public String fileName;
        public String pathName;
        public boolean produceJson = false;
        public boolean brutResult = false;

        public long pageNumber = 0;
        public long numberPerPage = 500;
        public boolean filterError;
        public String filterText;
        public boolean filterShortDate;
        public boolean filterTail;

        /**
         * if true, the analysis of error is enable
         */
        public boolean enableAnalysisError=false;

        public boolean analysisCompactBasedOnError;
        // ERROR or WARNING 
        
        public PERIMETER perimeter = PERIMETER.ERROR; 
        public POLICY policy = POLICY.ALL; 

        public List<String> zipanddownload;

        public String getFileName() {
            return fileName;
        }

        public String getCompleteFileName() {
            return pathName + File.separatorChar + getFileName();
        }

        /**
         * @param jsonSt
         * @return
         */
        public static LogParameter getInstanceFromJsonSt(final String jsonSt) {
            if (jsonSt == null) {
                return new LogParameter();
            }
            @SuppressWarnings("unchecked")
            final HashMap<String, Object> jsonHash = (HashMap<String, Object>) JSONValue.parse(jsonSt);
            if (jsonHash == null) {
                return new LogParameter();
            }
            final LogParameter logParameter = new LogParameter();
            logParameter.logInformation = Toolbox.getBoolean(jsonHash.get("logInformation"), false);
            logParameter.pageNumber = Toolbox.getLong(jsonHash.get("pagenumber"), 0L);
            logParameter.numberPerPage = Toolbox.getLong(jsonHash.get("numberperpage"), 100L);
            logParameter.brutResult = Toolbox.getBoolean(jsonHash.get("brutResult"), false);
            logParameter.fileName = (String) jsonHash.get("fileName");
            logParameter.pathName = (String) jsonHash.get("pathName");
            logParameter.analysisCompactBasedOnError = Toolbox.getBoolean(jsonHash.get("analysisCompactBasedOnError"), false);

            logParameter.filterError = Toolbox.getBoolean(jsonHash.get("filterError"), false);
            logParameter.filterText = (String) jsonHash.get("filterText");
            if (logParameter.filterText != null && logParameter.filterText.trim().length() == 0) {
                logParameter.filterText = null;
            }
            logParameter.filterShortDate = Toolbox.getBoolean(jsonHash.get("filterShortDate"), false);
            logParameter.filterTail = Toolbox.getBoolean(jsonHash.get("filterTail"), false);
            logParameter.zipanddownload = Toolbox.getListString(jsonHash.get("listdaysdownload"), null);
            @SuppressWarnings("unchecked")
            Map<String,Object> mapAnalyse = (Map<String,Object>) jsonHash.get("analyze" );
            try {
                logParameter.enableAnalysisError = Toolbox.getBoolean( mapAnalyse.get("enableAnalysisError"), false);
                logParameter.perimeter = PERIMETER.valueOf( mapAnalyse.get("perimeter").toString());
                logParameter.policy = POLICY.valueOf( mapAnalyse.get("policy").toString());

            } catch(Exception e )
            
            {}
            
            return logParameter;
        }
    }

    // TWOLINE : 2017-02-27 09:03:47.992 -0800 org.bonitasoft.tomcat.H2Listener
    // org.bonitasoft.tomcat.H2Listener lifecycleEvent
    // ONELINE : 2017-02-24 00:21:16.292 -0500 SEVERE:
    // org.bonitasoft.engine.execution.work.FailureHandlingBonitaWork
    // THREAD_ID=7472 | HOSTNAME=ip-10-74-58-84.ebiz.verizon.com | TENANT_ID=1 |
    // The work [ExecuteConnectorOfActivity: flowNodeInstanceId = 11040113,
    // connectorDefinitionName = Post NF job event] failed. The failure will be
    // handled.

    public enum FormatLineLog {
        ONELINE, TWOLINES
    }

    public static class FormatLog {

        FormatLineLog formatLineLog = null;
        int posEndDate = -1;

    }

    /*
     * *************************************************************************
     * *******
     */
    /*                                                                                  */
    /* GetFileLog */
    /*                                                                                  */
    /*                                                                                  */
    /*
     * *************************************************************************
     * *******
     */
    public static class FileInformation {

        public String fileName;
        public String pathName;

        public String getFileName() {
            return fileName;
        }

        public String getPathName() {
            return pathName;
        }

        public String getCompleteFileName() {
            return pathName + File.separatorChar + fileName;
        }

        public Map<String, String> getMap() {
            Map<String, String> map = new HashMap<String, String>();
            map.put("fileName", getFileName());
            map.put("pathName", getPathName());
            map.put("completeFileName", getCompleteFileName());
            return map;

        }
    }

    public static class filesInformation {

        final Map<String, List<FileInformation>> mapLogs = new HashMap<String, List<FileInformation>>();

    }

    /**
     * get the list of files
     *
     * @return a Map of all files. Key is the date ("2017-11-09"), content is a
     *         list of String (one string per file): "bonita.2017-11-08.log",
     *         "catalina.2017-11-08.log")
     */
    public static Map<String, List<FileInformation>> getFilesInfoLog() {
        logger.info("LogAccess: getFilesLog");

        final List<String> listLogPath = getLogPath();
        final Map<String, List<FileInformation>> mapLogs = new HashMap<String, List<FileInformation>>();
        if (listLogPath == null) {
            return mapLogs;
        }

        for (String logPath : listLogPath) {
            try {
                final File folder = new File(logPath);
                logger.info("LogAccess: listFiles=" + folder.getCanonicalPath());
                final File[] listOfFiles = folder.listFiles();
                if (listOfFiles == null) {
                    logger.info("LogAccess: no file under =" + folder.getCanonicalPath());
                    continue;
                }

                for (int i = 0; i < listOfFiles.length; i++) {
                    if (listOfFiles[i].isFile()) {
                        final String name = listOfFiles[i].getName();
                        if (!name.endsWith(".log"))
                            continue;

                        final StringTokenizer st = new StringTokenizer(name, ".");
                        @SuppressWarnings("unused")
                        final String filename = st.hasMoreTokens() ? st.nextToken() : "";
                        final String filedate = st.hasMoreTokens() ? st.nextToken() : "";
                        List<FileInformation> listFiles = (List<FileInformation>) mapLogs.get(filedate);
                        if (listFiles == null) {
                            listFiles = new ArrayList<>();
                        }
                        FileInformation infoFile = new FileInformation();
                        infoFile.fileName = name;
                        infoFile.pathName = logPath;
                        listFiles.add(infoFile);

                        mapLogs.put(filedate, listFiles);
                        // logger.info("Add in [" + filedate + "] :" +
                        // listFiles);
                    }
                }

            } catch (final Exception e) {
                final StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                final String exceptionDetails = sw.toString();
                logger.severe("LogAccess.getFilesLog :" + exceptionDetails);
                continue;

            }
        } // end loop all logPath
          // now, set the map to a list
          // logger.info("Before sort " + mapLogs);
        final Map<String, List<FileInformation>> sortMapLogs = new TreeMap<String, List<FileInformation>>(mapLogs);
        // logger.info("After sort " + sortMapLogs);
        return sortMapLogs;
    }

    
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
    /**
     * Get all files for a particular date
     * @param dateOfTheDay
     * @return
     */
    public static List<FileInformation> getFilesInfoLogOneDay( Date dateOfTheDay ) {
         Map<String, List<FileInformation>> listAllFiles = getFilesInfoLog();
         // Expected a structure
         String dateString = sdf.format( dateOfTheDay);
         return listAllFiles.get(dateString);
    }
    /**
     * return the same information for JSON
     * 
     * @return
     */
    public static Map<String, List<Map<String, String>>> getFilesLog() {
        final Map<String, List<FileInformation>> infoLogs = getFilesInfoLog();
        // now translate
        logger.fine("LogAccess.getFilesLog : transform the information for JSON");
        final Map<String, List<Map<String, String>>> mapLogs = new HashMap<String, List<Map<String, String>>>();
        for (String key : infoLogs.keySet()) {
            List<Map<String, String>> listFiles = new ArrayList<Map<String, String>>();
            mapLogs.put(key, listFiles);
            for (FileInformation fileInformation : infoLogs.get(key)) {
                listFiles.add(fileInformation.getMap());
            }
        }
        return mapLogs;
    }

    /*
     * *************************************************************************
     * *******
     */
    /*                                                                                  */
    /* GetLog */
    /*                                                                                  */
    /*                                                                                  */
    /*
     * *************************************************************************
     * *******
     */

    /**
     * Parse the log file and return all information inside
     *
     * @param fileName
     * @return
     */

    public static LogInformation getLog(final LogParameter logParameter) {
        final LogInformation logInformation = new LogInformation(logParameter);
        logInformation.logParameter = logParameter;
        logInformation.addAsJson = logParameter.produceJson;

        BufferedReader br = null;
        long lineNumber = 0;
        try {
            if (logParameter.logInformation) {
                logger.info("LogAccess.getLog : Start reading [" + logParameter.fileName + "=>" + logParameter.getCompleteFileName() + "] from Page[" + logParameter.pageNumber + "(" + logParameter.numberPerPage + ") Lines[" + logParameter.pageNumber * logParameter.numberPerPage + "-"
                        + (logParameter.pageNumber * logParameter.numberPerPage + logParameter.numberPerPage) + "]" + " filterError=" + logParameter.filterError + " fiterText[" + logParameter.filterText + "]");
            }

            if (logParameter.getFileName() == null || logParameter.getFileName().length() == 0) {
                // we need the CurrentLogFile
                final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
                logInformation.logFileName = "bonita." + sdf.format(new Date()) + ".log";
                logger.fine("LogAccess.getLog : Search Current Log based on file [" + logInformation.logFileName + "] or [server.log]");

                List<String> listPath = getLogPath();
                for (String logPath : listPath) {
                    File file = new File(logPath + File.separatorChar + logInformation.logFileName);
                    if (file.exists())
                        logInformation.completeLogFileName = file.getAbsolutePath();
                    else {
                        file = new File(logPath + File.separatorChar + "server.log");
                        if (file.exists())
                            logInformation.completeLogFileName = file.getAbsolutePath();
                    }

                }
                logger.info("LogAccess.getLog : Found Current Log based on[" + logInformation.logFileName + "] ?  [" + logInformation.completeLogFileName + "]");

            } else {
                logInformation.logFileName = logParameter.getFileName();
                logInformation.completeLogFileName = logParameter.getCompleteFileName();
            }
            BEvent errorDuringDecodage = null;
            // all lines
            // Attention : add the currentlogitem at the last moment
            LogItem currentLogItem = null;
            FormatLog formatLog = new FormatLog();
            String detectDate = null;

            logInformation.start();
            br = new BufferedReader(new FileReader(logInformation.completeLogFileName));

            String line = br.readLine();
            line = new String(line.getBytes(), StandardCharsets.UTF_8);

            lineNumber++;

            // manage the pagination
            long first = (logParameter.pageNumber - 1) * logParameter.numberPerPage;
            long end = (logParameter.pageNumber - 1) * logParameter.numberPerPage + logParameter.numberPerPage;

            // loop on all lines now
            while (line != null) {
                // do all calculation, just decide (or not) to save the result, according the pagination
                // logInformation.allowSave(lineNumber >= first && lineNumber < end);
                logInformation.allowSave(logInformation.lineNumberFiltered >= first && logInformation.lineNumberFiltered < end);

                if (logParameter.brutResult) {
                    currentLogItem = new LogItem(logParameter);
                    currentLogItem.lineNumber = lineNumber;
                    currentLogItem.addBrut(line);
                    logInformation.addLogItem(currentLogItem);
                } else {
                    try {
                        // manage the line - no date were detected ever, so
                        if (detectDate == null) {
                            // still trying to find a date
                            final int posBlank = line.indexOf(" ");
                            if (posBlank == -1) {
                                logInformation.listEvents.add(new BEvent(EventInvalidLogFormat, "on line " + lineNumber));
                            } else {
                                detectDate = line.substring(0, posBlank);
                            }
                        }

                        // do the decision now
                        if (detectDate == null) {
                            if (currentLogItem != null) {
                                logInformation.addLogItem(currentLogItem);
                            }

                            currentLogItem = new LogItem(logParameter);
                            currentLogItem.lineNumber = lineNumber;
                            currentLogItem.addContent(line);

                        } else { // we can get the structure
                            if (line.startsWith(detectDate)) {
                                // Two kind of format :
                                // 2017-02-27 09:03:47.992 -0800
                                // org.bonitasoft.tomcat.H2Listener
                                // org.bonitasoft.tomcat.H2Listener
                                // lifecycleEvent
                                // 2017-02-24 00:21:16.292 -0500 SEVERE:
                                // org.bonitasoft.engine.execution.work.FailureHandlingBonitaWork
                                // THREAD_ID=7472 |
                                // HOSTNAME=ip-10-74-58-84.ebiz.verizon.com
                                // | TENANT_ID=1 | The work
                                // [ExecuteConnectorOfActivity:
                                // flowNodeInstanceId = 11040113,
                                // connectorDefinitionName = Post NF job
                                // event] failed. The failure will be
                                // handled.
                                if (currentLogItem != null) {
                                    logInformation.addLogItem(currentLogItem);
                                }

                                currentLogItem = new LogItem(logParameter);
                                currentLogItem.lineNumber = lineNumber;
                                // 2017-11-09 16:36:24.183 -0400 INFO
                                // 2017-11-08 07:55:25,359 INFO
                                formatLog = detectFormatDate(formatLog, line);

                                // which kind ?

                                formatLog = detectFormat(formatLog, logParameter, line);
                                if (formatLog.formatLineLog == FormatLineLog.TWOLINES) {
                                    currentLogItem.setDate(line.substring(0, formatLog.posEndDate));
                                    currentLogItem.firstLine = true;
                                    final int posFirstBlanck = line.indexOf(" ", formatLog.posEndDate + 1);
                                    currentLogItem.localisation = posFirstBlanck == -1 ? line.substring(formatLog.posEndDate + 1) : line.substring(posFirstBlanck + 1);
                                } else {
                                    currentLogItem.firstLine = false;
                                    decodeLine(formatLog, currentLogItem, line);
                                }
                            } else if (currentLogItem != null && currentLogItem.firstLine) {
                                currentLogItem.firstLine = false;
                                decodeLine(formatLog, currentLogItem, line);

                            } else if (currentLogItem != null) {
                                currentLogItem.addContent(LogItem.newLine + line);
                            } else // not very normal : that's mean the
                                   // first line does not have a date
                            {
                                currentLogItem = new LogItem(logParameter);
                                currentLogItem.lineNumber = lineNumber;
                                currentLogItem.addContent(line);
                            }
                        }
                    } catch (final Exception e) {
                        if (errorDuringDecodage == null) {
                            errorDuringDecodage = new BEvent(EventErrorDuringDecodage, "Line " + lineNumber);
                        }
                    }

                }
                line = br.readLine();
                if (line!=null)
                    line = new String(line.getBytes(), StandardCharsets.UTF_8);

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

            logInformation.end(lineNumber);

            if (errorDuringDecodage != null) {
                logInformation.listEvents.add(errorDuringDecodage);
            }
        } catch (final Exception e) {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String exceptionDetails = sw.toString();

            logInformation.listEvents.add(new BEvent(EventReadLogFile, e, "FileName=[" + logParameter.getFileName() + "] at "+exceptionDetails));
            logInformation.end(lineNumber);

        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (final Exception e) {
                } 
            }
        }

        if (logParameter.logInformation) {
            logger.info("LogAccess.getLog : End reading [" + lineNumber + "]return [" + logInformation.listLogs.size() + "] lines");
        }

        return logInformation;
    }

    /* ******************************************************************** */
    /*                                                                      */
    /* LogPath */
    /*                                                                      */
    /*                                                                      */
    /* ******************************************************************** */
    /**
     * return All paths where logs are
     *
     * @return
     */
    protected static List<String> getLogPath() {

        // nota: by then handler, we may have nothing, because the handler may
        // be attach to a parent
        // secondly, even if the handler is a FileHandler, it will not give its
        // directory :-(
        List<String> listPath = new ArrayList<String>();

        getFilePathThomcat(listPath);
        getFilePathJboss(listPath);

        return listPath;
    }

    /**
     * get the file when the logging is a Java.util.logging (TOMCAT usage for
     * example)
     * 
     * @return
     */
    protected static void getFilePathThomcat(List<String> listPath) {
        LogManager logManager = LogManager.getLogManager();
        String handlers = logManager.getProperty("handlers");

        logger.fine("LogAccess.getFilePathThomcat : LogManagerClass[" + logManager.getClass().getName() + "] TOMCAT handlers[" + handlers + "]");
        if (handlers != null) {
            StringTokenizer st = new StringTokenizer(handlers, ",");
            while (st.hasMoreTokens()) {
                String handler = st.nextToken().trim();
                String directory = logManager.getProperty(handler + ".directory");
                String fileName = logManager.getProperty(handler + ".fileName");
                logger.fine("LogAccess.getFilePathThomcat: getCanonicalPath :detect [" + handler + "] directory[" + directory + "], fileName[" + fileName + "]");
                if (directory != null) {
                    File fileDirectory = new File(directory);
                    try {
                        if (!listPath.contains(fileDirectory.getCanonicalPath()))
                            listPath.add(fileDirectory.getCanonicalPath());

                    } catch (IOException e) {
                        logger.severe("LogAccess.getFilePathThomcat: getCanonicalPath Error  [" + fileDirectory.getAbsolutePath() + "] file[" + fileDirectory.getName() + "] error[" + e.toString() + "]");
                    }
                }
                if (fileName != null) {
                    File fileFileName = new File(fileName);
                    if (!listPath.contains(fileFileName.getParent()))
                        listPath.add(fileFileName.getParent());

                }
            }
        }
        String logPathVariable = System.getProperty("catalina.home");
        if (logPathVariable != null) {
            File fileDirectory = new File(logPathVariable);
            try {
                if (!listPath.contains(fileDirectory.getCanonicalPath()))
                    listPath.add(fileDirectory.getCanonicalPath());

            } catch (IOException e) {
                logger.severe("LogAccess.getFilePathThomcat: getCanonicalPath Error  [" + fileDirectory.getAbsolutePath() + "] file[" + fileDirectory.getName() + "] error[" + e.toString() + "]");
            }

        }
        logger.fine("LogAccess.getFilePathThomcat: getCanonicalPath : by env[catalina.home] logpath=" + logPathVariable);
        logger.info("LogAccess.getFilePathThomcat: listPath=" + listPath);
        return;

    }

    /**
     * in JBOSS, the fileHandler has a "getFile"
     */
    protected static void getFilePathJboss(List<String> listPath) {
        LogManager logManager = LogManager.getLogManager();
        Logger loggerBonitasoft = Logger.getLogger("org.bonitasoft");
        int loopParent = -1;
        while (loggerBonitasoft != null && loopParent < 10) {
            loopParent++;
            Handler[] handlers = loggerBonitasoft.getHandlers();
            logger.fine("LogAccess.getFilePathJboss : LogManagerClass[" + logManager.getClass().getName() + "] Logger[" + loggerBonitasoft.getName() + "] handlers[" + handlers.length + "] useParent[" + loggerBonitasoft.getUseParentHandlers() + "] loopParent=" + loopParent);

            for (int i = 0; i < handlers.length; i++) {
                Handler handler = handlers[i];
                logger.fine("LogAccess.getFilePathJboss handler.className[" + handler.getClass().getName() + "]");
                if (handler.getClass().getName().equals("org.jboss.logmanager.handlers.FileHandler") || handler.getClass().getName().equals("org.jboss.logmanager.handlers.PeriodicRotatingFileHandler")) {
                    try {

                        Class<?> classHandler = handler.getClass();
                        // I don't want to load the JBOSS class in that circumstance
                        Method methodeGetFile = classHandler.getMethod("getFile", (Class[]) null);
                        File fileDirectory = (File) methodeGetFile.invoke(handler);
                        if (fileDirectory != null) {
                            try {
                                String path;
                                if (fileDirectory.isDirectory())
                                    path = fileDirectory.getCanonicalPath();
                                else
                                    path = fileDirectory.getParent();
                                if (!listPath.contains(path))
                                    listPath.add(path);
                                logger.fine("LogAccess.getFilePathJboss Handler : file name=[" + fileDirectory.getName() + "] path=" + fileDirectory.getPath() + "] getCanonicalPath=" + fileDirectory.getCanonicalPath() + "]  getParent=" + fileDirectory.getParent() + "]");
                            } catch (Exception e) {
                                logger.severe("LogAccess.getFilePathThomcat: getCanonicalPath Error  [" + fileDirectory.getAbsolutePath() + "] file[" + fileDirectory.getName() + "] error[" + e.toString() + "]");
                            }
                        }
                    } catch (Exception e) {
                        logger.severe("LogAccess.getFilePathJboss Error during call GetFile method on a JBOSS object" + e.toString());
                    }

                }
            } // end for
              // search on the parent now
            if (loggerBonitasoft.getUseParentHandlers()) {
                loggerBonitasoft = loggerBonitasoft.getParent();
            } else
                loggerBonitasoft = null;

        }
        logger.info("LogAccess.getFilePathJboss: end detection listPath=" + listPath);
        return;

    }

    /* ******************************************************************** */
    /*                                                                      */
    /* Zip and Download */
    /*                                                                      */
    /*                                                                      */
    /* ******************************************************************** */

    public static class LogZip {

        public ByteArrayOutputStream containerZip = new ByteArrayOutputStream();
    }

    /**
     * get all the files requested, and compress in a ZIP file
     * 
     * @param logParameter
     * @return
     */
    public static LogZip getZipAndDownload(LogParameter logParameter) {
        Map<String, List<FileInformation>> allFiles = getFilesInfoLog();

        LogZip logZip = new LogZip();
        try {
            ZipOutputStream zos = new ZipOutputStream(logZip.containerZip);

            if (logParameter.zipanddownload != null)
                for (String dayKey : logParameter.zipanddownload) {
                    List<FileInformation> filesOfTheDay = allFiles.get(dayKey);
                    if (filesOfTheDay == null)
                        continue;
                    for (FileInformation oneFile : filesOfTheDay) {
                        ZipEntry ze = new ZipEntry(oneFile.fileName);
                        zos.putNextEntry(ze);

                        FileInputStream fr = new FileInputStream(oneFile.getCompleteFileName());
                        byte[] buffer = new byte[10024];

                        int len;
                        while ((len = fr.read(buffer)) > 0) {
                            zos.write(buffer, 0, len);
                        }
                        fr.close();
                        zos.closeEntry();

                    }
                }
            zos.close();
        } catch (Exception e) {
            logger.severe("Error during zip file [" + e.toString() + "]");
        }

        return logZip;
    }

    // 2017-11-09 16:36:24.183 -0400 INFO
    // 2017-11-08 07:55:25,359 INFO
    private static FormatLog detectFormatDate(FormatLog formatLog, String line) {
        if (formatLog.posEndDate >= 0)
            return formatLog;
        int posEndDay = line.indexOf(" ");
        int posEndHour = posEndDay == -1 ? -1 : line.indexOf(" ", posEndDay + 1);
        int posEndNextWord = posEndHour == -1 ? -1 : line.indexOf(" ", posEndHour + 1);
        if (posEndHour != -1 && posEndNextWord != -1) {
            String nextWord = line.substring(posEndHour, posEndNextWord).trim();
            char firstChar = nextWord.length() > 0 ? nextWord.charAt(0) : ' ';
            if (firstChar == '-' || firstChar == '+' || (firstChar >= '0' && firstChar <= '9'))
                formatLog.posEndDate = posEndNextWord + 1;
            else
                formatLog.posEndDate = posEndHour + 1;
            // attention, the posEndDate is maybe on the ' '(multiple blanck)
            while (line.length() > formatLog.posEndDate && line.charAt(formatLog.posEndDate) == ' ')
                formatLog.posEndDate++;
        } else
            formatLog.posEndDate = 0;
        return formatLog;
    }

    /**
     * detectFormat
     *
     * @param line
     * @return
     */
    private static FormatLog detectFormat(FormatLog formatLog, final LogParameter logParameter, final String line) {
        if (formatLog.formatLineLog != null)
            return formatLog;
        if (logParameter.logInformation) {
            logger.info("LogAccess.detectFormat on the line [" + line + "]");
        }
        final int posLevel = line.indexOf(" ", formatLog.posEndDate + 1);
        if (posLevel == -1) {
            formatLog.formatLineLog = FormatLineLog.TWOLINES;
            return formatLog;
        }
        String level = line.substring(formatLog.posEndDate, posLevel);

        // if the text is somethink like INFO, INFOS: SEVERE then this is a
        // level.
        // how to detect it ? Let's detect if this is a INFO, WARNING... if this is a word in UPPER CASE, this is
        // a level.
        if (level.endsWith(":"))
            level = level.substring(0, level.length() - 1);
        level = level.toUpperCase();
        if (LogInformation.listWarnings.contains(level)
                || LogInformation.listErrors.contains(level)
                || LogInformation.listInfos.contains(level)
                || LogInformation.listDebugs.contains(level))
            formatLog.formatLineLog = FormatLineLog.ONELINE;
        else
            formatLog.formatLineLog = FormatLineLog.TWOLINES;

        return formatLog;
    }

    /**
     * decode the line according the ONELINE or TWOLINES policy
     * Nota: the synthesis (error ? ...) will be done during the Loginformation.addLogItem() method
     */
    private static void decodeLine(final FormatLog formatLog, final LogItem logItem, final String line) {
        // TWOLINES:
        // 2017-02-27 13:27:11.260 -0800
        // org.bonitasoft.engine.api.impl.transaction.SetServiceState
        // org.bonitasoft.engine.log.technical.TechnicalLoggerSLF4JImpl log
        // INFO: THREAD_ID=76 | HOSTNAME=pc-pierre-yves | TENANT_ID=1 | resume
        // tenant-level service org.bonitasoft.engine.tracking.TimeTracker on
        // tenant with ID 1
        // ONELINE:
        // 2017-02-24 00:21:16.292 -0500 SEVERE:
        // org.bonitasoft.engine.execution.work.FailureHandlingBonitaWork
        // THREAD_ID=7472 | HOSTNAME=ip-10-74-58-84.ebiz.verizon.com |
        // TENANT_ID=1 | The work [ExecuteConnectorOfActivity:
        // flowNodeInstanceId = 11040113, connectorDefinitionName = Post NF job
        // event] failed. The failure will be handled.
        int beginContent = 0;
        if (FormatLineLog.ONELINE.equals(formatLog.formatLineLog)) {
            logItem.setDate(line.substring(0, formatLog.posEndDate));
            final int pos = line.indexOf(" ", formatLog.posEndDate);
            if (pos != -1) {
                logItem.setLevel(line.substring(formatLog.posEndDate, pos));

                final int posLoca = line.indexOf(" ", pos + 1);
                if (posLoca != -1) {
                    logItem.localisation = line.substring(pos + 1, posLoca);
                }
                beginContent = posLoca == -1 ? pos + 1 : posLoca + 1;
            } else {
                beginContent = 0;
            }
        } else if (FormatLineLog.TWOLINES.equals(formatLog.formatLineLog)) {
            // we are on the second line
            // INFO: THREAD_ID=76 | HOSTNAME=pc-pierre-yves | TENANT_ID=1 |
            // TimeTracker started.
            logItem.firstLine = true;
            final int pos = line.indexOf(" ");
            if (pos != -1) {
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
        // INFO: THREAD_ID=76 | HOSTNAME=pc-pierre-yves | TENANT_ID=1 |
        // TimeTracker started.

        logItem.addContent(line.substring(beginContent));

    }

}
