package org.bonitasoft.page.log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


/* ******************************************************************************** */
/*                                                                                  */
/* This class get a LogLine, and analyse it in order to build synthesis                                                                            */
/*                                                                                  */
/*                                                                                  */
/* ******************************************************************************** */

public class LogAnalyseError {

	private String logFileName;
	private boolean enableAnalysis;

	public class AnalyseItem {
		public int count = 0;
		public LogItem logItem;

		public AnalyseItem(LogItem logItem) {
			this.logItem = logItem;
		}
	}

	/**
	 * internal class to collect the synthesis
	 *
	 */
	public class AnalysisSynthese {
		private Map<String, AnalyseItem> mapSynthese = new LinkedHashMap<String, AnalyseItem>();

		private boolean tooMuchErrors = false;
		private int nbErrorsDetected=0;
		private final static int maxErrorSynthese=2000;
		
		public Map<String, Object> toJson() {
			Map<String,Object> result = new HashMap<String,Object>();
			List<Map<String,Object>> listSynthese  = new ArrayList<Map<String,Object>>();
			result.put("synthese", listSynthese);
				for (AnalyseItem item : mapSynthese.values())
			{
					Map<String,Object> mapItem  = new HashMap<String, Object>();
					listSynthese.add(mapItem);
					mapItem.put("count", item.count);
					
					if (item.logItem.processDefinitionId==null)
					{
						mapItem.put("type", "bycontent");
						mapItem.put("header", item.logItem.getHeader());
					}
					else
					{
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
				
				result.put("nbErrorsDetected", nbErrorsDetected);
				
				return result;
		}

		public void add(LogItem logItem) {
			String key = logItem.processDefinitionId + "#" + logItem.flowNodeDefinitionId + "#" + logItem.connectorImplementationClassName + "#" + logItem.causedBy;
			if (logItem.processDefinitionId==null)
				key=logItem.getHeader();
			if (!mapSynthese.containsKey(key)) {
				nbErrorsDetected++;
				// protect the server : if there are too much error, stop recorded it
				if (mapSynthese.size() > maxErrorSynthese)
					tooMuchErrors=true;
				else
					mapSynthese.put(key, new AnalyseItem(logItem));
			}
			AnalyseItem analyseItem = mapSynthese.get(key);
			analyseItem.count++;
		}
	}

	private AnalysisSynthese analysisSynthese = new AnalysisSynthese();

	/**
	 * internal class to collect the TimeLine
	 *
	 */
	public class AnalysisTimeLine {
		private Map<String, Integer> timeLine = new HashMap<String, Integer>();

		public void add(LogItem logItem) {
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
			long time = logItem.getTime();
			time = time / (1000*60); // in minute now
			time = time - time % 10; // remove the minute by 10
			
			String key = sdf.format( time * 1000*60 );
			
			Integer value = timeLine.get(key);
			if (value == null)
				value = Integer.valueOf(0);
			value = Integer.valueOf(value.intValue() + 1);
			timeLine.put(key, value);
		}

		public Map<String,Object> toJson() {
			
			List<GraphRange> listRange = new ArrayList<GraphRange>();
			 for (int i = 0; i <  24; i++) {
				 String hour = String.valueOf(i);
				 if (hour.length()==1)
					 hour = "0"+i;
				 for (int j=0;j<60;j+=10)
				 {
					 String minutes = String.valueOf(j);
					 if (minutes.length()==1)
						 minutes = "0"+i;
					 Integer count = timeLine.get(hour+":"+minutes);
					 listRange.add( new GraphRange( hour+":"+minutes, count==null ? 0 : count));
				 }
			 }
			 
			 Map<String,Object> result  = new HashMap<String, Object>();
				
			result.put("graph",getGraphRange("errors", listRange));
			return result;
		}

	}

	private AnalysisTimeLine analysisTimeLine = new AnalysisTimeLine();

	/**
	 * Constructor
	 * 
	 * @param logFileName
	 * @param enableAnalysis
	 */
	public LogAnalyseError(String logFileName, boolean enableAnalysis) {
		this.logFileName = logFileName;
		this.enableAnalysis = enableAnalysis;
	}

	/* ********************************************************************* */
	/*                                                                                  */
	/* Analysis */
	/*                                                                                  */
	/*                                                                                  */
	/* ********************************************************************* */

	private LogItem currentLogItem;

	public void analyse(LogItem logItem) {
		if (!enableAnalysis)
			return;
		// 1. multiple logItem can be group as the same errors in fact (Bonita
		// produce the same error on multiple line).
		// in that circonstance, we keep only the first logItem
		if (!logItem.isError()) {
			doAnalysis( currentLogItem);
			currentLogItem = null;
			return;
		}
		if (currentLogItem != null) {
			// is attached ?
			if (logItem.getTime() - currentLogItem.getTime() < 1000) {
				if (logItem.getFirstWord(2).equals(currentLogItem.getFirstWord(2)))
				{
					currentLogItem.link(logItem);
				
					return; // same starter, in less than 1 seconds : consider
							// as the same error in fact
				}
			}
			else 
			{
				// errors are too different, this is an another one
				doAnalysis( currentLogItem);
				currentLogItem=logItem;
			}
				
		}
		else
			currentLogItem=logItem;
		
	}

	public void end()
	{
		if (currentLogItem!=null)
			doAnalysis( currentLogItem);
	}
	/**
	 * do an analysis on the item
	 * @param logItem
	 */
	private void doAnalysis( LogItem logItem)
	{
		if (logItem == null)
			return;
		// here, we have to
				// ok, we considere this is a new error. Register it ?
				logItem.deepAnalysis();
				analysisSynthese.add(logItem);
				analysisTimeLine.add(logItem);
	}
	/*
	 * ***********************************************************************
	 */
	/*                                                                                  */
	/* Getter */
	/*                                                                                  */
	/*                                                                                  */
	/*
	 * ***********************************************************************
	 */

	/**
	 * getter information
	 */
	public AnalysisSynthese getSynthese() {
		return analysisSynthese;
	}

	public AnalysisTimeLine getTimeLine() {
		return analysisTimeLine;
	}

	public List<Map<String, Object>> toJson() {
		List resultAnalysis = new ArrayList<>();
		return resultAnalysis;
	}
	
	/* ********************************************************************* */
	/*                                                                                  */
	/* Graph */
	/*                                                                                  */
	/*                                                                                  */
	/* ********************************************************************* */

	public static class GraphRange
	{
		public String title;
		public long count;
		public GraphRange( String title, long count)
		{
			this.title = title;
			this.count = count;
		}
	}
	
	/**
     * ----------------------------------------------------------------
     * getGraphRange
     *
     * @return
     */
    public static String getGraphRange(final String title, final List<GraphRange> listRange) {

        /**
         * structure
         * "rows": [
         * {
         * c: [
         * { "v": "January" },"
         * { "v": 19,"f": "42 items" },
         * { "v": 12,"f": "Ony 12 items" },
         * ]
         * },
         * {
         * c: [
         * { "v": "January" },"
         * { "v": 19,"f": "42 items" },
         * { "v": 12,"f": "Ony 12 items" },
         * ]
         * },
         */
        String resultValue = "";

        for (int i = 0; i < listRange.size(); i++)
        {
            resultValue += "{\"c\":[{\"v\":\"" + listRange.get(i).title + "\"},{\"v\": " + listRange.get(i).count + "} ]},";

        }
        if (resultValue.length() > 0) {
            resultValue = resultValue.substring(0, resultValue.length() - 1);
        }

        final String resultLabel = "{ \"type\": \"string\", \"id\": \"whattime\", \"label\":\"whattime\" },"
                + "{ \"type\": \"number\", \"id\": \"value\", \"label\":\"Occurence\" }";

        final String valueChart = "	{"
                + "\"type\": \"ColumnChart\", "
                + "\"displayed\": true, "
                + "\"data\": {"
                + "\"cols\": [" + resultLabel + "], "
                + "\"rows\": [" + resultValue + "] "
                /*
                 * + "\"options\": { "
                 * + "\"bars\": \"horizontal\","
                 * + "\"title\": \""+title+"\", \"fill\": 20, \"displayExactValues\": true,"
                 * + "\"vAxis\": { \"title\": \"ms\", \"gridlines\": { \"count\": 100 } }"
                 */
                + "}"
                + "}";
        // 				+"\"isStacked\": \"true\","

        //		    +"\"displayExactValues\": true,"
        //
        //		    +"\"hAxis\": { \"title\": \"Date\" }"
        //		    +"},"
        // logger.info("TrackRangeChart >>"+ valueChart+"<<");
        //	String valueChartBar="{\"type\": \"BarChart\", \"displayed\": true, \"data\": {\"cols\": [{ \"id\": \"perf\", \"label\": \"Perf\", \"type\": \"string\" }, { \"id\": \"perfbase\", \"label\": \"ValueBase\", \"type\": \"number\" },{ \"id\": \"perfvalue\", \"label\": \"Value\", \"type\": \"number\" }], \"rows\": [{ \"c\": [ { \"v\": \"Write BonitaHome\" }, { \"v\": 550 },  { \"v\": 615 } ] },{ \"c\": [ { \"v\": \"Read BonitaHome\" }, { \"v\": 200 },  { \"v\": 246 } ] },{ \"c\": [ { \"v\": \"Read Medata\" }, { \"v\": 370 },  { \"v\": 436 } ] },{ \"c\": [ { \"v\": \"Sql Request\" }, { \"v\": 190 },  { \"v\": 213 } ] },{ \"c\": [ { \"v\": \"Deploy process\" }, { \"v\": 40 },  { \"v\": 107 } ] },{ \"c\": [ { \"v\": \"Create 100 cases\" }, { \"v\": 3600 },  { \"v\": 16382 } ] },{ \"c\": [ { \"v\": \"Process 100 cases\" }, { \"v\": 3700 },  { \"v\": 16469 } ] }]}, \"options\": { \"bars\": \"horizontal\",\"title\": \"Performance Measure\", \"fill\": 20, \"displayExactValues\": true,\"vAxis\": { \"title\": \"ms\", \"gridlines\": { \"count\": 100 } }}}";

        return valueChart;
    }

}
