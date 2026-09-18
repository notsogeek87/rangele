<img src="docs/branding/yakwa-logo.svg" alt="Logo Yakwa" width="96" height="96" />

# Yakwa

Application Android native (Kotlin, Jetpack Compose, Room) pour gérer l'inventaire d'un
placard/cuisine, avec import assisté par scan de ticket de courses (CameraX + ML Kit OCR).
Tout est local sur l'appareil, sans backend.

## Identité visuelle

Le logo évoque le rangement de placard : un cintre au-dessus d'une armoire à deux portes,
dans les couleurs du design system Pastel Modern de l'app (violet doux, rose pastel,
crème, bleu bébé, menthe — voir `app/src/main/java/com/rangele/inventory/ui/theme/Color.kt`).
Le mark est appliqué à l'icône de lancement (adaptive icon) et à l'écran d'accueil pour une
identité cohérente dans toute l'app.

- `docs/branding/yakwa-logo.svg` — icône seule
- `docs/branding/yakwa-wordmark.svg` — icône + nom, pour un usage marketing

## Fonctionnalités (MVP)

- **Inventaire** : liste des produits en stock, triée du dernier ajouté au plus ancien par
  défaut (tri par nom ou par date de péremption au choix, menu « ⇅ »), avec recherche.
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
`staging`/`main`. Le module a deux product flavors (`staging` et `production`, ce dernier
correspondant à `main`), donc chaque run publie les deux APK debug, quelle que soit la branche
à l'origine du push.

**Où récupérer les APK :**
- Onglet **Releases** du repo → `staging-latest` / `main-latest` : toujours le dernier build,
  c'est la page à utiliser pour télécharger l'APK.
- Onglet **Actions** → un run → artifacts en bas de page : copie horodatée par commit
  (`inventaire-placard-<env>-<sha court>`), expire au bout de 90 jours.
