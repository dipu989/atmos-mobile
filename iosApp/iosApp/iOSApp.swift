import SwiftUI
import shared

@main
struct iOSApp: App {

    init() {
        // Inject the Swift-side Google Sign-In bridge into Kotlin.
        // Kotlin calls IosGoogleSignInHolder.launcher.launch() → this drives GIDSignIn.
        IosGoogleSignInHolder.shared.launcher = SwiftGoogleSignInBridge()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    // Gmail OAuth deep link: atmos://gmail/connected
                    if url.scheme == "atmos", url.host == "gmail", url.pathComponents.contains("connected") {
                        DeepLinkHandlerKt.handleGmailOAuthCallback()
                    }
                }
        }
    }
}
