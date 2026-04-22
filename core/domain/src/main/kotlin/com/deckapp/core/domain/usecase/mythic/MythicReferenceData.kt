package com.deckapp.core.domain.usecase.mythic

/**
 * Datos de referencia extraídos de Mythic GME 2e.
 * Incluye tablas de Action/Description para eventos aleatorios por defecto.
 */
object MythicReferenceData {

    /**
     * Tabla de Foco del Evento Aleatorio (Mythic 2e).
     * @param roll 1-100
     */
    fun getEventFocus(roll: Int): String = when (roll) {
        in 1..5 -> "Evento Remoto"
        in 6..10 -> "Acción de un NPC"
        in 11..20 -> "Nuevo NPC"
        in 21..45 -> "Moverse hacia un Hilo"
        in 46..50 -> "Alejarse de un Hilo"
        in 51..55 -> "Cerrar un Hilo"
        in 56..70 -> "PC Negativo"
        in 71..80 -> "PC Positivo"
        in 81..85 -> "Evento Ambiguo"
        in 86..100 -> "NPC Positivo"
        else -> "Evento Desconocido"
    }

    val actionKeywords = listOf(
        "Abandonar", "Adquirir", "Admitir", "Atacar", "Cerrar", "Continuar", "Crear", "Crueldad",
        "Debilitar", "Disminuir", "Dividir", "Dominar", "Elementos", "Emociones", "Engañar", "Entregar",
        "Entusiasmo", "Escapar", "Esperanza", "Exponer", "Extravagancia", "Fracasar", "Gratitud", "Guardar",
        "Herir", "Ignorar", "Imitar", "Informar", "Inquirir", "Inspeccionar", "Interrumpir", "Luchar",
        "Maltratar", "Malicia", "Militar", "Mover", "Negociar", "Oponer", "Paz", "Placer", "Poder",
        "Posponer", "Preguntar", "Preparar", "Prohibir", "Protección", "Provocar", "Castigar", "Perseguir",
        "Liberar", "Regresar", "Vengar", "Recompensa", "Ruina", "Separar", "Detener", "Superar", "Triunfo",
        "Unir", "Usar", "Vengar", "Violar", "Vigilancia", "Violencia", "Desperdicio"
    )

    val subjectKeywords = listOf(
        "Aventura", "Adversario", "Aliado", "Ambición", "Animales", "Armas", "Arte", "Autoridad",
        "Búsqueda", "Camino", "Camino", "Castillo", "Ciencia", "Clima", "Comida", "Compañía", "Comunicación",
        "Conflicto", "Conocimiento", "Consejo", "Crimen", "Cuerpo", "Cultura", "Curiosidad", "Daño", "Dinero",
        "Dioses", "Dirección", "Dolor", "Duda", "Enfermedad", "Enigma", "Envidia", "Equilibrio", "Error",
        "Esfuerzo", "Espíritu", "Estado", "Éxito", "Exterior", "Extranjero", "Fama", "Familia", "Fantasía",
        "Felicidad", "Fin", "Fisura", "Fuerza", "Futuro", "Guerra", "Hogar", "Honor", "Idea", "Ilusión",
        "Imagen", "Imaginación", "Importancia", "Impulso", "Inocencia", "Instinto", "Intención", "Interés",
        "Interior", "Invento", "Inversión", "Ira", "Isla", "Juego", "Juicio", "Justicia", "Juventud", "Laberinto"
    )

    val descriptorKeywords = listOf(
        "Áspero", "Amable", "Oscuro", "Brillante", "Antiguo", "Nuevo", "Extraño", "Familiar",
        "Peligroso", "Seguro", "Grande", "Pequeño", "Rápido", "Lento", "Fuerte", "Débil",
        "Bello", "Feo", "Limpio", "Sucio", "Ruidoso", "Silencioso", "Frío", "Caliente",
        "Húmedo", "Seco", "Pesado", "Ligero", "Duro", "Blando", "Vivo", "Muerto"
    )
}
