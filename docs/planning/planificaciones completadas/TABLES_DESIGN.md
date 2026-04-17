# DeckApp — Tablas Aleatorias (Sprint 5)

Documento de diseño permanente del sistema de tablas aleatorias.

---

## Modelo de Dominio

### `RandomTable`
```kotlin
data class RandomTable(
    val id: Long,
    val name: String,
    val description: String,
    val category: String,        // "Encuentros", "Nombres", "Clima", "Botín", "Combate"
    val rollFormula: String,     // "1d6", "1d20", "2d8+3"
    val rollMode: TableRollMode,
    val entries: List<TableEntry>,
    val isBuiltIn: Boolean,
    val createdAt: Long
)
```

### `TableEntry`
```kotlin
data class TableEntry(
    val minRoll: Int,      // rango: 5-7 → "5-7 → Lobos"
    val maxRoll: Int,
    val weight: Int,       // para WEIGHTED mode (sin rango)
    val text: String,      // soporta [dados inline] y @SubTabla
    val subTableRef: String?,
    val subTableId: Long?,
    val sortOrder: Int
)
```

### `TableRollMode`
| Modo       | Descripción |
|------------|-------------|
| `RANGE`    | Resultado por rango minRoll..maxRoll. Modelo clásico D&D. |
| `WEIGHTED` | Entradas sin rango, probabilidad por `weight` relativo. |

---

## Sintaxis Especial en Texto de Entrada

### Dados Inline
Patrón: `[NdM]` o `[NdM+K]` o `[NdM-K]`

```
"Una manada de [1d4+1] lobos ataca desde el flanco."
"Encuentras [2d6] monedas de cobre."
"La trampa hace [1d6-1] daño."
```
El dado se evalúa al momento de la tirada y se embebe en el resultado.

### Sub-tabla
Patrón: `@NombreTabla`

```
"El líder se llama @Nombre Humano Masculino."
"Hay un bandido (@Nombre Humano Masculino) esperando."
```
La tabla referenciada se busca por nombre exacto (case-sensitive).
Profundidad máxima: 5 niveles (para evitar loops infinitos).

---

## Arquitectura de Módulos

```
:core:model             RandomTable, TableEntry, TableRollResult, TableRollMode
:core:domain            TableRepository (interface), RollTableUseCase
:core:data              RandomTableEntity, TableEntryEntity, TableRollResultEntity
                        RandomTableDao, TableRollResultDao
                        TableRepositoryImpl
:feature:tables         TablesTab, TableDetailSheet, TableEditorScreen
                        TablesViewModel, TableEditorViewModel
app/assets/tables/      bundled_tables.json (9 tablas predefinidas)
```

---

## Room Schema (DB v4)

### `random_tables`
| Columna       | Tipo     | Descripción |
|---------------|----------|-------------|
| id            | INTEGER  | PK auto |
| name          | TEXT     | Nombre único |
| description   | TEXT     | Descripción |
| category      | TEXT     | Categoría |
| rollFormula   | TEXT     | "1d6", "2d8+3" |
| rollMode      | TEXT     | TableRollMode.name |
| isBuiltIn     | INTEGER  | 1 = bundled, 0 = usuario |
| createdAt     | INTEGER  | Unix ms |

### `table_entries`
| Columna       | Tipo     | Descripción |
|---------------|----------|-------------|
| id            | INTEGER  | PK auto |
| tableId       | INTEGER  | FK → random_tables |
| minRoll       | INTEGER  | Rango mínimo |
| maxRoll       | INTEGER  | Rango máximo |
| weight        | INTEGER  | Peso para WEIGHTED |
| text          | TEXT     | Texto con [dados] y @refs |
| subTableRef   | TEXT?    | Nombre de sub-tabla |
| subTableId    | INTEGER? | ID resuelto de sub-tabla |
| sortOrder     | INTEGER  | Orden visual |

### `table_roll_results`
| Columna       | Tipo     | Descripción |
|---------------|----------|-------------|
| id            | INTEGER  | PK auto |
| tableId       | INTEGER  | Tabla tirada |
| tableName     | TEXT     | Nombre (copia denormalizada) |
| sessionId     | INTEGER? | Sesión en la que se tiró |
| rollValue     | INTEGER  | Resultado del dado |
| resolvedText  | TEXT     | Texto final con dados inline evaluados |
| timestamp     | INTEGER  | Unix ms |

---

## Tablas Predefinidas (Bundle)

Incluidas en `assets/tables/bundled_tables.json`. Se cargan al primer lanzamiento
si `countBuiltInTables() == 0`.

| Tabla                         | Fórmula | Entradas | Categoría |
|-------------------------------|---------|----------|-----------|
| Encuentro Aleatorio Bosque    | 1d12    | 7        | Encuentros |
| Encuentro Aleatorio Mazmorra  | 1d20    | 10       | Encuentros |
| Nombre Humano Masculino       | 1d20    | 20       | Nombres |
| Nombre Humano Femenino        | 1d20    | 20       | Nombres |
| Nombre Élfico                 | 1d12    | 12       | Nombres |
| Clima                         | 1d6     | 6        | Clima |
| Botín de Goblin               | 1d8     | 7        | Botín |
| Resultado de Crítico          | 1d10    | 6        | Combate |
| Resultado de Pifia            | 1d10    | 6        | Combate |

---

## Formato JSON de Import/Export (futuro Sprint 6)

```json
{
  "version": 1,
  "tables": [
    {
      "name": "Nombre de la tabla",
      "description": "Descripción opcional",
      "category": "Encuentros",
      "rollFormula": "1d12",
      "rollMode": "RANGE",
      "entries": [
        { "minRoll": 1, "maxRoll": 3, "text": "Sin encuentro" },
        { "minRoll": 4, "maxRoll": 6, "text": "[1d4+1] goblins" },
        { "minRoll": 7, "maxRoll": 9, "text": "Bandido (@Nombre Humano Masculino)" }
      ]
    }
  ]
}
```

---

## Roadmap de Features

| Feature | Sprint |
|---------|--------|
| Crear/editar tablas con rangos | 5 ✅ |
| Tirada con botón y historial | 5 ✅ |
| Dados inline en texto de entrada | 5 ✅ |
| Sub-tabla @Ref (1 nivel) | 5 ✅ |
| Tablas predefinidas bundled | 5 ✅ |
| Categorías y búsqueda | 5 ✅ |
| Import/Export JSON pack | 6 |
| Sub-tablas anidadas (3+ niveles) | 6 |
| Tablas ponderadas (WEIGHTED sin rango) | 6 |
| Tablas secuenciales (SEQUENTIAL mode) | 6 |
| FAB "TIRAR" conectado a tabla activa en sesión | 6 |
| Conditional logic ([if: repeat → re-roll]) | 7 |
| Variable passing entre tablas | 7 |
| Import CSV comunidad | 7 |
| UI drag & drop reordenar entradas | 7 |
