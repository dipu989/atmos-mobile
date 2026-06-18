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
                    // Gmail OAuth deep links: atmos://gmail/connected and atmos://gmail/error
                    // Check only the first non-slash path component to match Android's
                    // pathSegments.firstOrNull() semantics exactly.
                    let firstSegment = url.pathComponents.first(where: { $0 != "/" })
                    guard url.scheme == "atmos", url.host == "gmail" else { return }
                    if firstSegment == "connected" {
                        DeepLinkHandlerKt.handleGmailOAuthCallback()
                    } else if firstSegment == "error" {
                        DeepLinkHandlerKt.handleGmailOAuthError()
                    }
                }
        }
    }
}
