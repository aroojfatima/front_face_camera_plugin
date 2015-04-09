package com.cameraPlugin;

import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CameraCanvas extends CordovaPlugin{
	 private int _quality = 85;
	    private int _destType = DestinationTypeDataURL;
	    private boolean _allowEdit = false;
	    private int _encodeType = EncodingTypeJPEG;
	    private boolean _saveToPhotoAlbum = false;
	    private boolean _correctOrientation = true;

	    private int _width = 640;
	    private int _height = 480;

	@Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals("show")) {
            String message = args.getString(0);
            this.show(message, callbackContext);
            return true;
        }
        return false;
    }

    private void show(String message, CallbackContext callbackContext) {
        if (message != null && message.length() > 0) {
            callbackContext.success(message);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
    }
    private void start(JSONArray args, CallbackContext callbackContext) {
    	   _quality = 85;
           _destType = DestinationTypeFileURI;
           _encodeType = EncodingTypeJPEG;
           _saveToPhotoAlbum = false;
           _correctOrientation = true;
           _width = 640;
           _height = 480;

           // parse options
           if (args.length() > 0)
           {
               try {
                   JSONObject jsonData = args.getJSONObject(0);
                   getOptions(jsonData);
               } catch (Exception e) {
                   Log.d("CanvasCamera", "Parsing options error : " + e.getMessage());
               }
           }
        if (args != null && args.length() > 0) {
            callbackContext.success(args);
        } else {
            callbackContext.error("Expected one non-empty string argument.");
        }
        Intent intent = new Intent(this.cordova.getActivity(), CameraCanvasView.class);
		this.cordova.startActivityForResult(this, intent, CANVAS_CAMERA);
    }
}