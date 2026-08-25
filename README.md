# VelociCord

VelociCord conecta un proxy de Velocity con Discord mediante JDA. Permite sincronizar el chat, anunciar conexiones y desconexiones, reenviar mensajes de Discord a los jugadores y mostrar el número de jugadores conectados en un canal de voz.

## Características

- Chat de Minecraft a Discord por servidor.
- Mensajes de entrada, cambio de servidor y salida.
- Mensajes de Discord hacia el proxy o hacia los jugadores de un servidor concreto.
- Estado personalizado del bot de Discord.
- Respuestas automáticas configurables para comandos o palabras clave.
- Contador opcional de jugadores en el nombre de un canal de voz.
- Formatos configurables con MiniMessage y colores de Discord.
- Integración opcional con LuckPerms para grupos y permisos.
- Recarga de configuración mediante comando.

## Requisitos

- Velocity 3.1.1 o compatible.
- Java 17 o superior.
- Un bot de Discord con acceso al servidor y permisos para leer y enviar mensajes.
- LuckPerms es opcional.

## Instalación

1. Compila el proyecto con Maven:

   ```text
   mvn clean package
   ```

2. Copia el plugin resultante en la carpeta `target` a la carpeta `plugins` de Velocity.
3. Inicia Velocity una vez para generar la configuración.
4. Edita `plugins/velocicord/config.yml` y añade el token del bot y los IDs de los canales.
5. Reinicia el proxy.

## Configuración

La configuración se encuentra en `config.yml`.

```yaml
settings:
  joinLeaveMessages: true
  chatSync: true
  debug: false

discord:
  botToken: "TOKEN_DEL_BOT"
  status: "playing Velocity Server"
  useConfigStatus: true

channels:
  global: 123456789012345678
  lobby: 123456789012345678

autoReplies:
  "!ip": "miservidor.com"
  "!web": "https://miservidor.com"

voiceStatus:
  enabled: false
  channelId: 123456789012345678
  format: "🎮 Online: %count%"
  updateIntervalSeconds: 30
```

Los nombres bajo `channels` deben coincidir con los nombres de los servidores registrados en Velocity. El canal `global` recibe los mensajes de Discord para todo el proxy. El canal específico de un servidor solo reenvía el mensaje a los jugadores conectados allí.

Las claves de `autoReplies` se comparan exactamente, ignorando espacios al principio y al final. La respuesta se publica en el mismo canal de Discord.

## Comandos y permisos

| Comando                               | Permiso                        |
| ------------------------------------- | ------------------------------ |
| `/velocicord reload`                  | `velocicord.command.reload`    |
| `/velocicord status <modo> <mensaje>` | `velocicord.command.status`    |
| Notificaciones de conexión            | `velocicord.notify.connection` |

También están disponibles los alias `/vc`, `/vcord` y `/discord`.

Los modos de estado admitidos son `playing`, `watching`, `listening` y `custom`.

## Formatos

El formato `format.discord.from` se usa al enviar mensajes de Discord a Minecraft. El formato `format.discord.to` se usa al enviar mensajes de Minecraft a Discord.

Variables disponibles:

- `%user%`: nombre del jugador o usuario.
- `%role%`: grupo o rol.
- `%message%`: contenido del mensaje.
- `%server%`: servidor actual de Minecraft.
- `%color%`: color del rol de Discord.
- `%count%`: jugadores conectados en el nombre del canal de voz.

## Desarrollo

El proyecto usa Maven, Java 17, Velocity API, JDA, Adventure MiniMessage y SnakeYAML. El artefacto final se genera en `target/` y esa carpeta no se versiona.

Durante la investigación del actualizador de canales de voz se añadieron logs con el prefijo `[DIAG]`. Registran la instancia del updater, la tarea programada y el resultado de cada solicitud de cambio de nombre. Se pueden retirar después de confirmar el origen de las ejecuciones duplicadas.

## Licencia

VelociCord se distribuye bajo la [licencia MIT](LICENSE).
