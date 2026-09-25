CREATE TABLE IF NOT EXISTS eventos (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    titulo VARCHAR(255) NOT NULL,
                    descricao VARCHAR(2000),
                    inicio TIMESTAMP NOT NULL,
                    fim TIMESTAMP NOT NULL,
                    modalidade VARCHAR(20) NOT NULL,
                    status VARCHAR(20) NOT NULL
                );

CREATE TABLE IF NOT EXISTS usuarios (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    nome VARCHAR(255) NOT NULL,
                    email VARCHAR(255) NOT NULL UNIQUE,
                    senha_hash VARCHAR(255) NOT NULL,
                    papel VARCHAR(20) NOT NULL
                );

CREATE TABLE IF NOT EXISTS atividades (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    titulo VARCHAR(255) NOT NULL,
                    descricao VARCHAR(2000),
                    tipo VARCHAR(100) NOT NULL,
                    trilha VARCHAR(100),
                    local VARCHAR(255) NOT NULL,
                    inicio TIMESTAMP NOT NULL,
                    fim TIMESTAMP NOT NULL,
                    capacidade INTEGER,
                    evento_id BIGINT NOT NULL REFERENCES eventos(id) ON DELETE CASCADE
                );

CREATE TABLE IF NOT EXISTS atividade_pessoas (
                    atividade_id BIGINT NOT NULL REFERENCES atividades(id) ON DELETE CASCADE,
                    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
                    papel VARCHAR(50) NOT NULL,
                    PRIMARY KEY (atividade_id, usuario_id, papel)
                );

CREATE TABLE IF NOT EXISTS inscricoes (
                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                    usuario_id BIGINT NOT NULL REFERENCES usuarios(id) ON DELETE CASCADE,
                    evento_id BIGINT NOT NULL REFERENCES eventos(id) ON DELETE CASCADE,
                    status VARCHAR(20) NOT NULL,
                    data_inscricao TIMESTAMP NOT NULL
                );

CREATE TABLE IF NOT EXISTS inscricao_atividades (
                    inscricao_id BIGINT NOT NULL REFERENCES inscricoes(id) ON DELETE CASCADE,
                    atividade_id BIGINT NOT NULL REFERENCES atividades(id) ON DELETE CASCADE,
                    PRIMARY KEY (inscricao_id, atividade_id)
                );
