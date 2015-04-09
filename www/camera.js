cordova.define("cordova/plugin/cameraPluginVar", function(require, exports, module) {
    cameraPluginVar = {};
	cameraPluginVar.show = function(message, successCallback, errorCallback)  
	{  
       cordova.exec(successCallback, errorCallback, "CameraCanvas", "show", [message]);
	}
	module.exports = cameraPluginVar;
});