package de.v404.honorarcraftandroid

import androidx.annotation.Keep
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.math.BigDecimal

@Keep
@Entity(tableName = "company_data")
data class CompanyData(
    @PrimaryKey val id: Int = 1, // Fixed ID for single entry
    var eduCenter: String = "Ulm/Eselsberg",
    var locationNr: String = "42-362",
    var schoolType: String = "Nachhilfe",
    var customerSecondNameOrOrga: String = "Berufsbildung GmbH",
    var customerFirstName: String = "",
    var customerPlz: String = "89073",
    var customerCityName: String = "Ulm",
    var customerMailBox: String = "13 41 65",
    var customerStreet: String = "",
    var customerStreetNumber: String = "",
    var billerSecondName: String = "Mustermann",
    var billerFirstName: String = "Max",
    var billerStreetName: String = "Musterstraße",
    var billerStreetNumber: String = "12",
    var billerPlzNumber: String = "77500",
    var billerCityName: String = "Musterstadt",
    var taxNumber: String = "12/345/678/912",
    var billerIban: String = "DE12 3456 7810 1112 1314 15",
    var billerBIC: String = "XXX",
    var rate: BigDecimal = Constants.DEFAULT_RATE,
    var signaturePath: String = "",
    var pdfPath: String = ""
)
