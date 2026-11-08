# VelociCord

VelociCord conecta un proxy de Velocity con Discord usando JDA. Sincroniza el chat entre ambos, envía notificaciones de conexión y desconexión y permite mostrar los jugadores conectados en un canal de voz.

## Características

- Sincronización del chat de Minecraft con Discord.
- Notificaciones de entrada, cambio de servidor y salida.
- Envío de mensajes de Discord al proxy o a un servidor específico.
- Estado personalizado del bot.
- Respuestas automáticas para comandos o palabras clave.
- Contador de jugadores en un canal de voz.
- Formatos configurables con MiniMessage y colores de Discord.
- Integración opcional con LuckPerms.
- Recarga de configuración mediante comando.

## Requisitos

- Velocity 3.1.1 o compatible.
- Java 17 o superior.
- Un bot de Discord con acceso al servidor y permisos para leer y enviar mensajes.
- LuckPerms (opcional).

## Instalación

1. Compila el proyecto:

   ```bash
   mvn clean package
   ```

2. Copia el JAR generado en `target/` a la carpeta `plugins` de Velocity.
3. Inicia Velocity para que se genere la configuración.
4. Edita `plugins/velocicord/config.yml` y configura el bot y los canales de Discord.
5. Reinicia el proxy.

## Configuración

La configuración se encuentra en `plugins/velocicord/config.yml`.

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
  format: "Online: %count%"
  updateIntervalSeconds: 30
```

Los nombres dentro de `channels` deben coincidir con los nombres de los servidores registrados en Velocity.

- `global`: canal que recibe los mensajes de Discord para todo el proxy.
- Un canal específico, como `lobby`: los mensajes se envían únicamente a los jugadores conectados en ese servidor.

Las claves de `autoReplies` se comparan exactamente, ignorando los espacios al principio y al final. Las respuestas se envían al mismo canal donde se recibió el mensaje.

El token del bot debe mantenerse privado. No lo compartas con nadie.

## Comandos y permisos

| Comando | Permiso |
| --- | --- |
| `/velocicord reload` | `velocicord.command.reload` |
| `/velocicord status <modo> <mensaje>` | `velocicord.command.status` |
| Notificaciones de conexión | `velocicord.notify.connection` |

También puedes usar los alias `/vc`, `/vcord` y `/discord`.

Los modos de estado disponibles son `playing`, `watching`, `listening` y `custom`.

## Formatos

Hay dos formatos principales:

- `format.discord.from`: mensajes enviados de Discord a Minecraft.
- `format.discord.to`: mensajes enviados de Minecraft a Discord.

Variables disponibles:

| Variable | Descripción |
| --- | --- |
| `%user%` | Nombre del jugador o usuario. |
| `%role%` | Grupo o rol. |
| `%message%` | Contenido del mensaje. |
| `%server%` | Servidor actual de Minecraft. |
| `%color%` | Color del rol de Discord. |
| `%count%` | Número de jugadores conectados. |

Los formatos de Minecraft utilizan MiniMessage.

## Desarrollo

El proyecto utiliza Maven y Java 17. Entre sus dependencias están Velocity API, JDA, Adventure MiniMessage y SnakeYAML.

Para compilarlo:

```bash
mvn clean package
```

El JAR se genera en `target/`. Esta carpeta no se incluye en el repositorio.

## Licencia

VelociCord se distribuye bajo la [licencia MIT](LICENSE).
