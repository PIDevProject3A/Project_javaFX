# JavaFX Login/Register

Simple JavaFX (FXML) authentication demo with register/login, in-memory storage, validation, scene switching, and CSS styling.

## Run

```powershell
mvn clean javafx:run
```

L'ecran de login affiche un reCAPTCHA via `WebView` et le charge depuis une URL locale `http://localhost` pour rester compatible avec la verification de domaine Google.

Assurez-vous que `localhost` et `127.0.0.1` sont bien ajoutes dans la console reCAPTCHA pour la cle utilisee.

## Face ID (CompreFace)

Une option est disponible dans l'ecran de connexion:

- `Login with Face ID (camera PC)` (connexion biometrie sans email)

Quand un admin est connecte, une page `Manage Face ID` est disponible depuis le dashboard:

- selection d'un utilisateur (admin/event/finance)
- `Ajouter Face ID`
- `Modifier Face ID`
- `Supprimer Face ID`

Configuration attendue (variables d'environnement ou `-D` JVM):

```powershell
$env:FACE_RECOGNITION_PROVIDER="compreface"
$env:COMPREFACE_API_KEY="810092e7-85de-4f3f-97b2-099709ffa5a3"
$env:COMPREFACE_BASE_URL="http://localhost:8000"
```

Important: le sujet (`subject`) enregistre dans CompreFace doit etre le `face_subject` genere et stocke en base.

Dans cette version, l'application genere un `face_subject` unique par utilisateur et le stocke en base,
ce qui permet de se connecter par photo du visage sans saisir l'email.

### Champs BDD a ajouter

```sql
CREATE TABLE IF NOT EXISTS face_id_profiles (
	id INT AUTO_INCREMENT PRIMARY KEY,
	email VARCHAR(150) NOT NULL,
	face_subject VARCHAR(80) NOT NULL,
	is_enabled BOOLEAN NOT NULL DEFAULT TRUE,
	enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
	UNIQUE KEY uq_face_id_profiles_email (email),
	UNIQUE KEY uq_face_id_profiles_subject (face_subject)
);
```

Si votre base existe deja, appliquez seulement cette table (pas besoin de recreer les autres).

## Build only

```powershell
mvn clean compile
```

