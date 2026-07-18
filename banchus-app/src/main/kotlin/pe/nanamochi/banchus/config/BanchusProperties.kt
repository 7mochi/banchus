package pe.nanamochi.banchus.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "banchus")
data class BanchusProperties(
    val domainUrl: String = "",
    val menuIcon: MenuIconProperties = MenuIconProperties(),
    val seasonalBackgrounds: SeasonalBackgroundsProperties = SeasonalBackgroundsProperties(),
    val commandPrefix: String = "!",
    val ppCalculatorType: String = "osu-native",
    val storage: StorageProperties = StorageProperties(),
    val osuApi: OsuApiProperties = OsuApiProperties(),
)

data class MenuIconProperties(val imageUrl: String = "", val redirectUrl: String = "")

data class SeasonalBackgroundsProperties(val urls: List<String> = emptyList())

data class StorageProperties(val type: String = "local", val s3: S3Properties = S3Properties())

data class S3Properties(
    val bucket: String = "banchus-data",
    val region: String = "us-east-1",
    val endpoint: String = "",
    val accessKey: String = "",
    val secretKey: String = "",
)

data class OsuApiProperties(val v1: OsuApiV1Properties = OsuApiV1Properties())

data class OsuApiV1Properties(val key: String = "")
