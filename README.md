# Eclipse Paho Android Service

The Paho Android Service is an MQTT service library written in Java for developing applications on Android.

## Features
|                     |                    |   |                      |                    |
|---------------------|--------------------|---|----------------------|--------------------|
| MQTT 3.1            | :heavy_check_mark: |   | Automatic Reconnect  | :heavy_check_mark: |
| MQTT 3.1.1          | :heavy_check_mark: |   | Offline Buffering    | :heavy_check_mark: |
| LWT                 | :heavy_check_mark: |   | WebSocket Support    | :heavy_check_mark: |
| SSL / TLS           | :heavy_check_mark: |   | Standard TCP Support | :heavy_check_mark: |
| Message Persistence | :heavy_check_mark: |   |


## Project description:

The Paho project has been created to provide reliable open-source implementations of open and standard messaging protocols aimed at new, existing, and emerging applications for Machine-to-Machine (M2M) and Internet of Things (IoT).
Paho reflects the inherent physical and cost constraints of device connectivity. Its objectives include effective levels of decoupling between devices and applications, designed to keep markets open and encourage the rapid growth of scalable Web and Enterprise middleware and applications.

## Using the Paho Android Client

The client repo is [https://github.com/eclipse/paho.mqtt.java](https://github.com/eclipse/paho.mqtt.java) and the JAR can be build manually or retrieved from the maven repository.

#### Gradle

If you are using Android Studio and / or Gradle to manage your application dependencies and build then you can use the same repository to get the Paho Android Service. Add the Eclipse Maven repository to your `build.gradle` file and then add the Paho dependency to the `dependencies` section.

```
repositories {
    maven {
        url "https://repo.eclipse.org/content/repositories/paho-snapshots/"
    }
}


dependencies {
    compile 'org.eclipse.paho:org.eclipse.paho.client.mqttv3:1.2.5'
    compile 'org.eclipse.paho:org.eclipse.paho.android.service:$latest.build.version'
}
```

### Building from source

 - Open a terminal and navigate to this directory (org.eclipse.paho.android.service)
 - Run the command ``./gradlew clean assemble exportJar` or on Windows: `gradlew.bat clean assemble exportJar`
 - The Jar is located under PROJECT_FOLDER/build/intermediates/runtime_library_classes_jar/release