# Mini autorizador

API REST em Java 21, Spring Boot 3.5 e Maven para criar cartões, consultar saldos e autorizar transações. Os cartões são persistidos no MySQL 5.7 do `docker/docker-compose.yml` fornecido.

## Executar com Docker Compose

Clone o repositório e entre na pasta do projeto:

```sh
git clone https://github.com/sergioalvarengaaliceral/miniautorizador.git
cd miniautorizador
cd docker
```

Requisito: Docker com Docker Compose. Execute:
```sh
docker compose up --build -d miniautorizador
```

O Compose inicia o MySQL e a aplicação. A imagem da aplicação é compilada com JDK 21 e só inicia depois que o MySQL estiver saudável; não é necessário instalar Java ou Maven na máquina. A API fica em `http://localhost:8080`. Todas as rotas usam HTTP Basic: `username` / `password`.

Para parar a aplicação e o banco, ainda na pasta `docker`, execute `docker compose stop miniautorizador mysql`.

## Observabilidade

A aplicação disponibiliza liveness em `/actuator/health/liveness` e readiness em `/actuator/health/readiness`. A prontidão inclui a verificação de conexão com o MySQL. Esses endpoints retornam somente o estado geral e são públicos para uso do healthcheck do container. Métricas e scrape Prometheus ficam em `/actuator/metrics` e `/actuator/prometheus`, protegidos por HTTP Basic. A métrica `miniautorizador.transacoes` conta resultados por tipo, sem usar dados do cartão como dimensão. Cada requisição recebe um UUID no cabeçalho de resposta `X-Correlation-ID`; o log da autorização inclui esse identificador e o resultado, sem registrar número do cartão, senha ou valor.

## Executar localmente para desenvolvimento

Requisitos: JDK 21 e Maven Wrapper (`mvnw.cmd` no Windows ou `./mvnw` em Linux/macOS). Inicie somente o banco na pasta `docker` com `docker compose up -d mysql`. Depois, na raiz do projeto, execute `mvnw.cmd spring-boot:run` no Windows ou `./mvnw spring-boot:run` em Linux/macOS.

O Maven Wrapper fixa a versão do Maven, mas não instala nem seleciona Java. O Maven precisa ser iniciado com JDK 21. O datasource local usa por padrão `jdbc:mysql://localhost:3306/miniautorizador`, usuário `root` e senha vazia. Defina `DB_URL`, `DB_USER` e `DB_PASSWORD` para substituir a configuração. O Hibernate cria/atualiza a tabela de cartões.

## Swagger / OpenAPI

Com a aplicação iniciada, acesse a interface interativa em http://localhost:8080/swagger-ui/index.html ou o documento OpenAPI em http://localhost:8080/v3/api-docs. A interface e o documento são públicos para consulta; as operações da API continuam exigindo HTTP Basic (username / password). Use o botão Authorize no Swagger UI para informar as credenciais ao testar os endpoints.
## Contratos

- `POST /cartoes`, JSON `{"numeroCartao":"6549873025634501","senha":"1234"}`: retorna `201` e o mesmo JSON, com saldo inicial `500.00`; duplicidade retorna `422` com o corpo enviado.
- `GET /cartoes/{numeroCartao}`: retorna `200` com o saldo numérico, ou `404` sem corpo.
- `POST /transacoes`, JSON `{"numeroCartao":"6549873025634501","senhaCartao":"1234","valor":10.00}`: sucesso retorna `201` e `OK`; recusas retornam `422` com `CARTAO_INEXISTENTE`, `SENHA_INVALIDA` ou `SALDO_INSUFICIENTE`.
- Valores de transação com mais de duas casas decimais são inválidos e retornam `400 Bad Request`, sem código de autorização.
- Falta ou erro na autenticação retorna `401`.

## Decisões

### Escolhas desta implementação

- **Banco relacional:** foi escolhido o MySQL 5.7 já fornecido. O saldo é um valor monetário que precisa ser atualizado atomicamente, e o cartão tem uma identidade única usada para localizar e bloquear a linha durante o débito. Restrições relacionais, transações e bloqueio por linha se ajustam diretamente a esse fluxo. O MongoDB também poderia modelar o cartão como documento e oferecer atualizações atômicas, mas não traz uma vantagem necessária para este caso; manter os dois bancos na aplicação aumentaria configuração e operação sem atender a um requisito adicional. A configuração do serviço MySQL foi preservada. O caminho do script de inicialização do Mongo no Compose foi corrigido para o arquivo existente, embora esse banco não seja usado pela aplicação.
- **Organização em camadas:** `controller` trata HTTP e delega os casos de uso; `controller.request` e `controller.response` isolam os contratos REST; `service.cartao` e `service.transacao` agrupam os fluxos por funcionalidade; `domain` contém o cartão e suas operações; `repository` concentra o acesso persistente; `config` reúne a configuração transversal, como segurança. A separação deixa cada classe com uma responsabilidade local e permite testar regras e serviços sem depender sempre da API. É uma arquitetura em camadas pragmática para o tamanho da prova, não uma implementação completa de arquitetura hexagonal: a entidade `Cartao` ainda usa anotações JPA. Se o domínio ou as integrações crescerem, pode fazer sentido separar modelo de domínio e modelo de persistência; fazê-lo agora acrescentaria indireção sem benefício demonstrado.
- **Português na API e no domínio:** os caminhos e campos do contrato da prova são em português (`/cartoes`, `/transacoes`, `numeroCartao`, `senhaCartao`). Usar a mesma linguagem nos tipos e nos resultados (`CARTAO_INEXISTENTE`, por exemplo) reduz traduções entre o contrato e o código e torna os nomes reconhecíveis por quem avalia a solução. Em sistemas com vários times ou APIs públicas, a linguagem dos identificadores deve seguir o padrão organizacional já estabelecido; aqui prevalece o idioma do contrato recebido.
- **Autorização sem `if`:** as regras implementam `RegraAutorizacao` e são ordenadas com `@Order`, formando uma **Chain of Responsibility**: a primeira regra que produz uma recusa encerra a avaliação; se nenhuma recusar, o serviço debita o cartão. `Optional` e `Stream` expressam a busca pelo primeiro resultado sem ramificações `if/else` no fluxo de autorização. A ausência do cartão é tratada por `Optional`. A conversão do resultado para HTTP fica fora do serviço de negócio; as classes de resposta usam uma expressão condicional para escolher o status. A restrição da prova foi aplicada ao fluxo de negócio sem transformar a ausência de `if` em regra geral de estilo.
- **Precisão monetária:** `valor` aceita até 10 dígitos inteiros e 2 casas decimais, compatíveis com a coluna `DECIMAL(12,2)` do saldo. Mais de duas casas é erro de formato e retorna `400 Bad Request`; valor nulo ou não positivo segue a regra de saldo e retorna `SALDO_INSUFICIENTE`, pois a prova não especifica um código próprio para esses casos.
- **Concorrência no débito:** a autorização abre uma transação e busca o cartão com bloqueio pessimista (`PESSIMISTIC_WRITE`), que no MySQL resulta em bloqueio da linha (`SELECT ... FOR UPDATE`). As regras e o débito ocorrem enquanto o bloqueio está ativo. Assim, duas instâncias da aplicação que compartilham o banco não conseguem aprovar simultaneamente débitos incompatíveis para o mesmo saldo. A transação não depende de um bloqueio em memória da instância. Na implementação atual, a comparação BCrypt também acontece dentro desse intervalo; em cartões muito disputados, esse custo de CPU pode prolongar a retenção do bloqueio.

### Tradeoffs e evolução para produção

- **JPA/Hibernate e desempenho:** Spring Data JPA reduz código repetitivo e mantém o fluxo transacional simples; isso é adequado ao escopo e ao volume esperado na prova. JDBC pode reduzir trabalho de mapeamento e dar controle mais direto sobre SQL, mas não remove latência de rede, contenção na mesma linha nem o custo de serializar débitos do mesmo cartão. A troca não deve ser feita por suposição: em produção, medir latência e throughput sob carga representativa e inspecionar consultas e tempos de bloqueio. Se o acesso ao saldo se tornar gargalo, uma atualização condicional atômica (`UPDATE ... SET saldo = saldo - ? WHERE numero_cartao = ? AND saldo >= ?`) pode reduzir a operação a um comando e deixar o banco arbitrar a concorrência; será necessário distinguir cartão inexistente de saldo insuficiente e preservar a validação da senha. Uma evolução possível é ler o hash, validar a senha antes da transação de débito e então executar a atualização condicional em uma transação curta; se alteração de senha for adicionada, será necessário definir como evitar que a credencial mude entre a validação e o débito. JDBC, `JdbcTemplate` ou uma consulta de atualização específica do repositório são opções para esse caminho crítico, mantendo JPA onde sua produtividade for vantajosa. O Hibernate usado registra um aviso de que seu dialeto atual não dá mais suporte oficial ao MySQL 5.7; essa versão é mantida por exigência do Compose da prova e a aplicação subiu no teste local. Em produção, usaria uma versão de banco suportada pelo Hibernate adotado ou validaria explicitamente um dialeto compatível com o banco legado.
- **Persistência e inicialização:** transações não são armazenadas, conforme permitido; cartões e saldos são persistidos. Hibernate `ddl-auto: update` facilita iniciar a prova sem passos extras de banco. Em produção, o esquema deve ser evoluído por migrações versionadas (Flyway ou Liquibase), revisadas e executadas de forma controlada, em vez de depender da alteração automática do esquema pela aplicação.
- **Imagem da aplicação:** o Dockerfile usa um estágio de build com Maven e JDK 21 e uma imagem final apenas com o runtime Java 21. Assim, a versão de Java não depende da máquina de quem avalia. O Compose aguarda o healthcheck do MySQL antes de iniciar a API; essa espera evita a corrida de inicialização, mas não substitui política de reconexão e monitoramento para falhas que ocorram depois da subida.
- **Segurança e operação:** o `application.yml` não armazena uma senha literal do banco: lê `DB_USER` e `DB_PASSWORD` do ambiente; o usuário `root` e a senha vazia são padrões apenas para a execução local com o MySQL do Compose da prova. As credenciais HTTP Basic (`username` / `password`) ficam fixas em memória porque fazem parte do contrato de demonstração; isso não deve ser levado para produção. Em produção, credenciais e segredos devem ser injetados por configuração segura da plataforma ou um gerenciador de segredos, sem valores reais no repositório; também devem ser usados TLS, rotação de credenciais, limites contra tentativas de senha e auditoria sem registrar senhas ou dados sensíveis. A senha do cartão é armazenada com BCrypt; a resposta de criação repete a senha somente para cumprir o contrato recebido. Também seria necessário rever esse contrato antes de expor a API publicamente.

## Testes

Os testes unitários exercitam os serviços e as regras diretamente. Os testes de integração usam H2 em modo MySQL e MockMvc, sem iniciar o Docker. Também há um cenário de duas transações concorrentes tentando consumir o mesmo saldo.

```sh
mvn test
```
Para validar a concorrência no MySQL 5.7 do Compose, inicie o banco e rode o teste de integração dedicado:

```sh
docker compose -f docker/docker-compose.yml up -d mysql
mvn -Dtest=TransacaoMySqlIT -Dmysql.integration=true test
docker compose -f docker/docker-compose.yml stop mysql
```
