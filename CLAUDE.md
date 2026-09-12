# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project

Rangele: application Android native (Kotlin, Jetpack Compose, Room) pour gérer l'inventaire d'un
placard/cuisine, avec import assisté par scan de ticket de courses (CameraX + ML Kit OCR). Tout est
local sur l'appareil, sans backend.

Fonctionnalités MVP :
- **Inventaire** : liste des produits en stock, triée par nom, avec recherche.
- **Ajout manuel** : nom, quantité, unité — avec détection de doublon (propose de fusionner avec
  un produit existant plutôt que de le dupliquer).
- **Retrait/ajustement** : +/- ou saisie directe de la quantité, suppression, depuis la liste.
- **Scan de ticket** : photo du ticket → OCR on-device (ML Kit) → extraction heuristique des
  lignes produit → écran de vérification (édition/suppression/fusion) → import dans l'inventaire.

Le MVP est implémenté (`app/src/main/java/com/rangele/inventory`) : Room, repository, écrans
Compose (inventaire, ajout, scan/vérification de ticket) et thème Kawaii Pastel Pop.

## Commands

```
./gradlew ktlintCheck        # lint Kotlin (ktlint 1.3.1, échoue le build si violations)
./gradlew ktlintFormat       # auto-fix des violations ktlint
./gradlew test               # tests unitaires (toutes variantes)
./gradlew testDebugUnitTest  # tests unitaires, variante debug (celle utilisée en CI)
./gradlew assembleDebug      # build de l'APK debug
```

Pour lancer un seul test unitaire :
```
./gradlew testDebugUnitTest --tests "com.rangele.inventory.SomeClassTest"
./gradlew testDebugUnitTest --tests "com.rangele.inventory.SomeClassTest.someMethod"
```

Il n'y a pas de tests instrumentés (androidTest) au-delà de la dépendance `ui-test-junit4` déclarée
dans `app/build.gradle.kts` ; aucune suite androidTest n'existe encore.

## CI

`.github/workflows/build.yml` s'exécute sur chaque push vers `staging`/`main` : `ktlintCheck` →
`testDebugUnitTest` → `assembleDebug`, puis publie l'APK debug en artifact (nommé
`inventaire-placard-<branche>-<sha court>`) ainsi que les rapports de tests/lint.

## Architecture

Structure de package sous `com.rangele.inventory` (voir README) :
- `data` — entités Room, DAO, base de données, repository. Room génère son schéma JSON dans
  `app/schemas` (configuré via `ksp { arg("room.schemaLocation", ...) }` dans `app/build.gradle.kts`).
- `ocr` — reconnaissance de texte (ML Kit `text-recognition`) et heuristique de parsing des lignes
  d'un ticket de caisse en produits/quantités.
- `ui` — écrans Jetpack Compose en MVVM (ViewModel + `lifecycle-viewmodel-compose`), navigation via
  `androidx-navigation-compose`.

Points de configuration Gradle notables :
- `namespace`/`applicationId` : `com.rangele.inventory` ; `minSdk` 26, `targetSdk`/`compileSdk` 35.
- Java/Kotlin cible : JVM 17.
- Versions et coordonnées de dépendances centralisées dans `gradle/libs.versions.toml` (version
  catalog) — ajouter toute nouvelle dépendance là plutôt qu'en dur dans `app/build.gradle.kts`.
- Le build type `debug` a un `applicationIdSuffix = ".debug"` pour cohabiter avec une install release.
- CameraX (`camera-core`, `camera2`, `camera-lifecycle`, `camera-view`) est utilisé pour la capture
  photo du ticket ; la permission `CAMERA` est déclarée dans le manifest avec
  `android:required="false"` sur la feature caméra.
- Un `FileProvider` est déclaré dans le manifest (`file_paths.xml`) pour partager les fichiers
  images (ex. photo du ticket) entre l'app et la caméra système.

## Style

- Formatage : `ktlint` (config par défaut, voir `app/build.gradle.kts` — `android.set(true)`,
  `ignoreFailures.set(false)`), avec `.editorconfig` : indentation 4 espaces, ligne max 120
  caractères, et exception de nommage ktlint pour les fonctions annotées `@Composable`.
- `kotlin.code.style=official` (gradle.properties).
