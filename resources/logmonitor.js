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
	this.timerInterval=30;
	this.refreshisrunning = false;
	 

	 
	this.getAllFilesLog = function ()
	{
		var self=this;
		self.wait = true;

		$http.get( '?page=custompage_log&action=getFilesLog' )
				.success( function ( jsonResult ) {
					self.listfileslog 	= jsonResult.listfileslog;
					self.logpath 		= jsonResult.logpath;
					self.wait = false;

				})
				.error( function() {
					self.wait = false;
					alert('an error occure');
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
				'brutResult' : false,
				'showLine' : false,
				'showDate' : true,
				'showLevel' : true,
				'showContent': true,
				'showLocalisation' : true };
	this.listLogItems=[];
	
	this.getPageResult = function()
	{
		return this.listLogItems;
	}
	
	// ---------------------- file log
	this.getFileLog = function( logfile )
	{
		console.log("Start refresh");
		
		this.refreshisrunning=true;
		var self=this;
		self.wait = true;
		this.display.fileName = logfile;
		var json = angular.toJson(this.display, false);
		
		$http.get( '?page=custompage_log&action=getLog&json='+json )
				.success( function ( jsonResult ) {
						self.listLogItems 		= jsonResult.listLogItems;
						self.logFileName 		= jsonResult.logfilename;	
						self.display.completeLogFileName 		= jsonResult.completeLogFileName;	
						self.display.totallines 		= jsonResult.totalLines;
						self.wait = false;
						self.refreshisrunning=false;
						<!-- document.getElementById('countdowntimer').addCDSeconds( self.timerInterval); -->

				})
				.error( function() {
					self.wait = false;
					self.refreshisrunning=false;
					<!--  document.getElementById('countdowntimer').addCDSeconds( self.timerInterval); -->
					alert('an error occure');
					});
	}

	this.refresh=function()
	{
		this.getFileLog( this.logFileName );
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
	
	


});



})();