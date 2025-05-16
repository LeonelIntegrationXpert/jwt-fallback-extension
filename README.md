# 🔐 Multi IdP JWT Validation Extension - MuleSoft SDK

<p align="center">
  <img src="https://capsule-render.vercel.app/api?type=waving&color=0:147AD6,100:47e3ff&height=220&section=header&text=JWT%20Validation%20Extension&fontSize=40&fontColor=ffffff&animation=fadeIn" alt="JWT Extension" />
</p>

<p align="center">
  <a href="https://docs.mulesoft.com/mule-sdk/1.1/"><img src="https://img.shields.io/badge/Mule%20SDK-1.1.3-blue?logo=mulesoft" /></a>
  <a href="https://adoptium.net/"> <img src="https://img.shields.io/badge/Java-8-blue?logo=java" /></a>
  <a href="https://maven.apache.org/"> <img src="https://img.shields.io/badge/Maven-3.x-C71A36?logo=apache-maven" /></a>
  <a href="https://www.linkedin.com/in/leonel-dorneles-porto-b88600122"> <img src="https://img.shields.io/badge/Author-Leonel%20Porto-success?logo=linkedin" /></a>
</p>

---

## 📖 Visão Geral

Este projeto é uma extensão **Mule 4** desenvolvida com o **Mule SDK** que permite validar tokens **JWT** provenientes de múltiplos **Identity Providers (IdPs)**, com suporte a:

* 🔁 **Fallback** automático entre URLs JWKS.
* 🔐 Validação de assinatura usando o campo `x5c`.
* 🕒 Validação de claims padrão como `exp`, `nbf`, `jti`.
* 🔍 Validação de claims personalizados (header ou payload).

Ideal para ambientes que usam múltiplos domínios como **Azure AD**, **OAM** ou **IDCS**.

---

## 🧱 Estrutura do Projeto e POM

### 🎯 Group ID Obrigatório

Certifique-se de alterar o `groupId` no `pom.xml` para o seu **Organization ID** do **Anypoint Platform**:

```xml
<groupId>806818a4-2582-4d63-a6ef-021f493715a0</groupId>
```

### 🔧 Estrutura do `pom.xml`

* `mule-modules-parent`: Define o comportamento padrão de build para extensões Mule.
* `jackson-databind`: Usado para parsear os headers e payloads JWT.
* `fluent-hc`: Realiza requisições HTTP para JWKS endpoints.
* `distributionManagement`: Aponta para o Exchange da sua organização (obrigatório para `mvn deploy`).

---

## ⚙️ Configuração do Maven (`settings.xml`)

Para que o comando `mvn deploy` envie o artefato para o **Exchange**, inclua o arquivo `settings.xml` (recomendado: salve em `.maven/settings.xml`) com as credenciais da sua **Connected App**:

```xml
<server>
  <id>anypoint-exchange-v3</id>
  <username>~~~Client~~~</username>
  <password>clientId~?~clientSecret</password>
</server>
```

> 📁 O projeto já inclui um exemplo funcional de `settings.xml` na pasta `.maven`.

---

## 🛠️ Como Construir e Publicar

### 1. Gerar o Projeto

```bash
mvn org.mule.extensions:mule-extensions-archetype-maven-plugin:generate
```

### 2. Compilar Localmente

```bash
mvn clean install -DskipTests
```

### 3. Fazer o Deploy no Exchange

Se estiver usando um `settings.xml` customizado dentro da pasta `.maven`, use o comando:

```bash
mvn deploy -s .maven/settings.xml
```

🔁 **Ou**, se o seu arquivo `settings.xml` já estiver no diretório padrão (`.m2`), use:

```bash
mvn deploy -DskipTests
```

#### 📝 Resultado Esperado (resumo)

Ao rodar o comando `mvn deploy`, você verá uma sequência de logs que indicam o progresso da publicação. O comportamento esperado inclui:

* 📆 A extensão é empacotada como `.jar`.
* 🏠 Artefatos são instalados no repositório local `.m2`.
* 🚀 Os arquivos `.pom`, `.json`, `.jar` e `maven-metadata.xml` são enviados com sucesso ao **Anypoint Exchange**.
* ✅ A publicação é finalizada com a mensagem:

```plaintext
[INFO] BUILD SUCCESS
[INFO] Total time:  40.888 s
[INFO] Finished at: 2025-05-15T17:26:56-03:00
```

---

## 🚀 Exemplo de Uso no Mule XML

```xml
<jwt-fallback:validate-with-fallback
  doc:name="Validate with fallback"
  token="#[(attributes.headers.authorization)]"
  urlsCsv="https://domain1.com/jwks,https://domain2.com/jwks"
  requireJWTId="true"
  requireExpiration="true"
  requireNotBefore="false"
  target="isValid">

  <jwt-fallback:custom-claims-to-validates>
    <jwt-fallback:custom-claims-to-validate key="alg" value="RS256"/>
    <jwt-fallback:custom-claims-to-validate key="iss" value="https://issuer.com"/>
    <jwt-fallback:custom-claims-to-validate key="sub" value="user123"/>
    <!-- Adicione outros claims conforme necessário -->
  </jwt-fallback:custom-claims-to-validates>
</jwt-fallback:validate-with-fallback>
```

---

## 🔍 Explicação do Código

A classe principal `MultiIdPJWTValidationOperations` executa:

* 🔁 Laço entre múltiplas URLs JWKS.
* 📥 Busca do primeiro `x5c` do JWKS.
* ✅ Verificação da assinatura (RS256, RS384, RS512).
* 🧪 Validações condicionais de `exp`, `nbf`, `jti`.
* 🔎 Verificação de claims customizados (payload ou header).
* ❌ Caso a assinatura falhe ou o claim não bata, tenta a próxima URL.

---

## 📚 Documentação Oficial

🔗 [Criando Conectores com Mule SDK (MuleSoft)](https://blogs.mulesoft.com/dev-guides/api-connectors-templates/custom-connector-mule-sdk/)

---

## 🗪 Licença

Este projeto é de código aberto e foi desenvolvido por **Leonel Dorneles Porto**. Seu uso é permitido em ambientes MuleSoft, com foco em autenticação JWT e segurança de APIs.

---

<p align="center">
  <img src="https://readme-typing-svg.herokuapp.com?font=Fira+Code&size=22&pause=1000&color=47E3FF&center=true&vCenter=true&width=800&lines=Obrigado+por+visitar+🚀+;Conecte-se+no+LinkedIn+com+Leonel+Dorneles+Porto"/>
</p>
