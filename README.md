#Android Service Client

The Android MQTT Service Project has been restructured to work in Android Studio and use the Gradle Build system.

To get started, download [Android Studio](http://developer.android.com/tools/studio/index.html). You will also need to download the [Android SDK](https://developer.android.com/sdk/installing/adding-packages.html). Currently you will need the SDK for 19,21 and 22, This will hopefully be simplified soon.

## Running the Sample App:

* Open the this current directory in Android Studio (org.eclipse.paho.android.service).
* In the toolbar along the top, there should be a dropdown menu. Make sure that it contains 'org.eclipse.android.sample' then click the Green 'Run' Triangle. It should now build and launch an Virtual Android Device to run the App. If you have an Android device with developer mode turned on plugged in, you will have the oppertunity to run it directly on that.
* If you have any problems, check out the Android Developer Documentation for help: https://developer.android.com

## Building the service locally
* Open a terminal and navigate to this directory (org.eclipse.paho.android.service)
* Run the command ```./gradlew clean assemble exportJar``` or on Windows: ```gradlew.bat clean assemble exportJar```

Once it completes successfully, the service jar will be located in org.eclipse.paho.android/org.eclipse.paho.android.service/release
