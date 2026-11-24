# Configuración de Firebase y GitHub (Reto 12)

Estos pasos se hacen una sola vez, desde el navegador, con el plan gratuito (Spark) de Firebase.
No requieren tarjeta de crédito.

## 1. Crear el proyecto de Firebase

1. Entra a <https://console.firebase.google.com> con tu cuenta de Google y pulsa **Crear un proyecto**
   (por ejemplo `agro-ia`). Google Analytics no hace falta; puedes desactivarlo.
2. En la página del proyecto pulsa el ícono de **Android** para agregar una app.
3. En **Nombre del paquete de Android** escribe exactamente: `com.jffloresriera.maizellm`
4. El certificado SHA-1 no es necesario para GitHub; déjalo vacío.
5. Descarga el archivo **`google-services.json`**. Lo usarás en el paso 5.

## 2. Activar el inicio de sesión con GitHub

1. En Firebase: **Authentication > Comenzar > Sign-in method > Agregar proveedor > GitHub**.
2. Activa el interruptor. Firebase muestra una **URL de devolución de llamada** parecida a
   `https://agro-ia.firebaseapp.com/__/auth/handler`. Cópiala y deja esa ventana abierta.
3. En otra pestaña, en GitHub: **Settings > Developer settings > OAuth Apps > New OAuth App**.
   - **Application name:** `Agro IA`
   - **Homepage URL:** `https://github.com/jf-floresriera/Reto12`
   - **Authorization callback URL:** la URL que copiaste de Firebase.
4. Pulsa **Register application**, copia el **Client ID**, pulsa **Generate a new client secret** y
   copia el secreto.
5. Vuelve a Firebase, pega el Client ID y el Client secret y pulsa **Guardar**.

El *client secret* queda guardado en Firebase. No va dentro del APK ni en el repositorio.

## 3. Crear la base de datos Firestore

1. En Firebase: **Firestore Database > Crear base de datos**.
2. Elige una ubicación (por ejemplo `southamerica-east1`, São Paulo) y el **modo de producción**.
3. En la pestaña **Reglas**, borra el contenido, pega el archivo [`firestore.rules`](../firestore.rules)
   de este repositorio y pulsa **Publicar**.

## 4. Número de WhatsApp para las solicitudes de plan

En [`gradle.properties`](../gradle.properties) cambia `agroia.whatsappPagos` por tu número en formato
internacional, sin `+` ni espacios (Colombia: `57` + el celular, p. ej. `573001234567`).

## 5. Entregar `google-services.json` a la compilación

Elige una de dos opciones:

- **Subirlo al repo:** en GitHub, entra a la carpeta `app/`, pulsa **Add file > Upload files** y sube
  `google-services.json`. Este archivo identifica tu proyecto de Firebase; no contiene contraseñas, y la
  seguridad real la dan las reglas de Firestore.
- **Guardarlo como secret** (si el repo es público y prefieres no exponerlo): en el repo,
  **Settings > Secrets and variables > Actions > New repository secret**, nombre
  `GOOGLE_SERVICES_JSON`, y como valor pega el contenido completo del archivo.

## 6. Compilar y distribuir el APK

1. En el repo: **Actions > Compilar APK > Run workflow**.
2. En `modelo_url` pega la URL de descarga directa del `.gguf` (el mismo asset que usaste en el Reto 11).
3. Marca **Publicar el APK como Release** y escribe la versión (p. ej. `v0.2.0`).
4. Al terminar, el APK queda en la sección **Releases** del repo para que la gente lo descargue e instale.

## Administración de usuarios

| Qué quieres hacer | Dónde |
|---|---|
| Ver quién se registró, cuándo y su último acceso | **Authentication > Users** |
| Ver plan, tokens usados y solicitudes de cada usuario | **Firestore Database > Datos > `usuarios`** |
| Cambiar el plan de alguien | En su documento de `usuarios`, edita el campo `plan` |
| Quitarle el acceso a alguien | Pon `plan` = `bloqueado` (no borres el documento) |

Valores válidos del campo `plan`:

| Valor | Plan | Límite |
|---|---|---|
| `gratis` | Gratis | 1.000 tokens en total (valor inicial de todo usuario nuevo) |
| `pro` | Pro | 10.000 tokens por día |
| `ultra` | Ultra Plus | Ilimitado |
| `bloqueado` | Bloqueado | Sin acceso al chat |

El ID de cada documento es el `uid` del usuario. Ese mismo ID aparece en el mensaje de WhatsApp que
envía la persona al solicitar un plan, y además su documento queda con los campos `planSolicitado` y
`fechaSolicitud`.

El usuario recibe el cambio de plan la próxima vez que abra la app con Internet, o al pulsar
**Mi plan > Actualizar**.

No borres el documento de un usuario para bloquearlo: si lo borras y vuelve a iniciar sesión, se
registra de nuevo con el plan gratis y 1.000 tokens nuevos.
