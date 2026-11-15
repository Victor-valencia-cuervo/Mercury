# Aviso de atribución

Mercurio es una obra derivada de **BitChord** (https://github.com/kushagrasinghx/BitChord),
licenciada bajo GNU General Public License v3.0. El texto completo está en `LICENSE`.

## Cambios realizados respecto a BitChord

- Marca: nombre, íconos y logo sustituidos por los de Mercurio.
- Eliminados: actualizador interno, conteo de usuarios activos (Presence), pantalla festiva de Diwali,
  íconos alternativos, módulos `web`, `backend` y `tv`, y los enlaces y servidores del autor original.
- Listen Together queda sin servidor por defecto y oculto de Ajustes.
- Añadido: saludo aleatorio en el Home (`sharedUi/.../ui/greeting/Greeter.kt`).
- Añadido: capa de Supabase (`app/.../data/cloud/`) y esquema SQL (`supabase/schema.sql`).
- Nuevo identificador de aplicación: `com.mercurio.app` (el paquete interno de Kotlin se mantiene).

Si distribuyes Mercurio, la GPL-3.0 exige poner a disposición el código fuente correspondiente
y conservar esta licencia y los avisos de copyright originales.
