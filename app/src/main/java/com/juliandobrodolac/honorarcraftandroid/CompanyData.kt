package com.juliandobrodolac.honorarcraftandroid

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "company_data")
data class CompanyData(
    @PrimaryKey val id: Int = 1, // Fixed ID for single entry
    var eduCenter: String = "",
    var locationNr: String = "",
    var schoolType: String = "",
    var customerSecondNameOrOrga: String = "",
    var customerFirstName: String = "",
    var customerPlz: String = "",
    var customerCityName: String = "",
    var customerMailBox: String = "",
    var customerStreet: String = "",
    var customerStreetNumber: String = "",
    var billerSecondName: String = "",
    var billerFirstName: String = "",
    var billerStreetName: String = "",
    var billerStreetNumber: String = "",
    var billerPlzNumber: String = "",
    var billerCityName: String = "",
    var taxNumber: String = "",
    var billerIban: String = "",
    var billerBIC: String = "",
    var rate: String = "",
    var signaturePath: String = "",
    var pdfPath: String = ""
)
