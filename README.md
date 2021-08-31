KBootstrap is an alternative to https://github.com/FabricMC/fabric-language-kotlin that downloads up-to-date Kotlin libraries at runtime for mod developers to freely include it in their Kotlin mods so as to not bloat them. The libraries are cached under the operating system's temporary directory.

[The main module](./kbootstrap) includes the standard library; the other modules include other Kotlin libraries as suggested by their names.
kbootstrap-all (the root project) includes everything.
