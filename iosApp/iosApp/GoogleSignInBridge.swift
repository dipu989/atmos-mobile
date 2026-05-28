import UIKit
import shared

// ── SETUP INSTRUCTIONS ────────────────────────────────────────────────────────
//
// Before this file is active you need to:
//
// 1. Add the Google Sign-In iOS SDK via Swift Package Manager in Xcode:
//    File → Add Package Dependencies →
//    URL: https://github.com/google/GoogleSignIn-iOS
//    Version: Up to Next Major, starting from 7.1.0
//    Select: GoogleSignIn (not GoogleSignInSwift)
//
// 2. Replace IOS_CLIENT_ID below with the client ID from the "Atmos iOS"
//    credential in Google Cloud Console → APIs & Services → Credentials.
//    Example: "123456789-abc123def456.apps.googleusercontent.com"
//
// 3. Add the reversed client ID to Info.plist:
//    Key:   URL types (CFBundleURLTypes)
//    Item:  URL Schemes = "com.googleusercontent.apps.778685057864-b58pa3j4nbepeomg30ciubf0ltdgft4m"
//
// 4. Uncomment `import GoogleSignIn` and the entire SwiftGoogleSignInBridge class.
//
// 5. In iOSApp.swift, add this line inside init():
//    IosGoogleSignInHolder.shared.launcher = SwiftGoogleSignInBridge()
//
// ─────────────────────────────────────────────────────────────────────────────
//
// Once the SDK is added, remove these placeholder lines and uncomment below:
//

// import GoogleSignIn
//
// private let IOS_CLIENT_ID = "778685057864-b58pa3j4nbepeomg30ciubf0ltdgft4m.apps.googleusercontent.com"
//
// /// Swift bridge that implements Kotlin's GoogleSignInLauncher interface.
// /// It drives the native GIDSignIn bottom sheet and passes the raw ID token
// /// back to the Kotlin auth flow.
// class SwiftGoogleSignInBridge: NSObject, GoogleSignInLauncher {
//
//     /// Called from Kotlin when the user taps "Continue with Google".
//     func launch(callback: any GoogleSignInCallback) {
//         GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: IOS_CLIENT_ID)
//
//         guard let rootVC = UIApplication.shared.connectedScenes
//             .compactMap({ $0 as? UIWindowScene })
//             .flatMap({ $0.windows })
//             .first(where: { $0.isKeyWindow })?.rootViewController else {
//             callback.onResult(idToken: nil, error: "Could not find root view controller")
//             return
//         }
//
//         GIDSignIn.sharedInstance.signIn(withPresenting: rootVC) { result, error in
//             if let idToken = result?.user.idToken?.tokenString {
//                 callback.onResult(idToken: idToken, error: nil)
//             } else {
//                 let msg = error?.localizedDescription ?? "Failed to retrieve ID token"
//                 callback.onResult(idToken: nil, error: msg)
//             }
//         }
//     }
// }
