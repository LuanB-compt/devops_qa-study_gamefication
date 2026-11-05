pipeline {
    agent any

    environment {
        DOCKER_IMAGE = 'gamification-app'
        IMAGE_TAG = "${BUILD_NUMBER}"
        COMPOSE_FILE = 'docker-compose.homol.yml'
        // Jenkins roda em container e usa o Docker do host: os servicos do HOMOL
        // sao acessados pela rede do Compose (nomes de servico), nao por localhost.
        HOMOL_NETWORK = 'gamification-homol_net'
        APP_URL = 'http://app:8080'
    }

    options {
        timestamps()
        disableConcurrentBuilds()
    }

    stages {
        stage('1 - Checkout') {
            steps {
                checkout scm
            }
        }

        // Fitness function do ASR-1 (Manutenibilidade): ciclo entre módulos ou acesso a
        // Entity/Repository de outro módulo reprova aqui, antes dos testes mais lentos.
        // Os mesmos testes rodam de novo no `verify` abaixo (custo de poucos segundos);
        // o estágio existe para a falha de arquitetura aparecer isolada no Stage View.
        stage('2a - CI - Regras de Arquitetura (ArchUnit)') {
            steps {
                sh 'mvn -B test -Dtest=ArquiteturaTest'
            }
        }

        // Fitness function do ASR-6 (Consistência): conclusão + recompensa + evento na mesma
        // transação, e consumidor indisponível sem efeito sobre a conclusão. Isolado para que,
        // se alguém quebrar a outbox (ex.: gravar o evento fora da transação), a falha apareça
        // com nome próprio no Stage View. Os mesmos testes rodam de novo no `verify` abaixo.
        stage('2b - CI - Consistência Transacional (Outbox, ASR-6)') {
            steps {
                sh 'mvn -B test -Dtest=AtomicidadeDaConclusaoIntegrationTest,OutboxDispatcherTest,EventoDeDominioTest'
            }
        }

        stage('2 - CI - Testes JUnit + JaCoCo') {
            steps {
                sh 'mvn -B clean verify'
            }
            post {
                always {
                    junit testResults: 'target/surefire-reports/*.xml', allowEmptyResults: false
                    archiveArtifacts artifacts: 'target/site/jacoco/**', allowEmptyArchive: true
                }
            }
        }

        stage('3 - CI - Qualidade PMD') {
            steps {
                sh 'mvn -B pmd:pmd'
            }
            post {
                always {
                    archiveArtifacts artifacts: 'target/pmd.xml', allowEmptyArchive: true
                }
            }
        }

        stage('4 - CI - Package') {
            steps {
                sh 'mvn -B package -DskipTests'
            }
        }

        stage('5 - CD - Build dos Containers') {
            steps {
                sh 'docker compose -f $COMPOSE_FILE build'
            }
        }

        stage('6 - CD - Deploy HOMOL') {
            steps {
                sh '''
                    # A rede do Compose nao pode ser removida com o Jenkins conectado
                    docker network disconnect $HOMOL_NETWORK $(hostname) 2>/dev/null || true
                    docker compose -f $COMPOSE_FILE down --remove-orphans
                    docker compose -f $COMPOSE_FILE up -d
                    docker network connect $HOMOL_NETWORK $(hostname)
                '''
            }
        }

        stage('7 - CD - Health Check HOMOL') {
            steps {
                sh '''
                    for i in $(seq 1 12); do
                        if curl -fsS $APP_URL/actuator/health | grep -q '"status":"UP"'; then
                            echo "API saudavel (tentativa $i/12)."
                            exit 0
                        fi
                        echo "API ainda nao pronta. Tentativa $i/12..."
                        sleep 5
                    done
                    echo "Health check da API falhou."
                    docker compose -f $COMPOSE_FILE logs --tail=100 app
                    exit 1
                '''
            }
        }

        stage('8 - CD - Smoke Test API') {
            steps {
                sh 'bash scripts/integration_tests-endpoints.sh $APP_URL'
            }
        }

        stage('9 - Observabilidade') {
            steps {
                echo 'API:        http://localhost:8081  (actuator: /actuator/health, /actuator/prometheus)'
                echo 'Frontend:   http://localhost:5174'
                echo 'Prometheus: http://localhost:9091'
                echo 'Grafana:    http://localhost:3001  (admin / admin)'
                echo 'pgAdmin:    http://localhost:5051'
            }
        }
    }

    post {
        success {
            echo 'Pipeline CI/CD concluido: build, testes, qualidade, container, deploy HOMOL e smoke aprovados.'
        }
        failure {
            echo 'Pipeline interrompido. Identifique no Stage View o ponto de falha e consulte o Console Output.'
        }
        always {
            archiveArtifacts artifacts: 'target/site/jacoco/**,target/pmd.xml', allowEmptyArchive: true
        }
    }
}
