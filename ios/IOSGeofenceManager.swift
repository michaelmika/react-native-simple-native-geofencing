//
//  IOSGeofenceManager.swift
//  RNSimpleNativeGeofencing
//
//  Created by Fabian Puch on 13.01.19.
//  Copyright © 2019 Facebook. All rights reserved.
//

import Foundation
import CoreLocation
import UserNotifications

@objc(IOSGeofenceManager)
class IOSGeofenceManager: NSObject, CLLocationManagerDelegate, UNUserNotificationCenterDelegate {
    
    
    //MARK: - Init / Setup
    
    static let sharedInstance = IOSGeofenceManager()
    
    let locationManager = CLLocationManager()
    var notificationCenter: UNUserNotificationCenter!
    
    var currentActiveGeofences : [CLCircularRegion] = []
    var currentGeofences: [CLCircularRegion] = []
    
    var didEnterTitle = ""
    var didEnterBody = ""
    var didExitTitle = ""
    var didExitBody = ""
    var startTrackingTitle = ""
    var startTrackingBody = ""
    var stopTrackingTitle = ""
    var stopTrackingBody = ""
    
    
    override init() {
        super.init()
        
        self.locationManager.delegate = self
        self.locationManager.requestAlwaysAuthorization()
        
        self.notificationCenter = UNUserNotificationCenter.current()
        
        notificationCenter.delegate = self
        
    }
    
    
    //MARK: -  Public Interface
    
    
    public func initNotification(didEnterTitle:String?,
                                 didEnterBody:String?,
                                 didExitTitle:String?,
                                 didExitBody:String?,
                                 startTrackingTitle:String?,
                                 startTrackingBody:String?,
                                 stopTrackingTitle:String?,
                                 stopTrackingBody:String?)
    {
        
        DispatchQueue.main.async {
            
            self.didEnterTitle = didEnterTitle ?? "Be careful!"
            self.didEnterBody = didEnterBody ?? "It may be dangerous in the area where you are currently staying."
            self.didExitTitle = didExitTitle ?? ""
            self.didExitBody = didExitBody ?? ""
            self.startTrackingTitle = startTrackingTitle ?? ""
            self.startTrackingBody = startTrackingBody ?? ""
            self.stopTrackingTitle = stopTrackingTitle ?? ""
            self.stopTrackingBody = stopTrackingBody ?? ""
       
        }
        
    }
    
    
    public func addSmallGeofence(lat: Double,
                                 log: Double,
                                 radius: Int,
                                 id: String){
        
        DispatchQueue.main.async {
        
            let geofenceRegionCenter = CLLocationCoordinate2D(
                latitude: lat,
                longitude: log
            )
            
            let geofenceRegion = CLCircularRegion(
                center: geofenceRegionCenter,
                radius: CLLocationDistance(radius),
                identifier: id
            )
            
            geofenceRegion.notifyOnEntry = true
            geofenceRegion.notifyOnExit = false
            
            self.currentGeofences.append(geofenceRegion)
            
        }
    }
    
    
    public func startMonitoringAllGeofences(){
        
        DispatchQueue.main.async {
        
            for geo in self.currentGeofences {
                
                self.locationManager.startMonitoring(for: geo)
                self.currentActiveGeofences.append(geo)
                
            }
            
            self.currentGeofences = []
            notifyStart(started: true)
            
        }
    }
    
    public func stopMonitoringAllGeofences(){
        
        DispatchQueue.main.async {
        
            for geo in self.currentActiveGeofences {
                
                self.locationManager.stopMonitoring(for: geo)
                
            }
            
            self.currentActiveGeofences = []
            notifyStart(started: false)
            
        }
    }
    
    
    
    
    //MARK: - Setup Notifications
    
    private func handleEvent(region: CLRegion!, didEnter: Bool) {
        
        let content = UNMutableNotificationContent()
        content.sound = UNNotificationSound.default
        
        
        if didEnter {
            content.title = self.didEnterTitle
            content.body = self.didEnterBody
        }else{
            content.title = self.didExitTitle
            content.body = self.didExitBody
        }
        
        
        let timeInSeconds: TimeInterval = 0.1
        
        let trigger = UNTimeIntervalNotificationTrigger(
            timeInterval: timeInSeconds,
            repeats: false
        )
        
        let identifier = region.identifier
        
        let request = UNNotificationRequest(
            identifier: identifier,
            content: content,
            trigger: trigger
        )
        
        notificationCenter.add(request, withCompletionHandler: { (error) in
            if error != nil {
                print("Error adding notification with identifier: \(identifier)")
            }
        })
    }
    
    
    private func notifyStart(started: Bool) {
        
        let content = UNMutableNotificationContent()
        content.sound = UNNotificationSound.default
        
        
        if started {
            content.title = self.startTrackingTitle
            content.body = self.startTrackingBody
        }else{
            content.title = self.stopTrackingTitle
            content.body = self.startTrackingBody
        }
        
        
        let timeInSeconds: TimeInterval = 0.1
        
        let trigger = UNTimeIntervalNotificationTrigger(
            timeInterval: timeInSeconds,
            repeats: false
        )
        
        let identifier = self.randomString(length: 20)
        
        let request = UNNotificationRequest(
            identifier: identifier,
            content: content,
            trigger: trigger
        )
        
        notificationCenter.add(request, withCompletionHandler: { (error) in
            if error != nil {
                print("Error adding notification with identifier: \(identifier)")
            }
        })
    }
    
    
    
    
    //MARK: - Location Delegate Methodes
    
    func locationManager(_ manager: CLLocationManager, didEnterRegion region: CLRegion) {
        if region is CLCircularRegion {
            
            self.handleEvent(region:region, didEnter: true)
            
        }
    }
    
    func locationManager(_ manager: CLLocationManager, didExitRegion region: CLRegion) {
        if region is CLCircularRegion {
            
            self.handleEvent(region:region, didEnter: false)
            
        }
    }
    
    func locationManager(_ manager: CLLocationManager, didChangeAuthorization status: CLAuthorizationStatus) {
        if status != .authorizedAlways {
            print("Geofence will not Work, because of missing Authorization")
        }
    }
    
    
    
    //MARK: - Notification Delegate Methodes
    
    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        // when app is onpen and in foregroud
        completionHandler(.alert)
    }
    
    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse, withCompletionHandler completionHandler: @escaping () -> Void) {
        
        // get the notification identifier to respond accordingly
        let identifier = response.notification.request.identifier
        
        // do what you need to do
        print(identifier)
        // ...
    }
    
    
    
    
    
    //MARK: - helper Functions
    
    func randomString(length: Int) -> String {
        let letters = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
        return String((0...length-1).map{ _ in letters.randomElement()! })
    }
    
    
}
