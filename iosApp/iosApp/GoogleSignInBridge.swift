import UIKit
import GoogleSignIn
import shared

private let IOS_CLIENT_ID = "778685057864-b58pa3j4nbepeomg30ciubf0ltdgft4m.apps.googleusercontent.com"

/// Swift bridge that implements Kotlin's GoogleSignInLauncher interface.
/// It drives the native GIDSignIn sheet and passes the raw ID token
/// back to the Kotlin auth flow via the GoogleSignInCallback protocol.
class SwiftGoogleSignInBridge: NSObject, GoogleSignInLauncher {

    /// Called from Kotlin when the user taps "Continue with Google".
    func launch(callback: any GoogleSignInCallback) {
        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: IOS_CLIENT_ID)

        guard let rootVC = UIApplication.shared.connectedScenes
            .compactMap({ $0 as? UIWindowScene })
            .flatMap({ $0.windows })
            .first(where: { $0.isKeyWindow })?.rootViewController else {
            callback.onResult(idToken: nil, error: "Could not find root view controller", cancelled: false)
            return
        }

        GIDSignIn.sharedInstance.signIn(withPresenting: rootVC) { result, error in
            if let idToken = result?.user.idToken?.tokenString {
                callback.onResult(idToken: idToken, error: nil, cancelled: false)
            } else if let nsError = error as NSError?,
                      nsError.domain == "com.google.GIDSignIn",
                      nsError.code == -4 {
                // GIDSignInErrorCodeCanceled = -4 — user dismissed the sheet
                callback.onResult(idToken: nil, error: nil, cancelled: true)
            } else {
                let msg = error?.localizedDescription ?? "Failed to retrieve ID token"
                callback.onResult(idToken: nil, error: msg, cancelled: false)
            }
        }
    }
}
