package com.deckapp.feature.npcs.dialogs

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.deckapp.core.model.Npc
import kotlin.random.Random

// Colores atmosféricos de Choir of Flesh
private val CrimsonPrimary = Color(0xFF8B1515)
private val CrimsonContainer = Color(0xFF380808)
private val DarkBackground = Color(0xFF141212)
private val SurfaceDark = Color(0xFF1F1B1B)
private val GoldAccent = Color(0xFFD4AF37)
private val BoneWhite = Color(0xFFE6E1DC)
private val FleshSubtle = Color(0xFFA57D7D)

private data class OccupationData(
    val id: Int,
    val name: String,
    val skills: String,
    val burden: String,
    val startingWeapon: String
)

private val Occupations = listOf(
    OccupationData(1, "Campesino / Siervo", "Levantar Cargas (FUE), Robusto (CON), Perspicacia (VOL)", "Resentimiento profundo hacia nobles y sacerdotes.", "Guadaña o Horca de labranza (1d6)"),
    OccupationData(2, "Clero (Monje/Monja)", "Teología (INT), Investigación (INT), Fuerza Interior (VOL)", "Fe atormentada por la herejía celestial.", "Maza de hierro o báculo pesado (1d6)"),
    OccupationData(3, "Artesano (Herrero)", "Fuerza Bruta (FUE), Dedos Ágiles (DES), Regateo (PRE)", "Angustia visceral al ver obras reducidas a polvo.", "Martillo pesado de forja (1d6)"),
    OccupationData(4, "Soldado", "Frenesí (FUE), Golpes Precisos (DES), Entereza (VOL)", "La violencia armada es tu único lenguaje.", "Espada ancha o lanza de infante (1d8)"),
    OccupationData(5, "Noble Caído", "Tácticas (INT), Diplomacia (VOL), Liderazgo (VOL)", "Tus títulos y tierras ahora son ceniza.", "Estoque nobiliario o daga de parada (1d6)"),
    OccupationData(6, "Comerciante", "Resistencia (CON), Regateo (PRE), Red de Contactos (PRE)", "Visión transaccional y desconfianza crónica.", "Cuchillo de bota y bastón plomado (1d6)"),
    OccupationData(7, "Cazador", "Emboscador (DES), Sigilo (DES), Naturalismo (INT)", "Aversión extrema a los asentamientos.", "Arco de caza y cuchillo de monte (1d6)"),
    OccupationData(8, "Erudito", "Maestría del Saber (INT), Enigmas (INT), Introspección (VOL)", "Frágil ante la violencia física directa.", "Daga corta y compás metálico (1d4)"),
    OccupationData(9, "Sanador", "Resistencia a Enfermedades (CON), Curación (INT), Mente (VOL)", "Perseguido como brujo por los desesperados.", "Bisturí de barbero y bastón (1d4)"),
    OccupationData(10, "Marginado (Mendigo)", "Robusto (CON), Pies Rápidos (DES), Lectura de Labios (INT)", "Esperas la traición de cualquiera.", "Garrote nudoso y hondas con piedras (1d6)")
)

private val Anchors = listOf(
    "Orgullo (Soberbia)" to "Te niegas a arrodillarte ante coros divinos o masas de carne. Eres mejor que ellos. Tu arrogancia es tu armadura.",
    "Ira" to "Un fuego abrasador arde en tus entrañas contra los causantes del fin. No puedes someterte mientras queden enemigos por aniquilar.",
    "Codicia" to "Deseas poseer, acumular y sobrevivir a cualquier costo. El Coro exige entrega y la Carne disolución; tu avaricia se niega a soltar ni un átomo.",
    "Lujuria" to "Anhelo febril por el calor y el tacto carnal del viejo mundo. Rechazas la perfección estéril del canto por el pulso terrenal.",
    "Engaño" to "Toda tu identidad es una farsa construida. El Coro no puede moldear un alma que no es real y la Carne no puede atrapar a un fantasma.",
    "Desesperación" to "Has visto el vacío total. Sin esperanzas que ofrecerte ni temores que infundirte, eres inmune a sus promesas de salvación.",
    "Obsesión" to "Una única tarea terrenal te consume (un mensaje, una venganza, un pacto). El apocalipsis es una mera distracción de tu meta.",
    "Gula" to "Hambre insaciable por la vida, el dolor y la experiencia terrena. Te niegas a ser consumido porque tú devoras al mundo.",
    "Apatía" to "Desapego emocional absoluto. El cielo sangra y la tierra ruge, pero no es tu problema. El fin le sucede a otros.",
    "Duda" to "Escepticismo corrosivo. Cada milagro es un truco y cada cántico un fraude. No puedes ser convertido si te niegas a creer."
)

private val Fragments = listOf(
    "Crucifijo Oxidado" to "Dios no canta; Dios calla. Recuerda que la divinidad era paz silenciosa, no una vorágine celestial.",
    "Amuleto Pagano" to "Piedra grabada con una espiral. Recuerda que la tierra pertenecía a los bosques y no al cielo ni a la carroña.",
    "Dibujo de un Niño" to "Boceto de figuras tomadas de la mano. Baluarte de inocencia que demuestra que la alegría simple existió.",
    "Moneda de Plata" to "Con el perfil del Rey Roberto II. Recuerda que la humanidad forjó leyes, comercio y orden frente a las bestias.",
    "Página de Platón" to "Fragmento razonado sobre la existencia. Recuerda que eres un ser pensante, no alimento del foso ni títere del coro.",
    "Bolsa de Semillas" to "Promesa de vida buena y pura esperando ser plantada en tierra fértil, lejos de las pústulas.",
    "Llave de Casa Incendiada" to "Promesa de reconstruir un santuario seguro, aunque deba erigirse sobre las cenizas del mundo.",
    "Fragmento de Vidriera" to "Cristal de un santo olvidado. Testimonio de la verdadera historia antes de que los evangelios fueran violados.",
    "Flor Seca y sin Olor" to "Belleza marchita sin diseño divino. Símbolo del mundo apacible que florecía sin exigir adoración.",
    "Vial con Agua de Mar" to "Agua salada de costas deshechas. Recordatorio de los límites del mundo frente al apetito infinito de la Carne."
)

private val Whispers = listOf(
    "El Santo Silencioso" to "Buscas a un ermitaño inmune al himno celestial; un vacío viviente en la sinfonía divina que podría ser la clave.",
    "El Cántico Herético" to "Buscas las estrofas del Contra-Salmo Gnóstico, un rito discordante capaz de quebrar la fe de los Penitentes.",
    "Tierra Profana" to "Rumores de un valle o abadía oculta que permanece intacta tanto para el Coro como para la Carne: un santuario.",
    "El Primer Apóstata" to "Buscas a la primera alma que eligió la Carne sobre el Coro; un tumor consciente con el secreto de la resistencia.",
    "Hijo de Apocalipsis Gemelos" to "Buscas al niño nacido de un Penitente del Coro y un Apóstata de la Carne: un puente entre dos horrores.",
    "Cisterna de Viejas Plegarias" to "Una catacumba subterránea donde los ecos de un millón de oraciones antiguas amortiguan el cántico angélico.",
    "El Nombre Blasfemo" to "Buscas el nombre original de la Carne Que Alimenta para otorgarle mente y forzarla a negociar.",
    "El Testamento Papal" to "La encíclica secreta de Silvestre II condenando a los ángeles antes de caer, detallando su debilidad cósmica.",
    "La Campana Negra" to "Campana de hierro estelar cuyo tañido produce una onda de silencio absoluto que quiebra la determinación enemiga.",
    "El Director Divino" to "Cartas astrales que señalan la ubicación del ángel Maestro que dirige la sinfonía: tu meta es asesinarlo."
)

private fun calculateModifier(score: Int): Int = when {
    score <= 3 -> -3
    score in 4..5 -> -2
    score in 6..8 -> -1
    score in 9..12 -> 0
    score in 13..15 -> 1
    score in 16..17 -> 2
    else -> 3
}

private fun roll3d6(): Int = Random.nextInt(1, 7) + Random.nextInt(1, 7) + Random.nextInt(1, 7)

@Composable
fun ChoirOfFleshCharacterDialog(
    onDismiss: () -> Unit,
    onCharacterCreated: (Npc, String) -> Unit
) {
    var step by remember { mutableIntStateOf(1) }

    // Paso 1: Identidad & Ocupación
    var characterName by remember { mutableStateOf("") }
    var selectedOccupationIndex by remember { mutableIntStateOf(0) }

    // Paso 2: Atributos
    var strScore by remember { mutableIntStateOf(12) }
    var dexScore by remember { mutableIntStateOf(11) }
    var conScore by remember { mutableIntStateOf(13) }
    var intScore by remember { mutableIntStateOf(10) }
    var wilScore by remember { mutableIntStateOf(12) }
    var preScore by remember { mutableIntStateOf(9) }

    // Paso 3: Ancla
    var selectedAnchorIndex by remember { mutableIntStateOf(0) }

    // Paso 4: Fragmento
    var selectedFragmentIndex by remember { mutableIntStateOf(0) }

    // Paso 5: Misión
    var selectedWhisperIndex by remember { mutableIntStateOf(0) }

    // Cálculos derivados
    val conMod = calculateModifier(conScore)
    val dexMod = calculateModifier(dexScore)
    val maxHp = (10 + conMod).coerceAtLeast(6)
    val armorClass = 10 + dexMod
    val initiativeBonus = dexMod

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .fillMaxHeight(0.92f)
                .clip(RoundedCornerShape(16.dp))
                .border(2.dp, CrimsonPrimary, RoundedCornerShape(16.dp)),
            color = DarkBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "CREANDO TU INQUEBRANTABLE",
                            color = CrimsonPrimary,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "Choir of Flesh • Paso $step de 6",
                            color = BoneWhite.copy(alpha = 0.7f),
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = BoneWhite)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Progress Bar
                LinearProgressIndicator(
                    progress = { step / 6f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = CrimsonPrimary,
                    trackColor = SurfaceDark
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Body Steps
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    when (step) {
                        1 -> StepIdentityAndOccupation(
                            name = characterName,
                            onNameChange = { characterName = it },
                            selectedIndex = selectedOccupationIndex,
                            onSelect = { selectedOccupationIndex = it }
                        )
                        2 -> StepAttributes(
                            str = strScore, onStrChange = { strScore = it },
                            dex = dexScore, onDexChange = { dexScore = it },
                            con = conScore, onConChange = { conScore = it },
                            intel = intScore, onIntChange = { intScore = it },
                            wil = wilScore, onWilChange = { wilScore = it },
                            pre = preScore, onPreChange = { preScore = it },
                            onRollAll = {
                                strScore = roll3d6()
                                dexScore = roll3d6()
                                conScore = roll3d6()
                                intScore = roll3d6()
                                wilScore = roll3d6()
                                preScore = roll3d6()
                            }
                        )
                        3 -> StepAnchor(
                            selectedIndex = selectedAnchorIndex,
                            onSelect = { selectedAnchorIndex = it }
                        )
                        4 -> StepFragment(
                            selectedIndex = selectedFragmentIndex,
                            onSelect = { selectedFragmentIndex = it }
                        )
                        5 -> StepWhisper(
                            selectedIndex = selectedWhisperIndex,
                            onSelect = { selectedWhisperIndex = it }
                        )
                        6 -> StepSummary(
                            name = characterName.ifBlank { "Inquebrantable Sin Nombre" },
                            occupation = Occupations[selectedOccupationIndex],
                            str = strScore, dex = dexScore, con = conScore,
                            intel = intScore, wil = wilScore, pre = preScore,
                            anchor = Anchors[selectedAnchorIndex],
                            fragment = Fragments[selectedFragmentIndex],
                            whisper = Whispers[selectedWhisperIndex],
                            maxHp = maxHp,
                            armorClass = armorClass,
                            initiativeBonus = initiativeBonus
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Navigation Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (step > 1) {
                        OutlinedButton(
                            onClick = { step-- },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = BoneWhite),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonPrimary)
                        ) {
                            Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Anterior")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(1.dp))
                    }

                    if (step < 6) {
                        Button(
                            onClick = { step++ },
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary)
                        ) {
                            Text("Siguiente", color = BoneWhite, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    } else {
                        Button(
                            onClick = {
                                val finalName = characterName.ifBlank { "Inquebrantable ${Occupations[selectedOccupationIndex].name.split(" ").first()}" }
                                val occ = Occupations[selectedOccupationIndex]
                                val anc = Anchors[selectedAnchorIndex]
                                val fra = Fragments[selectedFragmentIndex]
                                val whi = Whispers[selectedWhisperIndex]

                                val sheetMarkdown = """
# Inquebrantable: $finalName
**Antigua Ocupación**: ${occ.name}
**Ancla (Pecado Capital)**: ${anc.first}
**Fragmento del Viejo Mundo**: ${fra.first}
**Misión Condenada**: ${whi.first}

---
### Atributos y Estadísticas
- **Fuerza (FUE)**: $strScore (${if (calculateModifier(strScore) >= 0) "+${calculateModifier(strScore)}" else "${calculateModifier(strScore)}"})
- **Destreza (DES)**: $dexScore (${if (calculateModifier(dexScore) >= 0) "+${calculateModifier(dexScore)}" else "${calculateModifier(dexScore)}"})
- **Constitución (CON)**: $conScore (${if (calculateModifier(conScore) >= 0) "+${calculateModifier(conScore)}" else "${calculateModifier(conScore)}"})
- **Inteligencia (INT)**: $intScore (${if (calculateModifier(intScore) >= 0) "+${calculateModifier(intScore)}" else "${calculateModifier(intScore)}"})
- **Voluntad (VOL)**: $wilScore (${if (calculateModifier(wilScore) >= 0) "+${calculateModifier(wilScore)}" else "${calculateModifier(wilScore)}"})
- **Presencia (PRE)**: $preScore (${if (calculateModifier(preScore) >= 0) "+${calculateModifier(preScore)}" else "${calculateModifier(preScore)}"})

**Puntos de Golpe (PV)**: $maxHp / $maxHp | **Clase de Dificultad (CD)**: $armorClass | **Iniciativa**: ${if (initiativeBonus >= 0) "+$initiativeBonus" else "$initiativeBonus"}
**Humanidad**: 20 / 20 | **Angustia**: 0 / 10 | **Trauma**: 0 Heridas

---
### Trasfondo y Recursos
- **Habilidades de Ocupación**: ${occ.skills}
- **Carga Psicológica**: ${occ.burden}
- **Arma Inicial**: ${occ.startingWeapon}
- **Dados de Uso (Ud)**: Raciones (Ud6), Antorchas (Ud6), Vendas (Ud6)
- **Reliquia del Pasado**: ${fra.first} — *${fra.second}*
- **Juramento / Susurro**: ${whi.first} — *${whi.second}*
- **Ancla Espiritual**: ${anc.first} — *${anc.second}*
                                """.trimIndent()

                                val npc = Npc(
                                    id = 0,
                                    name = finalName,
                                    description = "Inquebrantable (${occ.name}). Ancla: ${anc.first}.",
                                    imagePath = null,
                                    maxHp = maxHp,
                                    currentHp = maxHp,
                                    armorClass = armorClass,
                                    initiativeBonus = initiativeBonus,
                                    notes = sheetMarkdown,
                                    isMonster = false,
                                    createdAt = System.currentTimeMillis()
                                )

                                onCharacterCreated(npc, sheetMarkdown)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldAccent)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = DarkBackground, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("FORJAR INQUEBRANTABLE", color = DarkBackground, fontWeight = FontWeight.Black)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepIdentityAndOccupation(
    name: String,
    onNameChange: (String) -> Unit,
    selectedIndex: Int,
    onSelect: (Int) -> Unit
) {
    Column {
        Text("1. Identidad y Antigua Ocupación", color = BoneWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
        Text(
            "Antes de que el cielo comenzara a cantar y la tierra brotara en carne viva, tenías una vida común en la Europa del año 1000.",
            color = BoneWhite.copy(alpha = 0.7f),
            fontSize = 13.sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Nombre del Inquebrantable", color = FleshSubtle) },
            placeholder = { Text("Ej. Godofredo de Bouillon, Hilda, Raymond...") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = CrimsonPrimary,
                unfocusedBorderColor = FleshSubtle,
                focusedTextColor = BoneWhite,
                unfocusedTextColor = BoneWhite
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Ocupación Anterior (1d10)", color = GoldAccent, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            TextButton(onClick = { onSelect(Random.nextInt(0, Occupations.size)) }) {
                Icon(Icons.Default.Casino, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tirar Aleatorio", color = GoldAccent, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Occupations.forEachIndexed { index, occ ->
            val isSelected = index == selectedIndex
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (isSelected) CrimsonContainer else SurfaceDark)
                    .border(1.dp, if (isSelected) CrimsonPrimary else Color.Transparent, RoundedCornerShape(8.dp))
                    .clickable { onSelect(index) }
                    .padding(12.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("${occ.id}. ${occ.name}", color = BoneWhite, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        if (isSelected) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, tint = CrimsonPrimary, modifier = Modifier.size(18.dp))
                        }
                    }
                    Text("Habilidades: ${occ.skills}", color = FleshSubtle, fontSize = 12.sp)
                    Text("Carga: ${occ.burden}", color = BoneWhite.copy(alpha = 0.6f), fontSize = 11.sp)
                }
            }
        }
    }
}

@Composable
private fun StepAttributes(
    str: Int, onStrChange: (Int) -> Unit,
    dex: Int, onDexChange: (Int) -> Unit,
    con: Int, onConChange: (Int) -> Unit,
    intel: Int, onIntChange: (Int) -> Unit,
    wil: Int, onWilChange: (Int) -> Unit,
    pre: Int, onPreChange: (Int) -> Unit,
    onRollAll: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("2. Puntuación de Atributos", color = BoneWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Escala 3-18 (Modificador -3 a +3)", color = BoneWhite.copy(alpha = 0.7f), fontSize = 12.sp)
            }
            Button(
                onClick = onRollAll,
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonPrimary),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Icon(Icons.Default.Casino, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Tirar 3d6 para Todo", fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        AttributeSliderRow("Fuerza (FUE)", str, onStrChange)
        AttributeSliderRow("Destreza (DES)", dex, onDexChange)
        AttributeSliderRow("Constitución (CON)", con, onConChange)
        AttributeSliderRow("Inteligencia (INT)", intel, onIntChange)
        AttributeSliderRow("Voluntad (VOL)", wil, onWilChange)
        AttributeSliderRow("Presencia / Carisma (PRE)", pre, onPreChange)

        Spacer(modifier = Modifier.height(12.dp))

        // Vista previa de derivados
        Card(
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Puntos de Golpe", color = FleshSubtle, fontSize = 11.sp)
                    Text("${(10 + calculateModifier(con)).coerceAtLeast(6)} PV", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("CD Defensiva", color = FleshSubtle, fontSize = 11.sp)
                    Text("CD ${10 + calculateModifier(dex)}", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Iniciativa", color = FleshSubtle, fontSize = 11.sp)
                    val mod = calculateModifier(dex)
                    Text(if (mod >= 0) "+$mod" else "$mod", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
        }
    }
}

@Composable
private fun AttributeSliderRow(label: String, value: Int, onChange: (Int) -> Unit) {
    val mod = calculateModifier(value)
    val modStr = if (mod >= 0) "+$mod" else "$mod"

    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = BoneWhite, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$value", color = BoneWhite, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text("($modStr)", color = if (mod > 0) Color(0xFF66BB6A) else if (mod < 0) Color(0xFFEF5350) else FleshSubtle, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = 3f..18f,
            steps = 14,
            colors = SliderDefaults.colors(
                thumbColor = CrimsonPrimary,
                activeTrackColor = CrimsonPrimary,
                inactiveTrackColor = SurfaceDark
            )
        )
    }
}

@Composable
private fun StepAnchor(selectedIndex: Int, onSelect: (Int) -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("3. El Ancla (Tu Pecado Capital)", color = BoneWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("El pecado humano que te hace inmune a la asimilación", color = FleshSubtle, fontSize = 12.sp)
            }
            TextButton(onClick = { onSelect(Random.nextInt(0, Anchors.size)) }) {
                Icon(Icons.Default.Casino, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tirar 1d10", color = GoldAccent, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Anchors.forEachIndexed { index, pair ->
            val isSelected = index == selectedIndex
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onSelect(index) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) CrimsonContainer else SurfaceDark
                ),
                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, CrimsonPrimary) else null
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("${index + 1}. ${pair.first}", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(pair.second, color = BoneWhite.copy(alpha = 0.8f), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun StepFragment(selectedIndex: Int, onSelect: (Int) -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("4. Fragmento del Viejo Mundo", color = BoneWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Una reliquia terrenal que conservas de cuando el mundo era humano", color = FleshSubtle, fontSize = 12.sp)
            }
            TextButton(onClick = { onSelect(Random.nextInt(0, Fragments.size)) }) {
                Icon(Icons.Default.Casino, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tirar 1d10", color = GoldAccent, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Fragments.forEachIndexed { index, pair ->
            val isSelected = index == selectedIndex
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onSelect(index) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) CrimsonContainer else SurfaceDark
                ),
                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, CrimsonPrimary) else null
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("${index + 1}. ${pair.first}", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(pair.second, color = BoneWhite.copy(alpha = 0.8f), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun StepWhisper(selectedIndex: Int, onSelect: (Int) -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("5. Susurro de Condenación & Misión", color = BoneWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("El rumor o meta desesperada que empuja tus pasos en el páramo", color = FleshSubtle, fontSize = 12.sp)
            }
            TextButton(onClick = { onSelect(Random.nextInt(0, Whispers.size)) }) {
                Icon(Icons.Default.Casino, contentDescription = null, tint = GoldAccent, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Tirar 1d10", color = GoldAccent, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Whispers.forEachIndexed { index, pair ->
            val isSelected = index == selectedIndex
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .clickable { onSelect(index) },
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) CrimsonContainer else SurfaceDark
                ),
                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, CrimsonPrimary) else null
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("${index + 1}. ${pair.first}", color = GoldAccent, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(pair.second, color = BoneWhite.copy(alpha = 0.8f), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
private fun StepSummary(
    name: String,
    occupation: OccupationData,
    str: Int, dex: Int, con: Int,
    intel: Int, wil: Int, pre: Int,
    anchor: Pair<String, String>,
    fragment: Pair<String, String>,
    whisper: Pair<String, String>,
    maxHp: Int,
    armorClass: Int,
    initiativeBonus: Int
) {
    Column {
        Text("6. Resumen del Inquebrantable", color = GoldAccent, fontWeight = FontWeight.Black, fontSize = 18.sp)
        Text("Revisa los datos forjados antes de inscribir tu alma en el diario.", color = BoneWhite.copy(alpha = 0.7f), fontSize = 12.sp)

        Spacer(modifier = Modifier.height(14.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonPrimary)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(name, color = BoneWhite, fontWeight = FontWeight.Black, fontSize = 20.sp)
                Text("Antigua Ocupación: ${occupation.name}", color = FleshSubtle, fontSize = 13.sp)

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = CrimsonPrimary.copy(alpha = 0.5f))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Puntos de Golpe: $maxHp PV", color = GoldAccent, fontWeight = FontWeight.Bold)
                    Text("CD Defensiva: $armorClass", color = GoldAccent, fontWeight = FontWeight.Bold)
                    Text("Iniciativa: ${if (initiativeBonus >= 0) "+$initiativeBonus" else "$initiativeBonus"}", color = GoldAccent, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("FUE: $str (${calculateModifier(str)})", color = BoneWhite, fontSize = 12.sp)
                    Text("DES: $dex (${calculateModifier(dex)})", color = BoneWhite, fontSize = 12.sp)
                    Text("CON: $con (${calculateModifier(con)})", color = BoneWhite, fontSize = 12.sp)
                    Text("INT: $intel (${calculateModifier(intel)})", color = BoneWhite, fontSize = 12.sp)
                    Text("VOL: $wil (${calculateModifier(wil)})", color = BoneWhite, fontSize = 12.sp)
                    Text("PRE: $pre (${calculateModifier(pre)})", color = BoneWhite, fontSize = 12.sp)
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = CrimsonPrimary.copy(alpha = 0.5f))

                Text("• El Ancla: ${anchor.first}", color = BoneWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(anchor.second, color = BoneWhite.copy(alpha = 0.7f), fontSize = 11.sp)

                Spacer(modifier = Modifier.height(6.dp))

                Text("• Fragmento: ${fragment.first}", color = BoneWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(fragment.second, color = BoneWhite.copy(alpha = 0.7f), fontSize = 11.sp)

                Spacer(modifier = Modifier.height(6.dp))

                Text("• Misión: ${whisper.first}", color = BoneWhite, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                Text(whisper.second, color = BoneWhite.copy(alpha = 0.7f), fontSize = 11.sp)

                Spacer(modifier = Modifier.height(8.dp))

                Text("• Equipo Inicial: ${occupation.startingWeapon}, Raciones (Ud6), Antorchas (Ud6), Vendas (Ud6).", color = FleshSubtle, fontSize = 12.sp)
                Text("• Humanidad: 20 / 20 | Angustia: 0 / 10", color = FleshSubtle, fontSize = 12.sp)
            }
        }
    }
}
