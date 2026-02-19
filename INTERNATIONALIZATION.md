# 🌍 Internationalisation EcoTrace

## Langues supportées

L'application EcoTrace supporte actuellement **3 langues** :

- 🇫🇷 **Français** (par défaut)
- 🇬🇧 **Anglais**
- 🇪🇸 **Espagnol**

## Comment changer la langue

### Sur Android

L'application détecte automatiquement la langue du système. Pour changer la langue :

1. Ouvrez les **Paramètres** de votre appareil Android
2. Allez dans **Système** → **Langues et saisie** → **Langues**
3. Sélectionnez votre langue préférée (Français, English, ou Español)
4. Redémarrez l'application EcoTrace

L'interface sera automatiquement traduite dans la langue sélectionnée !

### Sur l'émulateur

Pour tester les différentes langues sur l'émulateur :

```bash
# Changer en anglais
adb shell "setprop persist.sys.locale en-US; stop; start"

# Changer en espagnol
adb shell "setprop persist.sys.locale es-ES; stop; start"

# Changer en français
adb shell "setprop persist.sys.locale fr-FR; stop; start"
```

Puis redémarrez l'application.

## Architecture de l'internationalisation

### Fichiers de ressources

Les traductions sont stockées dans des fichiers XML séparés :

```
app/src/main/res/
├── values/strings.xml           # Français (défaut)
├── values-en/strings.xml        # Anglais
└── values-es/strings.xml        # Espagnol
```

### Utilisation dans le code

Au lieu de textes hardcodés, nous utilisons des références aux ressources :

```kotlin
// ❌ Mauvais (hardcodé)
Text("Tableau de bord")

// ✅ Bon (traduit automatiquement)
Text(stringResource(R.string.home_title))
```

### Classe utilitaire

Pour faciliter l'accès aux ressources, utilisez la classe `Strings` :

```kotlin
import com.ecotrace.app.utils.Strings

@Composable
fun MyScreen() {
    Text(Strings.homeTitle())
    Text(Strings.comparisonVsFrance(750))
}
```

## Ajouter une nouvelle langue

Pour ajouter une nouvelle langue (par exemple l'allemand) :

1. **Créer le dossier de ressources** :
   ```bash
   mkdir app/src/main/res/values-de
   ```

2. **Copier le fichier strings.xml** :
   ```bash
   cp app/src/main/res/values/strings.xml app/src/main/res/values-de/
   ```

3. **Traduire tous les textes** dans `values-de/strings.xml`

4. **Compiler et tester** :
   ```bash
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

## Traductions actuelles

### Écrans traduits

- ✅ Navigation (Tableau, Ajouter, Scanner, Historique)
- ✅ Écran d'accueil (Dashboard)
- ✅ État vide avec call-to-action
- ✅ Comparaisons nationales
- ✅ Scopes (1, 2, 3)
- ✅ Catégories d'émissions (15 catégories)
- ✅ Unités de mesure
- ✅ Écran d'ajout d'émission
- ✅ Écran de scan de produits
- ✅ Écran d'historique
- ✅ Conseils personnalisés
- ✅ Messages d'erreur et de succès

### Textes restants à traduire

Les textes suivants sont encore hardcodés dans le code et nécessitent une extraction vers les ressources :

- Quelques labels dans les composants UI
- Certains messages de validation
- Formats de date (à adapter par locale)

## Formats localisés

### Nombres

Les nombres sont automatiquement formatés selon la locale :
- **FR** : `1 234,56`
- **EN** : `1,234.56`
- **ES** : `1.234,56`

### Dates

Les dates utilisent le format local :
- **FR** : `19 février 2026`
- **EN** : `February 19, 2026`
- **ES** : `19 de febrero de 2026`

### Devises

Actuellement en euros (€) pour toutes les langues. À adapter par pays dans une future version.

## Prochaines étapes

Pour une internationalisation complète :

1. **Extraire tous les textes hardcodés** vers les ressources
2. **Ajouter plus de langues** (DE, IT, PT, AR, ZH, JA)
3. **Adapter les facteurs d'émission** par pays
4. **Support multi-devises** (USD, GBP, etc.)
5. **Intégration Open Food Facts** pour base de données produits internationale
6. **Comparaisons nationales adaptées** par pays

## Contribution

Pour contribuer aux traductions :

1. Forkez le repository
2. Ajoutez/modifiez les fichiers `values-XX/strings.xml`
3. Testez avec `./gradlew assembleDebug`
4. Créez une Pull Request

---

**Note** : L'application détecte automatiquement la langue du système. Aucune configuration manuelle n'est nécessaire pour l'utilisateur final ! 🌍
