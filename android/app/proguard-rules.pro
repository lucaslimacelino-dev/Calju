# O Gson lê e escreve os DTOs da API pelos nomes dos campos: o R8 não pode renomeá-los.
-keep class io.github.lucaslimacelino_dev.calju.data.remote.dto.** { *; }
