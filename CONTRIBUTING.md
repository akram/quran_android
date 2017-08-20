# Contributing

Here are the few steps you need to follow if you want to contribute to Quran for Android.

## Fabric Account and API key

You need to create an account on https://fabric.io to enable the Gradle build to run Crashalytics.
You will have to retrieve your client API key and Secret API key and put them in apps/fabric.properties file;
```
apiSecret=6d8c2f0f405caeaf361fb161c9cc7d86b027becf22xxxxxxxxxxxxxxxx
apiKey=63a8585dfca3f545c2df5fc69xxxxxxxxxx

```


## Keystore file to sign your APK

keytool -keystore app/path/to/key  -genkey -alias alias -storepass  store_password -keypass key_password



