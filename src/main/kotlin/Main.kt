import com.fasterxml.jackson.annotation.JsonProperty
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*

operator fun StringBuilder.plusAssign(s: Any) {
	this.append(s)
}
suspend fun main() {
	val client = HttpClient(CIO) {
		install(ContentNegotiation) {
			jackson()
		}
		install(HttpTimeout) {
			requestTimeoutMillis = 30_000
		}
	}

	var context = Config.initialContext
	var conversationLength = 1

	while (true) {
		println("$conversationLength. (next query or EXIT to exit. Type <Enter> TWICE for submitting query.)")

		val queryBuilder = StringBuilder()
		do {
			print("> ")
			val line = readln()
			queryBuilder += line
		} while (line != "")

		val query = queryBuilder.toString()
		if (query == "EXIT") return

		val prompt = "\n\nHuman: $query\n\nAssistant:"
		context += prompt

		println("(asking Claude...)")
		val response = client.post("https://api.anthropic.com/v1/complete") {
			header(
				"x-api-key",
				Config.apiKey
			)
			contentType(ContentType.Application.Json)
			setBody(Request(context))
		}
		if (response.status == HttpStatusCode.OK) {
			val completionResponse: CompletionResponse = response.body()
			println(completionResponse.completion)
			context += completionResponse.completion
		}

		conversationLength++
	}
}

data class Request(
	val prompt: String,
	val model: String = "claude-v1",
	@JsonProperty("max_tokens_to_sample") val maxTokensToSample: Int = 2000
)

data class CompletionResponse(
	val completion: String,
	@JsonProperty("stop_reason") val stopReason: String,
	val truncated: Boolean,
	val stop: String,
	val model: String,
	@JsonProperty("log_id") val logId: String,
	val exception: String?,
)
