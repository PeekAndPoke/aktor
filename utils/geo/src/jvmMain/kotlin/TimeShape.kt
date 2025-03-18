package io.peekandpoke.geo

import de.peekandpoke.ultra.common.datetime.MpTimezone
import net.iakovlev.timeshape.TimeZoneEngine
import kotlin.jvm.optionals.getOrNull

// https://github.com/RomanIakovlev/timeshape
class TimeShape {

    companion object {
        private val engine by lazy {
            TimeZoneEngine.initialize();
        }
    }

    fun getTimeZone(
        lat: Double,
        lng: Double,
        default: MpTimezone = MpTimezone.UTC,
    ): MpTimezone {
        val id = engine.query(lat, lng).getOrNull()
            ?: return default

        return MpTimezone.of(id.id)
    }
}
