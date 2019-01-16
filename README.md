
# react-native-simple-native-geofencing

**Attention: This repo is not yet fully implemented and stable!**

## Getting started

`$ npm install react-native-simple-native-geofencing --save`

### Mostly automatic installation

`$ react-native link react-native-simple-native-geofencing`

### Manual installation


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
#### Android
Edit AndroidManifest.xml and add the following permission and ServiceIntent:
```
<manifest ...>
    <uses-permission android:name="android.permission.ACCESS_FINE_LOCATION"/>
    <application
        ...
        android:allowBackup="true">
        <service android:name="com.simplegeofencing.reactnative.GeofenceTransitionsIntentService"/>
    <application/>
</manifest>
```

## Usage
```javascript
import RNSimpleNativeGeofencing from 'react-native-simple-native-geofencing';

// TODO: What to do with the module?
RNSimpleNativeGeofencing.addGeofence(geofence, duration);
```
### Methods
| method      | arguments | notes |
| ----------- | ----------- | ----------- |
| `initNotification` | `settings`: SettingObject | Initializes the notification strings and when they should appear|
| `addGeofence` | `geofence`: GeofenceObject, `duration`: number | Adds one geofence to the native geofence list |
| `addGeofences` | `geofencesArray`: Array<GeofenceObject>, `monitoringGeofence`: MonitoringGeofenceObject, `duration`: number, `monitoringCallback`: function | Adds a list of geofences, a Geofence for monitoring and starts monitoring |
| `updateGeofences` | `geofencesArray`: Array<GeofenceObject>, `monitoringGeofence`: MonitoringGeofenceObject, `monitoringCallback`: function | Deletes Geofences and adds the new ones with the remaining duration |
| `addMonitoringBorder` | `geofence`: MonitoringGeofenceObject, `duration`: number, `callback`: function | Adds a MonitoringBorder which is a Geofence used when to update|
| `removeMonitoringBorder` | | Removes the MonitoringBorder and stops monitoring |
| `removeAllGeofences` |  | Removes all geofences and stops monitoring |
| `removeGeofence` |  `geofenceKey`: String| Removes a specific geofence |
| `startMonitoring` | | Start monitoring |
| `stopMonitoring` | | Stop monitoring |

### Types
```
type GeofenceObject {
  key: string,
  latitude: Number,
  longitude: Number,
  radius: Number,
  value: Number
}
```
```
type MonitoringGeofenceObject {
  key: string,
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
  