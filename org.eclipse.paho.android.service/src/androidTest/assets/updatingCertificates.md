# How to Add certificates to the BKS keystore

## Getting the certificates

```echo -n | openssl s_client -connect mqtt.eclipse.org:8883 | sed -ne '/-BEGIN CERTIFICATE-/,/-END CERTIFICATE-/p' > eclipse.crt```

Notes: you need all certificates in chain (Copy each to a .crt file)

## Adding to keystore

```keytool -importcert -v -trustcacerts -file eclipse.crt -alias mqtt.eclipse.org -keystore "test.bks" -provider org.bouncycastle.jce.provider.BouncyCastleProvider -providerpath "./bcprov-jdk15on-1.65.jar" -storetype BKS -storepass mqtttest```

## List certs

```keytool -list -v -keystore test.bks -provider org.bouncycastle.jce.provider.BouncyCastleProvider -providerpath "./bcprov-jdk15on-1.65.jar" -storetype BKS -storepass mqtttest```
