package com.music.bitchord.ui.greeting

import java.time.LocalTime
import java.util.Locale
import kotlin.random.Random

/**
 * Mercurio's greeting on the Home page, where the wordmark used to sit.
 *
 * The line is picked once per launch (and again each time it is tapped), by
 * time of day, in Spanish or English. Roughly four greetings in ten use the
 * name the person typed on first launch; the rest stay anonymous, so it does
 * not feel like a form letter.
 *
 * Pure and deterministic given its inputs, so it is easy to test and the same
 * line comes back across recompositions and tab switches.
 */
object Greeter {

    /** Fixed for the life of the process: a new launch is what rolls a new line. */
    private val sessionSeed: Int = Random.nextInt()

    private const val NAME_CHANCE = 0.4f

    private enum class Period { Morning, Afternoon, Evening, Night }

    private class Line(val plain: String, val named: String? = null)

    private fun l(plain: String, named: String? = null) = Line(plain, named)

    fun pick(
        name: String?,
        bump: Int = 0,
        hour: Int = LocalTime.now().hour,
        language: String = Locale.getDefault().language,
    ): String {
        val period = when (hour) {
            in 5..11 -> Period.Morning
            in 12..18 -> Period.Afternoon
            in 19..23 -> Period.Evening
            else -> Period.Night
        }
        val pool = (if (language == "es") es else en).getValue(period)
        val rnd = Random(sessionSeed * 31 + bump * 7919 + period.ordinal)
        val line = pool[rnd.nextInt(pool.size)]
        val first = name?.trim()?.substringBefore(' ')?.takeIf { it.isNotEmpty() }
        val useName = first != null && line.named != null && rnd.nextFloat() < NAME_CHANCE
        return if (useName) line.named!!.replace("{n}", first!!) else line.plain
    }

    private val es: Map<Period, List<Line>> = mapOf(
        Period.Morning to listOf(
            l("Buenos días", "Buenos días, {n}"),
            l("Arrancamos con música", "{n}, arrancamos con música"),
            l("Un café y buena música", "Un café y buena música, {n}"),
            l("Que empiece el día con ritmo", "{n}, que empiece el día con ritmo"),
            l("Hoy suena bien"),
            l("Despierta, hay canciones esperando", "Despierta {n}, hay canciones esperando"),
        ),
        Period.Afternoon to listOf(
            l("Buenas tardes", "Buenas tardes, {n}"),
            l("¿Qué suena esta tarde?", "¿Qué suena esta tarde, {n}?"),
            l("Hora de poner algo bueno", "{n}, hora de poner algo bueno"),
            l("La tarde pide música"),
            l("Dale play a la tarde", "Dale play a la tarde, {n}"),
            l("Un descanso con buena música"),
        ),
        Period.Evening to listOf(
            l("Buenas noches", "Buenas noches, {n}"),
            l("La noche es para escuchar", "{n}, la noche es para escuchar"),
            l("Baja las luces, sube el volumen"),
            l("Esta noche suena a ti", "Esta noche suena a ti, {n}"),
            l("Un tema más antes de cerrar el día"),
            l("Modo noche activado", "Modo noche activado, {n}"),
        ),
        Period.Night to listOf(
            l("Aún despierto", "¿Aún despierto, {n}?"),
            l("La madrugada tiene su propia banda sonora"),
            l("Silencio afuera, música adentro", "Silencio afuera, música adentro, {n}"),
            l("Los mejores temas suenan a esta hora"),
            l("Insomnio con buena música"),
            l("Todo está más tranquilo con música", "{n}, todo está más tranquilo con música"),
        ),
    )

    private val en: Map<Period, List<Line>> = mapOf(
        Period.Morning to listOf(
            l("Good morning", "Good morning, {n}"),
            l("Let's start with some music", "{n}, let's start with some music"),
            l("Coffee and a good playlist", "Coffee and a good playlist, {n}"),
            l("Start the day with rhythm"),
            l("Today sounds good"),
            l("Wake up, songs are waiting", "Wake up {n}, songs are waiting"),
        ),
        Period.Afternoon to listOf(
            l("Good afternoon", "Good afternoon, {n}"),
            l("What's playing this afternoon?", "What's playing this afternoon, {n}?"),
            l("Time to put something good on", "{n}, time to put something good on"),
            l("The afternoon needs music"),
            l("Press play on the afternoon", "Press play on the afternoon, {n}"),
            l("A break with good music"),
        ),
        Period.Evening to listOf(
            l("Good evening", "Good evening, {n}"),
            l("The night is for listening", "{n}, the night is for listening"),
            l("Lights down, volume up"),
            l("Tonight sounds like you", "Tonight sounds like you, {n}"),
            l("One more track before the day ends"),
            l("Night mode on", "Night mode on, {n}"),
        ),
        Period.Night to listOf(
            l("Still up?", "Still up, {n}?"),
            l("Late hours have their own soundtrack"),
            l("Quiet outside, music inside", "Quiet outside, music inside, {n}"),
            l("The best tracks play at this hour"),
            l("Sleepless, with good music"),
            l("Everything is calmer with music", "{n}, everything is calmer with music"),
        ),
    )
}
