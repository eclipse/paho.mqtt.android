# Eclipse Paho Android Service

The Paho Android Service is an MQTT client library written in Java for developing applications on Android.

To get started, download [Android Studio](http://developer.android.com/tools/studio/index.html). You will also need to download the [Android SDK](https://developer.android.com/sdk/installing/adding-packages.html). Currently you will need the SDK for 19,21 and 22, This will hopefully be simplified soon.


## Project description:

The Paho project has been created to provide reliable open-source implementations of open and standard messaging protocols aimed at new, existing, and emerging applications for Machine-to-Machine (M2M) and Internet of Things (IoT).
Paho reflects the inherent physical and cost constraints of device connectivity. Its objectives include effective levels of decoupling between devices and applications, designed to keep markets open and encourage the rapid growth of scalable Web and Enterprise middleware and applications.


## Links

- Project Website: [https://www.eclipse.org/paho](https://www.eclipse.org/paho)
- Eclipse Project Information: [https://projects.eclipse.org/projects/iot.paho](https://projects.eclipse.org/projects/iot.paho)
- Paho Android Client Page: [https://www.eclipse.org/paho/clients/android/](https://www.eclipse.org/paho/clients/android/)
- GitHub: [https://github.com/eclipse/paho.mqtt.android](https://github.com/eclipse/paho.mqtt.android)
- Twitter: [@eclipsepaho](https://twitter.com/eclipsepaho)
- Issues: [https://github.com/eclipse/paho.mqtt.android/issues](https://github.com/eclipse/paho.mqtt.android/issues)
- Mailing-list: [https://dev.eclipse.org/mailman/listinfo/paho-dev](https://dev.eclipse.org/mailman/listinfo/paho-dev)


## Running the Sample App:

* Open the this current directory in Android Studio (org.eclipse.paho.android.service).
* In the toolbar along the top, there should be a dropdown menu. Make sure that it contains 'org.eclipse.android.sample' then click the Green 'Run' Triangle. It should now build and launch an Virtual Android Device to run the App. If you have an Android device with developer mode turned on plugged in, you will have the oppertunity to run it directly on that.
* If you have any problems, check out the Android Developer Documentation for help: https://developer.android.com

## Building the service locally
* Open a terminal and navigate to this directory (org.eclipse.paho.android.service)
* Run the command ```./gradlew clean assemble exportJar``` or on Windows: ```gradlew.bat clean assemble exportJar```

Once it completes successfully, the service jar will be located in org.eclipse.paho.android/org.eclipse.paho.android.service/release
