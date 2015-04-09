module.exports = {
    greet: function (name, successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "CameraCanvas", "show", [name]);
    }
};