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

Fonctionnalités V3 (au-dessus du MVP) :
- **Dates de péremption** : champ optionnel sur le produit, saisi à l'ajout manuel ou au scan ;
  code couleur dans la liste (orange < 7 jours, rouge si dépassé) ; tri par date de péremption ;
  notification locale quotidienne (WorkManager) avec délai et heure réglables dans Paramètres.
- **Catégories** : champ optionnel sur le produit, gérées (créer/renommer/supprimer) dans un écran
  dédié ; filtre par catégorie dans l'inventaire. Supprimer une catégorie détache simplement les
  produits qui l'utilisaient (ils redeviennent sans catégorie).
- **Historique** : chaque retrait/suppression (pas les ajouts) journalisé et consultable dans un
  écran dédié, ordre chronologique inverse.
- **Liste de courses suggérée** : écran listant les produits sous leur `lowStockThreshold`
  (seuil optionnel par produit, comparé brut à la quantité — pas de conversion d'unité) ; les
  produits cochés peuvent être partagés en texte simple via l'intent de partage Android.

Le MVP et la V3 sont implémentés (`app/src/main/java/com/rangele/inventory`) : Room, repository,
écrans Compose (inventaire, ajout, scan/vérification de ticket, catégories, historique, liste de
courses, paramètres), WorkManager (`work/`), DataStore (`data/settings/`) et thème Kawaii Pastel Pop.

## Commands

```
./gradlew ktlintCheck              # lint Kotlin (ktlint 1.3.1, échoue le build si violations)
./gradlew ktlintFormat              # auto-fix des violations ktlint
./gradlew test                      # tests unitaires (staging + production)
./gradlew testStagingDebugUnitTest  # tests unitaires, un seul variant
./gradlew assembleDebug             # build des deux APK debug (staging + production)
```

Pour lancer un seul test unitaire :
```
./gradlew testStagingDebugUnitTest --tests "com.rangele.inventory.SomeClassTest"
./gradlew testStagingDebugUnitTest --tests "com.rangele.inventory.SomeClassTest.someMethod"
```

Il n'y a pas de tests instrumentés (androidTest) au-delà de la dépendance `ui-test-junit4` déclarée
dans `app/build.gradle.kts` ; aucune suite androidTest n'existe encore. Les tests qui ont besoin
d'une vraie base SQLite (migration Room, historique) tournent en JVM via Robolectric plutôt qu'en
androidTest — voir `MigrationTest` et `InventoryRepositoryImplTest`. `MigrationTest` n'utilise pas
`MigrationTestHelper` : celui-ci lit les JSON de schéma des versions passées, or seule la version
courante est générée au build et aucun schéma n'est versionné. Il crée donc une base v1 en SQL brut
et laisse Room appliquer les migrations, ce qui déclenche la validation du schéma final par Room.

## CI

`.github/workflows/build.yml` s'exécute sur chaque push vers `staging`/`main` : `ktlintCheck` →
`testStagingDebugUnitTest testProductionDebugUnitTest` → `assembleDebug` (uniquement les variants
debug, les variants release ne sont ni testés ni construits en CI). Grâce aux flavors Gradle
`staging`/`production` (voir Architecture),
un seul run produit et publie **deux** APK debug à chaque fois, quelle que soit la branche qui a
déclenché le build (le flavor `production` est renommé `main` uniquement dans le nom des
artifacts/releases CI). Deux façons de les récupérer :
- **Releases GitHub** (page "Releases" du repo) — une release **par build** (donc par push), pas
  de tag partagé qu'on écrase : tag `staging-v<versionName>-<run_number>` (pre-release) et
  `v<versionName>-<run_number>` (release), avec un APK `Yakwa-Staging-<versionName>-<run_number>.apk`
  / `Yakwa-<versionName>-<run_number>.apk` (ex. `Yakwa-1.0-25.apk`). GitHub marque automatiquement
  la release `main` la plus récente comme "Latest release" ; c'est l'endroit à privilégier pour
  télécharger un build donné ou le dernier en date.
- **Artifacts du run** (onglet Actions → run concerné → bas de page) — une copie horodatée par
  commit, nommée `inventaire-placard-staging-<sha court>` / `inventaire-placard-main-<sha court>`,
  utile pour retrouver un build précis. Expire au bout de 90 jours (défaut GitHub Actions).

Les rapports de tests/lint sont aussi publiés en artifact (pas en release).

## Architecture

Structure de package sous `com.rangele.inventory` (voir README) :
- `data` — entités Room, DAO, base de données, repository, et `data/settings` (Preferences
  DataStore pour les réglages de notification). `exportSchema = true` depuis la V3 (migrations 1→2
  puis 2→3, voir `Migrations.kt`) ; le plugin Gradle `androidx.room` exporte le schéma dans un dossier
  différent par flavor (`room { schemaDirectory(...) }` dans `app/build.gradle.kts`) pour éviter
  que les tâches KSP de `staging`/`production` écrivent en parallèle dans le même fichier.
- `ocr` — reconnaissance de texte (ML Kit `text-recognition`) et heuristique de parsing des lignes
  d'un ticket de caisse en produits/quantités.
- `ui` — écrans Jetpack Compose en MVVM (ViewModel + `lifecycle-viewmodel-compose`), navigation via
  `androidx-navigation-compose`.
- `work` — `ExpirationCheckWorker` (WorkManager), planifié une fois par jour par
  `ExpirationCheckScheduler` ; nécessite la permission runtime `POST_NOTIFICATIONS` (API 33+),
  demandée depuis l'écran Paramètres avant d'activer les notifications. L'initialisation par
  défaut de WorkManager est désactivée dans le manifest (`androidx.startup`) au profit d'un
  `WorkManager.initialize(...)` manuel dans `RangeleApplication`, pour lui fournir un
  `WorkerFactory` construit avec les dépendances de `AppContainer`.

Points de configuration Gradle notables :
- `namespace`/`applicationId` : `com.rangele.inventory` ; `minSdk` 26, `targetSdk`/`compileSdk` 35.
- Java/Kotlin cible : JVM 17.
- Versions et coordonnées de dépendances centralisées dans `gradle/libs.versions.toml` (version
  catalog) — ajouter toute nouvelle dépendance là plutôt qu'en dur dans `app/build.gradle.kts`.
- Le build type `debug` a un `applicationIdSuffix = ".debug"` pour cohabiter avec une install release.
- Deux product flavors sur la dimension `env` : `staging` (`applicationIdSuffix = ".staging"`,
  nom d'app "Yakwa Staging") et `production` (identité par défaut, pas de suffixe). Le nom
  `production` est utilisé plutôt que `main` pour éviter la collision avec le source set réservé
  `main` de Gradle/AGP ; c'est bien ce flavor qui correspond à la branche `main`. Les deux variants
  debug (`stagingDebug`/`productionDebug`) peuvent être installés simultanément sur le même appareil.
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
