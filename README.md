
# react-native-simple-native-geofencing

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
  - Add `import com.simnplegeofencing.reactnative.RNSimpleNativeGeofencingPackage;` to the imports at the top of the file
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


## Usage
```javascript
import RNSimpleNativeGeofencing from 'react-native-simple-native-geofencing';

// TODO: What to do with the module?
RNSimpleNativeGeofencing;
```
  