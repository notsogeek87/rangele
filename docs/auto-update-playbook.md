# Auto-update playbook (lielugit-updater) — lessons from Yakwa

Meant to be copied into `lielugit-updater/docs/` (see "Where this should live"). Everything below was
hit for real while wiring the library into this app; each point silently breaks updates if missed.

## Paste this prompt into Claude Code in the target app

> Add automatic updates to this Android app with **lielugit-updater 1.0.0**, following
> `docs/auto-update-playbook.md` of https://github.com/notsogeek87/lielugit-updater (all sections).
> No token or secret: use the vendored Maven repository. Check on every app open, explain the steps to the
> user in the update dialog and in the release text, make versionCode/tags increase on every CI build, then
> make sure the project builds and the CI is green before pushing to the delivery branches.

## 1. Dependency without any secret (works locally, in CI and in cloud sessions)

GitHub Packages needs a token even for public packages, and a repository secret exists per repository.
Avoid it: every release ships a public Maven repository (`lielugit-updater-<version>-maven.zip`).

```bash
mkdir -p libs/lielugit-maven
curl -fsSL -o /tmp/l.zip https://github.com/notsogeek87/lielugit-updater/releases/download/v1.0.0/lielugit-updater-1.0.0-maven.zip
unzip -oq /tmp/l.zip -d libs/lielugit-maven
rm -f libs/lielugit-maven/com/lielu/lielugit-updater/maven-metadata-local.xml
```

`settings.gradle.kts`, inside `dependencyResolutionManagement { repositories { … } }`:

```kotlin
maven {
    url = uri("$rootDir/libs/lielugit-maven")
    content { includeGroup("com.lielu") }
}
```

Add the dependency (version catalog if the project has one), keep `libs/lielugit-maven` in git (check
`.gitignore`). Upgrading = replace the folder with the new zip and bump the version.

## 2. Versioning that Android and the library both accept

- `versionCode` must **strictly increase** on every published APK, or Android refuses the update.
- The release tag must parse as a version **greater** than the installed `versionName`. A tag such as
  `v1.0-25` is read as the *pre-release* `1.0-25`, which is **lower** than `1.0`: no update is ever offered.
- Do both from the CI run number:

`gradle.properties`: `appVersionBase=1.0`

`app/build.gradle.kts` (top level):

```kotlin
val buildNumber = (System.getenv("BUILD_NUMBER") ?: providers.gradleProperty("buildNumber").orNull)?.toIntOrNull() ?: 1
val appVersionBase = providers.gradleProperty("appVersionBase").get()
// in defaultConfig:
versionCode = buildNumber
versionName = "$appVersionBase.$buildNumber"
```

GitHub Actions: job-level `env: BUILD_NUMBER: ${{ github.run_number }}`, version = `<base>.<run_number>`,
release tag `v<version>` (e.g. `v1.0.152`), APK named `App-<version>.apk`.

- Every APK must be signed with the **same key** and have the **same `applicationId`**. A flavor with another
  `applicationIdSuffix` (e.g. `.staging`) cannot be updated from the production release: disable updates there
  (`if (packageName.endsWith(".staging")) …`).
- The library reads the repository's **latest non-prerelease** release (`/releases/latest`).

## 3. Check on every app open — force it

`UpdateManager.checkForUpdate()` caches its answer for `checkIntervalHours`. A cached "up to date" hides a
release published a few minutes later. Call `checkForUpdate(force = true)` when the app comes to the
foreground (one request per open, far below GitHub's 60/h unauthenticated limit), not only when a screen is created:

```kotlin
LifecycleEventEffect(Lifecycle.Event.ON_START) { viewModel.checkOnOpen() }
```

In the ViewModel, skip the check when `state` is not `Idle`/`UpToDate`/`Error` (do not overwrite a visible
dialog or a running download) and swallow errors (they are also published in `state`).

## 4. Guide the user (in the app)

Show a dialog when `state` is `UpdateAvailable`, with the steps spelled out:

1. Tap "Install": the update downloads.
2. If Android asks to allow the app to install apps, enable the option, then go back.
3. Confirm "Update" when Android offers it. Data is kept.

After `Downloaded`, a second dialog: if `canInstallPackages()` is false, explain that the Android settings will
open and that the user must come back and tap "Install" again; otherwise just "Install". Show errors only
after the user tapped "Install". Keep a "Check for updates" button in Settings (`force = true`).

## 5. Guide the user (in each release)

Put install instructions in the release body, because the first install is manual: already installed → open
the app; first install → download the APK, open it, allow the source if Android blocks, tap Install.

## 6. Delivery checklist

- [ ] CI green on the work branch before merging into the delivery branches (build, tests, lint).
- [ ] Release published, APK attached, tag greater than the previous one.
- [ ] Installed once by hand on a device; the next release is then proposed at app open.

## Where this should live

`lielugit-updater/docs/auto-update-playbook.md`, linked from `docs/USING_IN_ANOTHER_PROJECT.md` and the
README, so any session that follows the usual prompt finds it. Copy the Compose code from
`app/src/main/java/com/rangele/inventory/ui/update/` (`AppUpdateViewModel.kt`, `UpdatePrompt.kt`) of this repo.
