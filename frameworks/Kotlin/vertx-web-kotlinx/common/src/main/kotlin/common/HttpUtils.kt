package common

import io.vertx.core.MultiMap
import io.vertx.core.http.HttpHeaders
import io.vertx.core.http.HttpServerRequest
import io.vertx.core.http.HttpServerResponse
import kotlinx.datetime.UtcOffset
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.format
import kotlinx.html.*
import kotlinx.html.stream.appendHTML
import kotlin.time.Clock

object HttpHeaderValues {
    val vertxWeb = HttpHeaders.createOptimized("Vert.x-Web")
    val applicationJson = HttpHeaders.createOptimized("application/json")
    val textHtmlCharsetUtf8 = HttpHeaders.createOptimized("text/html; charset=utf-8")
    val textPlain = HttpHeaders.createOptimized("text/plain")
}

@Suppress("NOTHING_TO_INLINE")
inline fun MultiMap.addCommonHeaders(date: String) {
    add(HttpHeaders.SERVER, HttpHeaderValues.vertxWeb)
    add(HttpHeaders.DATE, date)
}

@Suppress("NOTHING_TO_INLINE")
inline fun HttpServerResponse.addJsonResponseHeaders(date: String) {
    headers().run {
        addCommonHeaders(date)
        add(HttpHeaders.CONTENT_TYPE, HttpHeaderValues.applicationJson)
    }
}

fun HttpServerRequest.getQueries(): Int {
    val queriesParam: String? = getParam("queries")
    return queriesParam?.toIntOrNull()?.coerceIn(1, 500) ?: 1
}

fun getCurrentDate(): String =
    DateTimeComponents.Formats.RFC_1123.format {
        // We don't need a more complicated system `TimeZone` here (whose offset depends dynamically on the actual time due to DST) since UTC works.
        setDateTimeOffset(Clock.System.now(), UtcOffset.ZERO)
    }

fun buildFortunesHtml(fortunes: List<Fortune>): String =
    buildString {
        append("<!DOCTYPE html>")
        appendHTML(false).html {
            head {
                title("Fortunes")
            }
            body {
                table {
                    tr {
                        th { +"id" }
                        th { +"message" }
                    }
                    for (fortune in fortunes)
                        tr {
                            td { +fortune.id.toString() }
                            td { +fortune.message }
                        }
                }
            }
        }
    }
