# Eclipse Paho Android Service
[![Build Status](https://travis-ci.org/eclipse/paho.mqtt.android.svg?branch=master)](https://travis-ci.org/eclipse/paho.mqtt.android)

The Paho Android Service is an MQTT client library written in Java for developing applications on Android.


## Features
|                     |                    |   |                      |                    |
|---------------------|--------------------|---|----------------------|--------------------|
| MQTT 3.1            | :heavy_check_mark: |   | Automatic Reconnect  | :heavy_check_mark: |
| MQTT 3.1.1          | :heavy_check_mark: |   | Offline Buffering    | :heavy_check_mark: |
| LWT                 | :heavy_check_mark: |   | WebSocket Support    | :heavy_check_mark: |
| SSL / TLS           | :heavy_check_mark: |   | Standard TCP Support | :heavy_check_mark: |
| Message Persistence | :heavy_check_mark: |   |


To get started, download [Android Studio](http://developer.android.com/tools/studio/index.html). You will also need to download the [Android SDK](https://developer.android.com/sdk/installing/adding-packages.html). Currently you will need the SDK for 24.


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


## Using the Paho Android Client

### Downloading

#### Maven

Eclipse hosts a Nexus repository for those who want to use Maven to manage their dependencies.

Add the repository definition and the dependency definition shown below to your pom.xml.

Replace %REPOURL% with either ``` https://repo.eclipse.org/content/repositories/paho-releases/ ``` for the official releases, or ``` https://repo.eclipse.org/content/repositories/paho-snapshots/  ``` for the nightly snapshots. Replace %VERSION% with the level required .
The latest release version is ```1.1.1``` and the current snapshot version is ```1.1.2-SNAPSHOT```.

```
<project ...>
<repositories>
    <repository>
        <id>Eclipse Paho Repo</id>
        <url>%REPOURL%</url>
    </repository>
</repositories>
...
<dependencies>
    <dependency>
        <groupId>org.eclipse.paho</groupId>
        <artifactId>org.eclipse.paho.android.service</artifactId>
        <version>%VERSION%</version>
    </dependency>
</dependencies>
</project>

```

#### Gradle

If you are using Android Studio and / or Gradle to manage your application dependencies and build then you can use the same repository to get the Paho Android Service. Add the Eclipse Maven repository to your `build.gradle` file and then add the Paho dependency to the `dependencies` section.

```
repositories {
    maven {
        url "https://repo.eclipse.org/content/repositories/paho-snapshots/"
    }
}


dependencies {
    compile 'org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.1.0'
    compile 'org.eclipse.paho:org.eclipse.paho.android.service:1.1.1'
}
```
__Note:__ currently you have to include the `org.eclipse.paho:org.eclipse.paho.client.mqttv3` dependency as well. We are attempting to get the build to produce an Android `AAR` file that contains both the Android service as well as it's dependencies, however this is still experimental. If you wish to try it, remove the `org.eclipse.paho:org.eclipse.paho.client.mqttv3` dependency and append `@aar` to the end of the Android Service dependency. E.g. `org.eclipse.paho:org.eclipse.paho.android.service:1.1.1@aar`

If you find that there is functionality missing or bugs in the release version, you may want to try using the snapshot version to see if this helps before raising a feature request or an issue.

### Building from source

 - Open a terminal and navigate to this directory (org.eclipse.paho.android.service)
 - Run the command ``./gradlew clean assemble exportJar` or on Windows: `gradlew.bat clean assemble exportJar`

### Running the Sample App:

 * Open the this current directory in Android Studio (org.eclipse.paho.android.service).
 * In the toolbar along the top, there should be a dropdown menu. Make sure that it contains 'org.eclipse.android.sample' then click the Green 'Run' Triangle. It should now build and launch an Virtual Android Device to run the App. If you have an Android device with developer mode turned on plugged in, you will have the oppertunity to run it directly on that.
 * If you have any problems, check out the Android Developer Documentation for help: https://developer.android.com
