# Firebase setup

The supplied Android configuration uses package `com.thirtyseconds.minigames`. No service keys or OAuth client IDs are invented. `app/google-services.json` is ignored by Git.

1. Firebase Console → Project settings → Android application → add fingerprints:
   - Debug SHA-1: `BD:55:18:0D:B1:59:A1:C3:33:46:89:A0:1F:28:0D:10:B6:1E:A3:41`
   - Debug SHA-256: `44:64:55:C9:E6:4E:9B:0A:3D:15:D8:4D:95:F3:DB:A9:13:72:06:FB:5C:CD:45:7D:AE:43:CA:EC:BF:0F:EA:AB`
2. Authentication → Sign-in method → enable Google and Anonymous. Add a support email when Google asks for one.
3. Download the updated Android `google-services.json` and replace the file in `app/`. A Web OAuth client entry produces `default_web_client_id` through the Google Services Gradle plugin; the Google button remains disabled if this resource is absent.
4. Create the Firestore database in an appropriate region. Review and deploy `backend/firestore.rules`. Do not use test-mode public rules.
5. Sync/build Android Studio and connect Google from onboarding or Profile. Test cross-device restoration, linking an anonymous user, an existing-account collision, offline reconnect and simultaneous-device saves against this actual project.

The fingerprint above is only the local debug key. Add your release upload key and Google Play App Signing fingerprints separately before release. Production AdMob IDs and release signing are also external configuration.

References: [Firebase Android setup](https://firebase.google.com/docs/android/setup), [Google authentication / Credential Manager](https://firebase.google.com/docs/auth/android/google-signin), [anonymous account linking](https://firebase.google.com/docs/auth/android/anonymous-auth).
