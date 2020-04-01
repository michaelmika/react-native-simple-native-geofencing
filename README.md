
# react-native-simple-native-geofencing

**Attention: This repo is not yet fully tested !**

A native geofencing implementation for Android and iOS that allows to natively 
post notification when entering/exiting specified geofences and to fire a 
react-native/javascript asynchronous function when leaving a specific monitoring
geofence (see at the bottom of this README). This can be useful to trigger calculations
of new Geofences when leaving one area.
## Restrictions

### iOS
Needs `iOS 10` or higher

### Android
Needs a `minSdkVersion` of 19.

## Getting started

`$ npm install react-native-simple-native-geofencing --save`

### Mostly automatic installation

`$ react-native link react-native-simple-native-geofencing`

#### iOS only:

**Attention**

For the module to work at least one `.Swift` file and a `Bridging-Header.h` must be added in the xcode project. Simply create an empty Swift File and Xcode will ask you if you wish to add a `Bridging-Header.h` to your project. Both Files can be empty.

For background updates you have to activate this in your Xcode Project under: `Project ` ➜ `Capabilities` ➜ `Background Modes` with at least `Location updates` !

### Manual installation

**Not recommended**
#### iOS

1. In XCode, in the project navigator, right click `Libraries` ➜ `Add Files to [your project's name]`
2. Go to `node_modules` ➜ `react-native-simple-native-geofencing` and add `RNSimpleNativeGeofencing.xcodeproj`
3. In XCode, in the project navigator, select your project. Add `libRNSimpleNativeGeofencing.a` to your project's `Build Phases` ➜ `Link Binary With Libraries`
4. Run your project (`Cmd+R`)<

#### Android

1. Open up `android/app/src/main/java/[...]/MainActivity.java`
  - Add `import com.simplegeofencing.reactnative.RNSimpleNativeGeofencingPackage;` to the imports at the top of the file
  - Add `new RNSimpleNativeGeofencingPackage()` to the list returned by the `getPackages()` method
2. Append the following lines to `android/settings.gradle`:
  	```
  	include ':react-native-simple-native-geofencing'
  	project(':react-native-simple-native-geofencing').projectDir = new File(rootProject.projectDir, 	'../node_modules/react-native-simple-native-geofencing/android')
  	```
3. Insert the following lines inside the dependencies block in `android/app/build.gradle`:
  	```
      compile project(':react-native-simple-native-geofencing')
  	```
### Permissions

### iOS

The framework itself asks for permissions for the notifications and the location. However, best practice would be to ask for permission from the user in React Native part. 

The following changes must be in the `Info.plist` :

```...
<key>NSLocationAlwaysAndWhenInUseUsageDescription</key>
<string>bla bla</string>
<key>NSLocationWhenInUseUsageDescription</key>
<string>bla bla 2</string>
<key>UIBackgroundModes</key>
<array>
<string>location</string>
</array>
...
```
#### Android
Edit AndroidManifest.xml and add the following permission and ServiceIntent:
```xml
<manifest ...>
    ...
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
    <application
        ...
        android:allowBackup="true">
        <receiver android:name="com.simplegeofencing.reactnative.GeofenceTransitionsBroadcastReceiver" />
        <service android:name="com.simplegeofencing.reactnative.ShowTimeoutNotification" />
    <application/>
</manifest>
```
If you want to use a ***Monitoring Geofence***, that fires a react-native async function when leaving this geofence, you need to add the following as well:
```xml
<manifest ...>
    ...
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <application ...>
        ...
        <service android:name="com.simplegeofencing.reactnative.MonitorUpdateService"/>
    <application/>
</manifest>
```

The App also needs to ask for permission in react-native ***since Android 6.0 (API level 23)***. 
You can fire a function like this in the App's ``componentWillMount`` hook:
```javascript
import {PermissionsAndroid} from 'react-native';
async function requestLocationPermission() {
  try {
    const granted = await PermissionsAndroid.request(
        PermissionsAndroid.PERMISSIONS.ACCESS_FINE_LOCATION,
        {
          'title': 'Location permission',
          'message': 'Needed obviously'
        }
    )
    if (granted === PermissionsAndroid.RESULTS.GRANTED) {
      console.log("Granted Permission")
    } else {
      console.log("Denied Permission")
    }
  } catch (err) {
    console.warn(err)
  }
}
```
## Usage
```javascript
import RNSimpleNativeGeofencing from 'react-native-simple-native-geofencing';

export default class App extends Component {
    componentWillMount() {
        //see above
        if(Platform.OS === 'android'){
            requestLocationPermission();       
        }
    }
    componentDidMount(){
        //set up Notifications
         RNSimpleNativeGeofencing.initNotification(
            {
              channel: {
                title: "Message Channel Title",
                description: "Message Channel Description"
              },
              start: {
                notify: true,
                title: "Start Tracking",
                description: "You are now tracked"
              },
              stop: {
                notify: true,
                title: "Stopped Tracking",
                description: "You are not tracked any longer"
              },
              enter: {
                notify: true,
                title: "Attention",
                //[value] will be replaced ob geofences' value attribute
                description: "You entered a [value] Zone"
              },
              exit: {
                notify: true,
                title: "Left Zone",
                description: "You left a [value] Zone"
              }
            }
         );
    }
    fail(){
        console.log("Fail to start geofencing")
    }
    startMonitoring(){
        let geofences = [
          {
            key: "geoNum1",
            latitude: 38.9204,
            longitude: -77.0175,
            radius: 200,
            value: "yellow"
          },
          {
            key: "geoNum2",
            latitude: 38.9248,
            longitude: -77.0258,
            radius: 100,
            value: "green"
          },
          {
            key: "geoNum3",
            latitude: 47.423,
            longitude: -122.084,
            radius: 150,
            value: "red"
          }
        ];
        RNSimpleNativeGeofencing.addGeofences(geofences, 3000000, this.fail);
    }
    
    stopMonitoring(){
        RNSimpleNativeGeofencing.removeAllGeofences();
    }
}
```
### Methods
| method      | arguments | notes |
| ----------- | ----------- | ----------- |
| `initNotification` | `settings`: SettingObject | Initializes the notification strings and when they should appear **(required)**|
| `addGeofence` | `geofence`: GeofenceObject, `duration`: number | Adds one geofence to the native geofence list *(optional)* |
| `addGeofences` | `geofencesArray`: Array<GeofenceObject>, `duration`: number, `failCallback`: function | Adds a list of geofences, a Geofence for monitoring and starts monitoring **(required)** |
| `removeAllGeofences` |  | Removes all geofences and stops monitoring *(optional)* |
| `updateGeofences` | `geofencesArray`: Array<GeofenceObject>, `duration`: number | Deletes Geofences and adds the new ones without notifications *(optional)*|
| `removeGeofence` |  `geofenceKey`: String| Removes a specific geofence *(optional)* |
| `startMonitoring` | `failCallback`: function | Start monitoring *(optional)* |
| `stopMonitoring` | | Stop monitoring *(optional)* |

The function `monitoringCallback()` gets fired with the parameter of the remaining duration.
`duration` is in millisec. 
### Types
```
type GeofenceObject {
  key: string,
  latitude: Number,
  longitude: Number,
  radius: Number,
  value: String
}
``` 

MonitoringGeofenceObject is item of  `geofencesArray` with `key: "monitor"`:
```
type MonitoringGeofenceObject {
  key: "monitor",
  latitude: Number,
  longitude: Number,
  radius: Number
}
```
```
type SettingObject {
  start: {
    notify: boolean,    // If Notification should be fired on start tracking
    title: string,      // Title of Notification
    description: string // Content of Notification
  },
  stop: {
    notify: boolean,
    title: string,
    description: string
  },
  timeout: {            // automatic stop by end of duration 
    notify: boolean,
    title: string,
    description: string
  },
  enter: {
    notify: boolean,
    title: string,
    description: string
  },
  exit: {
    notify: boolean,
    title: string,
    description: string
  },
  channel: {            // Only Android specific
    title: string,
    description: string
  }
}
```
The string ``[value]`` is automatically replaced by the GeofenceObject's value string in enter & exit's notification's title & description/content 

### Monitoring Geofences
With this module you can also define a Monitoring Geofence that allows you to fire a react-native / javascript
function when leaving. Therefore include this geofence in your List with the key `"monitor"` 
(see `MonitoringGeofenceObject` above) . The implementation of the function depends on Android or iOS.

#### iOS

If the app is in the background and a geofence trigger, the app's React Part will also start in the background. With a `NativeEventEmitter` a corresponding event can be intercepted.

```
import {... , NativeEventEmitter} from 'react-native';
...
componentWillMount(){ 
    const myModuleEvt = new NativeEventEmitter(RNSimpleNativeGeofencing);
        let subscription = myModuleEvt.addListener(
        'leftMonitoringBorderWithDuration',
        (result) => {
        
            //result is a Object with the remaining 
            //duration of the activ geofences & a boolean
            //for leaving or entering the monitoring boarder
        
            console.log("Event :");
            console.log(JSON.stringify(result, null, 2));
        }
    );
}
```

#### Android
In Android this implementation uses Headless JS, which allows you to run javascript code in 
the background (see [Headless JS Docs](https://facebook.github.io/react-native/docs/headless-js-android)).

Therefore, you have to define a task as an asynchronous function with the name `leftMonitoringBorderWithDuration`
```javascript
import React from 'react';
import RNSimpleNativeGeofencing from "react-native-simple-native-geofencing";
module.exports = async (taskData) => {
    //taskData.remainingTime tells you the remaining time of the geofencing
    // so you can reuse it to update yours
    console.log(taskData);
    // do stuff
    RNSimpleNativeGeofencing.updateGeofences(
        newGeofencesArray,
        taskData.remainingTime
    );
};
```

This tasks needs to be registered in your `index.js` of your App:
```javascript
import {AppRegistry} from 'react-native';
import App from './App';
import {name as appName} from './app.json';

// Your App
AppRegistry.registerComponent(appName, () => App);
// Your defined task (see above)
AppRegistry.registerHeadlessTask('leftMonitoringBorderWithDuration', () => require('./leftMonitoringBorderWithDuration'));
```

# License
This code is under [MIT license](https://opensource.org/licenses/MIT) and opened for contribution! Authors are Fabian Puch and Michael Raring.

Contribution by cru3lgenius. Thanks! <3
