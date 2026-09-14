# Configuração do SpigotPlus

O SpigotPlus usa `spigotplus.properties` como configuração do **bootstrap**. Ele não substitui `server.properties` do Minecraft nem os arquivos de configuração dos plugins.

O arquivo é criado automaticamente na primeira inicialização a partir do template oficial incluído no JAR.

## Estrutura

```properties
# Servidor
server.spigot-version=26.2
server.java-version=26

# Memória JVM
memory.min=2048M
memory.max=4096M

# Compatibilidade
compatibility.geyser-version=2.11.2
compatibility.viaversion-version=5.11.0
compatibility.viabackwards-version=5.11.0
compatibility.geyser-port=19132

# Atualizações
updates.auto-update=true
updates.retries=3
updates.retry-delay-ms=1500

# Backup
backup.enabled=true
backup.directory=plugins/.backup

# Diagnóstico
diagnostics.enabled=true

# Integridade
sha256.Geyser-Spigot.jar=
sha256.ViaVersion.jar=
sha256.ViaBackwards.jar=
```

## Servidor

| Chave | Padrão | Função |
|---|---:|---|
| `server.spigot-version` | `26.2` | Versão esperada do Spigot |
| `server.java-version` | `26` | Versão mínima/exigida do Java para o launcher |

## Memória

`memory.min` e `memory.max` são passados diretamente para a JVM como `-Xms` e `-Xmx`.

Exemplo:

```properties
memory.min=4096M
memory.max=8192M
```

## Compatibilidade

As versões de Geyser, ViaVersion e ViaBackwards ficam centralizadas em um único lugar:

```properties
compatibility.geyser-version=2.11.2
compatibility.viaversion-version=5.11.0
compatibility.viabackwards-version=5.11.0
```

Isso evita versões espalhadas pelo código do launcher.

## Atualizações automáticas

```properties
updates.auto-update=true
updates.retries=3
updates.retry-delay-ms=1500
```

Com `updates.auto-update=false`, o SpigotPlus não tenta baixar ou substituir os componentes de compatibilidade durante o startup.

`updates.retries` define quantas tentativas são feitas e `updates.retry-delay-ms` controla o intervalo base entre tentativas.

## Backup e rollback

```properties
backup.enabled=true
backup.directory=plugins/.backup
```

Antes de substituir um componente existente, o SpigotPlus pode guardar a versão anterior no diretório configurado. Se o download/validação falhar, a versão funcional existente é preservada.

## Integridade SHA-256

Para instalações controladas, é possível exigir o SHA-256 de cada componente:

```properties
sha256.Geyser-Spigot.jar=<hash>
sha256.ViaVersion.jar=<hash>
sha256.ViaBackwards.jar=<hash>
```

Quando o valor estiver preenchido, o JAR só será aceito se o hash calculado for igual ao configurado.

## Diagnóstico

```properties
diagnostics.enabled=true
```

O modo `status` mostra o estado básico do runtime:

```bash
java -jar SpigotPlus.jar status
```

Ele informa Spigot, Java, memória, atualização automática, backup, diagnóstico e os principais plugins instalados.

## Configurações legadas

Para evitar quebra de instalações antigas, estas chaves continuam sendo reconhecidas como fallback:

```properties
spigot.version=26.2
java.version=26
geyser.version=2.11.2
viaversion.version=5.11.0
viabackwards.version=5.11.0
```

As chaves novas com prefixos `server.*` e `compatibility.*` têm prioridade quando configuradas.
