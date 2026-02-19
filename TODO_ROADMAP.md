# 📋 EcoTrace - Roadmap & TODO

## 🎯 Fonctionnalités à implémenter

### 🌍 Internationalisation (En cours)

#### ✅ Complété
- [x] Support FR/EN/ES
- [x] Fichiers strings.xml pour 3 langues
- [x] Navigation traduite
- [x] Documentation INTERNATIONALIZATION.md

#### 🔄 À compléter
- [ ] Extraire TOUS les textes hardcodés vers strings.xml
- [ ] Adapter les formats de nombres par locale
- [ ] Adapter les formats de dates par locale
- [ ] Ajouter plus de langues (DE, IT, PT, AR, ZH, JA)
- [ ] Tester changement de langue en temps réel

---

## 💰 Modèle économique - Plan Gratuit vs Payant

### 🆓 Plan GRATUIT
**Fonctionnalités incluses :**
- ✅ Suivi des émissions Scopes 1, 2, 3 (15 catégories)
- ✅ Scan de produits (limité à 10 produits/mois)
- ✅ Historique 3 mois
- ✅ Graphiques basiques
- ✅ 3 conseils personnalisés
- ✅ Comparaison nationale (France uniquement)
- ✅ Base de données 112 produits français

**Limitations :**
- ❌ Pas d'export de données
- ❌ Pas de synchronisation cloud
- ❌ Publicités (non intrusives)
- ❌ Support communautaire uniquement

### 💎 Plan PREMIUM (2,99€/mois ou 24,99€/an)
**Tout du gratuit +**
- ✅ Scan de produits illimité
- ✅ Historique illimité
- ✅ Graphiques avancés (évolution annuelle, prédictions)
- ✅ Conseils personnalisés illimités
- ✅ Comparaisons internationales (tous pays)
- ✅ Base de données produits mondiale (Open Food Facts)
- ✅ Export CSV/JSON/PDF
- ✅ Synchronisation multi-appareils (cloud)
- ✅ Objectifs personnalisés
- ✅ Notifications intelligentes
- ✅ Mode sombre
- ✅ Sans publicité
- ✅ Support prioritaire
- ✅ Accès anticipé aux nouvelles fonctionnalités

### 🏢 Plan ENTREPRISE (Sur devis)
**Tout du Premium +**
- ✅ Gestion d'équipe
- ✅ Dashboard entreprise
- ✅ Rapports personnalisés
- ✅ API d'intégration
- ✅ Support dédié
- ✅ Formation
- ✅ Branding personnalisé

---

## 📱 Implémentation technique du système d'abonnement

### Phase 1 : Infrastructure (2 semaines)
- [ ] Intégrer Google Play Billing Library
- [ ] Créer les SKUs (produits) dans Google Play Console
- [ ] Implémenter la vérification d'abonnement
- [ ] Créer l'écran de tarification
- [ ] Implémenter le flux d'achat
- [ ] Gérer les états d'abonnement (actif, expiré, annulé)

### Phase 2 : Restrictions (1 semaine)
- [ ] Limiter scan produits à 10/mois pour gratuit
- [ ] Limiter historique à 3 mois pour gratuit
- [ ] Bloquer export pour gratuit
- [ ] Afficher publicités pour gratuit (AdMob)
- [ ] Créer système de "paywall" élégant

### Phase 3 : Fonctionnalités Premium (3 semaines)
- [ ] Synchronisation cloud (Firebase)
- [ ] Export CSV/JSON/PDF
- [ ] Graphiques avancés
- [ ] Notifications intelligentes
- [ ] Mode sombre
- [ ] Objectifs personnalisés

---

## 🌐 Internationalisation complète

### Facteurs d'émission par pays
- [ ] Créer base de données facteurs par pays
- [ ] Électricité : adapter selon mix énergétique
  - France : 0.052 kg CO₂e/kWh (nucléaire)
  - Allemagne : 0.485 kg CO₂e/kWh (charbon)
  - Norvège : 0.013 kg CO₂e/kWh (hydro)
  - Pologne : 0.900 kg CO₂e/kWh (charbon)
- [ ] Transport : adapter selon standards locaux
- [ ] Alimentation : adapter selon modes de production
- [ ] Détection automatique du pays
- [ ] Sélection manuelle du pays dans paramètres

### Base de données produits internationale
- [ ] Intégrer Open Food Facts API
- [ ] Support codes-barres internationaux
- [ ] Cache intelligent des produits
- [ ] Fallback sur base locale
- [ ] Synchronisation périodique

### Devises et unités
- [ ] Support multi-devises (USD, GBP, EUR, JPY, etc.)
- [ ] Conversion automatique selon pays
- [ ] Support unités impériales (miles, gallons, pounds)
- [ ] Adapter seuils de validation par pays

### Comparaisons nationales adaptées
- [ ] Objectifs climatiques par pays
- [ ] Moyennes nationales par pays
- [ ] Contexte local (mix énergétique, transport dominant)

---

## 🔌 Intégrations API

### Open Food Facts (Priorité haute)
- [ ] Créer compte développeur
- [ ] Implémenter client API
- [ ] Gérer cache local
- [ ] Fallback si pas de connexion
- [ ] Contribuer données manquantes

### Carbon Interface API
- [ ] Intégrer pour facteurs d'émission précis
- [ ] Gérer quotas API
- [ ] Cache des résultats

### Firebase
- [ ] Authentication (Google, Email)
- [ ] Firestore pour sync cloud
- [ ] Analytics
- [ ] Crashlytics
- [ ] Remote Config

### Google Play Services
- [ ] Détection de localisation
- [ ] Billing Library
- [ ] AdMob (publicités)

---

## 🎨 Améliorations UI/UX

### Design
- [ ] Mode sombre complet
- [ ] Animations fluides
- [ ] Illustrations personnalisées
- [ ] Onboarding interactif
- [ ] Tutoriels in-app

### Accessibilité
- [ ] Support TalkBack
- [ ] Contraste élevé
- [ ] Tailles de police ajustables
- [ ] Navigation au clavier

### Widgets
- [ ] Widget dashboard
- [ ] Widget graphique mensuel
- [ ] Widget quick-add

---

## 📊 Analytics & Monitoring

- [ ] Google Analytics
- [ ] Firebase Analytics
- [ ] Crashlytics
- [ ] Performance monitoring
- [ ] User feedback in-app

---

## 🔒 Sécurité & Conformité

### RGPD (Europe)
- [ ] Politique de confidentialité
- [ ] Consentement cookies
- [ ] Droit à l'oubli
- [ ] Export données personnelles
- [ ] Anonymisation données

### CCPA (Californie)
- [ ] Privacy policy
- [ ] Opt-out vente données
- [ ] Transparence collecte

### COPPA (US, < 13 ans)
- [ ] Vérification âge
- [ ] Consentement parental
- [ ] Limitations collecte

### Sécurité
- [ ] Chiffrement données locales
- [ ] HTTPS uniquement
- [ ] ProGuard/R8 obfuscation
- [ ] Certificate pinning
- [ ] Audit sécurité

---

## 🚀 Fonctionnalités futures

### Social
- [ ] Partage sur réseaux sociaux
- [ ] Défis entre amis
- [ ] Classements
- [ ] Badges et récompenses

### Gamification
- [ ] Système de points
- [ ] Niveaux
- [ ] Achievements
- [ ] Streaks (séries)

### IA & Machine Learning
- [ ] Prédictions d'émissions
- [ ] Recommandations personnalisées
- [ ] Détection automatique d'activités
- [ ] OCR pour tickets de caisse

### Intégrations
- [ ] Google Fit (activités physiques)
- [ ] Google Maps (trajets)
- [ ] Calendrier (voyages)
- [ ] Banque (achats)

---

## 📱 Plateformes

### Android (Actuel)
- [x] Version 1.0 fonctionnelle
- [ ] Publication Google Play Store
- [ ] Optimisation performances
- [ ] Tests sur différents appareils

### iOS (Futur)
- [ ] Port Swift/SwiftUI
- [ ] Publication App Store
- [ ] Synchronisation cross-platform

### Web (Futur)
- [ ] Progressive Web App (PWA)
- [ ] Dashboard web
- [ ] Synchronisation avec mobile

---

## 📈 Marketing & Distribution

### Google Play Store
- [ ] Créer compte développeur (25$ one-time)
- [ ] Préparer assets (icône, screenshots, vidéo)
- [ ] Rédiger description optimisée ASO
- [ ] Définir catégories et tags
- [ ] Soumettre pour review
- [ ] Lancer campagne de lancement

### App Store (iOS futur)
- [ ] Créer compte développeur (99$/an)
- [ ] Préparer assets iOS
- [ ] Soumettre pour review

### Marketing
- [ ] Site web vitrine
- [ ] Blog (conseils écologie)
- [ ] Réseaux sociaux
- [ ] Partenariats ONG écologiques
- [ ] Relations presse

---

## 🧪 Tests & Qualité

### Tests unitaires
- [ ] ViewModels
- [ ] Repositories
- [ ] Use cases
- [ ] Utilities

### Tests d'intégration
- [ ] Database migrations
- [ ] API calls
- [ ] Navigation

### Tests UI
- [ ] Espresso
- [ ] Screenshot tests
- [ ] Accessibility tests

### Tests de performance
- [ ] Profiling mémoire
- [ ] Temps de démarrage
- [ ] Fluidité animations

---

## 📝 Documentation

- [ ] Documentation API
- [ ] Guide de contribution
- [ ] Architecture decision records (ADR)
- [ ] Guide de style code
- [ ] Changelog détaillé

---

## 🎯 Priorités

### 🔴 Priorité CRITIQUE (Avant publication)
1. Extraire tous textes hardcodés → strings.xml
2. Tests sur appareils réels
3. Politique de confidentialité
4. Mentions légales
5. Optimisation performances

### 🟠 Priorité HAUTE (Version 1.1)
1. Intégration Open Food Facts API
2. Mode sombre
3. Export CSV/PDF
4. Système d'abonnement Premium
5. Synchronisation cloud

### 🟡 Priorité MOYENNE (Version 1.2)
1. Plus de langues (DE, IT, PT)
2. Widgets Android
3. Notifications intelligentes
4. Gamification basique
5. Facteurs d'émission par pays

### 🟢 Priorité BASSE (Version 2.0+)
1. Version iOS
2. Progressive Web App
3. Intégrations tierces (Google Fit, Maps)
4. IA/ML prédictions
5. Version entreprise

---

## 💡 Idées en vrac

- [ ] Calculateur d'empreinte carbone pour événements
- [ ] Mode "voyage" avec tracking GPS
- [ ] Intégration Stripe pour paiements
- [ ] Programme de parrainage
- [ ] Marketplace de crédits carbone
- [ ] Partenariats avec marques éco-responsables
- [ ] Certification B-Corp
- [ ] Open source une partie du code
- [ ] API publique pour développeurs
- [ ] Extension navigateur pour e-commerce

---

**Dernière mise à jour** : 19 février 2026
**Version actuelle** : 1.0.0
**Prochaine version** : 1.1.0 (Internationalisation + Premium)
