package com.yueliangmanle.danci.core.pronunciation

class FakeCloudTtsTransport(
    private val response: CloudTtsHttpResponse = CloudTtsHttpResponse(
        statusCode = 200,
        body = """
            {
              "choices": [
                {
                  "message": {
                    "audio": {
                      "data": ""
                    }
                  }
                }
              ]
            }
        """.trimIndent(),
    ),
) : CloudTtsTransport {
    var lastRequestUrl: String? = null
    var lastHeaders: Map<String, String>? = null
    var lastBody: String? = null

    override suspend fun postJson(
        url: String,
        headers: Map<String, String>,
        body: String,
    ): CloudTtsHttpResponse {
        lastRequestUrl = url
        lastHeaders = headers
        lastBody = body
        return response
    }
}
