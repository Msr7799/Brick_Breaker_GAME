# Privacy + responsive update

## Added
- In-game **PRIVACY POLICY** button in Settings.
- Scrollable `PrivacyPolicyScreen` with local policy text.
- `PRIVACY_POLICY.md` ready to publish on a public HTTPS page.
- Configurable `BuildConfig.PRIVACY_POLICY_URL` from the Gradle property `privacyPolicyUrl`.
- **OPEN ONLINE** button appears after the public URL is configured.

## Responsive layout
- Shared UI screens now use `ExtendViewport(900, 1600)` instead of letterboxed `FitViewport`.
- The 900x1600 gameplay/UI safe area stays centered on phones and tablets.
- Extra screen area is filled by cropped backgrounds instead of black bars.
- Touch mapping remains in world coordinates.
- Scroll/scissor regions in Rules and Customization now use viewport projection, so they stay aligned on non-9:16 screens.
- Shop, splash, menu, and unlock backgrounds now fill the full visible display.
- Android activity is explicitly resizable for large-screen devices.

## Platform note
The included project is an Android application module. These LibGDX layout changes are aspect-ratio safe and reusable for an iOS target, but an actual iPad `.ipa` still requires a separate LibGDX iOS/RoboVM launcher and an Apple/Xcode build/signing setup.
