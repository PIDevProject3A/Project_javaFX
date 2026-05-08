# 🌍 Guide d'Utilisation: Changement Dynamique de Langue et Thème

## ✨ Fonctionnalités Implémentées

### 1. **Changement Dynamique de Langue**
- ✅ Boutons **English (EN)** et **Français (FR)** visibles sur:
  - Écran de Login
  - Écran de Registration
  - Et tous les autres écrans par la suite
- ✅ Changement instantané de TOUTE l'interface
- ✅ Persistence de la préférence de langue dans `UserSession`

### 2. **Mode Sombre Professionnel (Dark Mode)**
- ✅ Bouton Thème (🌙/☀️) pour basculer clair/sombre
- ✅ Redesign complet du dark mode:
  - Couleur de base: **#0F172A** (Slate 900 - Plus professionnel)
  - Couleur texte: **#F1F5F9** (Slate 100 - Plus lisible)
  - Accent gris: **#94A3B8** (Slate 400)
  - Bordures subtiles avec transparence
- ✅ Transitions fluides entre thèmes
- ✅ Tous les éléments supportent le dark mode

### 3. **Barre de Contrôle d'Apparence**
- Position: **En haut à droite** des écrans
- Contient:
  - Groupe de sélection de langue (EN/FR)
  - Bouton de basculement de thème
- Style: Carte élégante avec glassmorphism
- Support complet light/dark mode

---

## 🎨 Améliorations du Design

### Light Mode (Original)
```
Fond: Blanc #FFFFFF
Texte: Gris foncé #111827
Accent: Émeraude #10B981
```

### Dark Mode (Nouveau)
```
Fond: Slate 900 #0F172A
Texte: Slate 100 #F1F5F9
Accent: Émeraude #10B981 (inchangé)
Surface: Slate 800 #1E293B
Bordure: Slate 400 avec transparence
```

---

## 🔧 Architecture Technique

### Nouvelles Classes Créées

#### 1. **LanguageManager.java**
Gestionnaire centralisé pour la langue
```java
LanguageManager.getInstance().setLocale("en");  // English
LanguageManager.getInstance().setLocale("fr");  // Français
```

#### 2. **ThemeManager.java**
Gestionnaire centralisé pour le thème
```java
ThemeManager.getInstance().setDarkMode(true);   // Activer dark
ThemeManager.getInstance().setDarkMode(false);  // Désactiver dark
ThemeManager.getInstance().toggleDarkMode();    // Basculer
```

#### 3. **AppearanceControlBar.java**
Utilitaire pour créer la barre de contrôle
```java
HBox bar = AppearanceControlBar.createAppearanceBar(stage, scene);
```

### Fichiers Modifiés

| Fichier | Modification |
|---------|-------------|
| `Main.java` | Intégration LanguageManager et ThemeManager |
| `LoginController.java` | Ajout barre d'apparence + support langue/thème |
| `RegisterController.java` | Ajout barre d'apparence + support langue/thème |
| `Login.fxml` | Ajout HBox pour la barre d'apparence |
| `Register.fxml` | Ajout HBox pour la barre d'apparence |
| `style.css` | Redesign dark mode + nouveaux styles |
| `messages_en.properties` | Clés i18n pour langue/thème |
| `messages_fr.properties` | Clés i18n pour langue/thème |

---

## 🚀 Comment Utiliser

### Pour les Utilisateurs
1. Sur l'écran de Login ou Register
2. Cliquez sur **English** ou **Français** pour changer la langue
3. Cliquez sur **🌙** (mode sombre) ou **☀️** (mode clair) pour basculer le thème
4. Les changements s'appliquent immédiatement à toute l'interface

### Pour les Développeurs - Ajouter la Barre à une Autre Scène

#### Étape 1: Ajouter le HBox au FXML
```xml
<HBox fx:id="appearanceBar" spacing="12" alignment="TOP_RIGHT">
    <padding>
        <Insets top="12" right="12" bottom="12" left="12"/>
    </padding>
</HBox>
```

#### Étape 2: Ajouter le Code au Contrôleur
```java
import com.esprit.utils.AppearanceControlBar;
import javafx.application.Platform;
import javafx.scene.layout.HBox;

public class MyController {
    @FXML
    private HBox appearanceBar;
    
    @FXML
    private void initialize() {
        // ... votre code existant ...
        
        // Initialiser la barre d'apparence
        Platform.runLater(this::initializeAppearanceBar);
    }
    
    private void initializeAppearanceBar() {
        try {
            if (appearanceBar != null && someControl != null) {
                javafx.scene.Scene scene = someControl.getScene();
                if (scene != null) {
                    javafx.stage.Stage stage = (javafx.stage.Stage) scene.getWindow();
                    if (stage != null) {
                        HBox bar = AppearanceControlBar.createAppearanceBar(stage, scene);
                        appearanceBar.getChildren().setAll(bar);
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
```

---

## 🎯 CSS Classes Disponibles

### Light Mode
- `.appearance-container` - Conteneur principal
- `.lang-button` - Bouton de langue inactif
- `.lang-button-active` - Bouton de langue actif
- `.theme-button` - Bouton de thème

### Dark Mode (Automatique)
- `.dark-mode .appearance-container`
- `.dark-mode .lang-button`
- `.dark-mode .lang-button-active`
- `.dark-mode .theme-button`

---

## 📋 Checklist des Écrans

### Complétés ✅
- [x] Login.fxml
- [x] Register.fxml
- [x] LanguageManager + ThemeManager

### À Faire (Optionnel)
- [ ] UserDashboard.fxml
- [ ] AdminDashboard.fxml
- [ ] Tous les autres écrans FXML

---

## 🐛 Dépannage

### Si la barre n'apparaît pas
1. Vérifiez que `appearanceBar` est défini dans le FXML
2. Vérifiez que le contrôleur appelle `initializeAppearanceBar()` dans `Platform.runLater()`
3. Vérifiez les logs pour les exceptions

### Si la langue ne change pas
1. Vérifiez que les fichiers `messages_en.properties` et `messages_fr.properties` existent
2. Vérifiez que les clés i18n sont correctes: `%key.name`
3. Vérifiez que `ResourceBundle bundle` est injecté dans le contrôleur

### Si le dark mode ne s'applique pas
1. Vérifiez que `style.css` contient les styles `.dark-mode`
2. Vérifiez que `ThemeManager.getInstance().applyThemeToNode()` est appelé

---

## 🎨 Personnalisation

### Changer les Couleurs du Dark Mode
Modifiez dans `style.css`:
```css
.dark-mode {
    -color-bg: #YOUR_COLOR; /* Couleur de fond */
    -color-text: #YOUR_COLOR; /* Couleur du texte */
    /* ... etc ... */
}
```

### Changer la Position de la Barre
Modifiez l'alignment dans le FXML:
```xml
<HBox fx:id="appearanceBar" spacing="12" alignment="TOP_LEFT">  <!-- TOP_LEFT au lieu de TOP_RIGHT -->
```

---

## 📞 Besoin d'Aide?

Consultez:
- `LanguageManager.java` - Gestion complète de la langue
- `ThemeManager.java` - Gestion complète du thème
- `AppearanceControlBar.java` - Composant UI
- `style.css` - Tous les styles (lignes 400+)
