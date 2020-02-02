
Pod::Spec.new do |s|
  s.name         = "RNSimpleNativeGeofencing"
  s.version      = "1.0.0"
  s.summary      = "RNSimpleNativeGeofencing"
  s.description  = <<-DESC
                  RNSimpleNativeGeofencing
                   DESC
  s.homepage     = "https://github.com/michaelmika/react-native-simple-native-geofencing"
  s.license      = "MIT"
  # s.license      = { :type => "MIT", :file => "FILE_LICENSE" }
  s.author             = { "author" => "author@domain.cn" }
  s.platform     = :ios, "7.0"
  s.source       = { :git => "https://github.com/author/RNSimpleNativeGeofencing.git", :tag => "master" }
  s.source_files  = "RNSimpleNativeGeofencing/**/*.{h,m}"
  s.requires_arc = true


  s.dependency "React"
  #s.dependency "others"

end

  
