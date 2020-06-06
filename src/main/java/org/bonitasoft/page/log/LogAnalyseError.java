package org.bonitasoft.page.log;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bonitasoft.page.log.LogAccess.LogParameter;

/* ******************************************************************************** */
/*                                                                                  */
/* This class get a LogLine, and analyse it in order to build synthesis                                                                            */
/*                                                                                  */
/*                                                                                  */
/* ******************************************************************************** */

public class LogAnalyseError {

	private String logFileName;
	private boolean enableAnalysis;
	boolean analysisCompactBasedOnError;
	
	

	private LogAnalysisSynthese analysisSynthese = new LogAnalysisSynthese(this);

	/**
	 * internal class to collect the TimeLine
	 *
	 */
	public class AnalysisTimeLine {
		private Map<String, Integer> timeLine = new HashMap<String, Integer>();

		public void add(LogItem logItem) {
			SimpleDateFormat sdf = new SimpleDateFormat("HH:mm");
			long time = logItem.getTime();
			time = time / (1000 * 60); // in minute now
			time = time - time % 10; // remove the minute by 10

			String key = sdf.format(time * 1000 * 60);

			Integer value = timeLine.get(key);
			if (value == null)
				value = Integer.valueOf(0);
			value = Integer.valueOf(value.intValue() + 1);
			timeLine.put(key, value);
		}

		public Map<String, Object> toJson() {

			List<GraphRange> listRange = new ArrayList<GraphRange>();
			for (int i = 0; i < 24; i++) {
				String hour = String.valueOf(i);
				if (hour.length() == 1)
					hour = "0" + i;
				for (int j = 0; j < 60; j += 10) {
					String minutes = String.valueOf(j);
					if (minutes.length() == 1)
						minutes = "0" + i;
					Integer count = timeLine.get(hour + ":" + minutes);
					listRange.add(new GraphRange(hour + ":" + minutes, count == null ? 0 : count));
				}
			}

			Map<String, Object> result = new HashMap<String, Object>();

			result.put("graph", getGraphRange("errors", listRange));
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
	public LogAnalyseError(LogParameter logParameter) {
		this.logFileName = logParameter.fileName;
		this.enableAnalysis = logParameter.enableAnalysisError;
		this.analysisCompactBasedOnError = logParameter.analysisCompactBasedOnError;
	}

	/* ********************************************************************* */
	/*                                                                                  */
	/* Analysis */
	/*                                                                                  */
	/*                                                                                  */
	/* ********************************************************************* */

	private LogItem previousLogItem=null;

	/**
	 * start the analysis
	 */
	public void startAnalyse()
	{
	  previousLogItem=null;
	}
	/**
	 * Analysis. The analysis need the previous logItem to see if the previous is linked to the current one, in order to report only one error
	 * In Bonita, one Error generate a lot of ERROR message in the log.
	 * @param logItem
	 */
	public void analyse(LogItem logItem) {
		if (!enableAnalysis || logItem ==null)
			return;
		// 1. multiple logItem can be group as the same errors in fact (Bonita
		// produce the same error on multiple line).
		// in that circonstance, we keep only the first logItem
		if (!logItem.isError()) {
		  // is a previousLogItem is waiting ? So time to analysis it.
		  if (previousLogItem!=null)
		  {
		    doAnalysisError(previousLogItem);
		    previousLogItem=null;
		  }
			return;
		}
		
		// no previous error : let's register it, and wait for the next one
		if (previousLogItem==null)
		{
      previousLogItem = logItem;
      return;
		}
		
		// is the log item is attached to the previous one ?
		// the point is Bonita does not normalize the error message :-O and, for one error, log a lot of Error message.
		// how can we detect this is one Error ? 
		// 2 logs must be close in time
		// if they start by the same 2 first word, they shoud be the same error	
		// BUT if the content are identical, it should be a different error
		// 2019-04-23 07:56:18.095 +0200 org.apache.catalina.core.ContainerBase.[Catalina].[localhost].[/bonita].[CustomPageServlet] org.apache.catalina.core.StandardWrapperValve invoke 
		// 		  GRAVE: Servlet.service() for servlet [CustomPageServlet] in context with path [/bonita] threw exception [USERNAME=V004476 | Unable to find page with name: theme] with root cause
		//		  javax.servlet.ServletException: USERNAME=V004476 | Unable to find page with name: theme

		if (logItem.getTime() - previousLogItem.getTime() < 1000) 
		  if (logItem.getFirstWord(2).equals(previousLogItem.getFirstWord(2))) 
		    
		    // hum, let do a last think : if the 2 contents are completely indentical, this is a different error in fact !
		    if (! logItem.getHeader().equals(previousLogItem.getHeader())) {
		      // ok, consider the two are linked, register it, wait the next one.
		      previousLogItem.link(logItem);
		      return;
		    }
				
			
			// errors are too different, this is an another one
		
		// analysis the previous one, register the new one and wait
		doAnalysisError(previousLogItem);
		previousLogItem = logItem;
			

	}

	/**
	 * 
	 * @param nbTotalLines
	 */
	public void endAnalysis(long nbTotalLines) {
		if (previousLogItem != null)
		{
		  doAnalysisError(previousLogItem);
			previousLogItem=null;
		}
		analysisSynthese.nbTotalLines= nbTotalLines;
	}

	
	public void setTimeInMs( long timeAnalyseInMs)
	{
		analysisSynthese.timeAnalysisInms = timeAnalyseInMs;		
	}
	/**
	 * do an analysis on the item. Check is the 
	 * 
	 * @param logItem
	 */
	private void doAnalysisError(LogItem logItem) {
		if (logItem == null || ! logItem.isError())
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
	public LogAnalysisSynthese getSynthese() {
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

	public static class GraphRange {
		public String title;
		public long count;

		public GraphRange(String title, long count) {
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
		 * structure "rows": [ { c: [ { "v": "January" }," { "v": 19,"f": "42
		 * items" }, { "v": 12,"f": "Ony 12 items" }, ] }, { c: [ { "v":
		 * "January" }," { "v": 19,"f": "42 items" }, { "v": 12,"f": "Ony 12
		 * items" }, ] },
		 */
		String resultValue = "";

		for (int i = 0; i < listRange.size(); i++) {
			resultValue += "{\"c\":[{\"v\":\"" + listRange.get(i).title + "\"},{\"v\": " + listRange.get(i).count + "} ]},";

		}
		if (resultValue.length() > 0) {
			resultValue = resultValue.substring(0, resultValue.length() - 1);
		}

		final String resultLabel = "{ \"type\": \"string\", \"id\": \"whattime\", \"label\":\"whattime\" }," + "{ \"type\": \"number\", \"id\": \"value\", \"label\":\"Occurence\" }";

		final String valueChart = "	{" + "\"type\": \"ColumnChart\", " + "\"displayed\": true, " + "\"data\": {" + "\"cols\": [" + resultLabel + "], " + "\"rows\": [" + resultValue + "] "
		/*
		 * + "\"options\": { " + "\"bars\": \"horizontal\"," + "\"title\": \""
		 * +title+"\", \"fill\": 20, \"displayExactValues\": true," +
		 * "\"vAxis\": { \"title\": \"ms\", \"gridlines\": { \"count\": 100 } }"
		 */
				+ "}" + "}";
		// +"\"isStacked\": \"true\","

		// +"\"displayExactValues\": true,"
		//
		// +"\"hAxis\": { \"title\": \"Date\" }"
		// +"},"
		// logger.info("TrackRangeChart >>"+ valueChart+"<<");
		// String valueChartBar="{\"type\": \"BarChart\", \"displayed\": true,
		// \"data\": {\"cols\": [{ \"id\": \"perf\", \"label\": \"Perf\",
		// \"type\": \"string\" }, { \"id\": \"perfbase\", \"label\":
		// \"ValueBase\", \"type\": \"number\" },{ \"id\": \"perfvalue\",
		// \"label\": \"Value\", \"type\": \"number\" }], \"rows\": [{ \"c\": [
		// { \"v\": \"Write BonitaHome\" }, { \"v\": 550 }, { \"v\": 615 } ] },{
		// \"c\": [ { \"v\": \"Read BonitaHome\" }, { \"v\": 200 }, { \"v\": 246
		// } ] },{ \"c\": [ { \"v\": \"Read Medata\" }, { \"v\": 370 }, { \"v\":
		// 436 } ] },{ \"c\": [ { \"v\": \"Sql Request\" }, { \"v\": 190 }, {
		// \"v\": 213 } ] },{ \"c\": [ { \"v\": \"Deploy process\" }, { \"v\":
		// 40 }, { \"v\": 107 } ] },{ \"c\": [ { \"v\": \"Create 100 cases\" },
		// { \"v\": 3600 }, { \"v\": 16382 } ] },{ \"c\": [ { \"v\": \"Process
		// 100 cases\" }, { \"v\": 3700 }, { \"v\": 16469 } ] }]}, \"options\":
		// { \"bars\": \"horizontal\",\"title\": \"Performance Measure\",
		// \"fill\": 20, \"displayExactValues\": true,\"vAxis\": { \"title\":
		// \"ms\", \"gridlines\": { \"count\": 100 } }}}";

		return valueChart;
	}

}
