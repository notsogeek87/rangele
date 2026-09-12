<img src="docs/branding/yakwa-logo.svg" alt="Logo Yakwa" width="96" height="96" />

# Yakwa

Application Android native (Kotlin, Jetpack Compose, Room) pour gérer l'inventaire d'un
placard/cuisine, avec import assisté par scan de ticket de courses (CameraX + ML Kit OCR).
Tout est local sur l'appareil, sans backend.

## Identité visuelle

Le logo évoque le rangement de placard : un cintre au-dessus d'une armoire à deux portes,
sur un fond terracotta qui rappelle le bois. La palette (`app/src/main/java/com/rangele/inventory/ui/theme/Color.kt`)
et l'icône de l'application en découlent, pour une identité cohérente sur tout l'app
(icône de lancement, écran d'accueil, thème clair/sombre).

- `docs/branding/yakwa-logo.svg` — icône seule
- `docs/branding/yakwa-wordmark.svg` — icône + nom, pour un usage marketing

## Fonctionnalités (MVP)

- **Inventaire** : liste des produits en stock, triée par nom, avec recherche.
- **Ajout manuel** : nom, quantité, unité — avec détection de doublon (propose de fusionner
  avec un produit existant plutôt que de le dupliquer).
- **Retrait/ajustement** : +/- ou saisie directe de la quantité, suppression, depuis la liste.
- **Scan de ticket** : photo du ticket → OCR on-device (ML Kit) → extraction heuristique des
  lignes produit → écran de vérification (édition/suppression/fusion) → import dans l'inventaire.

## Structure

- `app/src/main/java/com/rangele/inventory/data` — Room (entité, DAO, base) et repository.
- `app/src/main/java/com/rangele/inventory/ocr` — reconnaissance de texte et heuristique de
  parsing de ticket.
- `app/src/main/java/com/rangele/inventory/ui` — écrans Compose (MVVM) et navigation.

## Build

```
./gradlew ktlintCheck   # lint Kotlin
./gradlew test          # tests unitaires
./gradlew assembleDebug # APK debug
```

Le pipeline `.github/workflows/build.yml` exécute ces trois étapes sur chaque push vers
`staging`/`main` et publie l'APK debug en artifact (nom incluant la branche et le hash de
commit court).
