# Privacy Policy release setup

The game now contains an in-app Privacy Policy screen and the same policy text is stored in `PRIVACY_POLICY.md`.

Before a production Google Play release:

1. Publish `PRIVACY_POLICY.md` at a public HTTPS URL that does not require login.
2. Add the URL to the project/root `gradle.properties`:

```properties
privacyPolicyUrl=https://bricks-breaker-ball.vercel.app/en/privacy
```

3. Use that exact same URL in Google Play Console > App content > Privacy policy.
4. Build the release again. The in-game Privacy Policy screen will show an **OPEN ONLINE** button when the property is configured.

Do not use a local file URL, private GitHub URL, or a page that requires sign-in.
