'use strict';
/**
 *
 */

(function() {


var appCommand = angular.module('logmonitor', [ 'googlechart', 'ui.bootstrap']);






// --------------------------------------------------------------------------
//
// Controler Ping
//
// --------------------------------------------------------------------------

// Ping the server
appCommand.controller('LogControler',
	function ( $http, $scope, $interval ) {

	this.wait = false;
	this.listfileslog=[];
	this.listfilesselected={};
	this.timerInterval=30;
	this.refreshisrunning = false;
	 

	 
	this.getAllFilesLog = function ()
	{
		var self=this;
		self.wait = true;
		var d = new Date();
		$http.get( '?page=custompage_log&action=getFilesLog&t='+d.getTime() )
				.success( function ( jsonResult ) {
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
	
	
	this.display={ 'brutResult':false,  'numberperpage':100, 'pagenumber':1, 'totallines':0,
				'showLogs' : true,
				'showFilter' : true,
				'filterError':false,
				'filterText':'',
				'filterShortDate':true,
				'filterAutomaticRefresh':false,
				'filterTail': false,
				'enableAnalysisError':true,
				'brutResult' : false,
				'showLine' : false,
				'showDate' : true,
				'showLevel' : true,
				'showContent': true,
				'showLocalisation' : true,
				'showCompact' : false};
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
	this.callGetLog = function()
	{
		console.log("Start callGetLog");
		
		var self=this;
		self.wait = true;
		self.refreshisrunning=true;
		
		var json= encodeURI( angular.toJson(this.display, true));
		var d = new Date();
		
		
		$http.get( '?page=custompage_log&action=getLog&paramjson='+json+'&t='+d.getTime() )
				.success( function ( jsonResult ) {
					self.listLogItems 					= jsonResult.listLogItems;
					self.analysisSynthese               = jsonResult.analysisSynthese;
					// self.analysisTimeLine				= jsonResult.analysisTimeLine;
					$scope.analysisTimeLine		 		= JSON.parse(jsonResult.analysisTimeLine.graph);
					
					self.logFileName 					= jsonResult.logfilename;	
					self.display.completeLogFileName 	= jsonResult.completeLogFileName;	
					self.display.totalLines 			= jsonResult.totalLines;
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
		var line= logline.stl;
		if (this.display.showCompact)
			line = line + ";padding:1px;";
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
});




})();