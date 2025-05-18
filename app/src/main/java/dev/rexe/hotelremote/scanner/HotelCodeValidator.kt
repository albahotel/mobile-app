package dev.rexe.hotelremote.scanner

object HotelCodeValidator {
    fun validate(code: String): Boolean {
        return code.split(";").size == 4
    }
}