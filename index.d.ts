// Type definitions for react-native-simple-native-geofencing@0.8.0
// Based on the README.md's usage section
// Template: https://www.typescriptlang.org/docs/handbook/declaration-files/templates/module-d-ts.html

type DangerGeofence = {
    key: string,
    latitude: number,
    longitude: number,
    radius: number,
    value: string
}
type MonitoringGeofence = {
    key: 'monitor',
    latitude: number,
    longitude: number,
    radius: number
}
export type Geofence = DangerGeofence | MonitoringGeofence;

export type InitNotificationsSettings = {
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

export function initNotification(settings: InitNotificationsSettings): any;
export function addGeofence(geofence: Geofence, duration: number): any;
export function addGeofences(geofences: Geofence[], duration: number, failCallback: function): any;
export function removeAllGeofences(successCallback: function): any;
export function updateGeofences(geofences: Geofence[], duration: number): any;
export function removeGeofence(key: string): any;
export function startMonitoring(failCallback: function): any;
export function stopMonitoring(): any;