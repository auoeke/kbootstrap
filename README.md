KBootstrap is an alternative to https://github.com/FabricMC/fabric-language-kotlin that downloads up-to-date Kotlin libraries at runtime for mod developers to freely include it in their Kotlin mods so as to not bloat them. The libraries are cached under the operating system's temporary directory.

By default, KBootstrap downloads only the standard library; mods may specify any of the additional modules found in [the test mod](./testmod/build.gradle).
