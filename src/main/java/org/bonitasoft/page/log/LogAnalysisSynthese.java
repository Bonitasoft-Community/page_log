package org.bonitasoft.page.log;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.store.toolbox.LoggerStore.LOGLEVEL;



/**
 * internal class to collect the synthesis
 */
public class LogAnalysisSynthese {

    public static class LogAnalyseItem {

        public int count = 0;
        public LogItem logItem;

        public LogAnalyseItem(LogItem logItem) {
            this.logItem = logItem;
        }
    }

    /**
     * 
     */
    private final LogAnalyseError logAnalyseError;

    /**
     * Default Constructor.
     * 
     * @param logAnalyseError
     */
    LogAnalysisSynthese(LogAnalyseError logAnalyseError) {
        this.logAnalyseError = logAnalyseError;
    }

    private Map<String, LogAnalyseItem> mapSynthese = new LinkedHashMap<>();

    private boolean tooMuchErrors = false;
    private long nbErrorsDetected = 0;
    private long nbDifferentErrorsDetected = 0;
    private final static int maxErrorSynthese = 2000;

    long timeAnalysisInms;
    private long nbLogItems;
    long nbTotalLines;

    /* ******************************************************************************** */
    /*                                                                                  */
    /* Access information */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */
    public long getNbErrors() {
        return nbErrorsDetected;
    }
    public long getNbDifferentsErrors() {
        return nbDifferentErrorsDetected;
    }
    public boolean isTooMuchErrors() {
        return tooMuchErrors;
    }
    
    public long getTotalLines() {
        return nbTotalLines;
    }
    public long getNbItemsDetected() {
        return nbLogItems;
    }

    
    public Map<String, LogAnalyseItem> getSyntheses() {
        return mapSynthese;
    }

    public List<LogAnalyseItem> getTopSyntheses(int topNumber, boolean onlyError) {
        @SuppressWarnings({ "unchecked", "rawtypes" })
        List<LogAnalyseItem> listSynthese = new ArrayList(mapSynthese.values());
        Collections.sort(listSynthese, new Comparator<LogAnalyseItem>() {

            public int compare(LogAnalyseItem s1,
                    LogAnalyseItem s2) {
                int countS1= s1.count;
                int countS2 = s2.count;
                if (onlyError)
                {
                    if (s1.logItem.isLevelWarning())
                        countS1=-1;
                    if (s2.logItem.isLevelWarning())
                        countS2=-1;
                }
                // we want the COUNT MAX at the top
                return Integer.valueOf( countS2 ).compareTo(Integer.valueOf( countS1));
            }
        });
        if (listSynthese.size() > topNumber)
            listSynthese = listSynthese.subList(0, topNumber);
        return listSynthese;
    }
    /* ******************************************************************************** */
    /*                                                                                  */
    /* ToJson */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */

    public Map<String, Object> toJson() {
        DecimalFormat myFormatter = new DecimalFormat("###,###,###,###,###,###,###");

        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> listSynthese = new ArrayList<>();
        result.put("synthese", listSynthese);
        for (LogAnalyseItem item : mapSynthese.values()) {
            Map<String, Object> mapItem = new HashMap<>();
            listSynthese.add(mapItem);
            mapItem.put("count", item.count);

            if (item.logItem.processDefinitionId == null) {
                mapItem.put("type", "bycontent");
                mapItem.put("header", item.logItem.getHeader());
            } else {
                mapItem.put("type", "byprocess");
                mapItem.put("processname", item.logItem.processDefinitionName);
                mapItem.put("processversion", item.logItem.processDefinitionVersion);
                mapItem.put("processid", item.logItem.processDefinitionId);
                mapItem.put("flownodeid", item.logItem.flowNodeDefinitionId);
                mapItem.put("flownodename", item.logItem.flowNodeDefinitionName);
                mapItem.put("connectorName", item.logItem.connectorImplementationClassName);
            }
            mapItem.put("logitem", item.logItem.toJson());
        }
        result.put("tooMuchErrors", tooMuchErrors);
        result.put("maxErrors", maxErrorSynthese);
        result.put("timeAnalysisInSec", timeAnalysisInms / 1000);
        result.put("nbLogItems", nbLogItems);
        result.put("nbLogItemsSt", myFormatter.format(nbLogItems));
        result.put("nbTotalLines", nbTotalLines);
        result.put("nbTotalLinesSt", myFormatter.format(nbTotalLines));

        result.put("nbErrorsDetected", nbErrorsDetected);
        result.put("nbDifferentErrorsDetected", nbDifferentErrorsDetected);

        return result;
    }

    /* ******************************************************************************** */
    /*                                                                                  */
    /* Collect information method */
    /*                                                                                  */
    /*                                                                                  */
    /* ******************************************************************************** */

    protected void add(LogItem logItem) {
        nbLogItems++;
        String key;
        if (this.logAnalyseError.analysisCompactBasedOnError) {
            if (logItem.causedBy != null) {
                // search the first : 
                int pos = logItem.causedBy.indexOf(":");
                if (pos != -1)
                    key = logItem.causedBy.substring(0, pos);
                else
                    key = logItem.causedBy.length() > 100 ? logItem.causedBy.substring(0, 100) : logItem.causedBy;
            } else
                key = logItem.getHeader();
        } else {
            key = logItem.processDefinitionId + "#" + logItem.flowNodeDefinitionId + "#" + logItem.connectorImplementationClassName + "#" + logItem.causedBy;

            if (logItem.processDefinitionId == null)
                key = logItem.getHeader();
        }
        nbErrorsDetected++;
        if (!mapSynthese.containsKey(key)) {
            nbDifferentErrorsDetected++;
            // protect the server : if there are too much error, stop
            // recorded it
            if (mapSynthese.size() > maxErrorSynthese) {
                tooMuchErrors = true;
                return;
            } else {
                mapSynthese.put(key, new LogAnalyseItem(logItem));
            }
        }
        LogAnalyseItem analyseItem = mapSynthese.get(key);
        analyseItem.count++;
    }
}
