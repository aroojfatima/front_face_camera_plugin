var cameraPlugin = function(){};  
cameraPlugin.prototype.show = function(message, successCallback, errorCallback)  
{  
       cordova.exec(successCallback, errorCallback, "CameraCanvas", "show", message);
}