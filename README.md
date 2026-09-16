# CobbleTabs

![CobbleTabs](src/main/resources/assets/cobbletabs/textures/gui/logo.png)

**Creador:** [Juanmi64](https://github.com/Juanmi64)

**Mod creado para Pokegalaxia**, pero se pueden usar en otros servers.

Mod **cliente** para **Minecraft 1.21.1 (Fabric)** que añade pestañas en los laterales del inventario para ejecutar comandos con un solo clic. Pensado para servidores con **Cobblemon** y plugins de comandos personalizados.

## ✨ Características

- **Pestañas laterales** en la GUI del inventario: 3 a la izquierda y 3 a la derecha (reparto automático según el número de pestañas)
- Un clic (izquierdo o derecho) ejecuta el comando de la pestaña al instante, sin escribir en el chat
- **Nombres en negrita y a color**, configurables por pestaña
- **Logo discreto** (marca de agua) en la esquina superior derecha del inventario del jugador
- **Botón para ocultar/mostrar** todas las pestañas, con estado persistente
- **Archivo de configuración** en JSON: añade, reordena, renombra o desactiva pestañas sin tocar el código
- **Tecla F8** para recargar la configuración sin reiniciar el juego
- No se muestran pestañas en cofres ni enderchests, y el logo solo aparece en tu inventario

## 📦 Instalación

1. Instala el [Fabric Loader](https://fabricmc.net/use/installer/) para Minecraft **1.21.1**
2. Descarga y añade a la carpeta `mods`:
   - [Fabric API](https://modrinth.com/mod/fabric-api) para 1.21.1
   - [Cobblemon](https://modrinth.com/mod/cobblemon) (cualquier versión reciente para 1.21.1)
   - `cobbletabs-1.0.0.jar` (este mod)
3. Inicia el juego. La primera vez se generará `config/cobbletabs.json`

> Requisitos: Java 21, Fabric Loader ≥ 0.16.0, Fabric API y Cobblemon (el mod los declara como dependencias y no arrancará sin ellos).

## 🖱️ Uso

| Acción | Resultado |
|--------|-----------|
| Clic en una pestaña | Ejecuta su comando (ej. `/pc`) |
| Clic en el botón con icono de barrera | Oculta las pestañas |
| Clic en el botón con icono de ojo de ender | Muestra las pestañas |
| **F8** | Recarga `config/cobbletabs.json` al instante |
| Pasar el ratón por una pestaña | Muestra el nombre de la pestaña |

La tecla F8 se puede cambiar en **Opciones → Controles → CobbleTabs → Recargar configuración**, y funciona tanto en el mundo como con el inventario abierto.

### Pestañas por defecto

| Pestaña | Comando | Icono | Color |
|---------|---------|-------|-------|
| PC | `/pc` | `cobblemon:pc` | Gris |
| Wiki | `/wiki` | `cobblemon:pokedex_red` | Rojo claro |
| Daycare | `/daycare` | `minecraft:book` | Rosa |
| Daily | `/daily` | `minecraft:clock` | Amarillo |
| STS | `/sts` | `cobblemon:verdant_ball` | Blanco |
| WT | `/wt` | `cobblemon:premier_ball` | Verde |

> Los comandos deben existir en el servidor donde juegas (por ejemplo, vía plugins). Si el servidor no los tiene, verás el error estándar de comando desconocido.

## ⚙️ Configuración

Archivo: `config/cobbletabs.json` (en el entorno de desarrollo: `run/config/cobbletabs.json`).
Edítalo con el juego cerrado **o** edítalo y pulsa **F8** para aplicarlo al momento.

```jsonc
{
  // Lista de pestañas: el orden en el archivo es el orden en pantalla.
  // La primera mitad se coloca a la izquierda y el resto a la derecha.
  "tabs": [
    {
      "id": "pc",              // identificador interno (único, sin espacios)
      "command": "/pc",        // comando a ejecutar (se añade "/" si falta)
      "icon": "cobblemon:pc",  // item usado como icono (cualquier mod)
      "enabled": true,         // false = pestaña oculta sin borrarla
      "label": "PC",           // nombre mostrado (vacío = traducción por defecto)
      "color": "gray",         // color del nombre (ver tabla de colores)
      "bold": true             // nombre en negrita
    },
    { "id": "wiki",    "command": "/wiki",    "icon": "cobblemon:pokedex_red",  "enabled": true, "label": "Wiki",    "color": "light_red", "bold": true },
    { "id": "daycare", "command": "/daycare", "icon": "minecraft:book",         "enabled": true, "label": "Daycare", "color": "pink",      "bold": true },
    { "id": "daily",   "command": "/daily",   "icon": "minecraft:clock",        "enabled": true, "label": "Daily",   "color": "yellow",    "bold": true },
    { "id": "sts",     "command": "/sts",     "icon": "cobblemon:verdant_ball", "enabled": true, "label": "STS",     "color": "white",     "bold": true },
    { "id": "wt",      "command": "/wt",      "icon": "cobblemon:premier_ball", "enabled": true, "label": "WT",      "color": "green",     "bold": true }
  ],

  // Logo del inventario
  "logo": {
    "enabled": true,  // mostrar u ocultar el logo
    "size": 44,       // tamaño en "píxeles de GUI" (16-256); escala con la Escala de GUI de Minecraft
    "opacity": 50     // opacidad 0-100 (50 = marca de agua sutil, 100 = opaco)
  },

  "showToggleButton": true, // false = quita el botón de ocultar/mostrar pestañas
  "tabsVisible": true       // estado actual del botón (se guarda solo, no hace falta tocarlo)
}
```

### Colores

El campo `color` acepta:

| Nombre (inglés) | Nombre (español) | Valor |
|-----------------|------------------|-------|
| `gray` | `gris` | `#AAAAAA` |
| `light_red` / `red` | `rojo_claro` / `rojo` | `#FF5555` |
| `pink` | `rosa` | `#FF9FDB` |
| `yellow` | `amarillo` | `#FFFF55` |
| `white` | `blanco` | `#FFFFFF` |
| `green` | `verde` | `#55FF55` |
| `#RRGGBB` | — | cualquier color en hexadecimal (ej. `#00BFFF`) |

Si dejas `color` vacío o escribes un valor inválido, la pestaña usa su color clásico por defecto (PC gris, Wiki rojo claro, Daycare rosa, Daily amarillo, STS blanco, WT verde).

### Ejemplos útiles

**Añadir una pestaña nueva** (por ejemplo, un comando `/tienda` con icono de esmeralda):
```json
{ "id": "tienda", "command": "/tienda", "icon": "minecraft:emerald", "enabled": true, "label": "Tienda", "color": "#50C878", "bold": true }
```

**Desactivar una pestaña sin borrarla:**
```json
{ "id": "sts", "command": "/sts", "icon": "cobblemon:verdant_ball", "enabled": false, "label": "STS", "color": "white", "bold": true }
```

**Quitar el logo y el botón** (mod mínimo):
```json
"logo": { "enabled": false, "size": 44, "opacity": 50 },
"showToggleButton": false
```

Si el archivo está roto o incompleto, el mod avisa en el log y regenera los valores por defecto; las pestañas clásicas recuperan su color por defecto automáticamente.

## 🛠️ Para desarrolladores

```bash
# Compilar el jar (requiere JDK 21)
./gradlew build          # resultado en build/libs/cobbletabs-1.0.0.jar

# Abrir el juego con el mod cargado (usa Cobblemon real descargado del Maven)
./gradlew runClient
```

- **Lenguaje:** Java 21 (sin mixins; usa eventos de Fabric API y un access widener mínimo)
- **Config:** Gson (incluido en Minecraft, sin dependencias extra)
- **Regenerar el logo:** `java -cp tools Resize <entrada> <salida> <tamaño> [relleno%]`

## 👤 Créditos

- **Creador:** [Juanmi64](https://github.com/Juanmi64)
- **Creado para:** Pokegalaxia (usable en cualquier server con Cobblemon)

## 📄 Licencia

CC0-1.0. Cobblemon es una propiedad de sus respectivos autores; este mod no está afiliado a ellos.
