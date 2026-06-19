package me.modmuss50.mpp.platforms.xyebbs

import me.modmuss50.mpp.*
import org.gradle.api.provider.ListProperty
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.jetbrains.annotations.ApiStatus
import java.nio.file.Path
import javax.inject.Inject

interface XyebbsOptions:
        PlatformOptions,
        LinkContainer,
        PlatformOptionsInternal<XyebbsOptions> {


    @get:Input
    val xyeSecret: Property<String>

    @get:Input
    val xyeKey: Property<String>

    @get:Input
    val projectId: Property<String>

    @get:Input
    val label: Property<String>

    @get:Input
    val minecraftVersions: ListProperty<String>

    @get:Input
    val environmentType: Property<String>

    @get:Input
    val apiEndpoint: Property<String>

    @get:Input
    val orderNumber: Property<Int>

    //todo how i handle this?
    /*@get:Input
    val customLink: ListProperty<XyebbsApi.CustomLinkItem>*/

    fun from(other: XyebbsOptions) {
        super.from(other)
        xyeKey.convention(other.xyeKey)
        xyeSecret.convention(other.xyeSecret)
        projectId.convention(other.projectId)
        label.convention(other.label)
        environmentType.convention(other.environmentType)
        minecraftVersions.convention(other.minecraftVersions)
        apiEndpoint.convention(other.apiEndpoint)
        orderNumber.convention(other.orderNumber)
//        customLink.convention(other.customLink)
        fromDependencies(other)
    }

    fun from(other: Provider<XyebbsOptions>) {
        from(other.get())
    }

    fun from(
        other: Provider<XyebbsOptions>,
        publishOptions: Provider<PublishOptions>,
    ) {
        from(other)
        from(publishOptions.get())
    }

    override fun setInternalDefaults() {
        apiEndpoint.convention("https://resource-api.xyeidc.com")
        orderNumber.convention(0)
        label.convention("1.0.0")
        xyeKey.convention("")
        xyeSecret.convention("")
        /*customLink.convention(listOf(
            XyebbsApi.CustomLinkItem(
                XyebbsApi.CustomLinkType.CF,
                "www.cursforge.com"
            )
        ))*/
    }
}

interface LinkContainer {
    @get:Input
    val downLoadLinks: ListProperty<Link>

    @get:ApiStatus.Internal
    @get:Inject
    val objectFactory: ObjectFactory

    fun curseForgeLink(url: String) {
        addInternal("CF", url, "CurseForge", 0)
    }

    fun modrinthLink(url: String) {
        addInternal("MR", url, "Modrinth", 1)
    }

    fun fromDependencies(other: LinkContainer) {
        downLoadLinks.convention(other.downLoadLinks)
    }

    @Internal
    fun addInternal(type: String, url: String, info: String, orderNum: Int) {
        downLoadLinks.add(
            objectFactory.newInstance(Link::class.java).apply {
                this.type.set(type)
                this.url.set(url)
                this.info.set(info)
                this.orderNum.set(orderNum)
            }
        )
    }

}

interface Link {
    val type: Property<String>
    @get:Input
    @get:Optional
    val url: Property<String>
    @get:Input
    @get:Optional
    val info: Property<String>
    val orderNum: Property<Int>
}

abstract class Xyebbs
@Inject
constructor(
    name: String,
): Platform(name),
    XyebbsOptions {
    //todo how i handle this?
    /*fun addLinks(action: Action<XyebbsApi.CustomLinkItem>) {
        val builder = objectFactory.newInstance(XyebbsApi.CustomLinkItem::class.java)
        action.execute(builder)

        println("==== Parsing Link Item ====")
        println(builder)

        customLink.add(builder)
    }*/

    override fun validateInputs() {
        super.validateInputs()
        check(xyeKey.isPresent && xyeKey.get().isNotBlank()) { "Xyebbs: apiKey must be configured in local.properties" }
        check(xyeSecret.isPresent && xyeSecret.get().isNotBlank()) { "Xyebbs: apiSecret must be configured in local.properties" }
        check(projectId.isPresent) { "Xyebbs: projectId must be set" }
        check(environmentType.isPresent) { "Xyebbs: environmentType missing (SERVER/CLIENT)" }
        check(minecraftVersions.get().isNotEmpty()) { "Xyebbs: minecraftVersionIds cannot be empty" }
    }

    override fun dryRunPublishResult(): PublishResult {
        return XyebbsPublishResult(
            projectId = projectId.get(),
            data = "null",
            code = 0,
            message = "test",
            title = displayName.get(),
        )
    }

    override fun printDryRunInfo(logger: org.gradle.api.logging.Logger) {
        logger.lifecycle("  Xyebbs Project ID: ${projectId.get()}")
        logger.lifecycle("  Env Type: ${environmentType.get()}")
        logger.lifecycle("  MineCraft Version Ids: ${minecraftVersions.get()}")
    }

    override fun publish(context: PublishContext) {
        context.submit(UploadWorkAction::class) {
            it.from(this)
        }
    }

    interface UploadParams :
        PublishWorkParameters,
        XyebbsOptions

    abstract class UploadWorkAction : PublishWorkAction<UploadParams> {
        override fun publish(): PublishResult {
            with(parameters) {
                val key = xyeKey.orNull
                val secret = xyeSecret.orNull
                checkNotNull(key) { "Xyebbs apiKey is empty" }
                checkNotNull(secret) { "Xyebbs apiSecret is empty" }

                val api = XyebbsApi(key, secret, apiEndpoint.get())

                val primaryFileKey = "file"
                val files = HashMap<String, Path>()
                files[primaryFileKey] = file.path

                println("==== Parsing File")
                println("File path: ${file.path}")
                println("File name: ${file.path.fileName}")

                val storedLinks = downLoadLinks.get()

                val jsonLinks = storedLinks.map {
                    XyebbsApi.CustomLinkItemJson(
                        linkType = it.type.get(),
                        url = it.url.get(),
                        info = it.info.get(),
                        orderNum = it.orderNum.get()
                    )
                }

                val metadata =
                    XyebbsApi.CreateVersion(
                        label = label.get(),
                        type = environmentType.get().uppercase(),
                        channel = XyebbsApi.VersionType.valueOf(type.get()).toString().uppercase(),
                        orderNum = orderNumber.get(),
                        notes = changelog.orNull,
                        versions = XyebbsGameVersion.versionsToIds(minecraftVersions.get()),
                        cores = XyebbsLoader.loadersToId(modLoaders.get()),
                        links = jsonLinks
                    )

                println("=== Creating Version ===")
                println("label: ${label.get()}")
                println("type: ${environmentType.get()}")
                println("channel: ${XyebbsApi.VersionType.valueOf(type.get())}")
                println("orderNum: ${orderNumber.get()}")
                println("versions: ${minecraftVersions.get()}")
                println("versions (mapped): ${XyebbsGameVersion.versionsToIds(minecraftVersions.get())}")
                println("cores: ${modLoaders.get()}")
                println("cores (mapped): ${XyebbsLoader.loadersToId(modLoaders.get())}")
                println("The url is: ${apiEndpoint.get()}/api/resources/${projectId.get()}/releases")

                val response =
                    Retry.run(maxRetries.get(), "Failed to create version") {
                        api.createVersion(metadata, projectId.get())
                    }

                val uploadResponse =
                    Retry.run(maxRetries.get().plus(7), "Failed to upload files") {
                        api.upload(projectId.get(), response.data?.id, files)
                    }

                println("The response is: $uploadResponse")

                return XyebbsPublishResult(
                    projectId = projectId.get(),
                    data = uploadResponse.data ?: "null",
                    code = uploadResponse.code,
                    message = uploadResponse.msg,
                    title = displayName.get(),
                )
            }
        }
    }
    }