cordova.define("cordova/plugin/cameraPluginVar", function(require, exports, module) {
    cameraPluginVar = function(){
        var _orientation = 'landscape';
        var _obj = null;
        var _context = null;
        var _camImage = null;

        var _x = 0;
        var _y = 0;
        var _width = 0;
        var _height = 0;
    };
	cameraPluginVar.initialize = function(obj) {
        var _this = this;
        this._obj = obj;

        this._context = obj.getContext("2d");

        this._camImage = new Image();

        this._camImage.onload = function() {
            _this._context.clearRect(0, 0, _this._width, _this._height);
            if (window.orientation == 90
               || window.orientation == -90)
            {
                _this._context.save();
                // rotate 90
                _this._context.translate(_this._width/2, _this._height/2);
                _this._context.rotate((90 - window.orientation) *Math.PI/180);
                _this._context.drawImage(_this._camImage, 0, 0, 352, 288, -_this._width/2, -_this._height/2, _this._width, _this._height);
                //
                _this._context.restore();
            }
            else
            {
                _this._context.save();
                // rotate 90
                _this._context.translate(_this._width/2, _this._height/2);
                _this._context.rotate((90 - window.orientation)*Math.PI/180);
                _this._context.drawImage(_this._camImage, 0, 0, 352, 288, -_this._height/2, -_this._width/2, _this._height, _this._width);
                //
                _this._context.restore();
            }
        };
        // register orientation change event
        window.addEventListener('orientationchange', this.doOrientationChange);
        this.doOrientationChange();
    };

  cameraPluginVar.doOrientationChange = function() {
        switch(window.orientation)
        {
            case -90:
            case 90:
                this._orientation = 'landscape';
                break;
            default:
                this._orientation = 'portrait';
                break;
        }

        var windowWidth = window.innerWidth;
        var windowHeight = window.innerHeight;
        var pixelRatio = window.devicePixelRatio || 1; /// get pixel ratio of device


        this._obj.width = windowWidth;// * pixelRatio;   /// resolution of canvas
        this._obj.height = windowHeight;// * pixelRatio;


        this._obj.style.width = windowWidth + 'px';   /// CSS size of canvas
        this._obj.style.height = windowHeight + 'px';


        this._x = 0;
        this._y = 0;
        this._width = windowWidth;
        this._height = windowHeight;
    };

	cameraPluginVar.show = function(message, successCallback, errorCallback)  
	{  
       cordova.exec(successCallback, errorCallback, "CameraCanvas", "show", [message]);
	};
	cameraPluginVar.start = function(option)  
	{  
       cordova.exec(false, false, "CameraCanvas", "start", [option]);
	};
	cameraPluginVar.capture = function(data) {
        this._camImage.src = data;
    };

	cameraPluginVar.takePicture = function(onsuccess) { alert("here in takePicture js");
        cordova.exec(onsuccess, function(){}, "CameraCanvas", "onTakePicture", []);
    };

	module.exports = cameraPluginVar;
});