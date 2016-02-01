Zhehao Wang <zhehao@cs.ucla.edu> Haitao Zhang <zhtaoxiang@gmail.com> Wang Yang <xxyangwang@gmail.com>

NDNFit Android application based on professor Jung's work. Did not fork as we don't have access to the original work on BitBucket.

Jan 20, 2015 - v0.1

### What it does:
* Capture time-location data (Use NETWORK\_PROVIDER instead of GPS\_PROVIDER for GPS for now)
* Use the identity provided by <a href = "https://github.com/zhehaowang/android-identity-manager/releases"> Android Identity Manager </a> to sign the captured data
* "Upload" captured data to the DSU running on remap server memoria.ndn.ucla.edu (the server side program is under <root>/DSUSync directory)

### How to use:
* run NFD (the latest version, <a href = "https://github.com/named-data/nfd">here</a>, route should be configured correcly based on the specific network environment), Repo-ng (use the customized version, <a href = "https://github.com/zhtaoxiang/repo-ng-for-ndnfit">here</a>. In repo-ng.conf file, tcp_bulk_insert must be enabled, and data and command prefixes should be configured correcly based on the specific network environment) and DSUSync (<a href = "https://github.com/zhehaowang/ndnfit/releases">download</a>, fix this link after merge to master branch) on server side. If you want to compile the DSUSync by your self, do the following steps
  * Check out ndn-cxx <a href = "https://github.com/named-data/ndn-cxx">here</a>
  * Move <root>/DSUSync/DSUSync.cpp to the examples directory under ndn-cxx
  * <a href = "https://code.google.com/archive/p/rapidjson/downloads">Download</a> rapidjson (rapidjson-0.11.zip) and follow readme to install it
  * Follow <a href = "https://github.com/cawka/ndn-cxx/blob/master/docs/INSTALL.rst">/docs/INSTALL.rst</a> of ndn-cxx to compile the code. Notice that you much use "./waf configure" to configure it
  * After run "./waf configure", "./waf" and "sudo ./waf install", run "./waf configure --with-examples", "./waf" again, the executable file will be under build/examples directory
* Install ndnfit, identity manager, and nfd-android on Android device
  * Ndnfit: <a href = "https://github.com/zhehaowang/ndnfit/releases">download</a>
  * Identity Manager: <a href = "https://github.com/zhehaowang/android-identity-manager/releases">download</a>
  * Nfd-Android: <a href = "https://play.google.com/apps/testing/net.named_data.nfd">download</a>
* Open NFD-Android (the <a href = "https://github.com/named-data-mobile/NFD-android">link</a> to github repo), and configure it. Need to connect nfd-Android to the NDN testbed (the best choice is remap server memoria.ndn.ucla.edu) and configure route.
  * Step 1: open the app, click "general"
  * Step 2: choose “faces” or “routes”
  * Step 3: click the “+” to add face after choosing "faces"
  * Step 4: click the “+” to add route after choosing "routes"

![NFD-Android screenshot](docs/NFD-Android.png)

* Open identity manager, request an NDNFit identity (see the <a href = "https://github.com/zhehaowang/android-identity-manager">steps</a>)
* Open ndnfit to capture and upload data
  * Choose an identity to use with the help of Identity Manager
  * Click “start tracking” to start to capture data
  * Click “stop tracking” to stop capturing data. The captured data will be uploaded to DSU within ten minutes
  * Click “show result” to show path on google map locally
  * Click “Reset Data” to clear local cache
  * “Encrypt” and “Decrypt” function have not been added yet

<p align="center"><img src ="docs/NDNFit.png" /></p>

### Known Issues:
* Nfd-Android stops working after about 20 minutes unless refresh it periodically
* The DSU (which uses <a href = "https://github.com/named-data/repo-ng"> Repo-ng </a>) is not robust enough. The number of data packets it can hold is limited.

### Development:

* Open in Android Studio, SDK 21, build tools 21.1.2; sync Gradle
