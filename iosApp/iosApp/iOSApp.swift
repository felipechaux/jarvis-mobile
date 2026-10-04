import Shared
import SwiftUI
import UIKit
import UserNotifications

final class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {
    func application(
        _ application: UIApplication,
        didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]? = nil
    ) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        // Registration itself is requested from Kotlin once the user grants
        // notifications; re-register on every launch so a rotated token reaches the Mac.
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            if settings.authorizationStatus == .authorized {
                DispatchQueue.main.async { application.registerForRemoteNotifications() }
            }
        }
        return true
    }

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        let hex = deviceToken.map { String(format: "%02x", $0) }.joined()
        MainViewControllerKt.onApnsToken(token: hex)
    }

    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        // Push needs a signing team with the Push Notifications capability;
        // without it the app still gets live events over the WebSocket.
        NSLog("JARVIS: APNs registration failed: \(error.localizedDescription)")
    }

    // Show "Claude terminó / pide permiso" banners while the app is open too.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        completionHandler([.banner, .sound])
    }
}

@main
struct iOSApp: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) private var appDelegate

    init() {
        MainViewControllerKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ComposeView().ignoresSafeArea(.keyboard)
        }
    }
}

struct ComposeView: UIViewControllerRepresentable {
    func makeUIViewController(context: Context) -> UIViewController {
        MainViewControllerKt.MainViewController()
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}
