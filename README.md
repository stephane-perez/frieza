# Frieza 🧊

Application Android (Kotlin + Jetpack Compose) de gestion de congélateur — 100% locale, aucun compte, aucune clé API.

## Fonctionnalités

- Plusieurs congélateurs, chacun avec des tiroirs nommés (ajout, renommage, réordonnancement, suppression)
- Vue verticale : tiroirs empilés et repliables, aliments en puces colorées par catégorie
- Catégories personnalisables (nom + couleur), plusieurs catégories par aliment
- Quantités : nombre, poids (g) et portions, cumulables
- Date de congélation, date « à consommer avant » (raccourcis +3/+6/+12 mois), notes
- Badges de péremption proche, recherche, filtre par catégorie, tri (nom / péremption / ajout récent)
- Dupliquer un aliment, le déplacer d'un tiroir ou congélateur à l'autre (formulaire)
- Liste de courses (avec « ranger au congélateur »)
- Sélection multiple (appui long) puis copie d'un prompt « recettes » pour l'IA de ton choix
- Sauvegarde / restauration JSON
- 100 % local, aucun compte, aucune clé API. Les données de la v1 sont migrées automatiquement.

## Générer l'APK via GitHub Actions (sans Android Studio)

Le dépôt contient déjà tout ce qu'il faut : le wrapper Gradle complet
(`gradlew`, `gradlew.bat`, `gradle/wrapper/gradle-wrapper.jar`) et un workflow
GitHub Actions (`.github/workflows/android-build.yml`) qui compile un APK de
debug à chaque push sur `main`, ou à la demande.

1. Crée un dépôt vide sur GitHub (public ou privé), sans README/licence
   générés automatiquement (pour éviter un conflit à la première synchro).
2. Depuis le dossier `Frieza` décompressé, sur ta machine :
   ```bash
   cd Frieza
   git init
   git add .
   git commit -m "Initial commit: squelette Frieza"
   git branch -M main
   git remote add origin https://github.com/<ton-utilisateur>/<ton-depot>.git
   git push -u origin main
   ```
3. Va dans l'onglet **Actions** du dépôt sur GitHub : le workflow "Build APK"
   se déclenche automatiquement sur ce push (compte quelques minutes, le temps
   d'installer le SDK Android et de compiler).
4. Une fois le run vert, ouvre-le et descends jusqu'à **Artifacts** : télécharge
   `frieza-debug-apk` (fichier zip contenant `app-debug.apk`).
5. Tu peux aussi relancer une build manuellement à tout moment via
   **Actions > Build APK > Run workflow** (bouton "workflow_dispatch").

## Installer l'APK sur ton téléphone

1. Transfère `app-debug.apk` sur ton téléphone (câble, Drive, mail à toi-même, etc.).
2. Ouvre le fichier depuis le téléphone ; Android demandera d'autoriser
   l'installation depuis cette source (à activer une fois dans les paramètres).
3. Installe. L'app est signée avec la clé de debug automatique de Gradle — cela
   suffit pour une utilisation perso, pas pour une publication sur le Play Store.

Minimum SDK : Android 8.0 (API 26).

## Structure du code

```
app/src/main/java/com/frieza/freezer/
├── data/            Entités Room (Food, FreezerConfig), DAOs, Repository
├── ui/setup/        Écran de configuration du congélateur (nb d'étages)
├── ui/freezer/      Écran principal : onglets par étage, sélection multiple,
│                    export presse-papier
├── ui/food/         Formulaire d'ajout / édition d'un aliment
├── ui/navigation/   Graphe de navigation (Setup ↔ Freezer ↔ FoodForm)
└── ui/theme/        Thème Material 3 (couleurs bleu glacé)
```

## Pistes pour la suite

- Alertes de péremption (WorkManager + notifications) à partir de `expirationDate`
- Tri / filtre par catégorie ou date de péremption sur l'écran principal
- Réordonnancement ou nommage personnalisé des étages
- Historique des aliments consommés / statistiques de gaspillage
