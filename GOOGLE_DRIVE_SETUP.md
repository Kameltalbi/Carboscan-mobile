# 🔐 Configuration Google Drive pour EcoTrace

## 📋 Vue d'ensemble

EcoTrace utilise **Google Drive** pour sauvegarder automatiquement les données de chaque utilisateur dans son propre Google Drive. Chaque utilisateur doit se connecter avec son compte Google pour utiliser l'application.

## ✨ Fonctionnalités

- ✅ **Connexion obligatoire** avec compte Google
- ✅ **Sauvegarde automatique** sur Google Drive
- ✅ **Synchronisation multi-appareils**
- ✅ **Données privées** (stockées dans le dossier appDataFolder)
- ✅ **Restauration automatique** au login
- ✅ **Historique illimité** dans le cloud

## 🚀 Configuration Firebase & Google Cloud

### Étape 1 : Créer un projet Firebase

1. Allez sur [Firebase Console](https://console.firebase.google.com/)
2. Cliquez sur **"Ajouter un projet"**
3. Nom du projet : `ecotrace-app`
4. Activez Google Analytics (optionnel)
5. Créez le projet

### Étape 2 : Ajouter l'application Android

1. Dans Firebase Console, cliquez sur l'icône Android
2. Package name : `com.ecotrace.app`
3. Nickname : `EcoTrace Android`
4. SHA-1 certificate (debug) :
   ```bash
   keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
   ```
5. Téléchargez `google-services.json`
6. Placez-le dans `app/google-services.json`

### Étape 3 : Activer Firebase Authentication

1. Dans Firebase Console → **Authentication**
2. Cliquez sur **"Commencer"**
3. Activez **Google** comme fournisseur de connexion
4. Configurez l'écran de consentement OAuth

### Étape 4 : Activer Google Drive API

1. Allez sur [Google Cloud Console](https://console.cloud.google.com/)
2. Sélectionnez votre projet Firebase
3. **APIs & Services** → **Library**
4. Recherchez **"Google Drive API"**
5. Cliquez sur **"Activer"**

### Étape 5 : Configurer OAuth 2.0

1. **APIs & Services** → **Credentials**
2. Créez un **OAuth 2.0 Client ID** de type **Android**
3. Package name : `com.ecotrace.app`
4. SHA-1 certificate (même que l'étape 2)
5. Créez également un **Web client ID** (pour Firebase Auth)
6. Copiez le **Web Client ID**

### Étape 6 : Mettre à jour le code

Ouvrez `AuthRepository.kt` et remplacez :

```kotlin
.requestIdToken("YOUR_WEB_CLIENT_ID") // À remplacer
```

Par :

```kotlin
.requestIdToken("VOTRE_WEB_CLIENT_ID_ICI.apps.googleusercontent.com")
```

## 📁 Structure des données sur Drive

Les données sont sauvegardées dans le dossier **appDataFolder** de Google Drive, qui est :
- ✅ **Privé** : Invisible pour l'utilisateur et les autres apps
- ✅ **Automatique** : Géré par l'app uniquement
- ✅ **Sécurisé** : Accessible uniquement par l'app

### Fichier sauvegardé

**Nom** : `ecotrace_user_data.json`

**Contenu** :
```json
{
  "emissions": [
    {
      "id": "uuid",
      "category": "CAR_ESSENCE",
      "value": 50.0,
      "kgCo2e": 10.9,
      "date": 1708387200000,
      "note": "Trajet Paris-Lyon"
    }
  ],
  "products": [
    {
      "id": "uuid",
      "barcode": "3017620422003",
      "name": "Nutella",
      "brand": "Ferrero",
      "kgCo2ePer100g": 5.3,
      "weight": 400.0,
      "date": 1708387200000
    }
  ],
  "lastSyncTimestamp": 1708387200000
}
```

## 🔄 Flux de synchronisation

### Premier login
1. Utilisateur se connecte avec Google
2. App vérifie si des données existent sur Drive
3. Si **OUI** → Restaure les données
4. Si **NON** → Sauvegarde les données locales (si existantes)

### Utilisation normale
1. Utilisateur ajoute une émission/produit
2. Données sauvegardées localement (Room)
3. **Synchronisation automatique** vers Drive
4. En cas d'erreur, retry automatique

### Changement d'appareil
1. Utilisateur se connecte sur nouvel appareil
2. App restaure automatiquement toutes les données depuis Drive
3. Synchronisation bidirectionnelle activée

## 🔒 Sécurité & Confidentialité

### Permissions demandées
- `DriveScopes.DRIVE_FILE` : Accès aux fichiers créés par l'app
- `DriveScopes.DRIVE_APPDATA` : Accès au dossier privé appDataFolder

### Ce que l'app NE PEUT PAS faire
- ❌ Lire les autres fichiers Drive de l'utilisateur
- ❌ Modifier les documents personnels
- ❌ Partager les données avec d'autres apps
- ❌ Accéder aux emails ou contacts

### Ce que l'app PEUT faire
- ✅ Créer/lire/modifier ses propres fichiers dans appDataFolder
- ✅ Sauvegarder les données de l'utilisateur
- ✅ Restaurer les données de l'utilisateur

## 🧪 Tests

### Tester la connexion Google

```bash
# Installer l'app
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Lancer l'app
adb shell am start -n com.ecotrace.app/.MainActivity
```

### Vérifier les logs

```bash
adb logcat | grep -E "EcoTrace|Drive|Auth"
```

### Tester la synchronisation

1. Connectez-vous avec un compte Google
2. Ajoutez des émissions/produits
3. Déconnectez-vous
4. Reconnectez-vous → Les données doivent être restaurées

### Tester multi-appareils

1. Connectez-vous sur appareil A
2. Ajoutez des données
3. Connectez-vous sur appareil B avec le même compte
4. Les données doivent apparaître automatiquement

## ⚠️ Limitations

### Quotas Google Drive API
- **Requêtes** : 1 000 requêtes/100 secondes/utilisateur
- **Stockage** : 15 GB gratuits par compte Google
- **Taille fichier** : Pas de limite pour appDataFolder

### Gestion des erreurs
- Pas de connexion internet → Données sauvegardées localement
- Quota dépassé → Retry automatique après délai
- Compte Google révoqué → Demande de reconnexion

## 🚀 Déploiement Production

### Certificat de signature

Pour la version release, générez un certificat :

```bash
keytool -genkey -v -keystore ecotrace-release.keystore -alias ecotrace -keyalg RSA -keysize 2048 -validity 10000
```

Récupérez le SHA-1 :

```bash
keytool -list -v -keystore ecotrace-release.keystore -alias ecotrace
```

Ajoutez ce SHA-1 dans :
1. Firebase Console → Paramètres du projet → SHA certificate fingerprints
2. Google Cloud Console → OAuth 2.0 Client ID

### Configuration build.gradle.kts

```kotlin
android {
    signingConfigs {
        create("release") {
            storeFile = file("ecotrace-release.keystore")
            storePassword = "VOTRE_MOT_DE_PASSE"
            keyAlias = "ecotrace"
            keyPassword = "VOTRE_MOT_DE_PASSE"
        }
    }
    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}
```

## 📊 Monitoring

### Firebase Analytics

Les événements suivants sont trackés :
- `login_success` : Connexion réussie
- `login_failed` : Échec de connexion
- `sync_success` : Synchronisation réussie
- `sync_failed` : Échec de synchronisation
- `restore_success` : Restauration réussie
- `restore_failed` : Échec de restauration

### Crashlytics

Activez Crashlytics pour monitorer les erreurs :

```kotlin
dependencies {
    implementation("com.google.firebase:firebase-crashlytics-ktx")
}
```

## 🆘 Dépannage

### Erreur "API not enabled"
→ Activez Google Drive API dans Google Cloud Console

### Erreur "Invalid client ID"
→ Vérifiez que le Web Client ID est correct dans AuthRepository.kt

### Erreur "Permission denied"
→ Vérifiez les scopes Drive dans GoogleSignInOptions

### Données non synchronisées
→ Vérifiez les logs et la connexion internet

### Compte Google non reconnu
→ Vérifiez le SHA-1 dans Firebase Console

## 📝 Checklist avant publication

- [ ] `google-services.json` configuré
- [ ] Web Client ID mis à jour dans le code
- [ ] Google Drive API activée
- [ ] OAuth 2.0 configuré (Android + Web)
- [ ] SHA-1 de release ajouté
- [ ] Tests de synchronisation effectués
- [ ] Politique de confidentialité mise à jour
- [ ] Mentions légales ajoutées
- [ ] Analytics configuré
- [ ] Crashlytics activé

---

**Documentation officielle** :
- [Firebase Authentication](https://firebase.google.com/docs/auth)
- [Google Drive API](https://developers.google.com/drive/api/guides/about-sdk)
- [Google Sign-In Android](https://developers.google.com/identity/sign-in/android/start)
