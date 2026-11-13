package me.modmuss50.mpp.platforms.xyebbs

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import me.modmuss50.mpp.ReleaseType
import me.modmuss50.mpp.networking.HttpApi.post
import me.modmuss50.mpp.networking.HttpException
import me.modmuss50.mpp.networking.MultipartBodyBuilder
import me.modmuss50.mpp.networking.RequestContext
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.nio.file.Path
import java.time.Duration
import kotlin.io.path.name

class XyebbsApi(
    private val apiKey: String,
    private val apiSecret: String,
    private val baseUrl: String,
) {
    companion object {
        val httpContext = RequestContext(
            json = RequestContext.Default.json,
            userAgent = RequestContext.Default.userAgent,
            client = HttpClient
                .newBuilder()
                .connectTimeout(Duration.ofSeconds(60))
                .build(),
            exceptionFactory = HttpException.jsonErrorFactory<BaseResponse<String>>(
                json = RequestContext.Default.json,
                messageExtractor = { it.message },
            ),
        )
    }

    @Serializable
    data class BaseResponse<T>(
        val data: T? = null,
        val code: Int,
        val message: String
    )

    @Serializable
    data class BaseResponseMsg<T>(
        val data: T? = null,
        val code: Int,
        val msg: String
    )

    @Serializable
    enum class VersionType {
        @SerialName("alpha")
        ALPHA,

        @SerialName("beta")
        BETA,

        @SerialName("release")
        RELEASE,
        ;

        companion object {
            fun valueOf(type: ReleaseType): VersionType =
                when (type) {
                    ReleaseType.STABLE -> RELEASE
                    ReleaseType.BETA -> BETA
                    ReleaseType.ALPHA -> ALPHA
                }
        }
    }

    @Serializable
    data class CustomLinkItemJson(
        @SerialName("linkType") val linkType: String,
        @SerialName("url") val url: String,
        @SerialName("displayType") val displayType: String,
        @SerialName("info") val info: String,
        @SerialName("orderNum") val orderNum: Int,
    )

    private val headers: Map<String, String>
        get() = mapOf(
            "Api-Key" to apiKey,
            "Api-Secret" to apiSecret,
            "Content-Type" to "application/json",
        )

    fun createVersion(
        metadata: CreateVersion,
        id: String,
    ): CreateVersionResponse {
        val metadataJson = Json.encodeToString(metadata)


//        println("=== Sending JSON ===")
//        println(metadataJson)
//        println("Headers: $headers")
//        println("URL: $baseUrl/api/resources/$id/releases")

        val createVersionBranch: CreateVersionResponse = httpContext.post("$baseUrl/api/resources/${id}/releases", HttpRequest.BodyPublishers.ofString(metadataJson,
            Charsets.UTF_8), headers)

        val branchId = createVersionBranch.data?.id
            ?: throw Exception("创建版本失败: ${createVersionBranch.msg}")

//        println("=== Parsing Branch ID ===")
//        println("The ID IS: $branchId")
//        println("Create Branch Response: $createVersionBranch")

        return createVersionBranch
    }

    @Serializable
    data class CreateVersion(
        @SerialName("label")
        val label: String,

        @SerialName("type")
        val type: String,

        @SerialName("channel")
        val channel: String,

        @SerialName("orderNum")
        val orderNum: Int,

        @SerialName("notes")
        val notes: String? = null,

        @SerialName("versions")
        val versions: List<Int>,

        @SerialName("cores")
        val cores: List<Int>,

        val links: List<CustomLinkItemJson>
    )

    @Serializable
    data class CreateVersionResponse(
        val data: CreateVersionData? = null,
        val code: Int,
        val msg: String
    )

    @Serializable
    data class CreateVersionData(
        val id: Long,
        val uuid: String,
        val resourceId: Long,
        val label: String,
        val type: String,
        val channel: String,
        val notes: String,
        val fileUuid: String? = null,
        val fileName: String? = null,
        val fileType: String? = null,
        val orderNum: Int,
        val downloadCount: Int? = null,
        val updateDate: String? = null,
        val createDate: String? = null,
        val versions: List<Int>,
        val cores: List<Int>,
        val links: List<CustomLinkItemJson>
    )

    fun upload(
        resourceId: String,
        branchId: Long?,
        files: Map<String, Path>
    ): BaseResponseMsg<String> {
        if (branchId == null) throw IllegalStateException("branchId is null")


        val bodyBuilder = MultipartBodyBuilder()
        for ((name, path) in files) {
            bodyBuilder.addFormDataPart(name, path.name, path, "application/java-archive")
//            println("==== Parsing BodyBuilder ====")
//            println("The name is $name")
        }

        val token: BaseResponseMsg<String> = httpContext.post("$baseUrl/api/resources/${resourceId}/releases/${branchId}/file/upload",
            HttpRequest.BodyPublishers.noBody(), headers)

        val tokenAuth = token.data ?: throw IllegalArgumentException("获取不到token: ${token.data}")

//        println("=== Parsing TOKEN ===")
//        println("The TOKEN IS: $tokenAuth")

        val multipartHeaders = headers.toMutableMap()
        multipartHeaders["Content-Type"] = bodyBuilder.getContentType()

//        println("=== Uploading with Content-Type: ${multipartHeaders["Content-Type"]} ===")
//        println("=== Headers: $multipartHeaders ===")

        val uploadHeaders = mapOf(
            "Content-Type" to bodyBuilder.getContentType()
        )

//        println("=== New Header: $uploadHeaders")

//        println("===== All Response ====")
//        println("Create Token Response: $token")
//        println("The Post Url: https://storage.xyebbs.com/client/file/upload?token=${tokenAuth}")

        val post = httpContext.post<BaseResponseMsg<String>>(
            "https://storage.xyebbs.com/client/file/upload?token=${tokenAuth}",
            bodyBuilder.build(),
            uploadHeaders
        )

        if (post.code != 200) throw Exception(post.msg)

        return post
    }
}