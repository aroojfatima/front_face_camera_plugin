window.show = function(str, callback) {
        cordova.exec(callback, function(err) {
            callback('Nothing to show.');
        }, "CameraCanvas", "show", [str]);
    };