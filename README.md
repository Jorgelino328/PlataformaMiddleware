# PlataformaMiddleware

Uma plataforma de middleware distribuído, simples e leve, em Java.

Este projeto fornece uma infraestrutura de middleware básica que utiliza um mecanismo de roteamento baseado em anotações, similar a frameworks web modernos, para lidar com requisições HTTP. Ele foi projetado para ser a base na construção de aplicações distribuídas.

## Como Executar

### Pré-requisitos

*   Java 17 ou superior
*   Apache Maven 3.6+

### Construção e Execução

1.  **Construir o middleware e gerar o JAR:**
    ```bash
    cd plataforma-middleware
    mvn clean package -DskipTests
    ```

2.  **Instalar o JAR do middleware no repositório Maven local:**
    ```bash
    cd ../aplicacao
    mvn install:install-file -Dfile=../plataforma-middleware/target/plataforma-middleware-1.0-SNAPSHOT.jar -DgroupId=imd.ufrn.br -DartifactId=plataforma-middleware -Dversion=1.0-SNAPSHOT -Dpackaging=jar
    ```

3.  **Construir a aplicação (que importa o middleware como JAR):**
    ```bash
    mvn clean package -DskipTests
    ```

4.  **Executar o sistema:**
    ```bash
    java -jar target/aplicacao-1.0-SNAPSHOT.jar
    ```

### Configuração opcional via linha de comando:
```bash
java -jar target/aplicacao-1.0-SNAPSHOT.jar --server.http.port=8090
```

O sistema inicia com o HTTP Gateway na porta configurada (padrão: 8080).

