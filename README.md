# 🌿 EcoTrace — App Android Empreinte Carbone

Application Android native en **Kotlin + Jetpack Compose** pour suivre son empreinte carbone personnelle selon le protocole GHG (Scopes 1, 2, 3) avec **scan de code-barres** pour les produits.

## Fonctionnalités

### 📊 Suivi des Émissions
- **Tableau de bord** — Score mensuel combiné (activités + produits) en tCO₂e
- **Scope 1, 2, 3** — Toutes les catégories du protocole GHG (voiture, énergie, alimentation, avion, achats...)
- **Validation des entrées** — Limites intelligentes par catégorie pour éviter les erreurs
- **Gestion d'erreurs** — Messages clairs en cas de problème

### � Scan de Produits
- **Scan de code-barres** — Utilisez la caméra pour scanner vos achats
- **Base de données produits** — 25+ produits français pré-enregistrés (Nutella, Coca-Cola, etc.)
- **Saisie manuelle** — Ajoutez des produits non reconnus
- **Calcul automatique** — Empreinte carbone calculée selon le poids

### �📈 Analyse & Historique
- **Historique 6 mois** — Graphique en barres de l'évolution mensuelle
- **Comparaison nationale** — Vs. moyenne française (750 kg/mois) et objectif Accord de Paris (167 kg/mois)
- **Onglets séparés** — Émissions d'activités et produits scannés
- **Suppression facile** — Cliquez sur une entrée pour la supprimer

### 💡 Conseils & Optimisation
- **Conseils personnalisés** — Générés dynamiquement selon vos émissions
- **Économies potentielles** — Calcul de l'impact de chaque conseil
- **Top 3 produits** — Produits avec le plus d'impact carbone

## Structure du projet

```
app/src/main/java/com/ecotrace/app/
├── MainActivity.kt              # Entry point + Navigation (4 écrans)
├── data/
│   ├── models/
│   │   └── Models.kt            # EmissionEntry, ScannedProduct, ProductInfo, etc.
│   └── repository/
│       ├── Database.kt          # Room DB + DAO + Migrations
│       ├── EmissionRepository.kt
│       ├── ProductRepository.kt # Nouveau: gestion produits
│       └── AppModule.kt         # Hilt DI
├── viewmodel/
│   ├── EmissionViewModel.kt     # + Validation & gestion d'erreurs
│   └── ProductViewModel.kt      # Nouveau: ViewModel produits
└── ui/
    ├── theme/
    │   └── Theme.kt             # Couleurs, typographie
    ├── components/
    │   └── Components.kt        # ScoreCard, ProductsSummaryCard, ErrorCard...
    └── screens/
        ├── HomeScreen.kt        # Dashboard (émissions + produits)
        ├── AddEntryScreen.kt    # Formulaire ajout avec validation
        ├── ScanScreen.kt        # Nouveau: Scan de code-barres
        └── HistoryScreen.kt     # Historique avec onglets
```

## Prérequis

- Android Studio Hedgehog (2023.1.1) ou plus récent
- JDK 17
- Android SDK 34
- Appareil avec caméra (pour le scan de produits)
- API 26+ (Android 8.0+)

## Installation

1. **Cloner le projet** :
   ```bash
   git clone <repository-url>
   cd ecotrace-android
   ```

2. **Ouvrir dans Android Studio** :
   `File → Open → sélectionner le dossier ecotrace-android/`

3. **Sync Gradle** (automatique à l'ouverture)

4. **Lancer** sur un appareil physique (recommandé pour le scan) ou émulateur (API 26+)

5. **Autoriser la caméra** lors de la première utilisation du scan

## Utilisation

### Ajouter une émission manuelle
1. Onglet **Ajouter** → Sélectionner une catégorie (Scope 1, 2 ou 3)
2. Entrer la valeur (km, kWh, kg, etc.)
3. Ajouter une note optionnelle
4. Cliquer sur **Ajouter l'émission**

### Scanner un produit
1. Onglet **Scanner** → Autoriser la caméra si demandé
2. Placer le code-barres dans le cadre
3. Entrer le poids du produit acheté
4. Cliquer sur **Ajouter à mon empreinte**

### Consulter l'historique
1. Onglet **Historique**
2. Basculer entre **Émissions** et **Produits**
3. Cliquer sur une entrée pour afficher le bouton de suppression

## Facteurs d'émission utilisés

### Activités (Scopes 1, 2, 3)
| Catégorie | Facteur | Source |
|-----------|---------|--------|
| Voiture essence | 0.218 kg CO₂e/km | ADEME |
| Voiture diesel | 0.171 kg CO₂e/km | ADEME |
| Voiture électrique | 0.020 kg CO₂e/km | ADEME |
| Gaz naturel | 2.04 kg CO₂e/m³ | ADEME |
| Fioul | 3.17 kg CO₂e/L | ADEME |
| Électricité (France) | 0.052 kg CO₂e/kWh | RTE 2023 |
| Avion court-courrier | 0.255 kg CO₂e/km | ADEME |
| Avion long-courrier | 0.195 kg CO₂e/km | ADEME |
| Train | 0.004 kg CO₂e/km | ADEME |
| Bœuf | 27 kg CO₂e/kg | GIEC |
| Porc/volaille | 6 kg CO₂e/kg | GIEC |
| Streaming vidéo | 0.036 kg CO₂e/h | ADEME |

### Produits (Base de données)
25+ produits français avec leurs empreintes carbone :
- Nutella, Kinder : ~5 kg CO₂e/100g
- Coca-Cola, Fanta : ~0.3 kg CO₂e/100g
- Fromages : 9-10 kg CO₂e/100g
- Charcuterie : 4-6 kg CO₂e/100g
- Pâtes : ~1 kg CO₂e/100g
- etc.

## Sécurité & Performance

### Corrections apportées
✅ **Migration Room** — Pas de perte de données lors des mises à jour
✅ **Validation des entrées** — Limites par catégorie (ex: max 10000 km/mois)
✅ **Gestion d'erreurs** — Try-catch sur toutes les opérations DB
✅ **ProGuard configuré** — Obfuscation pour release
✅ **Backup désactivé** — Sécurité des données
✅ **Build types** — Debug et Release configurés

## Améliorations futures

- [ ] API externe pour base de données produits (Open Food Facts)
- [ ] Export CSV/JSON des données
- [ ] Graphiques avancés (par catégorie, tendances)
- [ ] Objectifs personnalisés
- [ ] Partage sur réseaux sociaux
- [ ] Mode sombre/clair
- [ ] Internationalisation (EN, ES, etc.)
- [ ] Widget Android
- [ ] Notifications de rappel

## Technologies utilisées

- **Jetpack Compose** — UI déclarative
- **Room** — Persistence locale avec migrations
- **Hilt** — Injection de dépendances
- **Coroutines + Flow** — Asynchrone réactif
- **Material3** — Design System
- **CameraX** — Accès caméra pour scan
- **ML Kit Barcode Scanning** — Reconnaissance de code-barres
- **Accompanist Permissions** — Gestion des permissions
- **Vico Charts** — Graphiques

## Licence

Ce projet est sous licence MIT. Voir le fichier LICENSE pour plus de détails.

## Contribution

Les contributions sont les bienvenues ! N'hésitez pas à ouvrir une issue ou une pull request.

## Contact

Pour toute question ou suggestion, contactez-nous à [votre-email]
