<p align="center">
  <img src="Logo.png" width="120" alt="Mercurio" />
</p>

<h1 align="center">Mercurio</h1>

<p align="center">Reproductor de música personal para Android (y más adelante PC), con temas que cambian por completo la interfaz.</p>

> Mercurio es un fork de [BitChord](https://github.com/kushagrasinghx/BitChord) y se distribuye bajo **GPL-3.0**. Ver [NOTICE.md](NOTICE.md).

## Estado

Fases 1 a 5 hechas: limpieza, marca, saludo, Supabase y sistema de temas con fondo de pantalla.

| Fase | Contenido | Estado |
|---|---|---|
| 1 | Limpieza (actualizador, telemetría, servidores y enlaces del autor) | ✅ |
| 2 | Marca Mercurio, ícono, saludo aleatorio con tu nombre | ✅ |
| 3 | Supabase: sesión anónima, perfil (nombre) e historial | ✅ (ver abajo) |
| 4 | Sistema de temas (base): estilo, acento, fondo de pantalla | ✅ |
| 5 | Temas: Neobrutalismo, Windows 95, Vaporwave, Space Computer | ✅ (primera versión) |
| 6 | Versión de PC | ⏳ |
| 7 | Mascotas flotantes y extras | ⏳ |

## Supabase (Fase 3)

La app usa Supabase con **inicio de sesión anónimo** (sin pantalla de login) y Row Level Security: cada instalación solo ve sus propios datos.

Configuración, una sola vez:

1. En Supabase abre **SQL Editor**, pega todo `supabase/schema.sql` y pulsa **Run**.
2. Ve a **Authentication → Sign In / Providers** y activa **Allow anonymous sign-ins**.
3. (Opcional) Para usar otro proyecto, define `SUPABASE_URL` y `SUPABASE_KEY` (clave *publishable*) en `local.properties`. Nunca uses la clave `secret`/`service_role` en la app.

Qué se sincroniza hoy: el nombre del perfil (se restaura si la instalación local no lo tiene) y una fila por reproducción contada. Los "me gusta" siguen siendo los de la cuenta de YouTube Music; la tabla `favorites` ya existe para cuando Mercurio tenga los suyos.

Nota: una sesión anónima es de esa instalación. Si desinstalas, se crea otro usuario; para conservar los datos entre dispositivos hará falta vincular un correo (pendiente).

## Temas y fondo de pantalla

En **Ajustes → Apariencia**:

- **Tema** (sistema / claro / oscuro) funciona con todos los estilos.
- **Estilo**: BitChord (el original), Neobrutalismo, Windows 95, Vaporwave y Space Computer. Cada uno tiene versión clara y oscura.
- **Color de acento**: sustituye el color principal del estilo (o vuelve al del estilo).
- **Fondo de pantalla**: elige una imagen de la galería. Se copia dentro de la app (reducida a 1600 px), así que no pide permisos ni se pierde si borras la original. Con imagen puedes ajustar la **capa sobre la imagen** (oscurece en modo oscuro, aclara en modo claro) y el **desenfoque**.

Cómo está hecho (para añadir un estilo nuevo): `data/settings/ThemeStyle.kt` (la lista), `ui/theme/ThemeStyles.kt` (paletas, formas y marcos), `ui/theme/Theme.kt` (tipografía) y `ui/theme/ThemeBackdrop.kt` (fondo y fondo de pantalla). Los estilos no añaden fuentes ni imágenes: todo se dibuja con código, así que el APK no crece.

## Compilar el APK con GitHub Actions

1. Sube este proyecto a un repositorio de GitHub (rama `main`).
2. Abre la pestaña **Actions** → **Build Mercurio APK** → **Run workflow** (también corre solo en cada push).
3. Al terminar (10-25 min), abre la ejecución y descarga **mercurio-apk** en *Artifacts*.
4. Dentro del zip, el archivo `app-prod-universal-release.apk` funciona en cualquier teléfono.

### Versión lite

En **Actions → Build Mercurio APK → Run workflow** elige `lite` (o `both`) en *edition*. Descarga **mercurio-lite-apk**. Es solo arm64-v8a (casi todos los teléfonos de los últimos años), paquete `com.mercurio.app.lite` (se instala junto a la full) y arranca con desenfoque dinámico y animaciones reducidas y sin portadas animadas. En local: `./gradlew -Pmercurio.lite=true :app:assembleProdRelease`.

### Firma (opcional, recomendado)

Sin configuración, cada build usa una llave temporal: el APK se instala, pero para instalar uno nuevo hay que desinstalar el anterior. Para poder **actualizar encima**, crea tu llave una vez y guárdala como *secrets* del repositorio (*Settings → Secrets and variables → Actions*):

```bash
keytool -genkeypair -v -keystore mercurio-release.jks -keyalg RSA -keysize 2048 \
  -validity 10000 -alias mercurio
base64 -w0 mercurio-release.jks   # copia el resultado en KEYSTORE_BASE64
```

Secrets: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
**Guarda el `.jks` y las contraseñas en un lugar seguro**: si los pierdes, no podrás actualizar la app instalada.

## Compilar en local

Requiere JDK 17, Android SDK (compileSdk 37) y NDK con CMake 3.22.1.

```bash
./gradlew assembleProdRelease -Pbitchord.bundleWear=false
```

## Estructura

- `app/` — app Android (UI, reproducción, ajustes)
- `shared/`, `sharedUi/` — código compartido Android/JVM (base para el PC)
- `native/` — código C++ (análisis de audio)
- `wearos/` — app de Wear OS (desactivada por ahora)

## Licencia

GPL-3.0. Mercurio incluye código de BitChord © sus autores; ver [LICENSE](LICENSE) y [NOTICE.md](NOTICE.md).
