'use strict';
/**
 *
 */

(function() {


var appCommand = angular.module('logmonitor', [ 'googlechart', 'ui.bootstrap','ngCookies']);






// --------------------------------------------------------------------------
//
// Controler Ping
//
// --------------------------------------------------------------------------

// Ping the server
appCommand.controller('LogControler',
	function ( $http, $scope, $interval, $cookies ) {

	this.wait = false;
	this.listfileslog=[];
	this.listfilesselected={};
	this.timerInterval=30;
	this.refreshisrunning = false;
	 
	this.navbaractiv='logContent';
	
	this.getNavClass = function( tabtodisplay )
	{
		if (this.navbaractiv === tabtodisplay)
			return 'ng-isolate-scope active';
		return 'ng-isolate-scope';
	}

	this.getNavStyle = function( tabtodisplay )
	{
		console.log("getNavStyle: navbar="+this.navbaractiv+" - tabtodisplay="+tabtodisplay);
		if (this.navbaractiv === tabtodisplay)
			return 'border: 1px solid #c2c2c2;border-bottom-color: transparent;';
		return 'background-color:#cbcbcb';
	}

	this.getHttpConfig = function () {
		var additionalHeaders = {};
		var csrfToken = $cookies.get('X-Bonita-API-Token');
		if (csrfToken) {
			additionalHeaders ['X-Bonita-API-Token'] = csrfToken;
		}
		var config= {"headers": additionalHeaders};
		console.log("GetHttpConfig : "+angular.toJson( config));
		return config;
	}
	
	
	this.getAllFilesLog = function ()
	{
		var self=this;
		self.wait = true;
		var d = new Date();
		$http.get( '?page=custompage_log&action=getFilesLog&t='+d.getTime(), this.getHttpConfig() )
				.success( function ( jsonResult, statusHttp, headers, config ) {					
					// connection is lost ?
					if (statusHttp==401 || typeof jsonResult === 'string') {
						console.log("Redirected to the login page !");
						window.location.reload();
					}
					self.listfileslog 	= jsonResult.listfileslog;
					self.logpath 		= jsonResult.logpath;
					self.wait = false;

				})
				.error( function() {
					self.wait = false;
					// alert('an error occure');
					});
	}
	this.getAllFilesLog();
	
	
	this.display={ 'brutResult':false,  'numberperpage': '100', 'pagenumber':1, 'totallines':0,
				'showLogs' : true,
				'showFilter' : true,
				'filterError':false,
				'filterText':'',
				'filterShortDate':true,
				'filterAutomaticRefresh':false,
				'filterTail': true,				
				'brutResult' : false,
				'showLine' : false,
				'showDate' : true,
				'showLevel' : true,
				'showContent': true,
				'showLocalisation' : true,
				'showCompact' : false,
				'showAllLines' : true,
				'analyze' : {
					'enableAnalysisError':true,
					'analysisCompactBasedOnError':true,
					'perimeter':'ERROR',
					'policy' :'ALL'
							}
				};
	this.listLogItems=[];
	
	this.getPageResult = function()
	{
		return this.listLogItems;
	}
	
	// ---------------------- file log
	this.getCurrentLog = function ()
	{
		console.log("Start currentLog");
		this.display.fileName ="";
		this.display.pathName ="";
		this.callGetLog();
	}
	
	
	this.getLog = function( logfile )
	{
		this.display.fileName = logfile.fileName;
		this.display.pathName = logfile.pathName;
		this.callGetLog();
	}
	this.getClassLogFile = function(logfile)
	{
		// console.log("getClassLogFile file name=["+this.logFileName+"] - logFile=["+logfile+"]");
		if(this.logFileName===logfile){
			return "btn btn-success btn-xs";
		}
		else{
			return "btn btn-info btn-xs"
		}
	}
	this.callGetLog = function()
	{
		console.log("Start callGetLog");
		
		var self=this;
		self.wait = true;
		self.refreshisrunning=true;
		
		var json= encodeURI( angular.toJson(this.display, true));
		var d = new Date();
		
		
		$http.get( '?page=custompage_log&action=getLog&paramjson='+json+'&t='+d.getTime(), this.getHttpConfig() )
				.success( function ( jsonResult, statusHttp, headers, config ) {
					
					// connection is lost ?
					if (statusHttp==401 || typeof jsonResult === 'string') {
						console.log("Redirected to the login page !");
						window.location.reload();
					}
					self.listLogItems 					= jsonResult.listLogItems;
					self.analysisSynthese               = jsonResult.analysisSynthese;
					// self.analysisTimeLine				= jsonResult.analysisTimeLine;
					$scope.analysisTimeLine		 		= JSON.parse(jsonResult.analysisTimeLine.graph);
					
					self.logFileName 					= jsonResult.logfilename;	
					self.display.completeLogFileName 	= jsonResult.completeLogFileName;	
					self.display.totalLines 			= jsonResult.totalLines;
					self.display.totalLinesFiles		= jsonResult.totalLinesFiles;
					self.wait 							= false;
					self.refreshisrunning				= false;	
					// close the logs file panel
					self.display.showLogs = false;

				})
				.error( function() {
					self.wait = false;
					self.refreshisrunning=false;
					// alert('an error occure');
					});
	}

	this.refresh=function()
	{
		console.log("Refresh");
		this.callGetLog( this.logFileName );
	}
	this.refreshAnalyse=function()
	{
		console.log("Refresh");
		this.callGetLog( this.logFileName );
	}

	
	// auto refresh
	this.callAtInterval = function() {
		 console.log("Interval occurred automatic["+this.display.filterAutomaticRefresh+"] refreshisrunning["+this.refreshisrunning+"]");
		 if(this.display.filterAutomaticRefresh && ! this.refreshisrunning) {
				this.refreshisrunning = true;
				this.refresh();
		}
	}
	
	this.initInterval = function()
	{
		console.log("Set interval");
		var self=this;
		$interval( function(){ self.callAtInterval(); }, 30000);
	}
	this.initInterval();
	
	/**
	 * return the style of the line. If the compact display is required, add a short padding
	 */
	this.getLineStyle = function ( logline )
	{
		var line= logline.stl+";vertical-align:top";
		if (this.display.showCompact)
			line = line + ";padding:1px;";
		else
			line = line + ";padding:5px;";
		return line;
	}

	/**
	 * download the files
	 */
	this.getDowloadFile = function()
	{
		var listToDowload={ "listdaysdownload": []};
		for ( var i in this.listfilesselected)
			{
			if (this.listfilesselected[i] )
				listToDowload.listdaysdownload.push(  i );
			}
		if (listToDowload.length==0)
		{
			
			return;			
		}

		var json= encodeURI( angular.toJson(listToDowload, true));
		return json;
	};
	this.getCurrentLog(); // First initialisation display current log
});




})();