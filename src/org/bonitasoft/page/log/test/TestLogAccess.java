package org.bonitasoft.page.log.test;

import org.bonitasoft.page.log.LogAccess;
import org.bonitasoft.page.log.LogAccess.LogInformation;
import org.bonitasoft.page.log.LogAccess.LogItem;
import org.bonitasoft.page.log.LogAccess.LogParameter;
import org.junit.Before;
import org.junit.Test;


public class TestLogAccess {

    @Before
    public void setUp() throws Exception {
    }

    @Test
    public void testTwoLines() {
        final LogParameter logParameters = new LogParameter();
        logParameters.completeFileName = "E:/dev/workspace/CustomPageLog/src/org/bonitasoft/page/log/test/bonita.2017-02-27.log";
        logParameters.completeFileName = "C:/atelier/BPM-SP-7.2.4.B/workspace/tomcat/logs/bonita.2017-02-22.log";
        logParameters.pageNumber = 1; // 968
        logParameters.numberPerPage = 300;
        logParameters.filterError = false;
        logParameters.filterShortDate = true;
        logParameters.filterTail = true;

        final LogInformation logInformation = LogAccess.getLog(logParameters);
        for (final Object logItemOb : logInformation.listLogs)
        {
            if (logItemOb instanceof LogItem)
            {
                final LogItem logItem = (LogItem) logItemOb;
                System.out.println(logItem.toString());
            }
        }

    }

    // @Test
    public void testOneLine() {
        final LogParameter logParameters = new LogParameter();
        logParameters.completeFileName = "E:/dev/workspace/CustomPageLog/src/org/bonitasoft/page/log/test/bonita.Test.log";
        final LogInformation logInformation = LogAccess.getLog(logParameters);
        for (final Object logItemOb : logInformation.listLogs)
        {
            if (logItemOb instanceof LogItem)
            {
                final LogItem logItem = (LogItem) logItemOb;
                System.out.println(logItem.toString());
            }
        }

    }

}
